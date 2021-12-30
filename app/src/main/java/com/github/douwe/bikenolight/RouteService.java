package com.github.douwe.bikenolight;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.github.douwe.bikenolight.rest.Location;
import com.github.douwe.bikenolight.rest.RouteData;
import com.github.douwe.bikenolight.rest.RouteResource;
import com.github.douwe.bikenolight.rest.RouteStart;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RouteService extends Service implements Runnable {

    private final String TAG = "RouteService:"+System.nanoTime();

    private static final boolean FAKE_ROUTE = false;

    public static final String ACTION_STOP = "${BuildConfig.APPLICATION_ID}.stop";


    private IBinder binder = new LocalBinder();

    private final Object lock = new Object();

    private LocationSampleCollector sampleCollector;

    private RouteResource routeResource;

    private boolean bikeRouteActive;

    LocationSampleService locationSampleService;

    PowerManager.WakeLock wakeLock;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {

        Log.d(TAG, "binding");

        return binder;
    }

    public List<LocationList.BikeLocation> enlistLocal() {
        return locationSampleService.enlistLocations();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        locationSampleService = new LocationSampleService(getApplicationContext());

        Log.d(TAG, "onCreate");

        Thread thread = new Thread(this);
        thread.start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    enum State {
        INACTIVE,
        REQUEST_LOCATION_SAMPLER,
        WAIT_FOR_LOCATION_SAMPLER,
        START_ROUTE,
        COLLECT_LOCATION_DATA,
        DISCONNECT_LOCATION_SAMPLER
    }

    @Override
    public void run() {
        int waitPeriod = 0;
        State state = State.INACTIVE;
        BindLocationSampleService bindLocationSampleService = null;
        Long routeId = null;
        Handler handler = new Handler(getMainLooper());
        Long lastIndex = null;


        while(true) {
            Log.d(TAG, "waitPeriod "+waitPeriod+", state="+state);
            if (waitPeriod > 0) {
                synchronized (lock) {
                    try {
                        lock.wait(waitPeriod);
                    } catch (InterruptedException e) {
                    }
                }
            }

            try {

                switch (state) {
                    case INACTIVE:
                        waitPeriod = 2000;
                        if (bikeRouteActive) {
                            waitPeriod = 0;
                            state = State.REQUEST_LOCATION_SAMPLER;
                        }
                        break;

                    case REQUEST_LOCATION_SAMPLER:
                        bindLocationSampleService = new BindLocationSampleService(false);
                        handler.postAtFrontOfQueue(bindLocationSampleService);
                        waitPeriod = 0;
                        state = State.WAIT_FOR_LOCATION_SAMPLER;
                        break;

                    case WAIT_FOR_LOCATION_SAMPLER:
                        // TODO add timeout ?
                        waitPeriod = 50;
//                        locationSampleService.onCreate();
                        if (sampleCollector != null) {
                            state = State.START_ROUTE;
                        }
                        break;

                    case START_ROUTE:
                        if (bikeRouteActive) {
                            routeId = startRoute();
                            if (routeId == null) {
                                waitPeriod = 4000;
                            } else {
                                waitPeriod = 0;
                                state = State.COLLECT_LOCATION_DATA;

                                lastIndex = calculateInitialLastIndex();

                            }
                        } else {
                            waitPeriod = 0;
                            state = State.DISCONNECT_LOCATION_SAMPLER;
                        }
                        break;

                    case COLLECT_LOCATION_DATA :
                        if (bikeRouteActive) {
                            if (sampleCollector == null) {
                                state = State.DISCONNECT_LOCATION_SAMPLER;
                                break;
                            }
                            List<LocationSampleCollector.IndexedLocation> enlisted = sampleCollector.enlist(lastIndex);
                            if (enlisted.size() > 0) {
                                sendRouteData(routeId, enlisted);
                                lastIndex = enlisted.get(enlisted.size()-1).index;
                            }
                            waitPeriod = 1000;
                        } else {
                            state = State.DISCONNECT_LOCATION_SAMPLER;
                            waitPeriod = 0;
                        }
                        break;

                    case DISCONNECT_LOCATION_SAMPLER:
                        waitPeriod = 0;
                        if (bindLocationSampleService == null) {
                            bindLocationSampleService = new BindLocationSampleService(true);
                            handler.postAtFrontOfQueue(bindLocationSampleService);
                        }
                        sampleCollector = null;
                        state = State.INACTIVE;
                        routeId = null;
                        bindLocationSampleService = null;
                        routeResource = null;
                        break;
                }

            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
                e.printStackTrace();
                waitPeriod = 10000;
            }

        }
    }

    private Long calculateInitialLastIndex() {
        List<LocationSampleCollector.IndexedLocation> enlisted = sampleCollector.enlist(null);
        long nowTs = System.currentTimeMillis();
        for(int idx= enlisted.size()-1; idx>=0; idx--) {
            LocationSampleCollector.IndexedLocation indexedLocation = enlisted.get(idx);
            if (indexedLocation.location.ts<nowTs) {
                return indexedLocation.index;
            }
        }
        return null;
    }


    private Long startRoute() {
        if (FAKE_ROUTE) {
            return 10L;
        }
        Long result = null;
        Retrofit retrofit = new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl("http://douwe.dnsuser.de:8080/")
                .build();

        routeResource = retrofit.create(RouteResource.class);

        try {
            RouteStart startRoute = new RouteStart();
            startRoute.startTs = System.currentTimeMillis();
            Call<Long> longCall = routeResource.routeStart(startRoute);
            Response<Long> response = longCall.execute();
            result = response.body();
        } catch(Exception e) {
            Log.e(TAG, e.getMessage());
        }
        return result;
    }

    private void sendRouteData(long routeId, List<LocationSampleCollector.IndexedLocation> enlisted) {
        if (FAKE_ROUTE) {
            return;
        }
        List<Location> locations =  new ArrayList<>(enlisted.size());
        for(LocationSampleCollector.IndexedLocation indexedLocation : enlisted) {
            locations.add(asRouteLocation(indexedLocation));
        }
        RouteData data = new RouteData();
        data.setLocations(locations);
        data.setRouteId(routeId);

        try {
            Call<Void> routeDataCall = routeResource.routeData(data);
            routeDataCall.execute();
        } catch (Throwable e) {
            Log.e(TAG, e.getMessage());
            e.printStackTrace();
        }
    }

    private Location asRouteLocation(LocationSampleCollector.IndexedLocation indexedLocation) {
        LocationList.BikeLocation location = indexedLocation.location;
        Location result = new Location();
        result.setTs(location.ts);
        result.setLat(location.latitude);
        result.setLon(location.longitude);
        return  result;
    }

    class BindLocationSampleService implements Runnable {

        private final boolean stop;

        public BindLocationSampleService(boolean stop) {
            this.stop = stop;
        }

        @Override
        public void run() {
            if (stop) {
                sampleCollector = null;
            } else {
                locationSampleService.onCreate();
                sampleCollector = locationSampleService.sampleCollector;
            }
        }
    }

    private void setBikeRouteActive(boolean flag) {
        Log.d(TAG, "set bike-route-active "+flag+", currently="+bikeRouteActive);

        synchronized (lock) {
            if (bikeRouteActive == flag) {
                return;
            }
            bikeRouteActive = flag;
            lock.notifyAll();
        }
    }

    class LocalBinder extends Binder {
        RouteService getService() {
            return RouteService.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        boolean isStop = ACTION_STOP.equalsIgnoreCase(action);
        Log.d(TAG, "intent.action="+action+", isStop="+isStop);
        if (isStop) {
            if (wakeLock != null) {
                wakeLock.release();
                wakeLock = null;
            }
            setBikeRouteActive(false);
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }
        generateForegroundNotification();
        setBikeRouteActive(true);
        wakeLock = ((PowerManager)getSystemService(Context.POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "BNL:"+TAG);
        wakeLock.acquire();
        return START_STICKY;
    }



    //Notififcation for ON-going
    private Bitmap iconNotification;
    private Notification notification;
    NotificationManager mNotificationManager;
    int  mNotificationId = 123;

    private void generateForegroundNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent intentMainLanding = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent =
                    PendingIntent.getActivity(this, 0, intentMainLanding, 0);
            Resources resources = getResources();
            iconNotification = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher);
            if (mNotificationManager == null) {
                mNotificationManager = (NotificationManager)
                        this.getSystemService(Context.NOTIFICATION_SERVICE);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                assert(mNotificationManager != null);
                mNotificationManager.createNotificationChannelGroup(
                        new NotificationChannelGroup("chats_group", "Chats")
                );
                NotificationChannel notificationChannel =
                        new NotificationChannel(
                                "service_channel", "Service Notifications",
                                NotificationManager.IMPORTANCE_MIN
                        );
                notificationChannel.enableLights(false);
                notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
                mNotificationManager.createNotificationChannel(notificationChannel);
            }
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "service_channel")

                .setContentTitle(resources.getString(R.string.app_name)+ " service is running")
                .setTicker(resources.getString(R.string.app_name) + "service is running")
                .setContentText("Touch to open") //                    , swipe down for more options.
                .setSmallIcon(R.drawable.quantum_ic_keyboard_arrow_down_white_36)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setWhen(0)
                .setOnlyAlertOnce(true)
                .setContentIntent(pendingIntent)
                .setOngoing(true);
            if (iconNotification != null) {
                builder.setLargeIcon(Bitmap.createScaledBitmap(iconNotification, 128, 128, false));
            }
            builder.setColor(resources.getColor(R.color.purple_200));
            notification = builder.build();
            startForeground(mNotificationId, notification);
        }
    }

    public static class RouteServiceConnection implements ServiceConnection {

        volatile RouteService mService;

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            RouteService.LocalBinder sampleBinder = (RouteService.LocalBinder) service;
            mService = sampleBinder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }

        public RouteService getRouteService() {
            return mService;
        }
    };

}
