package com.github.douwe.bikenolight

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.util.Log

class LocationSampleService : Service() {

    private val binder = LocalBinder()

    private val locationList = LocationList()

    override fun onBind(intent: Intent): IBinder {
        return binder;
    }

    @SuppressLint("MissingPermission")
    override fun onCreate() {
        super.onCreate()
        Log.d("oncreate", "hiero")
        var locationManager=getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val providers = locationManager.getProviders(false)

        for (provider in providers) {
            Log.d("add as listener", ""+provider)
            locationManager.requestLocationUpdates(provider,100,0.01f, MylocationListener())
        }
    }

    fun enlistLocations(): MutableList<LocationList.BikeLocation>? {
        return locationList.flush();
    }

    inner class LocalBinder : Binder() {
        fun getService(): LocationSampleService = this@LocationSampleService
    }

    inner class MylocationListener: LocationListener {

        constructor():super(){
        }

        override fun onLocationChanged(location: Location) {

            var s = LocationList.BikeLocation(location.latitude, location.longitude)
            Log.d("location", "onLocationChanged: "+s)
            locationList.add(s)
        }

        override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {}
    }
}
