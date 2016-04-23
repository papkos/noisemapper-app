package no.uio.ifi.akosp.noisemapper.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import no.uio.ifi.akosp.noisemapper.R;
import no.uio.ifi.akosp.noisemapper.model.State;
import no.uio.ifi.akosp.noisemapper.services.PhoneStateService;

public class MainActivity extends AppCompatActivity implements PhoneStateService.PhoneStateRequestListener {

    public static final String TAG = "MainActivity";
    protected boolean psServiceBound;
    protected PhoneStateService psService;

    private PhoneStatusView phoneStatusView;

    protected Handler handler = new Handler();

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection psConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            Log.d(TAG, "Bound to service");
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            PhoneStateService.PSSBinder binder = (PhoneStateService.PSSBinder) service;
            psService = binder.getService();
            psServiceBound = true;

            handler.post(requester);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            psServiceBound = false;
        }
    };

    protected Runnable requester = new Runnable() {
        @Override
        public void run() {
            if (!psServiceBound) return;

            psService.requestPhoneState(MainActivity.this);
            handler.postDelayed(requester, 500);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        phoneStatusView = (PhoneStatusView) findViewById(R.id.appStatusView);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Bind to LocalService
        Intent intent = new Intent(this, PhoneStateService.class);
        bindService(intent, psConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (psServiceBound) {
            unbindService(psConnection);
            psServiceBound = false;
        }
    }

    @Override
    public void onStateAvailable(State state) {
        phoneStatusView.setState(state);
    }
}
