::if SET_FITNESS_BOOT_STEP_SUBSCRIBE::
package com.polydes.fitness;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.DataType;

public class BootBroadcastReceiver extends BroadcastReceiver
{
    private static final String TAG = "Fitness";

    @Override
    public void onReceive(Context context, Intent intent)
    {
        String action = intent.getAction();
        Log.d(TAG, action);
        if(Intent.ACTION_BOOT_COMPLETED.equals(action))
        {
            if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q)
            {
                int permissionResult = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION);
                if(permissionResult != PackageManager.PERMISSION_GRANTED)
                {
                    Log.d(TAG, "Activity recognition permission not granted.");
                    return;
                }
            }

            GoogleSignInAccount signin = GoogleSignIn.getLastSignedInAccount(context);
            if(signin == null)
            {
                Log.d(TAG, "Not signed in.");
                return;
            }

            FitnessOptions fitnessOptions = FitnessOptions.builder()
                .addDataType(DataType.TYPE_STEP_COUNT_CUMULATIVE)
                .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.TYPE_LOCATION_SAMPLE)
                .build();

            if (!GoogleSignIn.hasPermissions(signin, fitnessOptions))
            {
                Log.d(TAG, "Account hasn't granted fitness permissions.");
                return;
            }

            Fitness.getRecordingClient(context, signin)
                .subscribe(DataType.TYPE_STEP_COUNT_CUMULATIVE)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.i(TAG, "Successfully subscribed!");
                    } else {
                        Log.w(TAG, "There was a problem subscribing.", task.getException());
                    }
                });
        }
    }
}
::end::