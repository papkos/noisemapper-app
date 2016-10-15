package no.uio.ifi.akosp.noisemapper.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import no.uio.ifi.akosp.noisemapper.R;
import no.uio.ifi.akosp.noisemapper.Utils;
import no.uio.ifi.akosp.noisemapper.model.DaoSession;
import no.uio.ifi.akosp.noisemapper.model.ProcessedRecord;
import no.uio.ifi.akosp.noisemapper.model.ProcessedRecordDao;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class UploaderService extends IntentService {

    public static final String TAG = "UploaderService";

    public static final String ACTION_UPLOAD_ALL = "UploaderService::UploadAll";
    public static final String ACTION_UPLOAD_ONE = "UploaderService::UploadOne";

    public static final String EXTRA_PROCESSED_RECORD_ID = "UploaderService::processedRecordId";
    
    private DaoSession daoSession;
    private String host;
    private static final String path = "/api/upload_recording/";
    
    public UploaderService() {
        super("UploaderService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        daoSession = Utils.getDaoSession(getApplicationContext());
        host = PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                .getString("host_url", getString(R.string.defaultHost));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        daoSession.getDatabase().close();
    }

    public static void startUploadingAll(Context context) {
        Intent i = new Intent(context, UploaderService.class);
        i.setAction(ACTION_UPLOAD_ALL);
        context.startService(i);
    }

    public static void startUploadingOne(Context context, long id) {
        Intent i = new Intent(context, UploaderService.class);
        i.setAction(ACTION_UPLOAD_ONE);
        i.putExtra(EXTRA_PROCESSED_RECORD_ID, id);
        context.startService(i);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_UPLOAD_ALL.equals(action)) {
                handleUploadAll();
            } else if (ACTION_UPLOAD_ONE.equals(action)) {
                long id = intent.getLongExtra(EXTRA_PROCESSED_RECORD_ID, 0);
                handleUploadOne(id);
            }
        }
    }

    private void handleUploadOne(long id) {
        ProcessedRecord pr = daoSession.getProcessedRecordDao().loadDeep(id);
        uploadOneAndStore(pr);
    }

    private void handleUploadAll() {
        final List<ProcessedRecord> prList = daoSession.queryBuilder(ProcessedRecord.class)
                .where(ProcessedRecordDao.Properties.Uploaded.eq(false))
                .list();

        for (ProcessedRecord pr : prList) {
            uploadOneAndStore(pr);
        }
    }

    private void uploadOneAndStore(ProcessedRecord pr) {
        URL url;
        try {
            url = new URL(host + path);
        } catch (MalformedURLException e) {
            Log.e(TAG, "Wrong URL: " + host, e);
            return;
        }

        HttpURLConnection urlConnection = null;
        try {
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestProperty("Content-Type","application/json");
            urlConnection.setDoOutput(true);
//            urlConnection.setChunkedStreamingMode(0);
            urlConnection.setRequestMethod("POST");
            urlConnection.connect();


            OutputStreamWriter out = new OutputStreamWriter(urlConnection.getOutputStream());
            writeProcessedRecordToStream(out, pr);
            out.flush();

            final int responseCode = urlConnection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, String.format("Server returned %d - %s",
                        responseCode, urlConnection.getResponseMessage()));
                InputStream err = new BufferedInputStream(urlConnection.getErrorStream());
                String errorMessage = convertStreamToString(err);
                err.close();
                Log.e(TAG, "Server returned error message: " + errorMessage);
                return;
            }

            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            String responseText = convertStreamToString(in);
            in.close();

            JSONObject response = new JSONObject(responseText);
            if (response.has("result") && response.getBoolean("result")) {
                pr.setUploaded(true);
                daoSession.getProcessedRecordDao().save(pr);
                Log.i(TAG, String.format("Updated ProcessedRecord with id=%s to uploaded=true",
                        pr.getId().toString()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    private void writeProcessedRecordToStream(Writer out, ProcessedRecord pr) throws IOException {
        JSONObject payload = new JSONObject();

        try {
            payload.put("timestamp", pr.getTimestamp());
            payload.put("processResult", new JSONObject(pr.getProcessResult()));
            payload.put("state", Utils.stateToJson(pr.getState()));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Log.d(TAG, "Sending: " + payload.toString());
        out.write(payload.toString());
    }

    /**
     * Source: http://stackoverflow.com/a/5445161/1119508
     */
    static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
}
