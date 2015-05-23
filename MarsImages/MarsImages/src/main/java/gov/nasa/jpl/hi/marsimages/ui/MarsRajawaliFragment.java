package gov.nasa.jpl.hi.marsimages.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.powellware.marsimages.R;

import rajawali.RajawaliFragment;

/**
 * Created by mpowell on 4/25/15.
 */
public class MarsRajawaliFragment extends RajawaliFragment {

    private MarsMosaicRenderer renderer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        renderer = new MarsMosaicRenderer(getActivity());
        renderer.setSurfaceView(mSurfaceView);
        mSurfaceView.setOnTouchListener(renderer);
        //this prevents a crash when the activity is first destroyed, but it's not enough...need to figure out how to get this to work all the time
        mLayout = (android.widget.FrameLayout) getActivity().findViewById(R.id.mosaicSurfaceViewContainer);
        super.setRenderer(renderer);
    }

    public View onCreateView(LayoutInflater i, ViewGroup c, Bundle b) {
        return mSurfaceView;
    }

    public MarsMosaicRenderer getRenderer() {
        return renderer;
    }
}
