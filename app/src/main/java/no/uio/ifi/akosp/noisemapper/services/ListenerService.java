package no.uio.ifi.akosp.noisemapper.services;

import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import no.uio.ifi.akosp.noisemapper.R;
import no.uio.ifi.akosp.noisemapper.Utils;
import no.uio.ifi.akosp.noisemapper.model.DaoSession;
import no.uio.ifi.akosp.noisemapper.model.Record;
import no.uio.ifi.akosp.noisemapper.model.State;
import no.uio.ifi.akosp.noisemapper.ui.MainActivity;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class ListenerService extends Service
        implements PhoneStateService.PhoneStateRequestListener,
                   TempRecordingStorage.RecordingReadyHandler {

    public static final String TAG = "ListenerService";

    public static final String SERVICE_ID = ListenerService.class.getSimpleName();

    private static final String ACTION_START_LISTENING = "ListenerService::StartListening";
    private static final String ACTION_STOP_LISTENING = "ListenerService::StopListening";

    private volatile int recordingDurationMs = 5000;
    private volatile int repeatIntervalSec = 10;

    private final Handler handler;
    private Runnable recurringRecorderRunnable = new Runnable() {
        @Override
        public void run() {
            UUID uuid = UUID.randomUUID();
            new Thread(new Recorder(getApplicationContext(), uuid, recordingDurationMs)).start();
            handler.postDelayed(recurringRecorderRunnable, repeatIntervalSec * 1000);
        }
    };
    @SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
    private List<BroadcastReceiver> signalProcessors = new ArrayList<BroadcastReceiver>(
//            Arrays.asList(
//                    new CopyToPublicProcessor()
//            )
    );

    private TempRecordingStorage tempRecordingStorage = new TempRecordingStorage(this);

    private SharedPreferences.OnSharedPreferenceChangeListener timingPrefsChanged = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            Log.d(TAG, String.format("Preference %s changed", key));
            if (! ("repeat_interval".equals(key) || "record_duration".equals(key))) {
                return;
            }

            handler.removeCallbacks(recurringRecorderRunnable);

            initializeTimingValuesFromPreferences(sharedPreferences);

            Log.d(TAG, String.format("Preferences changed. New values are " +
                    "repeat_interval=%d s, record_duration=%d ms",
                    repeatIntervalSec, recordingDurationMs));

            handler.post(recurringRecorderRunnable);
        }
    };
    private PowerManager.WakeLock wakeLock;


    private BroadcastReceiver recordingStartedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Signals.ACTION_RECORDING_STARTED.equals(intent.getAction())) {
                Log.d(TAG, String.format("Received %s with extras: %s",
                        intent.getAction(), intent.getExtras()));
                UUID uuid = (UUID) intent.getSerializableExtra(Signals.EXTRA_UUID);
                psService.requestPhoneState(
                        new PhoneStateService.PhoneStateRequest(uuid, ListenerService.this)
                );
            }
        }
    };

    private BroadcastReceiver recordingStoppedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Signals.ACTION_RECORDING_STOPPED.equals(intent.getAction())) {
                Log.d(TAG, String.format("Received %s with extras: %s",
                        intent.getAction(), intent.getExtras()));
                UUID uuid = (UUID) intent.getSerializableExtra(Signals.EXTRA_UUID);
                File file = (File) intent.getSerializableExtra(Signals.EXTRA_FILE);
                tempRecordingStorage.addFile(uuid, file);
            }
        }
    };

    private void initializeTimingValuesFromPreferences(SharedPreferences sharedPreferences) {
        // Necessary to parse, as EditTextPreference always stores Strings.
        recordingDurationMs = Integer.parseInt(sharedPreferences.getString("record_duration", "5")) * 1000;
        repeatIntervalSec = Integer.parseInt(sharedPreferences.getString("repeat_interval", "60"));
    }

