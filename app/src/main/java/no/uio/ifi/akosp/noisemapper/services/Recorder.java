package no.uio.ifi.akosp.noisemapper.services;

import android.content.Context;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.UUID;

import no.uio.ifi.akosp.noisemapper.Utils;


/**
 * Created on 2016.08.28..
 *
 * @author Ákos Pap
 */
public class Recorder implements Runnable {

    public static final String TAG = "Recorder";

    protected final Context context;
    private final UUID uuid;
    protected final int durationMs;

    protected MediaRecorder recorder;
    protected volatile boolean isRecording = false;
    protected File outFile;

    public Recorder(Context context, UUID uuid, int duration) {
        this.context = context;
        this.uuid = uuid;
        this.durationMs = duration;
    }

    @Override
    public void run() {
        // Send signal `BeforeRecordingStart`
        Signals.sendBeforeRecordingStart(context, uuid, durationMs);

        recorder = new MediaRecorder();

        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        outFile = getOutFile();
        recorder.setOutputFile(outFile.getPath());
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
//        recorder.setMaxDuration(durationMs);
        recorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {
            @Override
            public void onInfo(MediaRecorder mr, int what, int extra) {
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                    isRecording = false;
                }
            }
        });

        try {
            recorder.prepare();
            recorder.start();
            isRecording = true;
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            Log.e(TAG, "prepare() failed");
        }

        if (!isRecording) {
            Signals.sendStartRecordingFailed(context, uuid);
            return;
        }

        // Send signal `RecordingStarted`
        Signals.sendRecordingStarted(context, uuid, outFile, durationMs);

        long startTime = System.currentTimeMillis();
        long elapsedTime = System.currentTimeMillis() - startTime;
        Log.d(TAG, "Time left from waiting: " + elapsedTime + " ms");
        while (isRecording &&  elapsedTime < durationMs) {
            try {
                Thread.sleep(durationMs / 20 + 1);  // +1 to ensure it is > 0
            } catch (InterruptedException e) {
                Log.d(TAG, "Interrupted, preferably because recording has finished.");
            }
            elapsedTime = System.currentTimeMillis() - startTime;
            Log.d(TAG, "Time left from waiting: " + elapsedTime + " ms");
        }
        elapsedTime = System.currentTimeMillis() - startTime;

        try {
            recorder.stop();
        } catch (Exception e) {
            Log.w(TAG, "Error when trying to stop MediaRecorder.", e);
        } finally {
            recorder.release();
            recorder = null;
            isRecording = false;
        }

        // Send signal: `RecordingStopped`
        Signals.sendRecordingStopped(context, uuid, outFile, (int) elapsedTime);

        // End of thread execution
    }

    private File getOutFile() {
        final String filename = "record_" + Utils.FILENAME_FORMATTER.format(new Date()) + ".3gp";
        return new File(context.getDir("recordings", Context.MODE_PRIVATE), filename);
    }
}
