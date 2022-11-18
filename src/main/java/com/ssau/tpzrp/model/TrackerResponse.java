package com.ssau.tpzrp.model;

import com.ssau.tpzrp.constants.TrackerResponseFields;
import com.ssau.tpzrp.exceptions.PeersParseException;
import com.ssau.tpzrp.utils.Bencode;
import com.ssau.tpzrp.utils.CommandRunner;
import lombok.Data;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static com.ssau.tpzrp.constants.Common.*;

@Data
public class TrackerResponse {

    private Long complete;
    private Long downloaded;
    private Long incomplete;
    private Long interval;
    private Long minInterval;
    private List<Peer> peers;

    public static TrackerResponse valueOf(byte[] trackerResponse) throws IOException, PeersParseException {

        TrackerResponse response = new TrackerResponse();

        InputStream stream = new ByteArrayInputStream(trackerResponse);
        Object trackerResponseObj = Bencode.parse(stream);
        Map<String, Object> trackerResponseMap = (Map<String, Object>) trackerResponseObj;
        parseParameters(response, trackerResponseMap);
        parsePeers(response, trackerResponse);
        return response;
    }

    @SuppressWarnings("unchecked")
    private static void parseParameters(TrackerResponse trackerResponse, Map<String, Object> map) throws IOException {

        Object complete = map.get(TrackerResponseFields.COMPLETE);

        Long thisComplete = Objects.isNull(complete) ? null : Long.parseLong(complete.toString());

        Object downloaded = map.get(TrackerResponseFields.DOWNLOADED);
        Long thisDownloaded = Objects.isNull(downloaded) ? null : Long.parseLong(downloaded.toString());

        Object incomplete = map.get(TrackerResponseFields.INCOMPLETE);
        Long thisIncomplete = Objects.isNull(incomplete) ? null : Long.parseLong(incomplete.toString());

        Object interval = map.get(TrackerResponseFields.INTERVAL);
        Long thisInterval = Objects.isNull(interval) ? null : Long.parseLong(interval.toString());

        Object minInterval = map.get(TrackerResponseFields.MIN_INTERVAL);
        Long thisMinInterval = Objects.isNull(minInterval) ? null : Long.parseLong(minInterval.toString());

        trackerResponse.setIncomplete(thisIncomplete);
        trackerResponse.setComplete(thisComplete);
        trackerResponse.setDownloaded(thisDownloaded);
        trackerResponse.setInterval(thisInterval);
        trackerResponse.setMinInterval(thisMinInterval);
        System.out.println(trackerResponse.getPeers());
    }

    private static void parsePeers(TrackerResponse trackerResponse, byte[] responseBytes) throws IOException, PeersParseException {

        Files.write(Path.of(TMP_FILE_WITH_TRACKER_RESPONSE), responseBytes);

        String command = String.format("python %s %s", GET_PEERS_PYTHON_SCRIPT_FILE_PATH, TMP_FILE_WITH_TRACKER_RESPONSE);
        List<String> output = CommandRunner.runWithReturn(command);

        for (String outputStr : output) {
            if (outputStr.contains("Traceback")) {
                throw new PeersParseException("Something wrong during python script execution");
            }
        }

        String[] peers = output.get(0).split(";");

        System.out.println("[DEBUG] Peers: " + Arrays.toString(peers));
        List<Peer> peersAddresses = new ArrayList<>();

        for (String peer : peers) {

            if (!peer.contains(":")) {
                continue;
            }

            Peer currentPeer = parsePeer(peer);
            if (!validatePeer(currentPeer)) {
                continue;
            }
            peersAddresses.add(currentPeer);
        }

        if (peersAddresses.isEmpty()) {
            throw new PeersParseException("Tracker returned 0 peers");
        }

        trackerResponse.setPeers(peersAddresses);
    }

    private static Peer parsePeer(String peerAddress) {
        String[] peerParts = peerAddress.split(":");
        return new Peer(peerParts[0], peerParts[1]);
    }

    private static boolean validatePeer(Peer peer) {
        if ("0".equals(peer.getPort())) {
            System.out.println("[WARN] Peer " + peer + " has incorrect port: " + peer.getPort());
            return false;
        }
        if ("0".equals(peer.getPart(4))) {
            System.out.println("[WARN] Peer " + peer + " has incorrect ip (it looks like subnet ip): " + peer.getHost());
            return false;
        }
        return true;
    }
}
