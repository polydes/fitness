package com.stencyl.fitness;

import static android.app.Activity.RESULT_OK;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.Value;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.request.DataSourcesRequest;
import com.google.android.gms.fitness.request.OnDataPointListener;
import com.google.android.gms.fitness.request.SensorRequest;
import com.google.android.gms.fitness.result.DataReadResponse;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;

import org.haxe.extension.Extension;
import org.haxe.lime.HaxeObject;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class AndroidFitness extends Extension
{
    private static final String TAG = "Fitness";

    private static int nextRequestID = 0;
    private static final SparseArray<FitAuthenticatedAction> fitActionRequests = new SparseArray<>();

    private static class FitAuthenticatedAction
    {
        int requestID;
        FitAction action;

        public FitAuthenticatedAction(FitAction action)
        {
            this.requestID = nextRequestID++;
            this.action = action;
        }

        void performAction()
        {
            fitActionRequests.remove(requestID);
            action.performAction();
        }
    }

    private interface FitAction
    {
        void performAction();
    }

    private static AndroidFitness instance;
    public static HaxeObject callback = null;

    // Need to hold a reference to this listener, as it's passed into the "unregister"
    // method in order to stop all sensors from sending data to this listener.
    private OnDataPointListener dataPointListener = null;

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
    public static void trySubscribeToStepRecording() {

        instance.checkPermissionsAndRun(() -> {
            Fitness.getRecordingClient(mainActivity, instance.getGoogleAccount())
                    .subscribe(DataType.TYPE_STEP_COUNT_CUMULATIVE)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.i(TAG, "Successfully subscribed!");
                        } else {
                            Log.w(TAG, "There was a problem subscribing.", task.getException());
                        }
                    });
        });
    }

    //Read history data

    @SuppressWarnings("unused")
    public static void tryReadStepHistoryData(int startTimeSeconds, int endTimeSeconds, HaxeObject callback) {

        instance.checkPermissionsAndRun(() -> {
            Fitness.getHistoryClient(mainActivity, instance.getGoogleAccount())
                    .readData(new DataReadRequest.Builder()
                            .read(DataType.TYPE_STEP_COUNT_DELTA)
                            .setTimeRange(startTimeSeconds, endTimeSeconds, TimeUnit.SECONDS)
                            .build())
                    .addOnSuccessListener((dataReadResponse) -> {
                        long stepsTaken = 0L;
                        for(DataSet dataSet : dataReadResponse.getDataSets())
                            if(!dataSet.isEmpty())
                                stepsTaken += dataSet.getDataPoints().get(0).getValue(Field.FIELD_STEPS).asInt();
                        final long stepsTakenResult = stepsTaken;
                        
                        Extension.callbackHandler.post (() -> callback.call ("stepsTaken", new Object[] {stepsTakenResult}));
                        Log.i(TAG, "Total steps taken over queried history: " + stepsTaken);
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "There was a problem reading the requested data.", e);
                    });
        });

    }

    //register for listening to step sensor

    @SuppressWarnings("unused")
    public static void tryRegisterStepSensorListener(int samplingRate, HaxeObject callback) {

        Log.i(TAG, "Called tryRegisterStepSensorListener");

        instance.checkPermissionsAndRun(() -> {

            if(instance.dataPointListener != null)
            {
                instance.unregisterFitnessDataListener();
            }

            // Note: Fitness.SensorsApi.findDataSources() requires the ACCESS_FINE_LOCATION permission.
            Fitness.getSensorsClient(mainActivity, instance.getGoogleAccount())
                    .findDataSources(new DataSourcesRequest.Builder()
                            .setDataTypes(DataType.TYPE_STEP_COUNT_DELTA)
                            //.setDataSourceTypes(DataSource.TYPE_RAW)
                            .build())
                    .addOnSuccessListener((dataSources) -> {
                        for (DataSource dataSource : dataSources) {
                            Log.i(TAG, String.format("Data source found: %s", dataSource.toString()));
                            Log.i(TAG, String.format("Data source type: %s", dataSource.getDataType().getName()));
                            // Let's register a listener to receive Activity data!
                            if (dataSource.getDataType().equals(DataType.TYPE_STEP_COUNT_DELTA) && instance.dataPointListener == null) {
                                Log.i(TAG, "Data source for STEP_COUNT_CUMULATIVE found!  Registering.");
                                instance.registerFitnessDataListener(dataSource, DataType.TYPE_STEP_COUNT_DELTA, samplingRate, callback);
                            }
                        }
                    })
                    .addOnFailureListener((e) -> Log.e(TAG, "failed", e));
        });
    }

    @SuppressWarnings("unused")
    public static void tryUnregisterStepSensorListener() {
        instance.checkPermissionsAndRun(() -> {
            instance.unregisterFitnessDataListener();
        });
    }

    private void registerFitnessDataListener(DataSource dataSource, DataType dataType, int samplingRate, HaxeObject callback) {
        dataPointListener = (@NonNull DataPoint dataPoint) -> {
            for (Field field : dataPoint.getDataType().getFields()) {
                Value value = dataPoint.getValue(field);
                Log.i(TAG, String.format("Detected DataPoint field: %s", field.getName()));
                Log.i(TAG, String.format("Detected DataPoint value: %s", value.toString()));
                if(field.getName().equals("steps"))
                {
                    Extension.callbackHandler.post (() -> callback.call ("stepsTaken", new Object[] {value.asInt()}));
                }
            }
        };
        Fitness.getSensorsClient(mainActivity, getGoogleAccount())
                .add(
                        new SensorRequest.Builder()
                                .setDataSource(dataSource) // Optional but recommended for custom data sets.
                                .setDataType(dataType) // Can't be omitted.
                                .setSamplingRate(samplingRate, TimeUnit.SECONDS)
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

    //---

    @SuppressWarnings("unused")
    public static boolean allPermissionsApproved() {
        return instance.permissionApproved() && instance.oAuthPermissionsApproved();
    }

    @SuppressWarnings("unused")
    public static void requestPermissions() {
        Log.i(TAG, "Try to request");
        instance.checkPermissionsAndRun(() -> {}); //do nothing
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

    @SuppressWarnings("unused")
    public static int currentTime() {
        return (int) (System.currentTimeMillis() / 1000L);
    }

    public void checkPermissionsAndRun(FitAction action) {
        FitAuthenticatedAction authAction = new FitAuthenticatedAction(action);
        if (permissionApproved())
        {
            fitSignIn(authAction);
        }
        else
        {
            requestRuntimePermissions(authAction);
        }
    }

    public void someJavaFunctionWithResult() {
        haxeCallback("onCallbackName", new Object[] { "Callback Return Value" });
    }

    static void haxeCallback(String function, Object[] args) {
        Extension.callbackHandler.post (() -> AndroidFitness.callback.call (function, args));
    }

    //

    private FitnessOptions fitnessOptions = FitnessOptions.builder()
            .addDataType(DataType.TYPE_STEP_COUNT_CUMULATIVE)
            .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.TYPE_LOCATION_SAMPLE)
            .build();

    /**
     * Checks that the user is signed in, and if so, executes the specified function. If the user is
     * not signed in, initiates the sign in flow, specifying the post-sign in function to execute.
     *
     * @param action The action to perform after sign in.
     */
    private void fitSignIn(FitAuthenticatedAction action)
    {
        if (oAuthPermissionsApproved())
        {
            action.performAction();
        }
        else
        {
            GoogleSignIn.requestPermissions(
                    mainActivity,
                    action.requestID,
                    getGoogleAccount(), fitnessOptions);
        }
    }

    /**
     * Handles the callback from the OAuth sign in flow, executing the post sign in function
     */
    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == RESULT_OK) {
            FitAuthenticatedAction postSignInAction = fitActionRequests.get(requestCode);
            postSignInAction.performAction();
        }
        else
        {
            oAuthErrorMsg(requestCode, resultCode);
        }
        return true;
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

    //fitSignIn(FitActionRequestCode.READ_DATA);

    //read from lastTime until start of today, if it's a previous day
    //this is added to an accumulator

    //read daily total
    //this is added to an accumulator and marked


    //newSteps = steps from (lastTimeRecorded, now)
    //stepsTotal += newSteps;
    //lastTimeRecorded = now;
    //
    //every x seconds, stepsTotal += stepDelta;
    //lastTimeRecorded = now;

    private boolean permissionApproved()
    {
        if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q)
            return true;

        return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                    mainActivity,
                    Manifest.permission.ACTIVITY_RECOGNITION);
    }

    private void requestRuntimePermissions(FitAuthenticatedAction action)
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
                    action.requestID);
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
            FitAuthenticatedAction fitAction = fitActionRequests.get(requestCode);
            fitSignIn(fitAction);
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