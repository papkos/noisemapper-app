package no.uio.ifi.akosp.noisemapper.ui;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import no.uio.ifi.akosp.noisemapper.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        FragmentManager fm = getSupportFragmentManager();
//        fm.beginTransaction()
//                .add(R.id.app_status_container, AppStatusFragment.newInstance("a", "b"))
//                .commit();

        AppStatusView appStatusView = (AppStatusView) findViewById(R.id.appStatusView);

    }
}
