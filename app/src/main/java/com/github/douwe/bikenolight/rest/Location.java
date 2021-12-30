package com.github.douwe.bikenolight.rest;

public class Location {

    private String prov;
    private long ts;
    private double lat;
    private double lon;
    private float acc;

    public long getTs() {
        return ts;
    }

    public void setTs(long ts) {
        this.ts = ts;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }
}
