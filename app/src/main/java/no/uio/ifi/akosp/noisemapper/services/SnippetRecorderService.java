package no.uio.ifi.akosp.noisemapper.services;

import android.app.IntentService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import no.uio.ifi.akosp.noisemapper.Utils;
import no.uio.ifi.akosp.noisemapper.model.State;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 */
public class SnippetRecorderService extends RecurringService implements PhoneStateService.PhoneStateRequestListener {
    public static final String TAG = "SnippetRecorderService";

    public static final String SERVICE_ID = SnippetRecorderService.class.getSimpleName();

    public static final String ACTION_START_SNIPPET = "SnippetRecorderService::StartSnippet";
    public static final String EXTRA_FORCE_RUN = "SnippetRecorderService::ForceRun";

    protected static final int SNIPPET_LENGTH = 5000;
    protected static final SimpleDateFormat FILENAME_FORMATTER
            = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);

    protected SnippetRecorderApp app;

    protected File outFolder;

    private Handler handler = new Handler();
    protected State state;
    protected File outFile;
    protected boolean forceRun = false;

    public static Intent startOneOff(Context context) {
        Intent intent = new Intent(context, SnippetRecorderService.class);
        intent.setAction(ACTION_START_SNIPPET);
        intent.putExtra(EXTRA_FORCE_RUN, true);

        context.startService(intent);

        return intent;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        app = (SnippetRecorderApp) getApplication();
        outFolder = app.getOutFolder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_START_SNIPPET.equals(intent.getAction())) {
            if (intent.hasExtra(EXTRA_FORCE_RUN) && intent.getBooleanExtra(EXTRA_FORCE_RUN, false)) {
                this.forceRun = true;
            }
            handleStartSnippet(startId);

            return START_COMMAND_RETURN;
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public String getServiceId() {
        return SERVICE_ID;
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     * @param startId
     */
    private void handleStartSnippet(int startId) {
        if (forceRun || app.isRecurringServiceEnabled(getServiceId())) {
            startRecording();
            handler.postDelayed(stopRecordingAndExit, SNIPPET_LENGTH);
        } else {
            Log.w(TAG, "Service started, but not enabled. Checking status...");
            stopSelf(startId);
            startService(makeCheckIntent(componentName, ACTION_START_SNIPPET));
        }
    }


    protected boolean phoneStateServiceConnected = false;
    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection psConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            Log.d(TAG, "Bound to service");
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            PhoneStateService.PSSBinder binder = (PhoneStateService.PSSBinder) service;
            PhoneStateService psService = binder.getService();
            phoneStateServiceConnected = true;

            psService.requestPhoneState(new PhoneStateService.PhoneStateRequest(SnippetRecorderService.this));
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {}
    };

    private Runnable stopRecordingAndExit = new Runnable() {
        @Override
        public void run() {
            stopRecording();
            doShutdownProtocol();
        }
    };

    protected boolean canStop = false;
    private void doShutdownProtocol() {
        if (! canStop) {
            canStop = true;
            return;
        }

        File log = app.getLogFile();
        FileWriter fw = null;
        try {
            fw = new FileWriter(log, true);
            String logMessage = Utils.stateToJson(state, outFile.getName());
            fw.write(logMessage);
            fw.write("\n");
            fw.flush();
        } catch (IOException e) {
            Log.e(TAG, "Cannot write to log file!", e);
            if (fw != null) {
                try {
                    fw.close();
                } catch (IOException e1) {
                    Log.wtf(TAG, e1);
                }
            }
        }

        Log.i(TAG, "Work is done, stopping service.");
        if (phoneStateServiceConnected) {
            try {
                unbindService(psConnection);
            } catch (Exception e) {
                Log.w(TAG, "Cannot unbind from phoneStateService", e);
            }
        }
        SnippetRecorderService.this.stopSelf();
    }

    private MediaRecorder recorder;
    protected boolean isRecording = false;

    private void startRecording() {
        recorder = new MediaRecorder();


        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        outFile = getOutFile();
        recorder.setOutputFile(outFile.getPath());
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            recorder.prepare();
            recorder.start(); // TODO error code: -38 (something else is using mic?)
            isRecording = true;
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            Log.e(TAG, "prepare() failed");
        }

        if (isRecording) {
            Intent intent = new Intent(this, PhoneStateService.class);
            bindService(intent, psConnection, Context.BIND_AUTO_CREATE);
        }

        Log.i(TAG, "Started recording into " + outFile.getName());
    }

    private File getOutFile() {
        final String filename = "record_" + FILENAME_FORMATTER.format(new Date()) + ".3gp";
        return new File(outFolder, filename);
    }

    private void stopRecording() {
        if (isRecording) {
            recorder.stop();
            Log.i(TAG, "Finished recording.");

            Utils.sendRefreshBroadcast(getApplicationContext());
        } else {
            Log.e(TAG, "Trying to stop recording, but it didn't even start!");
        }

        recorder.release();
        recorder = null;
        isRecording = false;
    }

    @Override
    public void onStateAvailable(UUID uuid, State state) {
        this.state = state;
        doShutdownProtocol();
    }

    public interface SnippetRecorderApp extends RecurringServiceApp {
        File getOutFolder();

        File getLogFile();
    }
}
