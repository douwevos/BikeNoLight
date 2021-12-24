package com.github.douwe.bikenolight

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import org.w3c.dom.Text

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private var latitude: Double? = 0.0;
    private var longitude: Double? = 0.0;
    lateinit var button : Button
    lateinit var text : EditText
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var mService: LocationSampleService
    private var mBound: Boolean = false

    private val mConnection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            val binder = service as LocationSampleService.LocationSampleBinder
            mService = binder.service
            mBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            mBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(applicationContext)

        setContentView(R.layout.activity_main)
        button = findViewById<Button>(R.id.button);
        button.setOnClickListener(this);
        text = findViewById<EditText>(R.id.editTextGps)
        Log.i("TAG", "onCreate: #############################")
        val intent: Intent = Intent(this, LocationSampleService::class.java)
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE)
    }


    override fun onClick(v: View?) {
        obtieneLocalizacion()
        text.setText("gps: lon="+longitude+", lat="+latitude);
    }

    private fun obtieneLocalizacion(){
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION ), 1)
            return
        }
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                latitude =  location?.latitude
                longitude = location?.longitude
            }
    }
}