package no.uio.ifi.akosp.noisemapper.ui;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.greenrobot.greendao.query.LazyList;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import no.uio.ifi.akosp.noisemapper.NoiseMapperApp;
import no.uio.ifi.akosp.noisemapper.R;
import no.uio.ifi.akosp.noisemapper.Utils;
import no.uio.ifi.akosp.noisemapper.model.DaoSession;
import no.uio.ifi.akosp.noisemapper.model.ProcessedRecord;
import no.uio.ifi.akosp.noisemapper.model.ProcessedRecordDao;
import no.uio.ifi.akosp.noisemapper.model.SimpleLocation;

public class AnalyticsActivity extends AppCompatActivity implements RWAdapter.OnSelectionChangedListener {

    public static final String TAG = "MainActivity";

    @Bind(R.id.smartScrollView)
    protected SmartScrollView smartScrollView;

    @Bind(R.id.recordsList)
    protected RecyclerView recordsListView;

    @Bind(R.id.mapCardView)
    protected MapCardView mapCardView;

    protected NoiseMapperApp app;

    protected Handler handler = new Handler();
    private RWAdapter adapter;
    private DaoSession daoSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analytics);
        ButterKnife.bind(this);

        smartScrollView.addInterceptScrollView(mapCardView);

        app = (NoiseMapperApp) getApplication();
        daoSession = Utils.getDaoSession(this);

        setUpViews();


        updateList();


    }

    LazyList<ProcessedRecord> lazyPrList = null;
    private void updateList() {
        lazyPrList = daoSession.getProcessedRecordDao().queryBuilder()
                .orderDesc(ProcessedRecordDao.Properties.Timestamp)
//                .limit(100)
                .listLazy();
        adapter.setRecordWrappers(lazyPrList);
    }

    protected void setUpViews() {
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recordsList);
        //noinspection ConstantConditions
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new RWAdapter();
        recyclerView.setAdapter(adapter);
        adapter.setOnSelectionChangedListener(this);
    }

    @Override
    public void onSelectionChanged(List<ProcessedRecord> selectedPrs) {
        List<MarkerOptions> markers = new ArrayList<>(selectedPrs.size());
        for (ProcessedRecord pr : selectedPrs) {
            final SimpleLocation location = pr.getState().getLocation();
            markers.add(new MarkerOptions().position(new LatLng(location.getLatitude(), location.getLongitude())));
        }
        mapCardView.showMarkers(markers, true);
    }

    @Override
    protected void onDestroy() {
        mapCardView.onDestroy();
        if (lazyPrList != null) {
            lazyPrList.close();
        }
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapCardView.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapCardView.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapCardView.onStop();
    }

    @Override
    protected void onPause() {
        mapCardView.onPause();
        super.onPause();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapCardView.onLowMemory();
    }
}
