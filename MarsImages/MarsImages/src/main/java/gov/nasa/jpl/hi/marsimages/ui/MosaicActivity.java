package gov.nasa.jpl.hi.marsimages.ui;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
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
    public static final String STATE_GYRO = "state_gyro";
    private static final String TAG = "MosaicActivity";
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
        if (savedInstanceState != null) {
            boolean gyroFlag = savedInstanceState.getBoolean(STATE_GYRO);
            if (scene.isGyroEnabled() != gyroFlag) {
                scene.toggleGyro();
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        boolean gyroFlag = scene.isGyroEnabled();
        outState.putBoolean(STATE_GYRO, gyroFlag);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_mosaic, menu);
        backMenuItem = menu.findItem(R.id.goBack);
        forwardMenuItem = menu.findItem(R.id.goForward);
        MarsImagesApp.disableMenuItem(backMenuItem);
        MarsImagesApp.enableMenuItem(forwardMenuItem);

        SensorManager mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        MenuItem item = menu.findItem(R.id.gyroMenuItem);
        if (accelerometer != null && magnetometer != null) {
            item.setIcon(mosaicFragment.getRenderer().isGyroEnabled() ? R.drawable.compass_on : R.drawable.compass_off);
        }
        else {
            menu.removeItem(R.id.gyroMenuItem);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.gyroMenuItem) {
            mosaicFragment.getRenderer().toggleGyro();
            item.setIcon(mosaicFragment.getRenderer().isGyroEnabled() ? R.drawable.compass_on : R.drawable.compass_off);
            return true;
        }
        else if (id == R.id.goBack) {
            scene.deleteImages();

            new AsyncTask<Void, Void, Void>() {

                @Override
                protected Void doInBackground(Void... params) {
                    int[] prevRMC = MARS_IMAGES.getPreviousRMC(scene.getRMC());
                    if (prevRMC != null) {
                        //load new image mosaic
                        scene.addImagesToScene(prevRMC);
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    updateLocationMenuItems();
                }
            }.execute();
            return true;
        }
        else if (id == R.id.goForward) {
            scene.deleteImages();

            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    int[] nextRMC = MARS_IMAGES.getNextRMC(scene.getRMC());
                    if (nextRMC != null) {
                        //load new image mosaic
                        scene.addImagesToScene(nextRMC);
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    updateLocationMenuItems();
                }
            }.execute();
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
