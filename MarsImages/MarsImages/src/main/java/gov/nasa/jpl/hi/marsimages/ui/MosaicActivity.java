package gov.nasa.jpl.hi.marsimages.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.powellware.marsimages.R;

import rajawali.RajawaliFragment;
import rajawali.RajawaliFragmentActivity;

public class MosaicActivity extends RajawaliFragmentActivity {

    public static final String INTENT_ACTION_MOSAIC = "gov.nasa.jpl.hi.marsimages.MOSAIC";

    private MarsMosaicRenderer mRenderer;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        mRenderer = new MarsMosaicRenderer(this);
        mRenderer.setSurfaceView(mSurfaceView);
        mSurfaceView.setOnTouchListener(mRenderer);

        super.setRenderer(mRenderer);
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
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
