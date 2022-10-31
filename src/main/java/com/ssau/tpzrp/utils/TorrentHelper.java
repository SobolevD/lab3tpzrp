package com.ssau.tpzrp.utils;

import com.squareup.okhttp.Call;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.ssau.tpzrp.model.Peer;
import com.ssau.tpzrp.model.TorrentFile;
import com.ssau.tpzrp.model.TrackerResponse;
import org.apache.commons.codec.binary.Hex;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class TorrentHelper {

    private static String buildRequest(String trackerUrl, String infoHash) {
        StringBuilder request = new StringBuilder();
        request.append(trackerUrl);
        addParameter(request, "info_hash", getInfoHashForRequest(infoHash));
        return request.toString();
    }

    private static void addParameter(StringBuilder request, String parameter, String value) {
        if (request.indexOf("?") == -1) {
            request.append('?').append(parameter).append("=").append(value);
        } else {
            request.append("&").append(parameter).append("=").append(value);
        }
    }
    private static String getInfoHashForRequest(String infoHash) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < infoHash.length(); ++i) {
            for (int j = 0; j < 2; ++j, ++i) {
                if (i == infoHash.length()) {
                    break;
                }
                if (j == 0) {
                    builder.append('%');
                }
                builder.append(infoHash.charAt(i));
            }
            --i;
        }
        return builder.toString();
    }

    public static String getPeers(TorrentFile torrentFile) throws IOException, InterruptedException {

        List<String> onlineTrackers = getOnlineTrackers(torrentFile);
        List<String> availableTrackerUrls = getAvailableTrackerUrls(onlineTrackers, torrentFile.getInfoHash());
        List<Peer> peers = getBestTracker(availableTrackerUrls, torrentFile.getInfoHash());

        for (Peer peer : peers) {
            Socket socket = null;
            try {
                socket = new Socket(peer.getHost(), Integer.parseInt(peer.getPort()));
                // установить соединение после получения выходного потока
                System.out.println("[INFO] Connected to peer " + peer);

                byte[] bytes = "19".getBytes();

                String message = "\\x13BitTorrent protocol\\x00\\x00\\x00\\x00\\x00\\x00\\x00\\x00" + getInfoHashForRequest(torrentFile.getInfoHash()) + "-TR2940-k8hj0wgej6ch";
                socket.getOutputStream().write(message.getBytes("UTF-8"));
                String result = Hex.encodeHexString(socket.getInputStream().readAllBytes());
                int a = 0;
            } catch (IOException e) {
                System.out.println("[WARN] Peer " + peer + " not accepting TCP connections");
            }
        }
//        String url = buildRequest(availableTracker, torrentFile.getInfoHash());
//
//        Request request = new Request.Builder()
//                .url(url)
//                .build();
//
//        OkHttpClient client = new OkHttpClient();
//        Call call = client.newCall(request);
//        Response response = call.execute();
//        InputStream stream = new ByteArrayInputStream(response.body().string().getBytes());
//        Object obj = Bencode.parse(stream);
//
//        return response.body().string();
        return null;
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
                    else {
                        System.out.println("[DEBUG] " + String.format("Tracker '%s' is not reachable", announceUrl));
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
                } catch (SocketTimeoutException e) {
                    String errorMessage = String.format("Online tracker '%s' not responding", currentTrackerUrl);
                    System.out.println("[INFO] " + errorMessage);
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

    private static List<Peer> getBestTracker(List<String> availableTrackerUrls, String infoHash) throws IOException {

        for (String currentTrackerUrl : availableTrackerUrls) {
            String url = buildRequest(currentTrackerUrl, infoHash);

            Request request = new Request.Builder()
                    .url(url)
                    .build();

            OkHttpClient client = new OkHttpClient();
            Call call = client.newCall(request);
            Response response = call.execute();

            byte[] bytes = response.body().bytes();

            TrackerResponse trackerResponse = TrackerResponse.valueOf(bytes);
            return trackerResponse.getPeers();
        }
        return null;
    }
}
