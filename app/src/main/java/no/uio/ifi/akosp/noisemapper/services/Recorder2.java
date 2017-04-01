package no.uio.ifi.akosp.noisemapper.services;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.UUID;

import no.uio.ifi.akosp.noisemapper.Utils;

/**
 * Created on 2017.01.24..
 *
 * @author Ákos Pap
 */
public class Recorder2 implements Runnable {

    public static final String TAG = "Recorder2";

    // the audio recording options
    private static final int RECORDING_RATE = 44100;
    private static final int CHANNEL = AudioFormat.CHANNEL_IN_MONO;
    private static final int FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    // the minimum buffer size needed for audio recording
    private static int BUFFER_SIZE = AudioRecord.getMinBufferSize(
            RECORDING_RATE, CHANNEL, FORMAT);

    protected final Context context;
    private final UUID uuid;
    protected final int durationMs;

    protected volatile boolean isRecording = false;
    protected File outFile;
    protected Handler handler = new Handler();

    public Recorder2(Context context, UUID uuid, int duration) {
        this.context = context;
        this.uuid = uuid;
        this.durationMs = duration;
    }

    private Runnable timeIsUp = new Runnable() {
        @Override
        public void run() {
            isRecording = false;
        }
    };


    @Override
    public void run() {
        // Send signal `BeforeRecordingStart`
        Signals.sendBeforeRecordingStart(context, uuid, durationMs);

        File outFile = getOutFile();
        BufferedOutputStream bos = null;
        try {
            bos = new BufferedOutputStream(new FileOutputStream(outFile));

            Log.d(TAG, "Creating the buffer of size " + BUFFER_SIZE);
            byte[] buffer = new byte[BUFFER_SIZE];

            AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    RECORDING_RATE, CHANNEL, FORMAT, BUFFER_SIZE * 10);
            try {
                recorder.startRecording();
                isRecording = true;

                // Send signal `RecordingStarted`
                Signals.sendRecordingStarted(context, uuid, outFile, durationMs);
            } catch (IllegalStateException e) {
                Signals.sendStartRecordingFailed(context, uuid, e);
                return;
            }

            handler.postDelayed(timeIsUp, durationMs);

            while (isRecording) {

                // read the data into the buffer
                int bytesRead = recorder.read(buffer, 0, buffer.length);
                bos.write(buffer, 0, bytesRead);

            }

            recorder.stop();
        } catch (IOException e) {
            Signals.sendStartRecordingFailed(context, uuid);
            e.printStackTrace();
        } finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        // Send signal: `RecordingStopped`
        Signals.sendRecordingStopped(context, uuid, outFile, durationMs);
    }

    private File getOutFile() {
        final String filename = "record_" + Utils.FILENAME_FORMATTER.format(new Date()) + ".pcm";
        return new File(context.getDir("recordings", Context.MODE_PRIVATE), filename);
    }
}
