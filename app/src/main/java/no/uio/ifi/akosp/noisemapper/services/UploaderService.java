package no.uio.ifi.akosp.noisemapper.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Base64;
import android.util.Base64OutputStream;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

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

    private static final String pathSingle = "/api/upload_recording/";
    private static final String pathBatch = "/api/upload_recording_batch/";

    public static final String API_AUTH_HEADER = "X-Noisemapper-Api-Auth";
    public static final String API_DEVICE_NAME = "X-Noisemapper-Api-Device-Name";
    public static final int MAX_PAYLOAD_SIZE = 10 * 1024 * 1024; /* bytes */


    private DaoSession daoSession;
    private String host;
    private String apiAuthSecret;
    private String deviceName;

    private boolean deleteSoundFileAfterUpload = false;

    public UploaderService() {
        super("UploaderService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        daoSession = Utils.getDaoSession(getApplicationContext());
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        host = preferences.getString("host_url", getString(R.string.defaultHost));
        String uploadTo = preferences.getString("upload_to", "remote");
        deleteSoundFileAfterUpload = preferences.getBoolean("delete_after_upload", false);

        if ("remote".equals(uploadTo)) {
            apiAuthSecret = getString(R.string.api_auth_remote);
        } else {
            apiAuthSecret = getString(R.string.api_auth_local);
        }

        deviceName = preferences.getString("device_name", "");
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

//        for (ProcessedRecord pr : prList) {
//            uploadOneAndStore(pr);
//        }

        uploadBatchAndStore(prList);
    }

    private void uploadBatchAndStore(List<ProcessedRecord> prList) {
        URL url;
        try {
            url = new URL(host + pathBatch);
        } catch (MalformedURLException e) {
            Log.e(TAG, "Wrong URL: " + host, e);
            return;
        }
        HttpURLConnection urlConnection = null;


        int requestCnt = 0;
        int prListI = 0;
        while (prListI < prList.size()) {
            List<JSONObject> payload = new ArrayList<>();
            int payloadSize = 0;
            do {
                // TODO: move out of the while-loop to cache, and avoid processing an overflowing record twice.
                final JSONObject json = processedRecordToJson(prList.get(prListI));
                if (payloadSize + json.toString().length() < MAX_PAYLOAD_SIZE) {
                    payload.add(json);
                    payloadSize += json.toString().length();
                    prListI++;
                } else {
                    break;
                }
            } while (prListI < prList.size());

            if (payload.isEmpty()) {
                Log.e(TAG, "Payload is empty!");
                return;
            }

            requestCnt++;
            try {
                urlConnection = (HttpsURLConnection) url.openConnection();
                urlConnection.setRequestProperty("Content-Type", "application/json");
                urlConnection.setRequestProperty(API_AUTH_HEADER, Base64.encodeToString(apiAuthSecret.getBytes(), Base64.DEFAULT));
                urlConnection.setRequestProperty(API_DEVICE_NAME, Base64.encodeToString(deviceName.getBytes(), Base64.DEFAULT));

                urlConnection.setDoOutput(true);
//                urlConnection.setChunkedStreamingMode(0);
                urlConnection.setRequestMethod("POST");
                urlConnection.connect();


                OutputStreamWriter out = new OutputStreamWriter(urlConnection.getOutputStream());
                out.write(payload.toString());
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

                JSONObject responseToLog = new JSONObject(responseText);
                responseToLog.put("uuids_processed_length", responseToLog.getJSONArray("uuids_processed").length());
                responseToLog.remove("uuids_processed");
                Log.i(TAG, "Server response:\n" + responseToLog.toString(2));

                if (response.has("success") && response.getBoolean("success") && response.has("uuids_processed")) {
                    JSONArray uuidsProcessedJson = response.getJSONArray("uuids_processed");
                    for (int i = 0; i < uuidsProcessedJson.length(); i++) {
                        ProcessedRecord pr = daoSession.getProcessedRecordDao().queryBuilder()
                                .where(ProcessedRecordDao.Properties.Uuid.eq(uuidsProcessedJson.getString(i)))
                                .unique();
                        if (pr == null) {
                            Log.w(TAG, "Got unknown UUID from server: " + uuidsProcessedJson.getString(i));
                            continue;
                        }
                        pr.setUploaded(true);
                        daoSession.getProcessedRecordDao().save(pr);
                        Log.d(TAG, String.format("Updated ProcessedRecord with id=%s to uploaded=true",
                                pr.getId().toString()));
                        if (deleteSoundFileAfterUpload) {
                            File f = new File(pr.getFilename());
                            boolean delRes = f.delete();
                            if (!delRes) {
                                Log.w(TAG, "Couldn't delete file " + f.getAbsolutePath());
                            }
                        }
                    }
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

        final String logMsg = String.format("Uploaded %d ProcessedRecordings in %d request(s)", prList.size(), requestCnt);
        Log.i(TAG, logMsg);
        Toast.makeText(getApplicationContext(), logMsg, Toast.LENGTH_SHORT).show();
    }

    private void uploadOneAndStore(ProcessedRecord pr) {
        URL url;
        try {
            url = new URL(host + pathSingle);
        } catch (MalformedURLException e) {
            Log.e(TAG, "Wrong URL: " + host, e);
            return;
        }

        HttpURLConnection urlConnection = null;
        try {
            urlConnection = (HttpsURLConnection) url.openConnection();
            urlConnection.setRequestProperty("Content-Type", "application/json");
            urlConnection.setRequestProperty(API_AUTH_HEADER, Base64.encodeToString(apiAuthSecret.getBytes(), Base64.DEFAULT));
            urlConnection.setRequestProperty(API_DEVICE_NAME, Base64.encodeToString(deviceName.getBytes(), Base64.DEFAULT));

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
            if (response.has("success") && response.getBoolean("success")) {
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
        JSONObject payload = processedRecordToJson(pr);

        Log.d(TAG, "Sending: " + payload.toString());
        out.write(payload.toString());
    }

    @NonNull
    private JSONObject processedRecordToJson(ProcessedRecord pr) {
        JSONObject payload = new JSONObject();

        try {
            payload.put("timestamp", Utils.dateToString(pr.getTimestamp()));
            payload.put("processResult", new JSONObject(pr.getProcessResult()));
            payload.put("state", Utils.stateToJson(pr.getState()));
            payload.put("uuid", pr.getUuid());

            try {
                FileInputStream inputStream = new FileInputStream(pr.getFilename());
                byte[] buffer = new byte[8192];
                int bytesRead;
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                Base64OutputStream output64 = new Base64OutputStream(output, Base64.NO_WRAP);
                try {
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        output64.write(buffer, 0, bytesRead);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                output64.close();

                payload.put("file", output.toString());

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return payload;
    }

    /**
     * Source: http://stackoverflow.com/a/5445161/1119508
     */
    static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
}
