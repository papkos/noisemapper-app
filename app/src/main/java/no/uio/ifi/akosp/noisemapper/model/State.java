package no.uio.ifi.akosp.noisemapper.model;

import android.location.Location;

import org.greenrobot.greendao.annotation.Convert;
import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import no.uio.ifi.akosp.noisemapper.model.converters.InCallStateConverter;
import no.uio.ifi.akosp.noisemapper.model.converters.OrientationConverter;
import no.uio.ifi.akosp.noisemapper.model.converters.SimpleLocationConverter;

/**
 * Created on 2016.03.30..
 *
 * @author Ákos Pap
 */
@Entity
public class State implements Serializable, Cloneable {

    static final long serialVersionUID = 1L;

//    @SuppressWarnings("PointlessBitwiseExpression")
    public static final int CHANGE_PROXIMITY = 1 << 0;
    public static final int CHANGE_LIGHT = 1 << 1;
    public static final int CHANGE_AZIMUTH = 1 << 2;
    public static final int CHANGE_PITCH = 1 << 3;
    public static final int CHANGE_ROLL = 1 << 4;
    public static final int CHANGE_IN_CALL_STATE = 1 << 5;
    public static final int CHANGE_LOCATION = 1 << 6;

    public static SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy. MM. dd. HH:mm.ss", Locale.US);

    @Id
    protected Long id;

    @Convert(converter = OrientationConverter.class, columnType = String.class)
    protected Orientation orientation;
    protected float proximity;
    protected String proximityText;
    protected float light;

    protected boolean inPocket;
    @Convert(converter = InCallStateConverter.class, columnType = String.class)
    protected InCallState inCallState;

    @Convert(converter = SimpleLocationConverter.class, columnType = String.class)
    protected SimpleLocation location;

    protected Date timestamp;

    protected int stepCount;

    protected transient int changes;

    public State() {}

    public State(Orientation position, float proximity, String proximityText, float light, boolean inPocket, InCallState inCallState, Location location, int stepCount, Date timestamp) {
        this.orientation = position;
        this.proximity = proximity;
        this.proximityText = proximityText;
        this.light = light;
        this.inPocket = inPocket;
        this.inCallState = inCallState;
        this.location = SimpleLocation.fromLocation(location);
        this.stepCount = stepCount;
        this.timestamp = timestamp;
    }

    @Generated(hash = 1178323427)
    public State(Long id, Orientation orientation, float proximity, String proximityText, float light, boolean inPocket, InCallState inCallState, SimpleLocation location, Date timestamp,
            int stepCount) {
        this.id = id;
        this.orientation = orientation;
        this.proximity = proximity;
        this.proximityText = proximityText;
        this.light = light;
        this.inPocket = inPocket;
        this.inCallState = inCallState;
        this.location = location;
        this.timestamp = timestamp;
        this.stepCount = stepCount;
    }

    public String getTimestampString() {
        return DATE_FORMATTER.format(timestamp);
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
            sb.append("(stepCount=").append(stepCount).append(")");

        sb.append("]");

        return sb.toString();
    }

    @Override
    public State clone() {
        // Just to make it public
        try {
            return (State) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            return new State();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Orientation getOrientation() {
        return orientation;
    }

    public void setOrientation(Orientation orientation) {
        this.orientation = orientation;
    }

    public float getProximity() {
        return proximity;
    }

    public void setProximity(float proximity) {
        this.proximity = proximity;
    }

    public String getProximityText() {
        return proximityText;
    }

    public void setProximityText(String proximityText) {
        this.proximityText = proximityText;
    }

    public float getLight() {
        return light;
    }

    public void setLight(float light) {
        this.light = light;
    }

    public boolean isInPocket() {
        return inPocket;
    }

    public void setInPocket(boolean inPocket) {
        this.inPocket = inPocket;
    }

    public InCallState getInCallState() {
        return inCallState;
    }

    public void setInCallState(InCallState inCallState) {
        this.inCallState = inCallState;
    }

    public SimpleLocation getLocation() {
        return location;
    }

    public void setLocation(SimpleLocation location) {
        this.location = location;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public int getChanges() {
        return changes;
    }

    public void setChanges(int changes) {
        this.changes = changes;
    }

    public boolean getInPocket() {
        return this.inPocket;
    }

    public int getStepCount() {
        return stepCount;
    }

    public void setStepCount(int stepCount) {
        this.stepCount = stepCount;
    }
}
