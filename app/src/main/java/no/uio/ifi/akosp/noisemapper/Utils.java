package no.uio.ifi.akosp.noisemapper;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

import no.uio.ifi.akosp.noisemapper.model.Orientation;
import no.uio.ifi.akosp.noisemapper.model.State;

/**
 * Created on 2016.04.23..
 *
 * @author Ákos Pap
 */
public class Utils {

    public static final String TAG = "Utils";

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

    public static void sendRefreshBroadcast(Context context) {
//        LocalBroadcastManager.getInstance(context)
//                .sendBroadcast(RecordingListFragment.getRefreshIntent());
    }

    public static String stateToJson(State state, String filename) {
        JSONObject root = new JSONObject();

        try {
            JSONObject orientation = new JSONObject();
                orientation.put("azimuth", state.getOrientation().azimuth);
                orientation.put("pitch", state.getOrientation().pitch);
                orientation.put("roll", state.getOrientation().roll);
            root.put("orientation", orientation);

            JSONObject location = new JSONObject();
                location.put("lat", state.getLocation().getLatitude());
                location.put("lon", state.getLocation().getLongitude());
                if (state.getLocation().hasSpeed()) { location.put("speed", state.getLocation().getSpeed()); }
                if (state.getLocation().hasBearing()) { location.put("bearing", state.getLocation().getBearing()); }
            root.put("location", location);

            root.put("proximity", state.getProximity());
            root.put("proximityText", state.getProximityText());
            root.put("light", state.getLight());
            root.put("inPocket", state.isInPocket());
            root.put("inCallState", state.getInCallState().name());
            root.put("timestamp", state.getTimestampString());

            root.put("file", filename);

        } catch (JSONException e) {
            Log.e(TAG, "Failed to serialize a state to JSON.", e);
        }

        return root.toString();
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
}
