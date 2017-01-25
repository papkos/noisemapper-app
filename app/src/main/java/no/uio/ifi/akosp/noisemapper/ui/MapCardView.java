package no.uio.ifi.akosp.noisemapper.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import no.uio.ifi.akosp.noisemapper.R;

/**
 * Created on 2016.04.17..
 *
 * @author Ákos Pap
 */
public class MapCardView extends CardView implements OnMapReadyCallback {

    public static final String TAG = "MapCardView";
    public static final MapCardViewInteractionListener DEFAULT_CALLBACK = new MapCardViewInteractionListener() {
        @Override
        public void onRequestProcessAll() {
            Log.w(TAG, "Callback not registered!");
        }

        @Override
        public void onRequestUploadAll() {
            Log.w(TAG, "Callback not registered!");
        }
    };



    @Bind(R.id.map)
    protected MapView mapView;


    protected boolean ready = false;
    private GoogleMap map;


    protected MapCardViewInteractionListener callback = DEFAULT_CALLBACK;

    public void setCallback(MapCardViewInteractionListener callback) {
        this.callback = callback;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        ButterKnife.bind(this, getRootView());
        if (! isInEditMode()) {
            Log.i(TAG, "Asking for map");
            mapView.onCreate(null);
            mapView.getMapAsync(this);
        }
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

    @Override
    public void onMapReady(GoogleMap map) {
        Log.i(TAG, "OnMapReady called");
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "No permission to display my location!");
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        this.map = map;
        this.map.setMyLocationEnabled(true);
    }

    public MapCardView(Context context) {
        super(context, null, R.attr.mapCardViewStyle);
        init(context, null, R.attr.mapCardViewStyle);
    }

    public MapCardView(Context context, AttributeSet attrs) {
        super(context, attrs, R.attr.mapCardViewStyle);
        init(context, attrs, R.attr.mapCardViewStyle);
    }

    public MapCardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        // TODO: custom initialization
        inflate(getContext(), R.layout.view_map_card, this);
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

    public void showMarkers(List<MarkerOptions> moList, boolean clearPrevious) {
        if (clearPrevious) {
            map.clear();
        }

        if (moList.isEmpty()) {
            map.moveCamera(CameraUpdateFactory.zoomTo(12F));
            return;
        }

        if (moList.size() == 1) {
            Marker m = map.addMarker(moList.get(0));
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(m.getPosition(), 15F));
            return;
        }

        List<Marker> markers = new ArrayList<>(moList.size());
        for (MarkerOptions mo : moList) {
            markers.add(map.addMarker(mo));
        }

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (Marker marker : markers) {
            builder.include(marker.getPosition());
        }
        LatLngBounds bounds = builder.build();
        int padding = 20; // offset from edges of the map in pixels
        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
        map.moveCamera(cu);
    }

    protected void onResume() {
        mapView.onResume();
    }

    protected void onStart() {
        mapView.onStart();
    }

    protected void onStop() {
        mapView.onStop();
    }

    protected void onPause() {
        mapView.onPause();
    }

    protected void onDestroy() {
        mapView.onDestroy();
    }

    public void onLowMemory() {
        mapView.onLowMemory();
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

    public interface MapCardViewInteractionListener {
        void onRequestProcessAll();
        void onRequestUploadAll();
    }
}
