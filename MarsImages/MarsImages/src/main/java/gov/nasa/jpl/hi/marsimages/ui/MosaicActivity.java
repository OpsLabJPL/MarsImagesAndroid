package gov.nasa.jpl.hi.marsimages.ui;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.powellware.marsimages.R;

import gov.nasa.jpl.hi.marsimages.MarsImagesApp;

import static gov.nasa.jpl.hi.marsimages.MarsImagesApp.MARS_IMAGES;
import static gov.nasa.jpl.hi.marsimages.MarsImagesApp.disableMenuItem;
import static gov.nasa.jpl.hi.marsimages.MarsImagesApp.enableMenuItem;


public class MosaicActivity extends AppCompatActivity {

    public static final String INTENT_ACTION_MOSAIC = "gov.nasa.jpl.hi.marsimages.MOSAIC";
    private MarsRajawaliFragment mosaicFragment;
    private MenuItem backMenuItem;
    private MenuItem forwardMenuItem;
    private MarsMosaicRenderer scene;
    private TextView caption;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mosaic);
        mosaicFragment = (MarsRajawaliFragment) getSupportFragmentManager().findFragmentById(R.id.mosaicFragment);
        scene = mosaicFragment.getRenderer();
        caption = (TextView) findViewById(R.id.captionTextView);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_mosaic, menu);
        backMenuItem = menu.findItem(R.id.goBack);
        forwardMenuItem = menu.findItem(R.id.goForward);
        MarsImagesApp.disableMenuItem(backMenuItem);
        MarsImagesApp.enableMenuItem(forwardMenuItem);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.gyroMenuItem) {
//            mosaicFragment.getRenderer().toggleGyro(); TODO re-enable this later when I want to fight with it again
            return true;
        }
        else if (id == R.id.goBack) {
            scene.deleteImages();

            int[] prevRMC = MARS_IMAGES.getPreviousRMC(scene.getRMC());
            if (prevRMC != null) {
                //load new image mosaic
                scene.addImagesToScene(prevRMC);
                updateLocationMenuItems();
            }
            return true;
        }
        else if (id == R.id.goForward) {
            scene.deleteImages();
            int[] nextRMC = MARS_IMAGES.getNextRMC(scene.getRMC());
            if (nextRMC != null) {
                //load new image mosaic
                scene.addImagesToScene(nextRMC);
                updateLocationMenuItems();
            }

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void updateLocationMenuItems() {
        MosaicActivity.this.caption.post(new Runnable() {
            @Override
            public void run() {
                int[] prevRMC = MARS_IMAGES.getPreviousRMC(scene.getRMC());
                if (prevRMC != null) {
                    enableMenuItem(backMenuItem);
                } else {
                    disableMenuItem(backMenuItem);
                }
                int[] nextRMC = MARS_IMAGES.getNextRMC(scene.getRMC());
                if (nextRMC != null) {
                    enableMenuItem(forwardMenuItem);
                } else {
                    disableMenuItem(forwardMenuItem);
                }
            }
        });
    }

    public void updateCaption(final int[] rmc) {
        MosaicActivity.this.caption.post(new Runnable() {
            @Override
            public void run() {
                caption.setText(MARS_IMAGES.getMissionName() + " at location " + rmc[0] + "-" + rmc[1]);
            }
        });
    }
}
