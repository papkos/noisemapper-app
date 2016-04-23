package no.uio.ifi.akosp.noisemapper.ui;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.util.Log;

import no.uio.ifi.akosp.noisemapper.R;

/**
 * Created on 2016.04.17..
 *
 * @author Ákos Pap
 */
public class AppStatusView extends CardView {

    public static final String TAG = "AppStatusView";

    public AppStatusView(Context context) {
        super(context, null, R.attr.appStatusViewStyle);
        init(context, null, R.attr.appStatusViewStyle);
        Log.d(TAG, "Ctr 1 called");
    }

    public AppStatusView(Context context, AttributeSet attrs) {
        super(context, attrs, R.attr.appStatusViewStyle);
        init(context, attrs, R.attr.appStatusViewStyle);
        Log.d(TAG, "Ctr 2 called");
    }

    public AppStatusView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
        Log.d(TAG, "Ctr 3 called");
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        // TODO: custom initialization
        inflate(getContext(), R.layout.view_app_status, this);
    }
}
