package com.stencyl.fitness;

import static android.app.Activity.RESULT_OK;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.Value;
import com.google.android.gms.fitness.request.DataSourcesRequest;
import com.google.android.gms.fitness.request.OnDataPointListener;
import com.google.android.gms.fitness.request.SensorRequest;

import org.haxe.extension.Extension;
import org.haxe.lime.HaxeObject;

import java.util.concurrent.TimeUnit;

public class AndroidFitness extends Extension
{
    private static final String TAG = "Fitness";

    public enum FitActionRequestCode
    {
        SUBSCRIBE,
        READ_DATA,
        FIND_DATA_SOURCES,
        DO_NOTHING
    }

    private static AndroidFitness instance;
    public static HaxeObject callback = null;


    // Need to hold a reference to this listener, as it's passed into the "unregister"
    // method in order to stop all sensors from sending data to this listener.
    private OnDataPointListener dataPointListener = null;

    private int stepCount = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeLogging();
        instance = this;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @SuppressWarnings("unused")
    public static void initialize(HaxeObject callback) {
        AndroidFitness.callback = callback;
    }

    @SuppressWarnings("unused")
    public static int getSteps() {
        return instance.stepCount;
    }

    @SuppressWarnings("unused")
    public static void recordSteps() {
        Log.i(TAG, "Try to subscribe");
        instance.checkPermissionsAndRun(FitActionRequestCode.SUBSCRIBE);
    }

    @SuppressWarnings("unused")
    public static void updateSteps() {
        //Log.i(TAG, "Try to read");
        //instance.checkPermissionsAndRun(FitActionRequestCode.READ_DATA);
    }

    @SuppressWarnings("unused")
    public static void findDataSources() {
        Log.i(TAG, "Try to find data sources");
        instance.checkPermissionsAndRun(FitActionRequestCode.FIND_DATA_SOURCES);
    }

    @SuppressWarnings("unused")
    public static boolean allPermissionsApproved() {
        return instance.permissionApproved() && instance.oAuthPermissionsApproved();
    }

    @SuppressWarnings("unused")
    public static void requestPermissions() {
        Log.i(TAG, "Try to request");
        instance.checkPermissionsAndRun(FitActionRequestCode.DO_NOTHING);
    }

    @SuppressWarnings("unused")
    public static void rescindPermissions() {
        Log.i(TAG, "Try to rescind");
        Fitness.getConfigClient(mainActivity, GoogleSignIn.getAccountForExtension(mainActivity, instance.fitnessOptions))
                .disableFit()
                .addOnSuccessListener(unused ->
                        Log.i(TAG, "Disabled Google Fit"))
                .addOnFailureListener(e ->
                        Log.w(TAG, "There was an error disabling Google Fit", e));
    }

    public void checkPermissionsAndRun(FitActionRequestCode requestCode) {
        if (permissionApproved())
        {
            fitSignIn(requestCode);
        }
        else
        {
            requestRuntimePermissions(requestCode);
        }
    }

    public void someJavaFunctionWithResult() {
        haxeCallback("onCallbackName", new Object[] { "Callback Return Value" });
    }

    static void haxeCallback(String function, Object[] args) {
        Extension.callbackHandler.post (() -> AndroidFitness.callback.call (function, args));
    }

    //aaa

    private FitnessOptions fitnessOptions = FitnessOptions.builder()
            .addDataType(DataType.TYPE_STEP_COUNT_CUMULATIVE)
            .addDataType(DataType.TYPE_STEP_COUNT_DELTA)
            .addDataType(DataType.TYPE_LOCATION_SAMPLE)
            .build();

    /**
     * Checks that the user is signed in, and if so, executes the specified function. If the user is
     * not signed in, initiates the sign in flow, specifying the post-sign in function to execute.
     *
     * @param requestCode The request code corresponding to the action to perform after sign in.
     */
    private void fitSignIn(FitActionRequestCode requestCode)
    {
        if (oAuthPermissionsApproved())
        {
            performActionForRequestCode(requestCode);
        }
        else
        {
            GoogleSignIn.requestPermissions(
                    mainActivity,
                    requestCode.ordinal(),
                    getGoogleAccount(), fitnessOptions);
        }
    }

