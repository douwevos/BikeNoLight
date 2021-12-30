package com.github.douwe.bikenolight;

import java.util.ArrayList;
import java.util.List;

public class LocationList {

    private List<BikeLocation> locations = new ArrayList<>();

    public synchronized void add(BikeLocation location) {
        locations.add(location);
    }

    public synchronized List<BikeLocation> flush() {
        List<BikeLocation> result = locations;
        locations = new ArrayList<>();
        return result;
    }

    public static class BikeLocation {
        long ts;
        String provider;
        double latitude;
        double longitude;
        float accuracy;

        public BikeLocation(long ts, String provider, double latitude, double longitude, float accuracy) {
            this.ts = ts;
            this.provider = provider;
            this.latitude = latitude;
            this.longitude = longitude;
            this.accuracy = accuracy;
        }

    }
}
