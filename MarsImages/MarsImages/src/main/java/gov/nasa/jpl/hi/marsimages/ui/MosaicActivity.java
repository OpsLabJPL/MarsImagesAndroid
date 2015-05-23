package gov.nasa.jpl.hi.marsimages.ui;

import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.powellware.marsimages.R;


public class MosaicActivity extends AppCompatActivity {

    public static final String INTENT_ACTION_MOSAIC = "gov.nasa.jpl.hi.marsimages.MOSAIC";
    private MarsRajawaliFragment mosaicFragment;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mosaic);
        mosaicFragment = new MarsRajawaliFragment();
        getSupportFragmentManager().beginTransaction()
                .add(R.id.mosaicSurfaceViewContainer, mosaicFragment, "mosaicFragment")
                .commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_mosaic, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.gyroMenuItem) {
            mosaicFragment.getRenderer().toggleGyro();
            return true;
        }
        else if (id == R.id.goBack) {
            return true;
        }
        else if (id == R.id.goForward) {

        }

        return super.onOptionsItemSelected(item);
    }

}
