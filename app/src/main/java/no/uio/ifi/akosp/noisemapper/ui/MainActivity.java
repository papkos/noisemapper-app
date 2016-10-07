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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import java.util.UUID;

import no.uio.ifi.akosp.noisemapper.NoiseMapperApp;
import no.uio.ifi.akosp.noisemapper.R;
import no.uio.ifi.akosp.noisemapper.model.State;
import no.uio.ifi.akosp.noisemapper.services.ListenerService;
import no.uio.ifi.akosp.noisemapper.services.PhoneStateService;
import no.uio.ifi.akosp.noisemapper.services.Recorder;

public class MainActivity extends AppCompatActivity implements PhoneStateService.PhoneStateRequestListener, AppStatusView.AppStatusViewInteractionListener {

    public static final String TAG = "MainActivity";

    protected boolean psServiceBound;
    protected PhoneStateService psService;
    protected NoiseMapperApp app;

    private PhoneStatusView phoneStatusView;
    private AppStatusView appStatusView;

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

            psService.requestPhoneState(new PhoneStateService.PhoneStateRequest(MainActivity.this));
            handler.postDelayed(requester, 500);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        app = (NoiseMapperApp) getApplication();

        phoneStatusView = (PhoneStatusView) findViewById(R.id.phoneStatusView);
        appStatusView = (AppStatusView) findViewById(R.id.appStatusView);
        appStatusView.setCallback(this);
        appStatusView.setSoundServiceEnabled(app.isRecurringServiceEnabled(ListenerService.SERVICE_ID));

        Button oneOff = (Button) findViewById(R.id.oneOff);
        oneOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(new Recorder(MainActivity.this, null, 10*1000)).start();
            }
        });
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
    public void onStateAvailable(UUID uuid, State state) {
        phoneStatusView.setState(state);
    }

    @Override
    public void onSoundServiceSwitchChanged(boolean checked) {
        app.setServiceEnabled(ListenerService.SERVICE_ID, checked);
        if (checked) {
            ListenerService.startListening(this, null, null);
        } else {
            ListenerService.stopListening(this, null, null);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_options, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
