package no.uio.ifi.akosp.noisemapper.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import no.uio.ifi.akosp.noisemapper.NoiseMapperApp;
import no.uio.ifi.akosp.noisemapper.Utils;
import no.uio.ifi.akosp.noisemapper.model.DaoSession;
import no.uio.ifi.akosp.noisemapper.model.ProcessedRecord;
import no.uio.ifi.akosp.noisemapper.model.Record;
import no.uio.ifi.akosp.noisemapper.model.RecordDao;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class ProcessorService2 extends IntentService {

    public static final String TAG = "ProcessorService2";

    public static final String ACTION_PROCESS_ALL = "ProcessorService2::ProcessAll";
    public static final String ACTION_PROCESS_ONE = "ProcessorService2::ProcessOne";

    public static final String EXTRA_RECORD_ID = "ProcessorService2::recordId";
    private DaoSession daoSession;
    private NoiseMapperApp app;

    public ProcessorService2() {
        super("ProcessorService2");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        daoSession = Utils.getDaoSession(getApplicationContext());

        app = (NoiseMapperApp) getApplication();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        daoSession.getDatabase().close();
    }

    public static void startProcessingAll(Context context) {
        Intent i = new Intent(context, ProcessorService2.class);
        i.setAction(ACTION_PROCESS_ALL);
        context.startService(i);
    }

    public static void startProcessingOne(Context context, long id) {
        Intent i = new Intent(context, ProcessorService2.class);
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
            pr.setUuid(record.getUuid());
            pr.setFilename(record.getFilename());

            daoSession.getProcessedRecordDao().save(pr);
            Log.i(TAG, String.format("Saved ProcessedRecord with id=%s", pr.getId().toString()));

            if (app.isAutoUploadEnabled()) {
                Log.i(TAG, String.format(
                        "Auto upload is enabled, so requesting to upload ProcessedRecord with id=%s",
                        pr.getId().toString()));
                UploaderService.startUploadingOne(getApplicationContext(), pr.getId());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String process(Record record) throws Exception {

        final File file = new File(record.getFilename());
        final BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
        final int fileLength = (int) file.length();

        ByteBuffer bb = ByteBuffer.allocate(2);
        bb.order(ByteOrder.LITTLE_ENDIAN);

        short[] samples = new short[(int) (fileLength / 2)];

        int i = 0;
        while (i < samples.length) {
            bb.clear();
            bb.put((byte) bis.read());
            bb.put((byte) bis.read());
            samples[i++] = bb.getShort(0);
        }
        bis.close();

        double avg = Math.abs(samples[0]);
        short max = (short) Math.abs(samples[0]);
        for (int j = 0; j < samples.length; j++) {
            final int abs = Math.abs(samples[j]);
            avg += abs;
            if (abs > max) {
                max = (short) abs;
            }
        }
        avg = avg / samples.length;
        Log.d(TAG, String.format("Stats (amplitude): avg=%.3f, max=%d", avg, max));

        double avgDbA = toDbA(avg);
        double maxDbA = toDbA(max);
        Log.d(TAG, String.format("Stats (dBA): avg=%.3f, max=%.3f", avgDbA, maxDbA));

        JSONObject ret = new JSONObject();
        ret.put("avg", avgDbA);
        ret.put("max", maxDbA);

        return ret.toString();
    }


    public static final double REFERENCE_MINIMUM_PRESSURE = 0.00002; // Pa(scal) // "lower"
    public static final double REFERENCE_MAXIMUM_PRESSURE = 0.6325; // Pa(scal) // "higher"
    // slope = (higher - lower) / (maxval - minval)
    public static final double SLOPE = (REFERENCE_MAXIMUM_PRESSURE - REFERENCE_MINIMUM_PRESSURE) / (Short.MAX_VALUE - 0);

    private static double amplitudeToPressure(double amplitude) {
        // lower + slope * (val - minval)
        return REFERENCE_MINIMUM_PRESSURE + SLOPE * (amplitude - 0);
    }

    private static double toDbA(double amplitude) {
        // 20 * log(p / p0)
        return 20 * Math.log10(amplitudeToPressure(amplitude) / REFERENCE_MINIMUM_PRESSURE);
    }
}
