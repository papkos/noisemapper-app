package no.uio.ifi.akosp.noisemapper.ui;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.Locale;

import butterknife.Bind;
import butterknife.ButterKnife;
import no.uio.ifi.akosp.noisemapper.R;
import no.uio.ifi.akosp.noisemapper.Utils;
import no.uio.ifi.akosp.noisemapper.model.Orientation;
import no.uio.ifi.akosp.noisemapper.model.State;

/**
 * Created on 2016.04.17..
 *
 * @author Ákos Pap
 */
public class PhoneStatusView extends CardView {

    public static final String TAG = "PhoneStatusView";

    @Bind(R.id.orientation)
    protected TextView orientationDisplay;

    @Bind(R.id.proximity)
    protected TextView proximityDisplay;

    @Bind(R.id.lightSensor)
    protected TextView lightSensorDisplay;

    @Bind(R.id.isInPocket)
    protected CheckBox isInPocketDisplay;

    @Bind(R.id.isInCall)
    protected CheckBox isInCallDisplay;

    protected State state;
    protected boolean ready = false;

    public void setState(State state) {
        this.state = state;
        updateViews();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        ready = true;
    }

    protected void updateViews() {
        if (! ready) return;

        final Orientation orientation = state.getOrientation();
        orientationDisplay.setText(String.format(Locale.US, "X: %.0f° | Y: %.0f° | Z: %.0f°",
                Math.toDegrees(orientation.pitch), Math.toDegrees(orientation.roll), Math.toDegrees(orientation.azimuth))
        );
        proximityDisplay.setText(String.format(Locale.US, "%s (%.1f cm)",
                state.getProximityText(), state.getProximity())
        );
        lightSensorDisplay.setText(String.format(Locale.US, "%.2f lux", state.getLight()));

        isInPocketDisplay.setChecked(Utils.isInPocket(state));

        switch (state.getInCallState()) {
            case IN_CALL:
                isInCallDisplay.setEnabled(true);
                isInCallDisplay.setChecked(true);
                break;
            case RINGING:
                isInCallDisplay.setEnabled(false);
                isInCallDisplay.setChecked(true);
                break;
            case NO_CALL:
                isInCallDisplay.setEnabled(true);
                isInCallDisplay.setChecked(false);
                break;
        }
    }

    public PhoneStatusView(Context context) {
        super(context, null, R.attr.phoneStatusViewStyle);
        init(context, null, R.attr.phoneStatusViewStyle);
    }

    public PhoneStatusView(Context context, AttributeSet attrs) {
        super(context, attrs, R.attr.phoneStatusViewStyle);
        init(context, attrs, R.attr.phoneStatusViewStyle);
    }

    public PhoneStatusView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        // TODO: custom initialization
        inflate(getContext(), R.layout.view_phone_status, this);
        ButterKnife.bind(this, getRootView());
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        //begin boilerplate code that allows parent classes to save state
        Parcelable superState = super.onSaveInstanceState();

        SavedState ss = new SavedState(superState);
        //end

        ss.stateToSave = this.state;

        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        //begin boilerplate code so parent classes can restore state
        if(!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState ss = (SavedState)state;
        super.onRestoreInstanceState(ss.getSuperState());
        //end

        this.state = ss.stateToSave;
        updateViews();
    }

    static class SavedState extends BaseSavedState {
        State stateToSave;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            this.stateToSave = (State) in.readSerializable();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeSerializable(this.stateToSave);
        }

        //required field that makes Parcelables from a Parcel
        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }
                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }
}
