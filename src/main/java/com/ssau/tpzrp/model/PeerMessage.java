package com.ssau.tpzrp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.codec.binary.Hex;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PeerMessage {

    private long dataLength;
    private int id;
    private byte[] payload;

    private abstract static class PartsLength {
        public static final int LENGTH_BYTES = 4;
    }

    public static PeerMessage parse(byte[] unsignedBytes) {

        PeerMessage message = new PeerMessage();

        byte[] messageLength    = new byte[PartsLength.LENGTH_BYTES];

        messageLength[0] = unsignedBytes[0];
        messageLength[1] = unsignedBytes[1];
        messageLength[2] = unsignedBytes[2];
        messageLength[3] = unsignedBytes[3];

        String hexDataLength = Hex.encodeHexString(messageLength);

        message.setDataLength(Long.parseLong(hexDataLength, 16));
        message.setId(unsignedBytes[4] & 0xFF);

        int payloadLength = unsignedBytes.length - 5;

        byte[] messagePayload = new byte[payloadLength];

        System.arraycopy(unsignedBytes, 5, messagePayload, 0, payloadLength);

        message.setPayload(messagePayload);
        return message;
    }
}
