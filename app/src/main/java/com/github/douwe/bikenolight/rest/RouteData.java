package com.github.douwe.bikenolight.rest;

import java.util.List;

public class RouteData {

    private long routeId;

    private List<Location> locations;

    public long getRouteId() {
        return routeId;
    }

    public void setRouteId(long routeId) {
        this.routeId = routeId;
    }

    public List<Location> getLocations() {
        return locations;
    }

    public void setLocations(List<Location> locations) {
        this.locations = locations;
    }
}
