package no.uio.ifi.akosp.noisemapper.ui;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Switch;

import butterknife.Bind;
import butterknife.ButterKnife;
import no.uio.ifi.akosp.noisemapper.R;

/**
 * Created on 2016.04.17..
 *
 * @author Ákos Pap
 */
public class AppStatusView extends CardView {

    public static final String TAG = "AppStatusView";
    public static final AppStatusViewInteractionListener DEFAULT_CALLBACK = new AppStatusViewInteractionListener() {
        @Override
        public void onSoundServiceSwitchChanged(boolean checked) {
            Log.w(TAG, "Callback not registered!");
        }
    };

    @Bind(R.id.snippetServiceSwitch)
    protected Switch snippetServiceSwitch;

    protected boolean ready = false;

    protected boolean soundServiceEnabled = false;

    protected AppStatusViewInteractionListener callback = DEFAULT_CALLBACK;

    public void setSoundServiceEnabled(boolean enabled) {
        this.soundServiceEnabled = enabled;
        updateViews();
    }

    public void setCallback(AppStatusViewInteractionListener callback) {
        this.callback = callback;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        ButterKnife.bind(this, getRootView());
        snippetServiceSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                callback.onSoundServiceSwitchChanged(isChecked);
            }
        });
        ready = true;
    }

    protected void updateViews() {
        if (! ready) return;

        snippetServiceSwitch.setChecked(soundServiceEnabled);
    }

    public AppStatusView(Context context) {
        super(context, null, R.attr.appStatusViewStyle);
        init(context, null, R.attr.appStatusViewStyle);
    }

    public AppStatusView(Context context, AttributeSet attrs) {
        super(context, attrs, R.attr.appStatusViewStyle);
        init(context, attrs, R.attr.appStatusViewStyle);
    }

    public AppStatusView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        // TODO: custom initialization
        inflate(getContext(), R.layout.view_app_status, this);
    }


    @Override
    protected Parcelable onSaveInstanceState() {
        //begin boilerplate code that allows parent classes to save state
        Parcelable superState = super.onSaveInstanceState();

        SavedState ss = new SavedState(superState);
        //end

//        ss.stateToSave = this.state;

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

//        this.state = ss.stateToSave;
//        updateViews();
    }

    static class SavedState extends BaseSavedState {
//        State stateToSave;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
//            this.stateToSave = (State) in.readSerializable();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
//            out.writeSerializable(this.stateToSave);
        }

        //required field that makes Parcelables from a Parcel
        public static final Creator<SavedState> CREATOR =
                new Creator<SavedState>() {
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }
                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }

    public interface AppStatusViewInteractionListener {
        void onSoundServiceSwitchChanged(boolean checked);
    }
}
