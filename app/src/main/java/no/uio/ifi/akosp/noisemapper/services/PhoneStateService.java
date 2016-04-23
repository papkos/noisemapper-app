package no.uio.ifi.akosp.noisemapper.services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import no.uio.ifi.akosp.noisemapper.Utils;
import no.uio.ifi.akosp.noisemapper.model.InCallState;
import no.uio.ifi.akosp.noisemapper.model.Orientation;
import no.uio.ifi.akosp.noisemapper.model.State;

/**
 * Created on 2016.04.23..
 *
 * @author Ákos Pap
 */
public class PhoneStateService extends Service {

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

    protected Orientation orientation = new Orientation(0, 0, 0);
    protected float proximity = 0;
    protected float light = 0;
    protected InCallState inCallState = InCallState.NO_CALL;

    protected boolean[] dataAvailable = new boolean[4];

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
                    dataAvailable(0);
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    };

    protected SensorEventListener proximityListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
                proximity = event.values[0];
                dataAvailable(1);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    };

    protected SensorEventListener lightSensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
                light = event.values[0];
                dataAvailable(2);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    };

    protected BroadcastReceiver callStateListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(intent.getAction())) {
                String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
                if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
                    inCallState = InCallState.RINGING;
                } else if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state)) {
                    inCallState = InCallState.IN_CALL;
                } else {
                    inCallState = InCallState.NO_CALL;
                }
                dataAvailable(3);
            }
        }
    };
    private List<PhoneStateRequestListener> listeners =
            Collections.synchronizedList(new ArrayList<PhoneStateRequestListener>());

    @Override
    public void onCreate() {
        super.onCreate();
        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);


        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        int state = telephonyManager.getCallState();
        if (TelephonyManager.CALL_STATE_RINGING == state) {
            inCallState = InCallState.RINGING;
        } else if (TelephonyManager.CALL_STATE_OFFHOOK == state) {
            inCallState = InCallState.IN_CALL;
        } else {
            inCallState = InCallState.NO_CALL;
        }
        dataAvailable[3] = true;
    }

    protected void dataAvailable(int dataType) {
        dataAvailable[dataType] = true;

        for (boolean b : dataAvailable) {
            if (!b) {
                return;
            }
        }

        State state = new State(orientation, proximity, makeProximityText(proximity), light,
                Utils.isInPocket(orientation, proximity, light), inCallState, new Date());

        if (! listeners.isEmpty()) {
            Iterator<PhoneStateRequestListener> it = listeners.iterator();
            while (it.hasNext()) {
                PhoneStateRequestListener listener = it.next();
                listener.onStateAvailable(state);
                it.remove();
            }
        }

        if (listeners.isEmpty()) {
            unregisterFromEvents();
        }
    }

    private String makeProximityText(float proximity) {
        return String.format(Locale.US, "%s (%.1f cm)",
                (proximity < proximitySensor.getMaximumRange() ? "near" : "far"),
                proximity
        );
    }

    public void requestPhoneState(PhoneStateRequestListener listener) {
        listeners.add(listener);
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
    }

    private void registerEvents() {
        sensorManager.registerListener(orientationListener, accelerometer, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(orientationListener, magnetometer, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(proximityListener, proximitySensor, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(lightSensorListener, lightSensor, SensorManager.SENSOR_DELAY_UI);
        registerReceiver(callStateListener, new IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED));
    }

    private void unregisterFromEvents() {
        sensorManager.unregisterListener(orientationListener);
        sensorManager.unregisterListener(proximityListener);
        sensorManager.unregisterListener(lightSensorListener);
        unregisterReceiver(callStateListener);
    }

    public class PSSBinder extends Binder {
        public PhoneStateService getService() {
            // Return this instance of LocalService so clients can call public methods
            return PhoneStateService.this;
        }
    }

    public interface PhoneStateRequestListener {
        void onStateAvailable(State state);
    }
}
