package com.ssau.tpzrp.utils;

import com.squareup.okhttp.Call;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.ssau.tpzrp.model.TorrentFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class TorrentHelper {

    private static String getHttpRequest(TorrentFile torrentFile) {
        StringBuilder request = new StringBuilder();

        request.append(torrentFile.getAnnounceUrl());

        addParameter(request, "info_hash", torrentFile.getInfoHashForRequest());

        return request.toString();
    }

    private static void addParameter(StringBuilder request, String parameter, String value) {
        if (request.indexOf("?") == -1) {
            request.append('?').append(parameter).append("=").append(value);
        } else {
            request.append("&").append(parameter).append("=").append(value);
        }
    }

    public static String getPeers(TorrentFile torrentFile) throws IOException {

        String url = getHttpRequest(torrentFile);

        Request request = new Request.Builder()
                .url(url)
                .build();

        OkHttpClient client = new OkHttpClient();
        Call call = client.newCall(request);
        Response response = call.execute();
        InputStream stream = new ByteArrayInputStream(response.body().string().getBytes());
        Object obj = Bencode.parse(stream);

        return response.body().string();
    }
}
