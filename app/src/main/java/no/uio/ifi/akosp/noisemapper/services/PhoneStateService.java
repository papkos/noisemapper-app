package no.uio.ifi.akosp.noisemapper.services;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import no.uio.ifi.akosp.noisemapper.Utils;
import no.uio.ifi.akosp.noisemapper.model.InCallState;
import no.uio.ifi.akosp.noisemapper.model.Orientation;
import no.uio.ifi.akosp.noisemapper.model.State;

/**
 * Created on 2016.04.23..
 *
 * @author Ákos Pap
 */
public class PhoneStateService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    public static final String TAG = "PhoneStateService";

    // Binder given to clients
    private final IBinder mBinder = new PSSBinder();

    protected SensorManager sensorManager;
    protected Sensor accelerometer;
    protected Sensor magnetometer;
    protected Sensor proximitySensor;
    protected Sensor lightSensor;
    float[] mGravity;
    float[] mGeomagnetic;
    boolean receiversRegistered = false;

    protected Orientation orientation = new Orientation(0, 0, 0);
    protected float proximity = 0;
    protected float light = 0;
    protected InCallState inCallState = InCallState.NO_CALL;

    protected boolean[] dataAvailable = new boolean[5];
    private final Object dataAvailableLock = new Object();

    protected SensorEventListener orientationListener = new SensorEventListener() {
        /* Based on http://www.codingforandroid.com/2011/01/using-orientation-sensors-simple.html */
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
                mGravity = event.values;
            if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
                mGeomagnetic = event.values;
            if (mGravity != null && mGeomagnetic != null) {
                float R[] = new float[9];
                float I[] = new float[9];
                boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
                if (success) {
                    float values[] = new float[3];
                    SensorManager.getOrientation(R, values);
                    orientation = new Orientation(values[0], values[1], values[2]);
                    Log.d(TAG, "Incoming orientation sensor reading");
                    dataAvailable(0);
                    sensorManager.unregisterListener(this);
                } else {
                    Log.w(TAG, "SensorManager.getRotationMatrix() unsuccessful.");
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    protected SensorEventListener proximityListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
                Log.d(TAG, "Incoming proximity sensor reading");
                proximity = event.values[0];
                dataAvailable(1);
                sensorManager.unregisterListener(this);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    protected SensorEventListener lightSensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
                light = event.values[0];
                dataAvailable(2);
                sensorManager.unregisterListener(this);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    protected void fetchPhoneState() {
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        int state = telephonyManager.getCallState();
        handlePhoneState(state);
    }

    /**
     * When explicitly polling the the {@link TelephonyManager} using
     * {@link TelephonyManager#getCallState()}
     * @param state A {@link TelephonyManager} call state.
     */
    protected void handlePhoneState(int state) {
        if (TelephonyManager.CALL_STATE_RINGING == state) {
            inCallState = InCallState.RINGING;
        } else if (TelephonyManager.CALL_STATE_OFFHOOK == state) {
            inCallState = InCallState.IN_CALL;
        } else {
            inCallState = InCallState.NO_CALL;
        }
        dataAvailable(3);
    }

    private List<PhoneStateRequest> requests =
            Collections.synchronizedList(new ArrayList<PhoneStateRequest>());

    private GoogleApiClient mGoogleApiClient;
    private Location location;

    @Override
    public void onCreate() {
        super.onCreate();
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        fetchPhoneState();

        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }

    protected void dataAvailable(int dataType) {
        synchronized (dataAvailableLock) {
            dataAvailable[dataType] = true;

            for (boolean b : dataAvailable) {
                if (!b) {
                    return;
                }
            }
            // All good, reset values for the next round
            dataAvailable = new boolean[5];
        }


        State state = new State(orientation, proximity, makeProximityText(proximity), light,
                Utils.isInPocket(orientation, proximity, light), inCallState, location, new Date());

        if (!requests.isEmpty()) {
            Iterator<PhoneStateRequest> it = requests.iterator();
            while (it.hasNext()) {
                PhoneStateRequest request = it.next();
                request.listener.onStateAvailable(request.uuid, state);
                it.remove();
            }
        }

        if (requests.isEmpty()) {
            unregisterFromEvents();
        }
    }

    private String makeProximityText(float proximity) {
        return (proximity < proximitySensor.getMaximumRange() ? "near" : "far");
    }

    public void requestPhoneState(PhoneStateRequest request) {
        requests.add(request);
        registerEvents();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "Binding to service");
        return mBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterFromEvents();
        mGoogleApiClient.disconnect();
    }

    private void registerEvents() {
        if (receiversRegistered) return;

        registerSensors();
        fetchPhoneState();
        if (mGoogleApiClient.isConnected()) {
            fetchLocation();
        } else if (mGoogleApiClient.isConnecting()) {
            /* nothing to do here, wait for successful connection */
        }else {
            mGoogleApiClient.connect();
        }
        receiversRegistered = true;
    }

    private void registerSensors() {
        sensorManager.registerListener(orientationListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(orientationListener, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(proximityListener, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(lightSensorListener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void unregisterFromEvents() {
        if (!receiversRegistered) return;

        sensorManager.unregisterListener(orientationListener);
        sensorManager.unregisterListener(proximityListener);
        sensorManager.unregisterListener(lightSensorListener);
        receiversRegistered = false;
    }

    @Override
    public void onConnected(@Nullable Bundle connectionHint) {
        fetchLocation();
    }

    protected void fetchLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        dataAvailable(4);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    public class PSSBinder extends Binder {
        public PhoneStateService getService() {
            // Return this instance of LocalService so clients can call public methods
            return PhoneStateService.this;
        }
    }

    public interface PhoneStateRequestListener {
        void onStateAvailable(UUID uuid,  State state);
    }

    public static class PhoneStateRequest {
        @Nullable
        public final UUID uuid;
        @NonNull
        public final PhoneStateRequestListener listener;
        @NonNull
        public final Date timestamp;

        public PhoneStateRequest(@NonNull PhoneStateRequestListener listener) {
            this(null, listener, new Date());
        }

        public PhoneStateRequest(UUID uuid, @NonNull PhoneStateRequestListener listener) {
            this(uuid, listener, new Date());
        }

        public PhoneStateRequest(@Nullable UUID uuid, @NonNull PhoneStateRequestListener listener, @NonNull Date timestamp) {
            this.uuid = uuid;
            this.listener = listener;
            this.timestamp = timestamp;
        }
    }
}
