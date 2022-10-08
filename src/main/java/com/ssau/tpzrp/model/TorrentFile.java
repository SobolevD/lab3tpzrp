package com.ssau.tpzrp.model;

import com.ssau.tpzrp.exceptions.TorrentParseException;
import com.ssau.tpzrp.utils.Bencode;
import com.ssau.tpzrp.utils.CommandRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public class TorrentFile {

    private final String announceUrl;
    private final List<String> announcesUrls;
    private final String comment;
    private final String createdBy;
    private final String encoding;
    private final String infoHash;

    private final Map<String, Object> info;

    private static final Logger logger = LoggerFactory.getLogger(TorrentFile.class);

    @SuppressWarnings("unchecked")
    public TorrentFile(String filePath) throws TorrentParseException {
        Path path = Paths.get(filePath);

        try {
            InputStream fileInputStream = Files.newInputStream(path);
            Object parsedFile = Bencode.parse(fileInputStream);

            Map<String, Object> parsedFileMap = (Map<String, Object>)parsedFile;

            this.announceUrl = parsedFileMap.get("announce").toString();
            this.announcesUrls = (List)parsedFileMap.get("announce-list");
            this.comment = parsedFileMap.get("comment").toString();
            this.createdBy = parsedFileMap.get("created by").toString();
            this.encoding = parsedFileMap.get("encoding").toString();
            this.info = (Map<String, Object>)parsedFileMap.get("info");

            String pythonInfoHashProviderPath = "src/main/resources/python/get_info_hash.py";
            String command = String.format("python %s %s", pythonInfoHashProviderPath, filePath);
            List<String> output = CommandRunner.runWithReturn(command);

            this.infoHash = output.get(0);
        } catch (IOException e) {
            String errorMessage = String.format("Could not find file by path %s", path);
            logger.error(errorMessage, e);
            throw new TorrentParseException(errorMessage, e);
        } catch (ClassCastException e) {
            String errorMessage = String.format("Something wrong during torrent file decoding. File: %s", path);
            logger.error(errorMessage, e);
            throw new TorrentParseException(errorMessage, e);
        }

    }

    public String getAnnounceUrl() {
        return announceUrl;
    }

    public List<String> getAnnouncesUrls() {
        return announcesUrls;
    }

    public String getComment() {
        return comment;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public String getEncoding() {
        return encoding;
    }

    public String getInfoHash() {
        return infoHash;
    }

    public String getInfoHashForRequest() {
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

    public Map<String, Object> getInfo() {
        return info;
    }
}
