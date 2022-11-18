package com.ssau.tpzrp.utils;

public abstract class BytesUtils {
    public static byte[] getUnsignedBytes(byte[] rawBytes) {
        byte[] unsignedBytes = new byte[rawBytes.length];
        for (int i = 0; i < rawBytes.length; ++i) {
            unsignedBytes[i] = (byte)(rawBytes[i] & 0xFF);
        }
        return unsignedBytes;
    }
}
