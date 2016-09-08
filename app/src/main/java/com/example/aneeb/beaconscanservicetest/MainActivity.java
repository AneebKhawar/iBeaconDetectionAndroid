package com.example.aneeb.beaconscanservicetest;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import de.greenrobot.event.EventBus;

public class MainActivity extends AppCompatActivity {

    Intent beaconMonitoring;
    boolean isServiceStopped;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        isServiceStopped = false;
        beaconMonitoring = new Intent(MainActivity.this, BeaconMonitoringService.class);
        startService(beaconMonitoring);
        EventBus.getDefault().register(this);
    }

    public void onEvent(BeaconRangedEvent beaconRangedEvent){
        Toaster.showToastOnMainThread(this, "Minor : " + beaconRangedEvent.beaconMinor, Toast.LENGTH_SHORT);
    }

    @Override
    protected void onPause(){
        if(isFinishing()) {
            stopService(beaconMonitoring);
            isServiceStopped = true;
        }
        super.onPause();
    }

    @Override protected void onStop(){
        /**
         * Tell the service about application going to background
         * */
        EventBus.getDefault().post(new MonitoringActivityModeShift(false));
        super.onStop();
    }

    @Override
    protected void onResume(){
        /**
         * Tell the service about application coming to foreground
         * */
        EventBus.getDefault().post(new MonitoringActivityModeShift(true));
        super.onResume();
    }

    @Override
    protected void onDestroy(){
        if(!isServiceStopped)
            stopService(beaconMonitoring);
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }
}