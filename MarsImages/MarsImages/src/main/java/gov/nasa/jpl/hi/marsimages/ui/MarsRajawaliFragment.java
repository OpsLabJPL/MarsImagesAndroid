package gov.nasa.jpl.hi.marsimages.ui;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.powellware.marsimages.R;

import rajawali.RajawaliFragment;

/**
 * Created by mpowell on 4/25/15.
 */
public class MarsRajawaliFragment extends RajawaliFragment implements SensorEventListener, View.OnTouchListener {

    public static final float MIN_ZOOM = 0.5f;
    public static final float MAX_ZOOM = 5.0f;
    private static final String TAG = "MarsRajawaliFragment";
    private static final int INVALID_POINTER_ID = -1;

    private MarsMosaicRenderer renderer;
    private float mScaleFactor = 1.0f;
    private float mPreviousX = 0f;
    private float mPreviousY = 0f;
    private SensorManager mSensorManager;
    private float deviceAzimuth;
    private float devicePitch;
    private final float[] mRotationMatrix = new float[16];
    private final float[] values = new float[3];
    float[] mGravity;
    float[] mGeomagnetic;
    private ScaleGestureDetector mScaleDetector;
    private int mActivePointerId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) {
        super.onCreateView(inflater, container, bundle);
        mLayout = (FrameLayout) inflater.inflate(R.layout.mars_mosaic_fragment, container, false);
        renderer = new MarsMosaicRenderer(getActivity());
        renderer.setSurfaceView(mSurfaceView);
        mSurfaceView.setOnTouchListener(this);
        super.setRenderer(renderer);

        mSensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        Sensor accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME);

        mScaleDetector = new ScaleGestureDetector(getActivity(), new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float scaleFactor = detector.getScaleFactor();
                mScaleFactor *= scaleFactor;
                // Don't let the object get too small or too large.
                mScaleFactor = Math.max(MIN_ZOOM, Math.min(mScaleFactor, MAX_ZOOM));
                renderer.setScaleFactor(mScaleFactor);
                return true;
            }
        });
        return mSurfaceView;
    }

    @Override
    public boolean onTouch(View v, MotionEvent ev) {
        // MotionEvent reports input details from the touch screen
        // and other input controls. In this case, you are only
        // interested in events where the touch position changed.

        // Let the ScaleGestureDetector inspect all events.
        mScaleDetector.onTouchEvent(ev);


        switch (ev.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                mPreviousX = ev.getX();
                mPreviousY = ev.getY();
                mActivePointerId = ev.getPointerId(0);
                break;

            case MotionEvent.ACTION_MOVE:
                int pointerIndex = ev.findPointerIndex(mActivePointerId);
                float x = ev.getX(pointerIndex);
                float y = ev.getY(pointerIndex);
                if (!mScaleDetector.isInProgress()) {
                    float xMovement = (x - mPreviousX) / mScaleFactor;
                    float yMovement = (y - mPreviousY) / mScaleFactor;
                    renderer.incrementCameraMotion(xMovement, yMovement);
                }
                mPreviousX = x;
                mPreviousY = y;
                break;

            case MotionEvent.ACTION_UP:
                mPreviousX = 0f;
                mPreviousY = 0f;
                mActivePointerId = INVALID_POINTER_ID;
                break;

            case MotionEvent.ACTION_CANCEL:
                mActivePointerId = INVALID_POINTER_ID;
                break;

            case MotionEvent.ACTION_POINTER_UP:
                pointerIndex = (ev.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK)
                        >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
                final int pointerId = ev.getPointerId(pointerIndex);
                if (pointerId == mActivePointerId) {
                    // This was our active pointer going up. Choose a new
                    // active pointer and adjust accordingly.
                    final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                    mPreviousX = ev.getX(newPointerIndex);
                    mPreviousY = ev.getY(newPointerIndex);
                    mActivePointerId = ev.getPointerId(newPointerIndex);
                }
                break;
        }
        return true;
    }

    public MarsMosaicRenderer getRenderer() {
        return renderer;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            // convert the rotation-vector to a 4x4 matrix. the matrix
            // is interpreted by Open GL as the inverse of the
            // rotation-vector, which is what we want.'
            SensorManager.getRotationMatrixFromVector(
                    mRotationMatrix, event.values);
            SensorManager.getOrientation(mRotationMatrix, values);
//            Log.d(TAG,"Values: "+Math.toDegrees(values[0])+"\t"+Math.toDegrees(values[1])+"\t"+Math.toDegrees(values[2]));
            deviceAzimuth = values[0]; //radian angle about Z axis pointing down toward Earth, counterclockwise
            if (deviceAzimuth < 0) deviceAzimuth += 2*Math.PI;
            devicePitch = values[1]; //radian angle about X axis pointing toward West, counterclockwise. Offset by 90 degrees'
            if (Float.isNaN(devicePitch)) devicePitch = 0;
            devicePitch += Math.PI/4; //offset of 90 degrees to put 0 at the horizon instead of up.
        }

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            mGravity = event.values;
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            mGeomagnetic = event.values;
        if (mGravity != null && mGeomagnetic != null) {
            float R[] = new float[9];
            float I[] = new float[9];
            boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
            if (success) {
                float orientation[] = new float[3];
                SensorManager.getOrientation(R, orientation);
                deviceAzimuth = orientation[0]; // orientation contains: azimut, pitch and roll
                devicePitch = orientation[1];
                renderer.setDeviceOrientationAngles(deviceAzimuth, devicePitch);
//                Log.d(TAG, "Azimuth: "+Math.toDegrees(deviceAzimuth)+"   Pitch: "+Math.toDegrees(devicePitch));
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }



}
