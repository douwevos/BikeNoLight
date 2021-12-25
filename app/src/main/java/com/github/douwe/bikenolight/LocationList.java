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
        double latitude;
        double longitude;

        public BikeLocation(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }

    }
}
