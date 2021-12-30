package com.github.douwe.bikenolight

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter


class LocationSampleService {

    private val context: Context;

    constructor(ctx : Context) {
        this.context = ctx;
    }

    private val locationList = LocationList()
    private val locationList2 = LocationList()

    public lateinit var sampleCollector : LocationSampleCollector;

    @SuppressLint("MissingPermission")
    fun onCreate() {
        sampleCollector = LocationSampleCollector(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS))
        var locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val providers = locationManager.getProviders(false)

        for (provider in providers) {
            Log.d("add as listener", ""+provider)
            locationManager.requestLocationUpdates(provider,100,0.01f, MylocationListener(provider))

        }
    }

    fun enlistLocations(): MutableList<LocationList.BikeLocation>? {
        return locationList2.flush();
    }

    inner class LocalBinder : Binder() {
        fun getService(): LocationSampleService = this@LocationSampleService
    }

    inner class MylocationListener: LocationListener {

        val provider: String;

        constructor(name: String):super() {
            provider = name;
        }

        override fun onLocationChanged(location: Location) {


            var s = LocationList.BikeLocation(System.currentTimeMillis(), provider, location.latitude, location.longitude, location.accuracy)
            Log.d("location", "onLocationChanged: "+s)
            locationList.add(s)
            locationList2.add(s)
            sampleCollector.newSample(s);
        }

        override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {
            Log.d(TAG, "status-changed: provider="+provider+", po="+p0+", p1"+p1);
        }
    }


}
