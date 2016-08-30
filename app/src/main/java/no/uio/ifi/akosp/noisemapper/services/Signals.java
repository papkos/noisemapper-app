package no.uio.ifi.akosp.noisemapper.services;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import java.util.Locale;

/**
 * Created on 2016.08.28..
 *
 * @author Ákos Pap
 */
public class Signals {

    public static final String TAG = "Signals";

    public static void sendBeforeRecordingStart(Context context, Bundle args) {
        Log.i(TAG, String.format(Locale.getDefault(),
                "Sending 'BeforeRecordingStart' signal [%s]",
                args.toString()));
    }

    public static void sendRecordingStarted(Context context, Bundle args) {
        Log.i(TAG, String.format(Locale.getDefault(),
                "Sending 'RecordingStarted' signal [%s]",
                args.toString()));
    }

    public static void sendRecordingStopped(Context context, Bundle args) {
        Log.i(TAG, String.format(Locale.getDefault(),
                "Sending 'RecordingStopped' signal [%s]",
                args.toString()));
    }

    public static void sendStartRecordingFailed(Context context, Bundle args) {
        Log.i(TAG, String.format(Locale.getDefault(),
                "Sending 'StartRecordingFailed' signal [%s]",
                args.toString()));
    }
}
