package no.uio.ifi.akosp.noisemapper.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import org.json.JSONObject;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.List;

import no.uio.ifi.akosp.noisemapper.Utils;
import no.uio.ifi.akosp.noisemapper.model.DaoSession;
import no.uio.ifi.akosp.noisemapper.model.ProcessedRecord;
import no.uio.ifi.akosp.noisemapper.model.Record;
import no.uio.ifi.akosp.noisemapper.model.RecordDao;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class ProcessorService extends IntentService {

    public static final String TAG = "ProcessorService";

    public static final String ACTION_PROCESS_ALL = "ProcessorService::ProcessAll";
    public static final String ACTION_PROCESS_ONE = "ProcessorService::ProcessOne";

    public static final String EXTRA_RECORD_ID = "ProcessorService::recordId";
    private DaoSession daoSession;

    public ProcessorService() {
        super("ProcessorService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        daoSession = Utils.getDaoSession(getApplicationContext());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        daoSession.getDatabase().close();
    }

    public static void startProcessingAll(Context context) {
        Intent i = new Intent(context, ProcessorService.class);
        i.setAction(ACTION_PROCESS_ALL);
        context.startService(i);
    }

    public static void startProcessingOne(Context context, long id) {
        Intent i = new Intent(context, ProcessorService.class);
        i.setAction(ACTION_PROCESS_ONE);
        i.putExtra(EXTRA_RECORD_ID, id);
        context.startService(i);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_PROCESS_ALL.equals(action)) {
                handleProcessAll();
            } else if (ACTION_PROCESS_ONE.equals(action)) {
                long id = intent.getLongExtra(EXTRA_RECORD_ID, 0);
                handleProcessOne(id);
            }
        }
    }

    private void handleProcessOne(long id) {
        Record record = daoSession.getRecordDao().loadDeep(id);
        processOneAndStore(record);
    }

    private void handleProcessAll() {
        final List<Record> unprocessedRecords = daoSession.queryBuilder(Record.class)
                .where(RecordDao.Properties.Processed.eq(false))
                .list();

        for (Record unprocessedRecord : unprocessedRecords) {
            processOneAndStore(unprocessedRecord);
        }
    }

    private void processOneAndStore(Record record) {
        try {
            String result = process(record);

            record.setProcessed(true);
            daoSession.getRecordDao().save(record);
            Log.i(TAG, String.format("Updated Record with id=%s to processed=true",
                    record.getId().toString()));

            ProcessedRecord pr = new ProcessedRecord();
            pr.setProcessResult(result);
            pr.setState(record.getState());
            pr.setTimestamp(record.getTimestamp());

            daoSession.getProcessedRecordDao().save(pr);
            Log.i(TAG, String.format("Saved ProcessedRecord with id=%s", pr.getId().toString()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String process(Record record) throws Exception {
        final int timeoutUs = 1 * 1000 * 1000;

        final ByteBuffer fullBytes = ByteBuffer.allocate(1*1024*1024);  // 1 MB

        final File file = new File(record.getFilename());
        FileDescriptor fd = new FileInputStream(file).getFD();

        final MediaExtractor extractor = new MediaExtractor();
        extractor.setDataSource(fd);

        Log.d(TAG, String.format("TRACKS #: %d", extractor.getTrackCount()));
        MediaFormat format = extractor.getTrackFormat(0);
        String mime = format.getString(MediaFormat.KEY_MIME);
        Log.d(TAG, String.format("MIME TYPE: %s", mime));
        Log.d(TAG, "MediaFormat: " + format.toString());

        extractor.selectTrack(0); // <= You must select a track. You will read samples from the media from this track!


        MediaCodec codec = MediaCodec.createDecoderByType(mime);
        codec.configure(format, /* surface */ null, /* crypto */ null, /* flags */ 0);
        codec.start();

        boolean sawInputEOS = false;
        boolean sawOutputEOS = false;
        while (!(sawInputEOS || sawOutputEOS)) {

            /* INPUT */
            int inputBufferId = codec.dequeueInputBuffer(timeoutUs);
            if (inputBufferId >= 0) {
                ByteBuffer inputBuffer = codec.getInputBuffer(inputBufferId);
                int sampleSize = extractor.readSampleData(inputBuffer, 0);
                long presentationTimeUs = 0;
                if (sampleSize < 0) {
                    sawInputEOS = true;
                    sampleSize = 0;
                } else {
                    presentationTimeUs = extractor.getSampleTime();
                }
                codec.queueInputBuffer(inputBufferId,
                        0, //offset
                        sampleSize,
                        presentationTimeUs,
                        sawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);
                if (!sawInputEOS) {
                    extractor.advance();
                }
            }

            /* OUTPUT */
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            int outputBufferId = codec.dequeueOutputBuffer(info, timeoutUs);
            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                sawOutputEOS = true;
            }
            if (outputBufferId >= 0) {
                ByteBuffer outputBuffer = codec.getOutputBuffer(outputBufferId);
                MediaFormat bufferFormat = codec.getOutputFormat(outputBufferId); // option A
                // bufferFormat is identical to outputFormat
                // outputBuffer is ready to be processed or rendered.
                final byte[] chunk = new byte[info.size];
                outputBuffer.get(chunk); // Read the buffer all at once
                outputBuffer.clear(); // ** MUST DO!!! OTHERWISE THE NEXT TIME YOU GET THIS SAME BUFFER BAD THINGS WILL HAPPEN

                fullBytes.put(chunk);

                codec.releaseOutputBuffer(outputBufferId, /* render */ false);
            }
        }
        codec.stop();
        codec.release();

        fullBytes.rewind();
        byte[] rawBytes = new byte[fullBytes.capacity()];
        fullBytes.get(rawBytes);
        Log.d(TAG, String.format("Found %d raw bytes", rawBytes.length));

        fullBytes.rewind();
        final ShortBuffer shortBuffer = fullBytes.asShortBuffer();
        short[] amplitudes = new short[shortBuffer.capacity()];
        shortBuffer.get(amplitudes);
        Log.d(TAG, String.format("Found %d samples", amplitudes.length));

        double avg = Math.abs(amplitudes[0]);
        short max = (short) Math.abs(amplitudes[0]);
        for (int i = 0; i < amplitudes.length; i++) {
            final int abs = Math.abs(amplitudes[i]);
            avg += abs;
            if (abs > max) { max = (short) abs; }
        }
        avg = avg/amplitudes.length;
        Log.d(TAG, String.format("Stats (amplitude): avg=%.3f, max=%d", avg, max));

        double avgDbA = toDbA(avg);
        double maxDbA = toDbA(max);
        Log.d(TAG, String.format("Stats (dBA): avg=%.3f, max=%.3f", avgDbA, maxDbA));

        JSONObject ret = new JSONObject();
        ret.put("avg", avgDbA);
        ret.put("max", maxDbA);

        return ret.toString();
    }


    public static final double REFERENCE_MINIMUM_PRESSURE = 0.00002; // Pa(scal)
    public static final double REFERENCE_MAXIMUM_PRESSURE = 0.6325; // Pa(scal)
    public static final double RATIO = Short.MAX_VALUE/REFERENCE_MAXIMUM_PRESSURE;

    /**
     * Source: http://stackoverflow.com/a/14870458/1119508
     */
    private static double toDbA(double amplitude) {
        return 20 * Math.log10(amplitude/RATIO/REFERENCE_MINIMUM_PRESSURE);
    }
}
