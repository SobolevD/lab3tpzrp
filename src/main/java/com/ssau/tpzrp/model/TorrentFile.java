package com.ssau.tpzrp.model;

import com.ssau.tpzrp.constants.TorrentFields;
import com.ssau.tpzrp.exceptions.TorrentParseException;
import com.ssau.tpzrp.utils.Bencode;
import com.ssau.tpzrp.utils.CommandRunner;
import lombok.Data;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static com.ssau.tpzrp.constants.Common.HASH_ENCODER_PYTHON_SCRIPT_FILE_PATH;
import static com.ssau.tpzrp.constants.TorrentFields.INFO_PIECES;

@Data
public class TorrentFile {

    private String announceUrl;
    private List<String> announcesUrls;
    private String comment;
    private String createdBy;
    private String encoding;
    private String infoHash;
    private Map<String, Object> info;

    private int piecesCount;

    @SuppressWarnings("unchecked")
    public TorrentFile(String filePath) throws TorrentParseException {
        Path path = Paths.get(filePath);

        try {
            InputStream fileInputStream = Files.newInputStream(path);
            Object parsedFile = Bencode.parse(fileInputStream);

            Map<String, Object> parsedFileMap = (Map<String, Object>)parsedFile;
            parseParameters(parsedFileMap);

            String command = String.format("python %s %s", HASH_ENCODER_PYTHON_SCRIPT_FILE_PATH, filePath);
            List<String> output = CommandRunner.runWithReturn(command);

            this.infoHash = output.get(0);

        } catch (IOException e) {
            String errorMessage = String.format("Could not find file by path %s", path);
            System.out.println("[ERROR] " + errorMessage);
            throw new TorrentParseException(errorMessage, e);
        } catch (ClassCastException e) {
            String errorMessage = String.format("Something wrong during torrent file decoding. File: %s", path);
            System.out.println("[ERROR] " + errorMessage);
            throw new TorrentParseException(errorMessage, e);
        }
    }

    public TorrentFile(String filePath, Set<String> acceptableProtocols) throws TorrentParseException {
        this(filePath);

        System.out.println("[DEBUG] Protocol supports: " + acceptableProtocols);

        List<String> acceptableTrackerUrls = new ArrayList<>();
        for (String trackerUrl : this.announcesUrls) {
            for (String acceptableProtocol : acceptableProtocols) {
                if (trackerUrl.startsWith(acceptableProtocol)) {
                    acceptableTrackerUrls.add(trackerUrl);
                    break;
                }
            }
        }
        if (acceptableTrackerUrls.isEmpty()) {
            String errorMessage = "Torrent file has announces urls only with unacceptable protocols";
            System.out.println("[ERROR] " + errorMessage);
            throw new TorrentParseException(errorMessage);
        }
        this.announcesUrls = acceptableTrackerUrls;
    }



    @SuppressWarnings("unchecked")
    private void parseParameters(Map<String, Object> map) throws TorrentParseException {

        Object announceUrl = map.get(TorrentFields.ANNOUNCE);
        this.announceUrl = Objects.isNull(announceUrl) ? null : announceUrl.toString();

        Object announcesLists = map.get(TorrentFields.ANNOUNCE_LIST);
        if (Objects.nonNull(announcesLists)) {
            this.announcesUrls = new ArrayList<>();
            for (List<String> announcesList : (List<List<String>>)announcesLists) {
                this.announcesUrls.addAll(announcesList);
            }
        }

        if (Objects.isNull(this.announcesUrls)) {
            this.announcesUrls = List.of(this.announceUrl);
        }

        System.out.println("Trackers found: " + this.announcesUrls);

        Object comment = map.get(TorrentFields.COMMENT);
        this.comment = Objects.isNull(comment) ? null : comment.toString();

        Object createdBy = map.get(TorrentFields.CREATED_BY);
        this.createdBy = Objects.isNull(createdBy) ? null : createdBy.toString();

        Object encoding = map.get(TorrentFields.ENCODING);
        this.encoding = Objects.isNull(encoding) ? null : encoding.toString();

        Object info = map.get(TorrentFields.INFO);
        if (Objects.isNull(info)) {
            String errorMessage = "Could not parse INFO block of torrent file";
            System.out.println("[ERROR] " + errorMessage);
            throw new TorrentParseException(errorMessage);
        }
        this.info = (Map<String, Object>)info;

        Object pieces = this.info.get(INFO_PIECES);
        this.piecesCount = ((String)pieces).length() / 20;
    }
}
