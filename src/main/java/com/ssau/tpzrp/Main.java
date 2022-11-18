package com.ssau.tpzrp;


import com.ssau.tpzrp.constants.TorrentTrackerProtocols;
import com.ssau.tpzrp.exceptions.TorrentParseException;
import com.ssau.tpzrp.model.Peer;
import com.ssau.tpzrp.model.TorrentFile;
import com.ssau.tpzrp.utils.TorrentHelper;
import org.apache.commons.codec.DecoderException;

import java.io.*;
import java.util.List;
import java.util.Set;

import static com.ssau.tpzrp.constants.Common.TORRENT_EXAMPLE_FILE_PATH;

public class Main {
    public static void main(String[] args) throws TorrentParseException, IOException, InterruptedException, DecoderException {

        Set<String> acceptableProtocols = Set.of(TorrentTrackerProtocols.HTTPS, TorrentTrackerProtocols.HTTP);
        TorrentFile torrentFile = new TorrentFile(TORRENT_EXAMPLE_FILE_PATH, acceptableProtocols);
        byte[] bytes = toBytes(13);
        List<Peer> peers = TorrentHelper.getPeers(torrentFile);
        TorrentHelper.download(torrentFile, peers);
    }

    private static byte[] toBytes(int i)
    {
        byte[] result = new byte[4];

        result[0] = (byte) (i >> 24);
        result[1] = (byte) (i >> 16);
        result[2] = (byte) (i >> 8);
        result[3] = (byte) (i /*>> 0*/);

        return result;
    }
}