//    // TODO: Rename parameters
//    private static final String EXTRA_PARAM1 = "ListenerService::PARAM1";
//    private static final String EXTRA_PARAM2 = "ListenerService::PARAM2";

    public ListenerService() {
        handler = new Handler();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Intent intent = new Intent(this, PhoneStateService.class);
        bindService(intent, psConnection, Context.BIND_AUTO_CREATE);
    }

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    public static void startListening(Context context, String param1, String param2) {
        Intent intent = new Intent(context, ListenerService.class);
        intent.setAction(ACTION_START_LISTENING);
//        intent.putExtra(EXTRA_PARAM1, param1);
//        intent.putExtra(EXTRA_PARAM2, param2);
        context.startService(intent);
        Log.i(TAG, "Started service");
    }

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    public static void stopListening(Context context, String param1, String param2) {
        Intent intent = new Intent(context, ListenerService.class);
        intent.setAction(ACTION_STOP_LISTENING);
//        intent.putExtra(EXTRA_PARAM1, param1);
//        intent.putExtra(EXTRA_PARAM2, param2);
        context.startService(intent);
        Log.i(TAG, "Stopped service");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_START_LISTENING.equals(action)) {
//                final String param1 = intent.getStringExtra(EXTRA_PARAM1);
//                final String param2 = intent.getStringExtra(EXTRA_PARAM2);
//                handleActionFoo(param1, param2);
                handleStartListening();
            } else if (ACTION_STOP_LISTENING.equals(action)) {
//                final String param1 = intent.getStringExtra(EXTRA_PARAM1);
//                final String param2 = intent.getStringExtra(EXTRA_PARAM2);
                handleStopListening();
            }
        }
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    protected boolean psServiceBound;
    protected PhoneStateService psService;

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
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            psServiceBound = false;
        }
    };

    protected void handleStartListening() {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(getString(R.string.notification_ongoing_title))
                        .setContentText(getString(R.string.notification_ongoing_content));

        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, resultIntent, 0);
        mBuilder.setContentIntent(pendingIntent);
        Notification notification = mBuilder.build();
        Log.d(TAG, "Notification for foreground: " + notification);
        startForeground(42, notification);

        final LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(getApplicationContext());
        final IntentFilter filter = new IntentFilter();
        filter.addAction(Signals.ACTION_BEFORE_RECORDING_START);
        filter.addAction(Signals.ACTION_RECORDING_STARTED);
        filter.addAction(Signals.ACTION_RECORDING_STOPPED);
        filter.addAction(Signals.ACTION_START_RECORDING_FAILED);
        for (BroadcastReceiver receiver : signalProcessors) {
            lbm.registerReceiver(receiver, filter);
        }

        lbm.registerReceiver(recordingStartedReceiver, new IntentFilter(Signals.ACTION_RECORDING_STARTED));
        lbm.registerReceiver(recordingStoppedReceiver, new IntentFilter(Signals.ACTION_RECORDING_STOPPED));

        PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                .registerOnSharedPreferenceChangeListener(timingPrefsChanged);

        initializeTimingValuesFromPreferences(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));

        handler.post(recurringRecorderRunnable);

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NoiseMapper::ListenerService.wakeLock");
        wakeLock.acquire();
    }

    protected void handleStopListening() {

        wakeLock.release();

        stopForeground(true);

        handler.removeCallbacks(recurringRecorderRunnable);
        Log.i(TAG, "Removed handler callbacks, stopping service");

        final LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(getApplicationContext());
        for (BroadcastReceiver receiver : signalProcessors) {
            lbm.unregisterReceiver(receiver);
        }

        lbm.unregisterReceiver(recordingStartedReceiver);
        lbm.unregisterReceiver(recordingStoppedReceiver);

        PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                .unregisterOnSharedPreferenceChangeListener(timingPrefsChanged);

        if (psServiceBound) {
            unbindService(psConnection);
            psServiceBound = false;
        }

        stopSelf();
    }


    @Override
    public void onStateAvailable(UUID uuid, State state) {
        tempRecordingStorage.addPhoneState(uuid, state);
    }

    @Override
    public void handleRecordingReady(UUID uuid, File file, State phoneState) {
        Record record = new Record();
        record.setFilename(file.getAbsolutePath());
        record.setState(phoneState);
        record.setTimestamp(new Date(file.lastModified()));

        DaoSession session = Utils.getDaoSession(getApplicationContext());
        session.getRecordDao().save(record);
        Log.i(TAG, String.format("Saved record with id=%s", record.getId().toString()));
    }


}
