package com.ssau.tpzrp.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Peer {
    private String host;
    private String port;

    public String getPart(int partNum) {
        String[] ipParts = host.split("\\.");
        return ipParts[partNum - 1];
    }

    @Override
    public String toString() {
        return String.format("%s:%s", host, port);
    }
}
