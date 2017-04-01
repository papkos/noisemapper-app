package no.uio.ifi.akosp.noisemapper;

import android.content.Context;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.Log;

import org.greenrobot.greendao.database.Database;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import no.uio.ifi.akosp.noisemapper.model.DaoMaster;
import no.uio.ifi.akosp.noisemapper.model.DaoSession;
import no.uio.ifi.akosp.noisemapper.model.Orientation;
import no.uio.ifi.akosp.noisemapper.model.SimpleLocation;
import no.uio.ifi.akosp.noisemapper.model.State;

import static no.uio.ifi.akosp.noisemapper.R.string.databaseName;

/**
 * Created on 2016.04.23..
 *
 * @author Ákos Pap
 */
public class Utils {

    public static final String TAG = "Utils";

    public static final SimpleDateFormat FILENAME_FORMATTER
            = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);

    public static boolean isInPocket(State state) {
        return isInPocket(state.getOrientation(), state.getProximity(), state.getLight());
    }

    public static boolean isInPocket(Orientation orientation, float proximity, float light) {
        return isInPocket(orientation, proximity, light, C.ORIENTATION_THRESHOLD, C.LIGHT_THRESHOLD);
    }

    public static boolean isInPocket(Orientation orientation, float proximity, float light,
                                     float orientationThreshold, float lightThreshold) {
        return (
                proximity < 10  // near
                        && light < lightThreshold  // For some reason on the Nexus 6 even when covered, light is 2.0 lux
                        && (Math.abs(orientation.pitch - Math.toRadians(-90)) < orientationThreshold  // vertical, head down
                        || Math.abs(orientation.pitch - Math.toRadians(90)) < orientationThreshold  // vertical, head up
                )

        );
    }

    public static JSONObject stateToJson(State state) {
        JSONObject root = new JSONObject();

        try {
            JSONObject orientation = orientationToJson(state.getOrientation());
            root.put("orientation", orientation);

            JSONObject location = simpleLocationToJson(state.getLocation());
            root.put("location", location);

            root.put("proximity", state.getProximity());
            root.put("proximityText", state.getProximityText());
            root.put("light", state.getLight());
            root.put("inPocket", state.isInPocket());
            root.put("inCallState", state.getInCallState().name());
            root.put("stepCount", state.getStepCount());
            root.put("micSource", state.getMicSource().name());
            root.put("timestamp", state.getTimestampString());

        } catch (JSONException e) {
            Log.e(TAG, "Failed to serialize a state to JSON.", e);
        }

        return root;
    }

    @NonNull
    public static JSONObject orientationToJson(Orientation orientation) {
        JSONObject ret = new JSONObject();
        try {
            ret.put("azimuth", orientation.azimuth);
            ret.put("pitch", orientation.pitch);
            ret.put("roll", orientation.roll);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public static Orientation orientationFromJson(String json) {
        JSONObject jo;
        try {
            jo = new JSONObject(json);
            return new Orientation(
                    (float) jo.getDouble("azimuth"),
                    (float) jo.getDouble("pitch"),
                    (float) jo.getDouble("roll")
            );
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    @NonNull
    public static JSONObject simpleLocationToJson(SimpleLocation loc) {
        JSONObject ret = new JSONObject();
        try {
            ret.put("lat", loc.getLatitude());
            ret.put("lon", loc.getLongitude());
            if (loc.hasAltitude()) { ret.put("altitude", loc.getAltitude()); }
            if (loc.hasSpeed()) { ret.put("speed", loc.getSpeed()); }
            if (loc.hasBearing()) { ret.put("bearing", loc.getBearing()); }
            if (loc.hasAccuracy()) { ret.put("accuracy", loc.getAccuracy()); }
            ret.put("provider", loc.getProvider());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public static SimpleLocation simpleLocationFromJson(String json) {
        JSONObject jo;
        try {
            jo = new JSONObject(json);
            return new SimpleLocation(
                    jo.getDouble("lat"),
                    jo.getDouble("lon"),
                    jo.has("altitude"),
                    (float) (jo.has("altitude") ? jo.getDouble("altitude") : 0),
                    jo.has("speed"),
                    (float) (jo.has("speed") ? jo.getDouble("speed") : 0),
                    jo.has("bearing"),
                    (float) (jo.has("bearing") ? jo.getDouble("bearing") : 0),
                    jo.has("accuracy"),
                    (float) (jo.has("accuracy") ? jo.getDouble("accuracy") : 0),
                    jo.getString("provider")
            );
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
    public static String dateToString(Date date) {
        return TIMESTAMP_FORMAT.format(date);
    }

    /**
     * Source: http://stackoverflow.com/a/29685580
     * @param sourceFile
     * @param destFile
     * @throws IOException
     */
    public static void copyFile(File sourceFile, File destFile) throws IOException {
        if(!destFile.exists()) {
            destFile.createNewFile();
        }

        FileChannel source = null;
        FileChannel destination = null;

        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            destination.transferFrom(source, 0, source.size());
        }
        finally {
            if(source != null) {
                source.close();
            }
            if(destination != null) {
                destination.close();
            }
        }
    }

    /**
     * Source: http://stackoverflow.com/a/3758880/1119508
     * @param bytes
     * @param si
     * @return
     */
    public static String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    public static String humanReadableByteCount(long bytes) {
        return humanReadableByteCount(bytes, false);
    }

    public static void exportRecording(String filename) {
        try {
            File data = Environment.getDataDirectory();
            File sd = Environment.getExternalStorageDirectory();

            if (sd.canWrite()) {
                String exportPath = "temp";
                File recordingFile = new File(filename);
                File exportDir = new File(sd, exportPath);
                exportDir.mkdirs();
                File destFile = new File(exportDir, recordingFile.getName());

                if (recordingFile.exists()) {
                    copyFile(recordingFile, destFile);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* methods using a Context ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ */

    public static void sendRefreshBroadcast(Context context) {
//        LocalBroadcastManager.getInstance(context)
//                .sendBroadcast(RecordingListFragment.getRefreshIntent());
    }

    public static DaoSession getDaoSession(Context context) {
        DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(context, context.getString(databaseName));
        Database db = helper.getWritableDb();
        return new DaoMaster(db).newSession();
    }

    public static void exportDatabase(Context context) {
        exportDatabase(context, context.getString(databaseName));
    }

    public static void exportDatabase(Context context, String databaseName) {
        try {
            File data = Environment.getDataDirectory();
            File sd = Environment.getExternalStorageDirectory();

            if (sd.canWrite()) {
                String currentDBPath = "/data/" + context.getPackageName() + "/databases/" + databaseName;
                String backupDBPath = "temp";
                File currentDB = new File(data, currentDBPath);
                File backupDbDir = new File(sd, backupDBPath);
                backupDbDir.mkdirs();
                File backupDb = new File(backupDbDir, databaseName + "_backup");

                if (currentDB.exists()) {
                    copyFile(currentDB, backupDb);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
