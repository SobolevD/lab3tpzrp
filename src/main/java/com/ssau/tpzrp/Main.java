package com.ssau.tpzrp;


import com.ssau.tpzrp.constants.TorrentTrackerProtocols;
import com.ssau.tpzrp.exceptions.TorrentParseException;
import com.ssau.tpzrp.model.TorrentFile;
import com.ssau.tpzrp.utils.Bencode;
import com.ssau.tpzrp.utils.TorrentHelper;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Set;

import static com.ssau.tpzrp.constants.Common.TORRENT_EXAMPLE_FILE_PATH;

public class Main {
    public static void main(String[] args) throws TorrentParseException, IOException, InterruptedException {

        Set<String> acceptableProtocols = Set.of(TorrentTrackerProtocols.HTTP);
        TorrentFile torrentFile = new TorrentFile(TORRENT_EXAMPLE_FILE_PATH, acceptableProtocols);

        TorrentHelper.getPeers(torrentFile);
    }
}