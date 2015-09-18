package gov.nasa.jpl.hi.marsimages.ui;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.powellware.marsimages.R;

import rajawali.RajawaliFragment;

/**
 * Created by mpowell on 4/25/15.
 */
public class MarsRajawaliFragment extends RajawaliFragment implements SensorEventListener, View.OnTouchListener {

    public static final float MIN_ZOOM = 0.5f;
    public static final float MAX_ZOOM = 5.0f;
    static final String TAG = "MarsRajawaliFragment";
    private static final int INVALID_POINTER_ID = -1;

    private MarsMosaicRenderer renderer;
    private float mScaleFactor = 1.0f;
    private float mPreviousX = 0f;
    private float mPreviousY = 0f;
    private SensorManager mSensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private ScaleGestureDetector mScaleDetector;
    private int mActivePointerId;
    private boolean mLastAccelerometerSet = false;
    private boolean mLastMagnetometerSet = false;
    private float[] mR = new float[9];
    private float[] mR2= new float[9];
    private float[] mI = new float[9];
    private float[] mOrientation = new float[3];

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
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
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

    public void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME);
    }

    public void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
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

    float[] accelVals = new float[3];
    float[] compassVals = new float[3];

    static final float ANTI_JITTER_FACTOR = 1f;

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == accelerometer) {
            accelVals = lowPass( event.values.clone(), accelVals );
            mLastAccelerometerSet = true;
        } else if (event.sensor == magnetometer) {
            compassVals = lowPass( event.values.clone(), compassVals );
            mLastMagnetometerSet = true;
        }
        if (mLastAccelerometerSet && mLastMagnetometerSet) {
            SensorManager.getRotationMatrix(mR, mI, accelVals, compassVals);
//            SensorManager.remapCoordinateSystem(mR, SensorManager.AXIS_X, SensorManager.AXIS_Z, mR2);
            int rotation = ((WindowManager) getActivity().getApplicationContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
//            Log.d(TAG, "Rotation is "+rotation);
            if(rotation == 0) { // Default display rotation is portrait
                SensorManager.remapCoordinateSystem(mR, SensorManager.AXIS_MINUS_Z, SensorManager.AXIS_X, mR2);
            }
            else {  // Default display rotation is landscape
                SensorManager.remapCoordinateSystem(mR, SensorManager.AXIS_X, SensorManager.AXIS_Z, mR2);
            }
            System.arraycopy(mR2, 0, mR, 0, mR2.length);
            SensorManager.getOrientation(mR, mOrientation);
            float azimuthInRadians = -mOrientation[0];
            float pitchInRadians = -mOrientation[1];
            float currentAz = (float) renderer.getDeviceAzimuth();
            float currentPitch = (float) renderer.getDevicePitch();
            float deltaAz = constrainAngle(azimuthInRadians - currentAz);
            float deltaPitch = constrainAngle(pitchInRadians - currentPitch);
            azimuthInRadians = (float) (currentAz + deltaAz*Math.abs(Math.sin(deltaAz*ANTI_JITTER_FACTOR)));
            pitchInRadians = (float) (currentPitch + deltaPitch*Math.abs(Math.sin(deltaPitch*ANTI_JITTER_FACTOR)));
            renderer.setDeviceOrientationAngles(azimuthInRadians, pitchInRadians);
        }
    }

    float constrainAngle(float angle) {
        if (angle < -Math.PI) return (float) Math.min(Math.PI/2, angle + Math.PI*2);
        if (angle > Math.PI) return (float) Math.max(Math.PI/2, angle - Math.PI*2);
        return angle;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    /*
     * time smoothing constant for low-pass filter
     * 0 ≤ alpha ≤ 1 ; a smaller value basically means more smoothing
     * See: http://en.wikipedia.org/wiki/Low-pass_filter#Discrete-time_realization
     */
    static final float ALPHA = 0.15f;

    /**
     * @see http://en.wikipedia.org/wiki/Low-pass_filter#Algorithmic_implementation
     * @see http://developer.android.com/reference/android/hardware/SensorEvent.html#values
     * http://blog.thomnichols.org/2011/08/smoothing-sensor-data-with-a-low-pass-filter
     */
    protected float[] lowPass( float[] input, float[] output ) {
        if ( output == null ) return input;

        for ( int i=0; i<input.length; i++ ) {
            output[i] = output[i] + ALPHA/mScaleFactor * (input[i] - output[i]);
        }
        return output;
    }

}
