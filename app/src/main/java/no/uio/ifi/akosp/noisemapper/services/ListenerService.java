package no.uio.ifi.akosp.noisemapper.services;

import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import no.uio.ifi.akosp.noisemapper.R;
import no.uio.ifi.akosp.noisemapper.ui.MainActivity;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class ListenerService extends Service {

    public static final String TAG = "ListenerService";

    public static final String SERVICE_ID = ListenerService.class.getSimpleName();

    private static final String ACTION_START_LISTENING = "ListenerService::StartListening";
    private static final String ACTION_STOP_LISTENING = "ListenerService::StopListening";

    private final Handler handler;
    private Runnable recurringRecorderRunnable = new Runnable() {
        @Override
        public void run() {
            new Thread(new Recorder(getApplicationContext(), 5000)).start();
            handler.postDelayed(recurringRecorderRunnable, 10 * 1000);
        }
    };

//    // TODO: Rename parameters
//    private static final String EXTRA_PARAM1 = "ListenerService::PARAM1";
//    private static final String EXTRA_PARAM2 = "ListenerService::PARAM2";

    public ListenerService() {
        handler = new Handler();
    }

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    public static void startListening(Context context, String param1, String param2) {
        Intent intent = new Intent(context, ListenerService.class);
        intent.setAction(ACTION_START_LISTENING);
//        intent.putExtra(EXTRA_PARAM1, param1);
//        intent.putExtra(EXTRA_PARAM2, param2);
        context.startService(intent);
        Log.i(TAG, "Started service");
    }

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    public static void stopListening(Context context, String param1, String param2) {
        Intent intent = new Intent(context, ListenerService.class);
        intent.setAction(ACTION_STOP_LISTENING);
//        intent.putExtra(EXTRA_PARAM1, param1);
//        intent.putExtra(EXTRA_PARAM2, param2);
        context.startService(intent);
        Log.i(TAG, "Stopped service");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_START_LISTENING.equals(action)) {
//                final String param1 = intent.getStringExtra(EXTRA_PARAM1);
//                final String param2 = intent.getStringExtra(EXTRA_PARAM2);
//                handleActionFoo(param1, param2);
                handleStartListening();
            } else if (ACTION_STOP_LISTENING.equals(action)) {
//                final String param1 = intent.getStringExtra(EXTRA_PARAM1);
//                final String param2 = intent.getStringExtra(EXTRA_PARAM2);
                handleStopListening();
            }
        }
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    protected void handleStartListening() {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("My notification")
                        .setContentText("Hello World!");

        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, resultIntent, 0);
        mBuilder.setContentIntent(pendingIntent);
        Notification notification = mBuilder.build();
        Log.d(TAG, "Notification for foreground: " + notification);
        startForeground(42, notification);

        handler.post(recurringRecorderRunnable);


    }

    private void handleStopListening() {

        stopForeground(true);

        handler.removeCallbacks(recurringRecorderRunnable);
        Log.i(TAG, "Removed handler callbacks, stopping service");

        stopSelf();
    }


}
