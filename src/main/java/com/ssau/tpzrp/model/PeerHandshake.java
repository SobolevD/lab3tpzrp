package com.ssau.tpzrp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.ArrayUtils;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PeerHandshake {

    private static final String IDENTIFIER = "BitTorrent protocol";
    private static final int IDENTIFIER_LENGTH = IDENTIFIER.length();
    private static final byte[] RESERVED_BYTES = {0, 0, 0, 0, 0, 0, 0, 0};
    private static final String PEER_IDENTIFIER = "-TR2940-k8hj0wgej6ch";

    private int identifierLength;
    private String identifier;
    private byte[] reservedBytes;
    private String peerId;
    private String hexInfoHash;

    private byte[] bitField;

    public static PeerHandshake get(String hexInfoHash) {
        return PeerHandshake.builder()
                .identifier(IDENTIFIER)
                .identifierLength(IDENTIFIER_LENGTH)
                .reservedBytes(RESERVED_BYTES)
                .peerId(PEER_IDENTIFIER)
                .hexInfoHash(hexInfoHash).build();
    }

    public static PeerHandshake get(byte[] peerHandshake) throws DecoderException {

        if (peerHandshake.length == 0) {
            throw new DecoderException("[WARN] Could not parse peer handshake response. Response is empty");
        }

        PeerHandshake handshake = new PeerHandshake();

        String hexHandshake = Hex.encodeHexString(peerHandshake);
        String identifierLength = hexHandshake.substring(0, 2);
        handshake.setIdentifierLength(Integer.parseInt(identifierLength, 16));

        int identifierStartPos = 2;
        String identifier = hexHandshake.substring(identifierStartPos, identifierStartPos + 2 * handshake.getIdentifierLength());
        handshake.setIdentifier(new String(Hex.decodeHex(identifier)));

        int reservedBytesStartPos = identifierStartPos + 2 * handshake.getIdentifierLength();
        String reservedBytes = hexHandshake.substring(reservedBytesStartPos, reservedBytesStartPos + 2 * 8);
        handshake.setReservedBytes(Hex.decodeHex(reservedBytes));

        int infoHashStartPos = reservedBytesStartPos + 2 * 8;
        String infoHash = hexHandshake.substring(infoHashStartPos, infoHashStartPos + 2 * 20);
        handshake.setHexInfoHash(infoHash);

        int peerIdStartPos = infoHashStartPos + 2 * 20;
        String peerId = hexHandshake.substring(peerIdStartPos, peerIdStartPos + 2 * 20);
        handshake.setPeerId(peerId);

        int bitFieldStartPos = peerIdStartPos + 2 * 20;
        String bitFieldStr = hexHandshake.substring(bitFieldStartPos + 2 * 20);
        handshake.setBitField(Hex.decodeHex(bitFieldStr));

        return handshake;
    }

    public byte[] getBytes() throws DecoderException {
        BigInteger bigInt = BigInteger.valueOf(IDENTIFIER_LENGTH);
        byte[] identifierLength = bigInt.toByteArray();
        byte[] identifier = IDENTIFIER.getBytes(StandardCharsets.UTF_8);
        byte[] infoHash = Hex.decodeHex(hexInfoHash);
        byte[] peerIdentifier = PEER_IDENTIFIER.getBytes(StandardCharsets.UTF_8);

        byte[] concatBytes = ArrayUtils.addAll(identifierLength, identifier);
        concatBytes = ArrayUtils.addAll(concatBytes, RESERVED_BYTES);
        concatBytes = ArrayUtils.addAll(concatBytes, infoHash);
        concatBytes = ArrayUtils.addAll(concatBytes, peerIdentifier);

        return concatBytes;
    }

    public String getString() {
        BigInteger bigInt = BigInteger.valueOf(IDENTIFIER_LENGTH);
        byte[] identifierLength = bigInt.toByteArray();
        String identifierLengthHex = "\\x" + Hex.encodeHexString(identifierLength);
        String reservedBytesHex = getHexBytesWithDelimiter(Hex.encodeHexString(RESERVED_BYTES), "\\x");
        String infoHashHex = getHexBytesWithDelimiter(hexInfoHash, "\\x");
        return identifierLengthHex + IDENTIFIER + reservedBytesHex + infoHashHex + PEER_IDENTIFIER;
    }

    private static String getHexBytesWithDelimiter(String string, String delimiter) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < string.length(); ++i) {
            for (int j = 0; j < 2; ++j, ++i) {
                if (i == string.length()) {
                    break;
                }
                if (j == 0) {
                    builder.append(delimiter);
                }
                builder.append(string.charAt(i));
            }
            --i;
        }
        return builder.toString();
    }
}
