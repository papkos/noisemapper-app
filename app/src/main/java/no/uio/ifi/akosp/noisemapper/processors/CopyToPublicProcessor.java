package no.uio.ifi.akosp.noisemapper.processors;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.File;
import java.io.IOException;

import no.uio.ifi.akosp.noisemapper.Utils;
import no.uio.ifi.akosp.noisemapper.services.Signals;

/**
 * Created on 2016.09.03..
 *
 * @author Ákos Pap
 */
public class CopyToPublicProcessor extends BroadcastReceiver {

    public static final String TAG = "CopyToPublicProcessor";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Signals.ACTION_RECORDING_STOPPED.equals(intent.getAction())) {
            Log.d(TAG, String.format("Received %s with extras: %s",
                    intent.getAction(), intent.getExtras()));
            File file = (File) intent.getSerializableExtra(Signals.EXTRA_FILE);
            if (file.canRead()) {
                File outFolder = getOutFolder(context);
                File newFile = new File(outFolder, file.getName());
                try {
                    Utils.copyFile(file, newFile);
                } catch (IOException e) {
                    Log.w(TAG, "Lost a file, cannot copy it to a safe place.", e);
                }

//                if (file.canWrite()) {
//                    file.delete();
//                    Log.i(TAG, "Deleted old file: " + file);
//                }

            } else {
                Log.w(TAG, "Lost a file, can't read it: " + file);
            }
        }
    }

    private File getOutFolder(Context context) {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        final String folderName = preferences.getString("outFolder", "NoiseMapper");

        String extDir =  Environment.getExternalStorageDirectory().getAbsolutePath();
        File outFolder = new File(extDir, folderName);
        boolean dirCreated = outFolder.mkdirs();
        Log.d(TAG, "Dir  created = " + dirCreated);

        return outFolder;
    }
}
