package com.example.aneeb.beaconscanservicetest;

import android.content.Context;
import android.os.Handler;
import android.widget.Toast;

/**
 * utility class to show toast messages on the main thread
 */
public class Toaster {
    public static void showToastOnMainThread(final Context context, final String message, final int duration){

            Handler handler = new Handler(context.getApplicationContext().getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context.getApplicationContext(), message, duration).show();
                }
            });
    }
}
