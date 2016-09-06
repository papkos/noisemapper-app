package no.uio.ifi.akosp.noisemapper.services;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.File;
import java.util.Locale;

/**
 * Created on 2016.08.28..
 *
 * @author Ákos Pap
 */
public class Signals {

    public static final String TAG = "Signals";
    public static final String ACTION_BEFORE_RECORDING_START = "Signals::BeforeRecordingStart";
    public static final String ACTION_RECORDING_STARTED = "Signals::RecordingStarted";
    public static final String ACTION_RECORDING_STOPPED = "Signals::RecordingStopped";
    public static final String ACTION_START_RECORDING_FAILED = "Signals::StartRecordingFailed";

    public static final String EXTRA_DURATION_MS = "Signals::durationMs";
    public static final String EXTRA_FILE = "Signals::file";

    public static void sendBeforeRecordingStart(Context context, int expectedDurationMs) {
        Intent i = new Intent();
        i.setAction(ACTION_BEFORE_RECORDING_START);
        i.putExtra(EXTRA_DURATION_MS, expectedDurationMs);
        LocalBroadcastManager.getInstance(context).sendBroadcast(i);

        Log.i(TAG, String.format(Locale.getDefault(),
                "Sending 'BeforeRecordingStart' signal [%s]",
                i.getExtras().toString()));
    }

    public static void sendRecordingStarted(Context context, File targetFile, int expectedDurationMs) {
        Intent i = new Intent();
        i.setAction(ACTION_RECORDING_STARTED);
        i.putExtra(EXTRA_FILE, targetFile);
        i.putExtra(EXTRA_DURATION_MS, expectedDurationMs);
        LocalBroadcastManager.getInstance(context).sendBroadcast(i);

        Log.i(TAG, String.format(Locale.getDefault(),
                "Sending 'RecordingStarted' signal [%s]",
                i.getExtras().toString()));
    }

    public static void sendRecordingStopped(Context context, File file, int actualDuration) {
        Intent i = new Intent();
        i.setAction(ACTION_RECORDING_STOPPED);
        i.putExtra(EXTRA_FILE, file);
        i.putExtra(EXTRA_DURATION_MS, actualDuration);
        LocalBroadcastManager.getInstance(context).sendBroadcast(i);

        Log.i(TAG, String.format(Locale.getDefault(),
                "Sending 'RecordingStopped' signal [%s]",
                i.getExtras().toString()));
    }

    public static void sendStartRecordingFailed(Context context) {
        Intent i = new Intent();
        i.setAction(ACTION_START_RECORDING_FAILED);
        LocalBroadcastManager.getInstance(context).sendBroadcast(i);

        Log.i(TAG, String.format(Locale.getDefault(),
                "Sending 'StartRecordingFailed' signal [%s]",
                i.getExtras().toString()));
    }
}
