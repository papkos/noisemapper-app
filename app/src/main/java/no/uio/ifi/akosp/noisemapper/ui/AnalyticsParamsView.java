package no.uio.ifi.akosp.noisemapper.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.util.Log;

import butterknife.ButterKnife;
import no.uio.ifi.akosp.noisemapper.R;

/**
 * Created on 2016.04.17..
 *
 * @author Ákos Pap
 */
public class AnalyticsParamsView extends CardView {

    public static final String TAG = "AnalyticsParamsView";
    public static final AnalyticsParamsViewInteractionListener DEFAULT_CALLBACK = new AnalyticsParamsViewInteractionListener() {
        @Override
        public void onRequestProcessAll() {
            Log.w(TAG, "Callback not registered!");
        }

        @Override
        public void onRequestUploadAll() {
            Log.w(TAG, "Callback not registered!");
        }
    };

//    @Bind(R.id.cntRecords)
//    protected TextView cntRecordsView;
//    @Bind(R.id.cntUnprocessed)
//    protected TextView cntUnprocessedView;
//    @Bind(R.id.cntNotUploaded)
//    protected TextView cntNotUploadedView;
//    @Bind(R.id.files)
//    protected TextView filesView;
//
//    @Bind(R.id.processAll)
//    protected Button processAllButton;
//    @Bind(R.id.uploadAll)
//    protected Button uploadAllButton;

    protected boolean ready = false;

//    protected Statistics statistics = null;

    protected AnalyticsParamsViewInteractionListener callback = DEFAULT_CALLBACK;

    public void setCallback(AnalyticsParamsViewInteractionListener callback) {
        this.callback = callback;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        ButterKnife.bind(this, getRootView());
//        processAllButton.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                callback.onRequestProcessAll();
//            }
//        });
//
//        uploadAllButton.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                callback.onRequestUploadAll();
//            }
//        });
        ready = true;
    }

    @SuppressLint({"SetTextI18n", "DefaultLocale"})
    protected void updateViews() {
        if (! ready) return;

//        if (this.statistics != null) {
//            cntRecordsView.setText(Integer.toString(this.statistics.records));
//            cntUnprocessedView.setText(Integer.toString(this.statistics.unprocessed));
//            cntNotUploadedView.setText(Integer.toString(this.statistics.notUploaded));
//            filesView.setText(String.format("%d (%s)",
//                    this.statistics.files, Utils.humanReadableByteCount(this.statistics.spaceUsed)));
//        }
    }

    public AnalyticsParamsView(Context context) {
        super(context, null, R.attr.analyticsParamsViewStyle);
        init(context, null, R.attr.analyticsParamsViewStyle);
    }

    public AnalyticsParamsView(Context context, AttributeSet attrs) {
        super(context, attrs, R.attr.analyticsParamsViewStyle);
        init(context, attrs, R.attr.analyticsParamsViewStyle);
    }

    public AnalyticsParamsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        // TODO: custom initialization
        inflate(getContext(), R.layout.view_analytics_params, this);
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

    public interface AnalyticsParamsViewInteractionListener {
        void onRequestProcessAll();
        void onRequestUploadAll();
    }
}
