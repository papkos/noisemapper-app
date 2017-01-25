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

import java.io.File;
import java.util.UUID;

import no.uio.ifi.akosp.noisemapper.NoiseMapperApp;
import no.uio.ifi.akosp.noisemapper.R;
import no.uio.ifi.akosp.noisemapper.Utils;
import no.uio.ifi.akosp.noisemapper.model.DaoSession;
import no.uio.ifi.akosp.noisemapper.model.ProcessedRecordDao;
import no.uio.ifi.akosp.noisemapper.model.RecordDao;
import no.uio.ifi.akosp.noisemapper.model.State;
import no.uio.ifi.akosp.noisemapper.services.ListenerService;
import no.uio.ifi.akosp.noisemapper.services.PhoneStateService;
import no.uio.ifi.akosp.noisemapper.services.ProcessorService2;
import no.uio.ifi.akosp.noisemapper.services.UploaderService;

public class MainActivity extends AppCompatActivity
        implements PhoneStateService.PhoneStateRequestListener,
                   AppStatusView.AppStatusViewInteractionListener,
                   PhoneStatusView.PhoneStatusViewInteractionListener,
                   StatisticsView.StatisticsViewInteractionListener {

    public static final String TAG = "MainActivity";
    private static final int STATISTICS_COLLECTION_INTERVAL_MS = 1 * 1000;

    protected boolean psServiceBound;
    protected PhoneStateService psService;
    protected NoiseMapperApp app;

    private PhoneStatusView phoneStatusView;
    private AppStatusView appStatusView;
    private StatisticsView statisticsView;

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

    protected Runnable statsCollector = new Runnable() {

        @Override
        public void run() {
            StatisticsView.Statistics stats = new StatisticsView.Statistics();
            DaoSession daoSession = Utils.getDaoSession(getApplicationContext());
            stats.records = (int) daoSession.getRecordDao().count();
            stats.unprocessed = (int) daoSession.getRecordDao().queryBuilder().where(RecordDao.Properties.Processed.eq(false)).count();
            stats.notUploaded = (int) daoSession.getProcessedRecordDao().queryBuilder().where(ProcessedRecordDao.Properties.Uploaded.eq(false)).count();

            stats.files = stats.spaceUsed = 0;
            File recDir = getApplicationContext().getDir("recordings", Context.MODE_PRIVATE);
            for (File f : recDir.listFiles()) {
                if (f.exists() && f.isFile()) {
                    stats.files++;
                    stats.spaceUsed += f.length();
                }
            }

            statisticsView.setStatistics(stats);
            handler.postDelayed(statsCollector, STATISTICS_COLLECTION_INTERVAL_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        app = (NoiseMapperApp) getApplication();

        phoneStatusView = (PhoneStatusView) findViewById(R.id.phoneStatusView);
        phoneStatusView.setCallback(this);
        phoneStatusView.setUpdateViewsEnabled(app.isUpdateViewsEnabled());

        appStatusView = (AppStatusView) findViewById(R.id.appStatusView);
        appStatusView.setCallback(this);
        appStatusView.setSoundServiceEnabled(app.isServiceEnabled(ListenerService.SERVICE_ID));

        statisticsView = (StatisticsView) findViewById(R.id.statisticsView);
        statisticsView.setCallback(this);
        handler.post(statsCollector);

    }

    @Override
    protected void onStart() {
        super.onStart();
        // Bind to LocalService
        if (app.isUpdateViewsEnabled()) {
            bindToPSService();
        }
    }

    private void bindToPSService() {
        Intent intent = new Intent(this, PhoneStateService.class);
        bindService(intent, psConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindFromPSService();
    }

    private void unbindFromPSService() {
        if (psServiceBound) {
            handler.removeCallbacks(requester);
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
    public void onUpdateViewSwitchChanged(boolean checked) {
        app.setUpdateViewsEnabled(checked);
        if (checked) {
            bindToPSService();
        } else {
            unbindFromPSService();
        }
    }

    @Override
    public void onRequestProcessAll() {
        ProcessorService2.startProcessingAll(getApplicationContext());
    }

    @Override
    public void onRequestUploadAll() {
        UploaderService.startUploadingAll(getApplicationContext());
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
            case R.id.analytics:
                startActivity(new Intent(this, AnalyticsActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
