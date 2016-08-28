package no.uio.ifi.akosp.noisemapper.model;

import android.location.Location;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created on 2016.03.30..
 *
 * @author Ákos Pap
 */
public class State implements Serializable {

    @SuppressWarnings("PointlessBitwiseExpression")
    public static final int CHANGE_PROXIMITY = 1 << 0;
    public static final int CHANGE_LIGHT = 1 << 1;
    public static final int CHANGE_AZIMUTH = 1 << 2;
    public static final int CHANGE_PITCH = 1 << 3;
    public static final int CHANGE_ROLL = 1 << 4;
    public static final int CHANGE_IN_CALL_STATE = 1 << 5;
    public static final int CHANGE_LOCATION = 1 << 6;

    public static SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy. MM. dd. HH:mm.ss", Locale.US);

    protected final Orientation orientation;
    protected final float proximity;
    protected final String proximityText;
    protected final float light;

    protected final boolean inPocket;
    protected final InCallState inCallState;

    protected final SimpleLocation location;

    protected final Date timestamp;
    protected int changes;

    public State(Orientation position, float proximity, String proximityText, float light, boolean inPocket, InCallState inCallState, Location location, Date timestamp) {
        this.orientation = position;
        this.proximity = proximity;
        this.proximityText = proximityText;
        this.light = light;
        this.inPocket = inPocket;
        this.inCallState = inCallState;
        this.location = SimpleLocation.fromLocation(location);
        this.timestamp = timestamp;
    }

    public Orientation getOrientation() {
        return orientation;
    }

    public float getProximity() {
        return proximity;
    }

    public String getProximityText() {
        return proximityText;
    }

    public float getLight() {
        return light;
    }

    public boolean isInPocket() {
        return inPocket;
    }

    public InCallState getInCallState() {
        return inCallState;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public String getTimestampString() {
        return DATE_FORMATTER.format(timestamp);
    }

    public void setChanges(int changes) {
        this.changes = changes;
    }

    public int getChanges() {
        return changes;
    }

    public SimpleLocation getLocation() {
        return location;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("PhoneState[");

            sb.append("(orientation=").append(orientation.toString()).append(")");
            sb.append("(proximity=")
                    .append(String.format(Locale.US, "%s (%.1f cm)", getProximityText(), proximity))
                    .append(")");
            sb.append("(light=").append(String.format(Locale.US, "%.2f lux", light)).append(")");
            sb.append("(inPocket=").append(inPocket).append(")");
            sb.append("(inCallState=").append(inCallState.toString()).append(")");
            sb.append("(location=").append(location.toString()).append(")");

        sb.append("]");

        return sb.toString();
    }
}
