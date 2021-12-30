package com.github.douwe.bikenolight;

import com.github.douwe.bikenolight.rest.Location;
import com.github.douwe.bikenolight.rest.RouteData;
import com.github.douwe.bikenolight.rest.RouteResource;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class LocationSampleCollector implements Runnable {
    private final long SAMPLE_PERIOD_MS = 2000l;
    private final LocationList samples = new LocationList();

    private final File storagePath;

    private final List<IndexedLocation> indexedLocations = new ArrayList<>();

    private final AtomicLong sequence = new AtomicLong(10000);

    private File activeFile;
    private PrintStream out;
    private volatile boolean mKeepRunning = true;

    public LocationSampleCollector(File storagePath) {
        this.storagePath = storagePath;
        Thread thread = new Thread(this);
        thread.start();

    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        mKeepRunning = false;
    }

    public void newSample(LocationList.BikeLocation location) {
        samples.add(location);
    }

    @Override
    public void run() {
        long lastTs = System.currentTimeMillis();
        while (mKeepRunning) {
            long nowTs = System.currentTimeMillis();
            long nextTs = lastTs + SAMPLE_PERIOD_MS;
            if (nowTs > nextTs) {
                // are we to slow here ???
                nextTs = nowTs + SAMPLE_PERIOD_MS;
            }

            try {
                Thread.sleep(nextTs - nowTs);
            } catch (InterruptedException e){
            }

            List<LocationList.BikeLocation> locations = samples.flush();
            if (locations.isEmpty()) {
                continue;
            }

            LocationList.BikeLocation bestLocation = null;
            for(LocationList.BikeLocation bikeLocation : locations) {
                if (bikeLocation.accuracy<70f) {
                    if ((bestLocation==null) || bestLocation.accuracy> bikeLocation.accuracy) {
                        bestLocation = bikeLocation;
                    }
                }
            }

            if (bestLocation == null) {
                double sumLat = 0d;
                double sumLon = 0d;
                double sumAcc = 0d;
                double sumTs = 0d;
                double sum = 0d;

                for(LocationList.BikeLocation bikeLocation : locations) {
//                    if (bikeLocation.accuracy < 300f) {
                        double mul = 400d - bikeLocation.accuracy;
                        sumLat += bikeLocation.latitude * mul;
                        sumLon += bikeLocation.longitude * mul;
                        sumAcc += bikeLocation.accuracy * mul;
                        sumTs += bikeLocation.ts * mul;
                        sum += mul;
//                    }
                }

                if (sum >0d) {
                    sumLat = sumLat / sum;
                    sumLon = sumLon / sum;
                    sumAcc = sumAcc / sum;
                    bestLocation = new LocationList.BikeLocation((long) Math.round(sumTs/sum), "combined", sumLat, sumLon, (float) sumAcc);
                }
            }

            if (bestLocation == null) {
                continue;
            }

            recordLocation(bestLocation);
            try {
                write(bestLocation);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

        }
    }

    private synchronized void recordLocation(LocationList.BikeLocation location) {
        long index = sequence.incrementAndGet();
        IndexedLocation indexedLocation = new IndexedLocation(location, index);
        indexedLocations.add(indexedLocation);
        if (indexedLocations.size()>1100) {
            indexedLocations.subList(0, 100).clear();
        }
    }

    public synchronized List<IndexedLocation> enlist(Long lastIndex) {
        if (lastIndex == null) {
            return new ArrayList<>(indexedLocations);
        }
        ArrayList<IndexedLocation> result = new ArrayList<>(indexedLocations.size());
        for(int idx=indexedLocations.size()-1; idx>=0; idx--) {
            IndexedLocation indexedLocation = indexedLocations.get(idx);
            if (indexedLocation.index<=lastIndex) {
                break;
            }
            result.add(indexedLocation);
        }
        return result;
    }

    private synchronized void write(LocationList.BikeLocation bikeLocation) throws FileNotFoundException {
        if (activeFile == null) {
            activeFile = new File(storagePath, "nobikedata_"+System.currentTimeMillis()+".txt");
            out = new PrintStream(activeFile);
        }
        out.println("lat="+bikeLocation.latitude+", lon="+bikeLocation.longitude);
    }


    public static class IndexedLocation {
        public final LocationList.BikeLocation location;
        public final long index;
        public IndexedLocation(LocationList.BikeLocation location, long index) {
            this.index = index;
            this.location = location;
        }
    }
}
