package com.ssau.tpzrp.utils;

import com.squareup.okhttp.Call;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.ssau.tpzrp.exceptions.PeersParseException;
import com.ssau.tpzrp.model.*;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.ArrayUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class TorrentHelper {

    private static String buildRequest(String trackerUrl, String infoHash) {
        StringBuilder request = new StringBuilder();
        request.append(trackerUrl);
        addParameter(request, "info_hash", getInfoHashForRequest(infoHash, "%"));
        return request.toString();
    }

    private static void addParameter(StringBuilder request, String parameter, String value) {
        if (request.indexOf("?") == -1) {
            request.append('?').append(parameter).append("=").append(value);
        } else {
            request.append("&").append(parameter).append("=").append(value);
        }
    }
    private static String getInfoHashForRequest(String infoHash, String delimiter) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < infoHash.length(); ++i) {
            for (int j = 0; j < 2; ++j, ++i) {
                if (i == infoHash.length()) {
                    break;
                }
                if (j == 0) {
                    builder.append(delimiter);
                }
                builder.append(infoHash.charAt(i));
            }
            --i;
        }
        return builder.toString();
    }

    public static List<Peer> getPeers(TorrentFile torrentFile) throws IOException, InterruptedException, DecoderException {

        List<String> onlineTrackers = getOnlineTrackers(torrentFile);
        System.out.println("Trackers online: " + onlineTrackers);

        List<String> availableTrackerUrls = getAvailableTrackerUrls(onlineTrackers, torrentFile.getInfoHash());
        System.out.println("Available trackers: " + availableTrackerUrls);

        return getBestTrackerPeers(availableTrackerUrls, torrentFile.getInfoHash());
    }

    public static void download(TorrentFile torrentFile, List<Peer> peers) throws InterruptedException {

        List<Thread> threads = new ArrayList<>();

        for (Peer peer : peers) {
            Runnable runnable = () -> {
                Socket socket;
                try {
                    //System.out.println("[TRACE] Trying to connect to peer " + peer);
                    socket = new Socket(peer.getHost(), Integer.parseInt(peer.getPort()));

                    OutputStream outputStream = socket.getOutputStream();
                    InputStream inputStream = socket.getInputStream();

                    System.out.println("[INFO] Connected to peer " + peer);

                    PeerHandshake handshake = PeerHandshake.get(torrentFile.getInfoHash());

                    outputStream.write(handshake.getBytes());

                    byte[] peerHandshake = inputStream.readAllBytes();
                    if (peerHandshake.length == 0) {
                        socket.close();
                        throw new IOException("Peer don't want to provide a file piece");
                    }

                    System.out.println("Peer " + peer + " sent " + peerHandshake.length + " bytes");

                    PeerHandshake peerHandshakeObj = PeerHandshake.get(peerHandshake);

                    if (!peerHandshakeObj.getHexInfoHash().equals(torrentFile.getInfoHash())) {
                        socket.close();
                        throw new IOException("Peer hasn't required file");
                    }

                    byte[] requestPiece = {0,0,0,13, 6, 0,0,0,1, 0,0,0,1, 0,0,64,0};
                    byte[] interested = {0,0,0,1,2};

                    outputStream.write(interested);

                    while (true) {
                        byte[] result = inputStream.readAllBytes();
                        Thread.sleep(1000);

                        if (result.length > 0){
                            break;
                        }
                    }


                    //int msgLen = 1 + torrentFile.getPiecesCount();


                    byte[] bitFieldMessage = {3,0,5,7,5};
                    byte[] zeros = new byte[torrentFile.getPiecesCount()];
                    Arrays.fill( zeros, (byte) 0 );

                    bitFieldMessage = ArrayUtils.addAll(bitFieldMessage, zeros);
                    outputStream.write(bitFieldMessage);

                    byte[] peerResponse2 = inputStream.readAllBytes();
                    System.out.println("Peer " + peer + " sent " + peerResponse2.length + " bytes after interested");
                    socket.close();
                } catch (IOException | InterruptedException ignored) {
                    //System.out.println("[WARN] Peer " + peer + " doesn't accept TCP connections");
                } catch (DecoderException ignored) {
                    //System.out.println("[ERROR] Bad handshake message: " + e.getMessage());
                }
            };
            threads.add(new Thread(runnable));
        }

        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
    }

    private byte[] toBytes(int i)
    {
        byte[] result = new byte[4];

        result[0] = (byte) (i >> 24);
        result[1] = (byte) (i >> 16);
        result[2] = (byte) (i >> 8);
        result[3] = (byte) (i /*>> 0*/);

        return result;
    }

    private static List<String> getOnlineTrackers(TorrentFile torrentFile) throws IOException, InterruptedException {

        List<String> availableTrackers = new ArrayList<>();

        List<String> announcesUrls = torrentFile.getAnnouncesUrls();

        if (Objects.isNull(announcesUrls) || announcesUrls.isEmpty()) {
            String announceUrl = torrentFile.getAnnounceUrl();
            if (Objects.isNull(announceUrl)) {
                throw new RuntimeException("IMPLEMENT ME");
            }
            availableTrackers.add(announceUrl);
            return availableTrackers;
        }

        List<Thread> threads = new ArrayList<>();
        for (String announceUrl : announcesUrls) {
            String host = announceUrl.split("/")[2];
            host = host.split(":")[0];

            String finalHost = host;

            Runnable runnable = () -> {
                try {
                    if (pingHost(finalHost, TimeUnit.SECONDS.toMillis(3))) {
                        availableTrackers.add(announceUrl);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            };
            threads.add(new Thread(runnable));
        }

        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }

        return availableTrackers;
    }

    private static List<String> getAvailableTrackerUrls(List<String> onlineTrackerUrls, String infoHash) throws IOException, InterruptedException {
        List<String> availableTrackerUrls = new ArrayList<>();

        List<Thread> threads = new ArrayList<>();
        for (String currentTrackerUrl : onlineTrackerUrls) {
            String url = buildRequest(currentTrackerUrl, infoHash);

            Request request = new Request.Builder()
                    .url(url)
                    .build();

            OkHttpClient client = new OkHttpClient();
            Call call = client.newCall(request);

             Runnable runnable = () -> {
                try {
                    call.execute();
                    availableTrackerUrls.add(currentTrackerUrl);
                } catch (IOException ignored) {

                }
             };
            threads.add(new Thread(runnable));
        }

        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }

        return availableTrackerUrls;
    }

    public static boolean pingHost(String host, long timeout) throws IOException {

        InetAddress address;
        try {
            address = InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            return false;
        }

        return address.isReachable((int)timeout);
    }

    private static List<Peer> getBestTrackerPeers(List<String> availableTrackerUrls, String infoHash) throws IOException {

        int maxPeersCount = 0;
        String bestTracker = "";
        Map<TrackerResponse, Integer> trackerResponses = new HashMap<>();
        for (String currentTrackerUrl : availableTrackerUrls) {
            String url = buildRequest(currentTrackerUrl, infoHash);

            Request request = new Request.Builder()
                    .url(url)
                    .build();

            OkHttpClient client = new OkHttpClient();
            Call call = client.newCall(request);

            Response response;
            try {
                response = call.execute();
            } catch (SocketTimeoutException | ConnectException e) {
                System.out.println("[INFO] Tracker " + currentTrackerUrl + " not responding");
                continue;
            }

            byte[] bytes = response.body().bytes();

            TrackerResponse trackerResponse = null;
            try {
                trackerResponse = TrackerResponse.valueOf(bytes);
            } catch (PeersParseException e) {
                System.out.println("[WARN] Tracker " + currentTrackerUrl + " sent incorrect response");
                continue;
            }
            int peersCount = trackerResponse.getPeers().size();
            System.out.println("[DEBUG] Tracker \"" + currentTrackerUrl + "\" sent " + peersCount + " peers");
            trackerResponses.put(trackerResponse, peersCount);

            if (maxPeersCount < peersCount) {
                bestTracker = currentTrackerUrl;
                maxPeersCount = peersCount;
            }
        }

        System.out.println("[INFO] Tracker \"" + bestTracker + "\" contains most of peers: " + maxPeersCount);

        for (Map.Entry<TrackerResponse, Integer> trackerResponse : trackerResponses.entrySet()) {
            if (trackerResponse.getValue() == maxPeersCount) {
                return trackerResponse.getKey().getPeers();
            }
        }
        return null;
    }


}
