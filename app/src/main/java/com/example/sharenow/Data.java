package com.example.sharenow;

import java.io.Serializable;

public class Data implements Serializable {
    private String path;
    private byte[] bytes;
    private boolean endPart;
    private double ratio;
    public Data () {
        bytes = new byte[0];
    }
    public Data (String path, byte[] bytes, double ratio, boolean endPart) {
        this.path = path;
        this.bytes = bytes;
        this.ratio = ratio;
        this.endPart = endPart;
    }
    public byte[] getBytes() {
        return bytes;
    }

    public boolean isEndPart() {
        return endPart;
    }

    public String getPath() {
        return path;
    }

    public double getRatio() {
        return ratio;
    }
}
