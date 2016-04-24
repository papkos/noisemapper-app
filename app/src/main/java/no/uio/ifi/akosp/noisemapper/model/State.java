package no.uio.ifi.akosp.noisemapper.model;

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

    public static SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy. MM. dd. HH:mm.ss", Locale.US);

    protected final Orientation orientation;
    protected final float proximity;
    protected final String proximityText;
    protected final float light;

    protected final boolean inPocket;
    protected final InCallState inCallState;

    protected final Date timestamp;
    protected int changes;

    public State(Orientation position, float proximity, String proximityText, float light, boolean inPocket, InCallState inCallState, Date timestamp) {
        this.orientation = position;
        this.proximity = proximity;
        this.proximityText = proximityText;
        this.light = light;
        this.inPocket = inPocket;
        this.inCallState = inCallState;
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
}