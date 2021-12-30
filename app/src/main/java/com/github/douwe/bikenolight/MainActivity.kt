package com.github.douwe.bikenolight

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.gesture.OrientedBoundingBox
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.preference.PreferenceManager
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.w3c.dom.Text

const val TAG : String = "MainActivity"

class MainActivity : AppCompatActivity() {

    private val REQUEST_PERMISSIONS_REQUEST_CODE = 1;

    lateinit var bikeToggle : BikeToggleButton
    private lateinit var map : MapView;
    private lateinit var thread : Thread



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        bikeToggle = findViewById<BikeToggleButton>(R.id.bikeToggle)

        var s = BikeToggleButton.Listener() {
            Log.d(TAG, "toggle-state=$it")
            val intent = Intent(this, RouteService::class.java)
            if (!it) {
                intent.action = RouteService.ACTION_STOP;
            }
            startService(intent)
        }
        bikeToggle.setListener(s)


        val ctx = applicationContext
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
        Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID

        map = findViewById<MapView>(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK);
    }


    private val routeServiceConnection = RouteService.RouteServiceConnection()


    override fun onStart() {
        super.onStart()
        Log.d("blah", "starting")

        val intentR = Intent(this, RouteService::class.java);
        intentR.setAction("nul");
        if (bindService(intentR, routeServiceConnection, Context.BIND_AUTO_CREATE)) {
            Log.d(TAG, "route-service bound")
        } else {
            Log.e(TAG, "route-service NOT bound")
        }



        var r = Runnable {
            while (true) {
                try {
                    Thread.sleep(2000L)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
                val lRouteService = routeServiceConnection.routeService;
                Log.d(TAG, "wake up: "+lRouteService)
                if (lRouteService != null) {
                    val enlistedLocations = lRouteService.enlistLocal();
                    Log.d(TAG, "enlistedLocations: "+enlistedLocations)
                    if (enlistedLocations.isNotEmpty()) {
                        runOnUiThread {
                            var lmap = map
                            if (lmap != null) {
                                var box : BoundingBox?;
                                box = null;
//                                val box = BoundingBox()

                                lmap.overlays.clear()

                                for (bikelocation in enlistedLocations) {
                                    var lm = Marker(lmap)
                                    GeoPoint(
                                        bikelocation.latitude,
                                        bikelocation.longitude
                                    ).also { lm.position = it };
                                    lm.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                                    lmap.overlays.add(lm);

                                    if (box == null) {
                                        box = BoundingBox()
                                        box.latNorth = bikelocation.latitude - 0.001;
                                        box.latSouth = bikelocation.latitude + 0.001;
                                        box.lonEast = bikelocation.longitude + 0.001;
                                        box.lonWest = bikelocation.longitude - 0.001;

                                    } else {

                                        val llat = bikelocation.latitude;
                                        if (llat < box.latNorth) {
                                            box.latNorth = llat;
                                        }
                                        if (llat > box.latSouth) {
                                            box.latSouth = llat;
                                        }

                                        val llon = bikelocation.longitude;
                                        if (llon < box.lonWest) {
                                            box.lonWest = llon;
                                        }
                                        if (llon > box.lonEast) {
                                            box.lonEast = llon;
                                        }


                                    }

                                }

                                if (box != null) {
                                    lmap.zoomToBoundingBox(box, true);
                                }

                            }
                        }
                    }
                }
/*                val enlistedLocations = lService.enlistLocations() ?: continue
                Log.d("MA", "enlistedLocations="+enlistedLocations)
                if (enlistedLocations.isNotEmpty()) {
                    val bikeLocation = enlistedLocations[0]
                    Log.d("MA", "bikeLocation="+bikeLocation)
                    runOnUiThread {
                        var ltext = text
                        if (ltext != null) {
                            ltext.setText("gps: lon="+bikeLocation.longitude+", lat="+bikeLocation.latitude);
                        }
                        var lmap = map
                        if (lmap != null) {
                            val box = BoundingBox()
                            box.latNorth = bikeLocation.latitude-0.001;
                            box.latSouth  = bikeLocation.latitude+0.001;
                            box.lonEast = bikeLocation.longitude+0.001;
                            box.lonWest  = bikeLocation.longitude-0.001;

                            var lm = marker;
                            if (lm == null) {
                                lm = Marker(lmap)
                                marker = lm
                                lmap.overlays.add(marker);
                            }
                            GeoPoint(bikeLocation.latitude, bikeLocation.longitude).also { lm.position = it };
                            lm.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

                            lmap.zoomToBoundingBox(box, true);

                        }
                    }
                } */

            }
        }
        thread = Thread(r)
        thread.start()
    }


    override fun onResume() {
        super.onResume()
        map.onResume();
    }

    override fun onPause() {
        super.onPause();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().save(this, prefs);
        map.onPause();  //needed for compass, my location overlays, v6.0.0 and up
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        val permissionsToRequest = ArrayList<String>();
        var i = 0;
        while (i < grantResults.size) {
            permissionsToRequest.add(permissions[i]);
            i++;
        }
        if (permissionsToRequest.size > 0) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }


    private fun obtainLocalization(){
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
    }
}
