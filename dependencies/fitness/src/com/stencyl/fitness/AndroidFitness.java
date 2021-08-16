package com.stencyl.fitness;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.haxe.extension.Extension;
import org.haxe.lime.HaxeObject;

public class AndroidFitness extends Extension
{
    private static String publicKey = "";
    private static HaxeObject callback = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @SuppressWarnings("unused")
    public static void initializeFromHaxe(String stringArg, HaxeObject callback) {
        AndroidFitness.publicKey = publicKey;
        AndroidFitness.callback = callback;
    }

    @SuppressWarnings("unused")
    public static void checkPermission(String stringArg, HaxeObject callback) {
        if(ContextCompat.checkSelfPermission(mainActivity,
                Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(mainActivity,
                    new String[] {Manifest.permission.ACTIVITY_RECOGNITION},
                    MY_PERMISSIONS_REQUEST_ACTIVITY_RECOGNITION);
        }
        AndroidFitness.publicKey = publicKey;
        AndroidFitness.callback = callback;
    }

    public void someJavaFunctionWithResult() {
        haxeCallback("onCallbackName", new Object[] { "Callback Return Value" });
    }

    private static void haxeCallback(String function, Object[] args) {
        Extension.callbackHandler.post (() -> AndroidFitness.callback.call (function, args));
    }
}