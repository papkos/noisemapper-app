package no.uio.ifi.akosp.noisemapper.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/**
 * Base class to implement a recurring task that can run in the background.
 *
 * Steps to do:
 * <ol>
 *     <li>Subclass {@link RecurringService}.</li>
 *     <li>
 *         Declare an action constant, and catch it in the overridden
 *         {@link #onStartCommand(Intent, int, int)} method.
 *         e.g.:
 *         <pre>
    public static final String ACTION_LOG = "TestService::log";

    {@literal @}Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_LOG.equals(intent.getAction())) {
        handleLog(startId);

        return START_COMMAND_RETURN;
        }

        return super.onStartCommand(intent, flags, startId);
    }
 *         </pre>
 *     </li>
 *     <li>
 *         If you want to use some custom methods in the Application, create
 *         an interface that extends {@link RecurringService.RecurringServiceApp RecurringServiceApp}
 *         and make your Application implement it. <p>
 *         Then, add the {@link #onCreate()} method, and override the parent's {@link #app} field with yours.
 *         Don't forget to add a new field, that shadows the parent's {@link #app} field
 *         with your own type!
 *     </li>
 * </ol>
 *
 * <p>
 *     Usage:<br/>
 *     Make the {@link RecurringServiceApp#isRecurringServiceEnabled(String)} method
 *     return {@code true} (usually by changing a SharedPreference), then start
 *     the service in check mode with the {@link RecurringService#makeCheckIntent(ComponentName, String)}
 *     method:
 *     <pre>
     startService(TestService.makeCheckIntent(
             new ComponentName(this, TestService.class),
             TestService.ACTION_LOG
     ));
 *     </pre>
 */
public abstract class RecurringService extends Service {
    public static final String TAG = "RecurringService";

    /** The action that triggers a status check and possibly fixes */
    protected static final String ACTION_CHECK_STATUS = "RecurringService::checkStatus";

    /** The return value of {@link #onStartCommand(Intent, int, int)} */
    protected static final int START_COMMAND_RETURN = START_NOT_STICKY;

    private static final String RECURRING_ACTION = "RecurringService::recurringAction";

    /** The component name of the actual service */
    protected ComponentName componentName;

    /** The {@link android.app.Application} instance implementing an interface - for common operations */
    protected RecurringServiceApp app;

    /**
     * Creates an intent with {@link #ACTION_CHECK_STATUS} action. Use this to trigger a status
     * check, and the process of fixing it. (Fixing: setting/removing the alarm as necessary.)
     *
     * @param target The identifier of the actual service to start.
     * @param recurringAction The ID for the action that the descendant service will listen to.
     * @return An intent to be used with {@link Context#startService(Intent)}.
     */
    public static Intent makeCheckIntent(ComponentName target, String recurringAction) {
        Intent i = new Intent();
        i.setAction(ACTION_CHECK_STATUS);
        i.setComponent(target);
        i.putExtra(RECURRING_ACTION, recurringAction);
        return i;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        app = (RecurringServiceApp) getApplication();
        componentName = new ComponentName(getApplicationContext(), this.getClass());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_CHECK_STATUS.equals(action)) {
                Log.i(TAG, "Received intent, checking status");
                String recurringAction = intent.getStringExtra(RECURRING_ACTION);
                handleCheckStatus(startId, intent.getComponent(), recurringAction);
            }
        }

        return START_COMMAND_RETURN;
    }

    public abstract String getServiceId();

    /**
     * This method checks if the recurring setting is enabled in the settings, and the state of the
     * alarm, and if they don't match, takes action:
     * <ol>
     *     <li>Creates the alarm if the service is enabled but there's no alarm, or</li>
     *     <li>Removes the alarm, it it is set, but the service is disabled.</li>
     * </ol>
     *
     * @param startId Parameter from the {@link #onStartCommand(Intent, int, int)}, used to stop the
     *                appropriate instance.
     * @param componentName The identifier of the actual service.
     * @param recurringAction The ID for the action that the descendant service will listen to.
     */
    protected final void handleCheckStatus(int startId, ComponentName componentName, String recurringAction) {
        Intent recurringIntent = new Intent();
        recurringIntent.setAction(recurringAction);
        recurringIntent.setComponent(componentName);

        boolean alarmShouldBeSet = app.isRecurringServiceEnabled(getServiceId());

        final Context context = getApplicationContext();
        PendingIntent pi = PendingIntent.getService(context, 0, recurringIntent, PendingIntent.FLAG_NO_CREATE);

        boolean alarmIsSet = (pi != null);

        Log.i(TAG, "STATUS: enabled: " + alarmShouldBeSet + " running: " + alarmIsSet);
        if ((alarmShouldBeSet && alarmIsSet) || (!alarmShouldBeSet && !alarmIsSet)) {
            // everything is in order, nothing to do here
            Log.i(TAG, "Everything is OK, doing nothing.");
            stopSelf(startId);
            return;
        }

        if (alarmShouldBeSet && !alarmIsSet) {
            Log.i(TAG, "Enabled but not running. Setting the recurring alarm.");
            AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            pi = PendingIntent.getService(context, 0, recurringIntent, 0);
            alarmMgr.setRepeating(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,   // type
                    0,                                      // first
                    60 * 1000,                              // repeat interval (ms)
                    pi                                      // intent to fire
            );
            stopSelf(startId);
            return;
        }

        if (!alarmShouldBeSet && alarmIsSet) {
            Log.i(TAG, "Running but disabled. Cancelling the alarm.");
            AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarmMgr.cancel(pi);
            stopSelf(startId);
            return;
        }
    }

    @Override
    public final IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * A simple interface to provide access to the shared preferences though the
     * {@link android.app.Application} instance.
     * <p>
     * Extend it in the subclass to add more functions.
     * @see RecurringService RecurringService main documentation for examples.
     */
    public interface RecurringServiceApp {

        /**
         * Gets the state of the recurring service setting.
         * @return {@code true} if the recurring action is enabled, {@code false} otherwise.
         */
        boolean isRecurringServiceEnabled(String serviceId);

        Context getApplicationContext();
    }
}
