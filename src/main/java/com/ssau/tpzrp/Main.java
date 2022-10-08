package com.ssau.tpzrp;


import com.ssau.tpzrp.exceptions.TorrentParseException;
import com.ssau.tpzrp.model.TorrentFile;
import com.ssau.tpzrp.utils.Bencode;
import com.ssau.tpzrp.utils.TorrentHelper;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;

public class Main {
    public static void main(String[] args) throws TorrentParseException, IOException {

        String fileName = "C:\\Users\\soboi\\Downloads\\IntelliJ-IDEA-Ultimate-2022.2.torrent";

        TorrentFile torrentFile = new TorrentFile(fileName);

        TorrentHelper.getPeers(torrentFile);
    }
}