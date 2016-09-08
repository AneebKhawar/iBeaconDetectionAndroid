package com.example.aneeb.beaconscanservicetest;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.service.ArmaRssiFilter;

import java.util.ArrayList;
import java.util.Collection;

import de.greenrobot.event.EventBus;

import static android.media.RingtoneManager.getDefaultUri;
import static org.altbeacon.beacon.service.BeaconService.TAG;


/**
 * Created by aneeb on 6/27/16.
 */

public class BeaconMonitoringService extends Service implements BeaconConsumer {
    private BeaconManager beaconManager;
    private boolean isParentActivityInForeground = true;
    private NotificationManager notificationManager;
    private Region myRegion;
    private static ArrayList<Integer> visitedMinors;
    private static final int FOREGROUND_CONSTANT = 20;

    public BeaconMonitoringService(){}

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        /**
         * Start the service in foreground to make it less likely for the OS to kill it.
         * */
        buildForegroundNotification();

        visitedMinors = new ArrayList<>();

        myRegion = new Region("region",null,null,null);

        EventBus.getDefault().register(this);
        beaconManager.bind(this);

        super.onStartCommand(intent,flags,startId);
        return START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        /**
         * Set the RSSI filter to ArmaRssiFilter to ensure that a decent range approximation is
         * achieved while moving and scanning simultaneously.
         *
         * Set the BeaconManger's parser to iBeacon format.
         * */

        BeaconManager.setRssiFilterImplClass(ArmaRssiFilter.class);
        beaconManager = BeaconManager.getInstanceForApplication(this);

        /**
         * Set the BeaconLayout as that of an iBeacon
         * */

        beaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));

        /**
         * Scanning interval set to a 4 second scan every 3 seconds.
         * */
        beaconManager.setForegroundScanPeriod(4000l);
        beaconManager.setForegroundBetweenScanPeriod(3000l);
    }

    /**
    * Event to inform the service about monitoring activities shifting to background and
    * back to foreground.
    */
    public void onEvent(MonitoringActivityModeShift monitoringActivityModeShift) {
        isParentActivityInForeground = monitoringActivityModeShift.mIsInForeground;
    }

    /**
     * Create the Notification for when the App is in background and scanning is enabled.
     * */
    private void sendNotification(String notificationText) {

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                        .setContentTitle("StriveOn")
                        .setContentText(notificationText)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setSound(getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));

        builder.setAutoCancel(true);


        notificationManager =
                (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, builder.build());
    }

    private void buildForegroundNotification() {
        Bitmap icon = BitmapFactory.decodeResource(getResources(),
                R.mipmap.ic_launcher);

        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle("StriveOn")
                .setTicker("Station scanning started")
                .setContentText("Looking for stations ...")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(Bitmap.createScaledBitmap(icon, 128, 128, false))
                .build();

        startForeground(FOREGROUND_CONSTANT,
                notification);
    }

    @Override
    public void onBeaconServiceConnect() {

        /**
         * Start Monitoring the relevant region.
         * */

        try {
            beaconManager.stopMonitoringBeaconsInRegion(myRegion);
            beaconManager.startMonitoringBeaconsInRegion(myRegion);
        }
        catch (RemoteException e){
            e.printStackTrace();
        }

        beaconManager.addMonitorNotifier(new MonitorNotifier() {

            /**
             * Start ranging for Beacons when a region is entered.
             * */

            @Override
            public void didEnterRegion(Region region) {
                try {
                    beaconManager.startRangingBeaconsInRegion(region);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            /**
             * Stop ranging for Beacons on region exit.
             * */
            @Override
            public void didExitRegion(Region region) {
                try {
                    beaconManager.stopRangingBeaconsInRegion(region);
                }
                catch (RemoteException e) {
                    e.printStackTrace();
                }
                 Log.i(TAG, "SERVICE : I no longer see a beacon");
            }

            @Override
            public void didDetermineStateForRegion(int state, Region region) {
                Log.i(TAG, "SERVICE : I have just switched from seeing/not seeing beacons: " + state);
            }
        });


        beaconManager.addRangeNotifier(new RangeNotifier() {

            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> collection, Region region) {


                    Beacon closestBeacon = null;
                    for (Beacon beacon : collection) {
                        if (closestBeacon == null) {
                            closestBeacon = beacon;
                        } else {
                            if (closestBeacon.getDistance() > beacon.getDistance()) {
                                closestBeacon = beacon;
                            }
                        }
                    }

                    /**
                     * If the Beacon hasn't been visited already, trigger event on it and add to visitedMinors list.
                     * */

                    if(!isContaining(closestBeacon.getId3().toInt())){
                        visitedMinors.add(closestBeacon.getId3().toInt());

                        /**
                         * If the application is in the foreground, just show a simple Toast indicating the Minor of detected beacon
                         *
                         * If the application is in background, show a notification to user with detected Beacon's Minor.
                         *
                         * */

                        if(isParentActivityInForeground)
                        EventBus.getDefault().post(new BeaconRangedEvent(closestBeacon.getId3().toInt()));
                        else
                            sendNotification("BeaconFound!! : " + closestBeacon.getId3().toInt());
                    }
                }

        });

    }

    private boolean isContaining(int id){
        for(int item : visitedMinors){
            if(id == item)
                return true;
        }
        return false;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        try {
            beaconManager.stopRangingBeaconsInRegion(myRegion);
            beaconManager.removeAllMonitorNotifiers();
            beaconManager.unbind(this);
            EventBus.getDefault().unregister(this);
        }

        catch (Exception e){
        }

        /**
         * Remove all notifications
         * */

        if(notificationManager != null)
            notificationManager.cancelAll();

        super.onDestroy();
    }
}