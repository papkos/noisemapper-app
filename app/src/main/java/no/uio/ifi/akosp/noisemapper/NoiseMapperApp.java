package no.uio.ifi.akosp.noisemapper;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.File;

/**
 * Created on 2016.04.17..
 *
 * @author Ákos Pap
 */
public class NoiseMapperApp extends Application {

    public static final String TAG = "NoiseMapperApp";

    public static final String PREF_SERVICE_ENABLED = "NoiseMapper::pref::serviceEnabled";
    public static final String PREF_UPDATE_VIEWS_ENABLED = "NoiseMapper::pref::updateViewsEnabled";

    protected File outFolder;
    protected File logFile;

    @Override
    public void onCreate() {
        super.onCreate();
        String extDir =  Environment.getExternalStorageDirectory().getAbsolutePath();
        outFolder = new File(extDir, "AudioRecordTest");
        boolean dirCreated = outFolder.mkdirs();
        Log.d(TAG, "Dir  created = " + dirCreated);

        logFile = new File(outFolder, "__states.json");
    }

    public boolean isServiceEnabled(String serviceId) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        return preferences.getBoolean(PREF_SERVICE_ENABLED + serviceId, false);
    }

    public void enableService(String serviceId) {
        setServiceEnabled(serviceId, true);
    }

    public void disableService(String serviceId) {
        setServiceEnabled(serviceId, false);
    }

    public void setServiceEnabled(String serviceId, boolean enabled) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(PREF_SERVICE_ENABLED + serviceId, enabled);
        editor.apply();
    }

    public boolean isUpdateViewsEnabled() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        return preferences.getBoolean(PREF_UPDATE_VIEWS_ENABLED, false);
    }

    public void setUpdateViewsEnabled(boolean enabled) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(PREF_UPDATE_VIEWS_ENABLED, enabled);
        editor.apply();
    }
}
