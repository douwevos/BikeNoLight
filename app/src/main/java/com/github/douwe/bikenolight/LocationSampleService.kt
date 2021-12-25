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
import android.os.Environment
import android.os.IBinder
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter

class LocationSampleService : Service() {

    private val binder = LocalBinder()

    private val locationList = LocationList()
    private val locationList2 = LocationList()

    private var thread : Thread? = null;

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

        var lthread = Thread {
            val file = File(applicationContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "nobikedata.txt")

            var fos = FileOutputStream(file)
            var out = PrintWriter(fos)
            while (true) {
                try {
                    Thread.sleep(2000L)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
                Log.d("write", ""+file.absolutePath)
                var list = locationList.flush()
                if (list.isNotEmpty()) {
                    for(l in list) {
                        out.println("lat="+l.latitude+", lon="+l.longitude);
                    }
                    out.flush();
                }
            }
        }
        lthread.start()
        thread = lthread;

    }

    fun enlistLocations(): MutableList<LocationList.BikeLocation>? {
        return locationList2.flush();
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
            locationList2.add(s)
        }

        override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {}
    }
}
