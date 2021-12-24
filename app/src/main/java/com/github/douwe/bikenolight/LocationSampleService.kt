package com.github.douwe.bikenolight

import android.Manifest
import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Binder
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import kotlin.concurrent.thread

class LocationSampleService : Service(), LocationListener {

    private val mBinder: LocationSampleBinder = LocationSampleBinder();

    private lateinit var fusedLocationClient: FusedLocationProviderClient


    // globally declare LocationRequest
    lateinit var locationRequest: LocationRequest

    // globally declare LocationCallback
    private lateinit var locationCallback: LocationCallback

    var latitude: Double? = 0.0;
    var longitude: Double? = 0.0;
    var time: Long? = 0;


    override fun onBind(intent: Intent): IBinder {
        Log.i("TAG", "oNbIND #############################")
        return mBinder;
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i("TAG", "onStartCommand: ")
        return super.onStartCommand(intent, flags, startId)
    }

    @SuppressLint("MissingPermission")
    override fun onCreate() {
        Log.i("TAG", "onCreate loc #############################")
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(applicationContext)
        getLocationUpdates()
        startLocationUpdates()
        thread {
            while(true) {
                Thread.sleep(3000)

                Log.i("tag", "sample !!! alt:" +latitude + " lon:" + longitude+ " "+time)

            }

        }
    }


    /**
     * call this method in onCreate
     * onLocationResult call when location is changed
     */
    @SuppressLint("MissingPermission")
    private fun getLocationUpdates()
    {

        var locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationManager.allProviders.forEach {
            print(""+it)
            locationManager.requestLocationUpdates(it, 0, 0.1f, this as android.location.LocationListener)
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(getApplicationContext())
        locationRequest = LocationRequest.create()
        locationRequest.interval = 10000
        locationRequest.fastestInterval  = 3000
        locationRequest.smallestDisplacement = 3f // 170 m = 0.1 mile
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY //set according to your app function
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return

                Log.i("resdd", "got result")
                if (locationResult.locations.isNotEmpty()) {
                    // get latest location
                    val location =
                        locationResult.lastLocation
                    latitude = location.latitude
                    longitude = location.longitude
                    time = location.time
                }
            }
        }
    }

    //start location updates
    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()

        )
    }


    inner class LocationSampleBinder : Binder() {
        val service: LocationSampleService
            get() = this@LocationSampleService
    }

    override fun onLocationChanged(location: Location) {
        Log.i("onLoCh ", ""+location.longitude+", "+location.longitude+", "+location.accuracy+", "+location.time)
    }
}