    /**
     * Handles the callback from the OAuth sign in flow, executing the post sign in function
     */
    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == RESULT_OK) {
            FitActionRequestCode postSignInAction = FitActionRequestCode.values()[requestCode];
            performActionForRequestCode(postSignInAction);
        }
        else
        {
            oAuthErrorMsg(requestCode, resultCode);
        }
        return true;
    }

    /**
     * Runs the desired method, based on the specified request code. The request code is typically
     * passed to the Fit sign-in flow, and returned with the success callback. This allows the
     * caller to specify which method, post-sign-in, should be called.
     *
     * @param requestCode The code corresponding to the action to perform.
     */
    private void performActionForRequestCode(FitActionRequestCode requestCode)
    {
        switch(requestCode) {
            case READ_DATA:
                readData(); break;
            case SUBSCRIBE:
                subscribe(); break;
            case FIND_DATA_SOURCES:
                fitnessFindDataSources(); break;
        }
    }

    private void oAuthErrorMsg(int requestCode, int resultCode)
    {
        String message =
            "There was an error signing into Fit." + "\n" +
            "Request code was: " + requestCode + "\n" +
            "Result code was: " + resultCode;
        Log.e(TAG, message);
    }

    private boolean oAuthPermissionsApproved()
    {
        return GoogleSignIn.hasPermissions(getGoogleAccount(), fitnessOptions);
    }

    /**
     * Gets a Google account for use in creating the Fitness client. This is achieved by either
     * using the last signed-in account, or if necessary, prompting the user to sign in.
     * `getAccountForExtension` is recommended over `getLastSignedInAccount` as the latter can
     * return `null` if there has been no sign in before.
     */
    private GoogleSignInAccount getGoogleAccount()
    {
        return GoogleSignIn.getAccountForExtension(mainActivity, fitnessOptions);
    }

    /** Records step data by requesting a subscription to background step data.  */
    private void subscribe() {
        // To create a subscription, invoke the Recording API. As soon as the subscription is
        // active, fitness data will start recording.
        Fitness.getRecordingClient(mainActivity, getGoogleAccount())
                .subscribe(DataType.TYPE_STEP_COUNT_CUMULATIVE)
                .addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.i(TAG, "Successfully subscribed!");
            } else {
                Log.w(TAG, "There was a problem subscribing.", task.getException());
            }
        });
    }

    //fitSignIn(FitActionRequestCode.READ_DATA);

    /**
     * Reads the current daily step total, computed from midnight of the current day on the device's
     * current timezone.
     */
    private void readData()
    {
        Fitness.getHistoryClient(mainActivity, getGoogleAccount())
                .readDailyTotal(DataType.TYPE_STEP_COUNT_DELTA)
                .addOnSuccessListener(dataSet -> {
                    stepCount = 0;
                    if(!dataSet.isEmpty())
                        stepCount = dataSet.getDataPoints().get(0).getValue(Field.FIELD_STEPS).asInt();
                    Log.i(TAG, "Total steps: " + stepCount);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "There was a problem getting the step count.", e);
                });
    }

    private void fitnessFindDataSources()
    {
        // Note: Fitness.SensorsApi.findDataSources() requires the ACCESS_FINE_LOCATION permission.
        Fitness.getSensorsClient(mainActivity, getGoogleAccount())
                .findDataSources(
                        new DataSourcesRequest.Builder()
                                .setDataTypes(DataType.TYPE_STEP_COUNT_CUMULATIVE)
                                .setDataSourceTypes(DataSource.TYPE_RAW)
                                .build())
                .addOnSuccessListener((dataSources) -> {
                    for (DataSource dataSource : dataSources) {
                        Log.i(TAG, String.format("Data source found: %s", dataSource.toString()));
                        Log.i(TAG, String.format("Data source type: %s", dataSource.getDataType().getName()));
                        // Let's register a listener to receive Activity data!
                        if (dataSource.getDataType().equals(DataType.TYPE_STEP_COUNT_CUMULATIVE) && dataPointListener == null) {
                            Log.i(TAG, "Data source for STEP_COUNT_CUMULATIVE found!  Registering.");
                            registerFitnessDataListener(dataSource, DataType.TYPE_STEP_COUNT_CUMULATIVE);
                        }
                    }
                })
                .addOnFailureListener((e) -> Log.e(TAG, "failed", e));
    }

    private void registerFitnessDataListener(DataSource dataSource, DataType dataType) {
        dataPointListener = (@NonNull DataPoint dataPoint) -> {
            for (Field field : dataPoint.getDataType().getFields()) {
                Value value = dataPoint.getValue(field);
                Log.i(TAG, String.format("Detected DataPoint field: %s", field.getName()));
                Log.i(TAG, String.format("Detected DataPoint value: %s", value.toString()));

            }
        };
        Fitness.getSensorsClient(mainActivity, getGoogleAccount())
                .add(
                        new SensorRequest.Builder()
                                .setDataSource(dataSource) // Optional but recommended for custom data sets.
                                .setDataType(dataType) // Can't be omitted.
                                .setSamplingRate(1, TimeUnit.SECONDS)
                                .build(),
                        dataPointListener)
                .addOnCompleteListener((task) -> {
                    if (task.isSuccessful()) {
                        Log.i(TAG, "Listener registered!");
                    } else {
                        Log.e(TAG, "Listener not registered.", task.getException());
                    }
                });
    }

    /** Unregisters the listener with the Sensors API.  */
    private void unregisterFitnessDataListener() {
        if (dataPointListener == null) {
            // This code only activates one listener at a time.  If there's no listener, there's
            // nothing to unregister.
            return;
        }

        // Waiting isn't actually necessary as the unregister call will complete regardless,
        // even if called from within onStop, but a callback can still be added in order to
        // inspect the results.
        Fitness.getSensorsClient(mainActivity, getGoogleAccount())
                .remove(dataPointListener)
                .addOnCompleteListener((task) -> {
                    if (task.isSuccessful() && task.getResult()) {
                        Log.i(TAG, "Listener was removed!");
                    } else {
                        Log.i(TAG, "Listener was not removed.");
                    }
                });
    }

    private boolean permissionApproved()
    {
        if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q)
            return true;

        return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                    mainActivity,
                    Manifest.permission.ACTIVITY_RECOGNITION);
    }

    private void requestRuntimePermissions(FitActionRequestCode requestCode)
    {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(mainActivity, Manifest.permission.ACTIVITY_RECOGNITION);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
//            Snackbar.make(
//                    findViewById(R.id.main_activity_view),
//                    R.string.permission_rationale,
//                    Snackbar.LENGTH_INDEFINITE)
//                    .setAction(R.string.ok) {
//                // Request permission
//                ActivityCompat.requestPermissions(this,
//                        arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
//                        requestCode.ordinal)
//            }
//                    .show()
        } else {
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(mainActivity,
                    new String[] {Manifest.permission.ACTIVITY_RECOGNITION},
                    requestCode.ordinal());
        }
    }

    @Override
    public boolean onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        if(grantResults.length == 0)
        {
            // If user interaction was interrupted, the permission request
            // is cancelled and you receive empty arrays.
        }
        else if(grantResults[0] == PackageManager.PERMISSION_GRANTED)
        {
            // Permission was granted.
            FitActionRequestCode fitActionRequestCode = FitActionRequestCode.values()[requestCode];
            fitSignIn(fitActionRequestCode);
        }
        else if (grantResults[0] == PackageManager.PERMISSION_DENIED)
        {
            // Permission denied.

            // In this Activity we've chosen to notify the user that they
            // have rejected a core permission for the app since it makes the Activity useless.
            // We're communicating this message in a Snackbar since this is a sample app, but
            // core permissions would typically be best requested during a welcome-screen flow.

            // Additionally, it is important to remember that a permission might have been
            // rejected without asking the user for permission (device policy or "Never ask
            // again" prompts). Therefore, a user interface affordance is typically implemented
            // when permissions are denied. Otherwise, your app could appear unresponsive to
            // touches or interactions which have required permissions.

//            Snackbar.make(
//                    findViewById(R.id.main_activity_view),
//                    R.string.permission_denied_explanation,
//                    Snackbar.LENGTH_INDEFINITE)
//                    .setAction(R.string.settings, ()-> {
//                        // Build intent that displays the App settings screen.
//                        Intent intent = new Intent();
//                        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS;
//                        Uri uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null);
//                        intent.data = uri;
//                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK;
//                        startActivity(intent);
//                    })
//                    .show();
        }

        return true;
    }

    /** Initializes a custom log class that outputs both to in-app targets and logcat.  */
    private void initializeLogging() { // Wraps Android's native log framework.
        LogWrapper logWrapper = new LogWrapper();
        // Using Log, front-end to the logging chain, emulates android.util.log method signatures.
        Log.setLogNode(logWrapper);
        // Filter strips out everything except the message text.
        //MessageOnlyLogFilter msgFilter = new MessageOnlyLogFilter();
        //logWrapper.setNext(msgFilter);
        // Send logs to Haxe
        //msgFilter.setNext(new HaxeTraceLogger());
        logWrapper.setNext(new HaxeTraceLogger());
        Log.i(TAG, "Ready");
    }
}