package gov.nasa.jpl.hi.marsimages.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.evernote.edam.type.Note;
import com.powellware.marsimages.R;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.microedition.khronos.opengles.GL10;

import gov.nasa.jpl.hi.marsimages.EvernoteMars;
import gov.nasa.jpl.hi.marsimages.models.CameraModel;
import gov.nasa.jpl.hi.marsimages.models.ImageQuad;
import gov.nasa.jpl.hi.marsimages.models.M;
import gov.nasa.jpl.hi.marsimages.models.Model;
import gov.nasa.jpl.hi.marsimages.models.Quad;
import gov.nasa.jpl.hi.marsimages.rovers.Rover;
import rajawali.lights.PointLight;
import rajawali.materials.AMaterial;
import rajawali.materials.DiffuseMaterial;
import rajawali.materials.SimpleMaterial;
import rajawali.materials.TextureInfo;
import rajawali.materials.TextureManager;
import rajawali.primitives.Plane;
import rajawali.renderer.RajawaliRenderer;

import static android.opengl.GLES20.GL_LINE_LOOP;
import static gov.nasa.jpl.hi.marsimages.EvernoteMars.EVERNOTE;
import static gov.nasa.jpl.hi.marsimages.MarsImagesApp.MARS_IMAGES;
import static gov.nasa.jpl.hi.marsimages.MarsImagesApp.TAG;

/**
 * Created by mpowell on 3/21/15.
 */
public class MarsMosaicRenderer extends RajawaliRenderer {

    private static final double Y_ROTATION_UPPER_LIMIT = Math.PI / 2;
    private static final double Y_ROTATION_LOWER_LIMIT = -Math.PI / 2;
    public static final float FAR_PLANE = 12.0f;
    public static final float NEAR_PLANE = 0.1f;
    public static final int NOMINAL_FOV = 45;
    private final float TOUCH_SCALE_FACTOR = .0015f;
    private final float COMPASS_HEIGHT = 0.5f;
    private final double[] x_axis = {1f, 0f, 0f};
    private final double[] y_axis = {0f, 1f, 0f};

    private PointLight mLight;
    private int[] rmc;
    private double[] qLL;
    private Map<String, Note> notesInScene = new ConcurrentHashMap<>();
    private float mScaleFactor = 1f;
    private float cameraRelativeXMotion = 0f;
    private float cameraRelativeYMotion = 0f;

    private DiffuseMaterial yellowMaterial;
    private Quad mCompass;
    private Map<String, ImageQuad> photoQuads = new ConcurrentHashMap<>();
    private double[] forwardVector = {0, 0, -1};
    private double rotAz[] = new double[4], rotEl[] = new double[4];
    private double look1[] = new double[3], look2[] = new double[3];
    private double rotationX = 0;
    private double rotationY = 0;
    private boolean gyroEnabled = false;
    private double deviceAzimuth = 0;
    private double devicePitch = 0;
    private Plane plane;
    private boolean mScaleChanged = false;

    public ConcurrentLinkedQueue<Runnable> glRunnables = new ConcurrentLinkedQueue<Runnable>();

    public MarsMosaicRenderer(Context context) {
        super(context);

        //framework places camera at Z=4 by default. Fix that.
        mEyeZ = 0f;
        mCamera.setZ(mEyeZ);

        setFrameRate(60);
        IntentFilter filter = new IntentFilter(EvernoteMars.END_NOTE_LOADING);
        LocalBroadcastManager.getInstance(context).registerReceiver(mMessageReceiver, filter);
    }

    protected void initScene() {
        mLight = new PointLight();
        mLight.setColor(1.0f, 1.0f, 1.0f);

        yellowMaterial = new DiffuseMaterial();
        yellowMaterial.setAmbientColor(1f, 1f, 0f, 1f);

        addHoverCompass();

        int[] rmc = EVERNOTE.getNearestRMC();
        addImagesToScene(rmc);
        ((MosaicActivity)getContext()).updateLocationMenuItems();
    }

    @Override
    public void onDrawFrame(GL10 glUnused) {

        setCameraLookDirection();
        mCamera.setFieldOfView(NOMINAL_FOV / mScaleFactor);
        mCamera.setFarPlane(FAR_PLANE);
        mCamera.setNearPlane(NEAR_PLANE);
        mCamera.setProjectionMatrix(mSurfaceView.getWidth(), mSurfaceView.getHeight());

        Runnable task = glRunnables.poll();
        int runnableCount = 0;
        while (task != null && runnableCount < 5) {
            runnableCount++;
            task.run();
            task = glRunnables.poll();
        }
        if (task != null) task.run();

        //update imagequads to use image texture or line mode depending on visibility and if the image is loaded yet into memory
        prepareImageQuads();

        super.onDrawFrame(glUnused);
    }

    public void setScaleFactor(float scale) { mScaleFactor = scale; mScaleChanged = true; }

    public void addImagesToScene(int[] rmc) {
        this.rmc = rmc;
        Rover mission = MARS_IMAGES.getMission();
        ((MosaicActivity)getContext()).updateCaption(rmc);
        int site_index = rmc[0];
        int drive_index = rmc[1];
        qLL = mission.localLevelQuaternion(site_index, drive_index);
        EVERNOTE.setSearchWords(String.format("RMC %06d-%06d", site_index, drive_index), mContext);
        EVERNOTE.reloadNotes(mContext); //rely on the resultant note load end broadcast receiver to populate images in the scene
    }

    @Override
    protected void destroyScene() {
        deleteImages();
        super.destroyScene();
    }

    public void toggleGyro() {
        gyroEnabled = !gyroEnabled;
        Log.d(TAG, "Gyro " + (gyroEnabled ? "True" : "False"));
    }

    public int[] getRMC() {
        return rmc;
    }

    public void setDeviceOrientationAngles(float deviceAzimuth, float devicePitch) {
        this.deviceAzimuth = deviceAzimuth;
        this.devicePitch = devicePitch;
    }

    public void incrementCameraMotion(float xMovement, float yMovement) {
        cameraRelativeXMotion += xMovement;
        cameraRelativeYMotion += yMovement;
    }

    public void deleteImages() {
        mSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                notesInScene.clear();
                for (ImageQuad photoQuad : photoQuads.values()) {
                    mTextureManager.removeTextures(photoQuad.getTextureInfoList());
                }
                photoQuads.clear();
                clearChildren();
                addChild(plane);
            }
        });
    }

    private void prepareImageQuads() {

        int skippedImages = 0;
        int drawnImages = 0;

        float[] viewMatrix = mCamera.getViewMatrix();
        float[] projectionMatrix = mCamera.getProjectionMatrix();
        mCamera.updateFrustum(projectionMatrix, viewMatrix);

        for (String title: photoQuads.keySet()) {
            ImageQuad imageQuad = photoQuads.get(title);

//            if (!mCamera.mFrustum.sphereInFrustum(imageQuad.getBoundsCenter(), (float)imageQuad.getBoundsRadius())) { //This doesn't work. Bad on them.

            if (!imageQuad.cameraIsLookingAtMe(getCamera())) {
                skippedImages++;
                imageQuad.setVisible(false);
                imageQuad.stopLoading();
                mTextureManager.removeTextures(imageQuad.getTextureInfoList());
                for (TextureInfo textureInfo : imageQuad.getTextureInfoList())
                    imageQuad.removeTexture(textureInfo);
                imageQuad.getTextureInfoList().clear();
                continue;
            }
            imageQuad.setVisible(true);
            drawnImages++;
            if (imageQuad.getTextureInfoList().size() == 0 || mScaleChanged) {
                Log.d(TAG, "No texture for "+title+ " isLoading: "+imageQuad.isLoading());
                prepareImageQuad(imageQuad, title);
            }
        }
        mScaleChanged = false;
//        Log.d(TAG, "images skipped: "+skippedImages);
//        Log.d(TAG, "images drawn: "+drawnImages);
    }

    private void prepareImageQuad(ImageQuad imageQuad, String title) {
//        if (imageQuad.getTextureInfoList().isEmpty()) {
            Note photo = notesInScene.get(title);
            imageQuad.loadImageAndTexture(photo, title, computeIdealImageResolution(photo), this);
//        }
    }

    private final BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final Rover rover = MARS_IMAGES.getMission();
            if (intent.getAction() == null) return;

            if (intent.getAction().equals(EvernoteMars.END_NOTE_LOADING)) {
                Integer notesReturned = intent.getIntExtra(EvernoteMars.NUM_NOTES_RETURNED, 0);
                Log.d("mosaic receiver", "Notes returned: " + notesReturned);


                if (notesReturned > 0) {
                    EVERNOTE.loadMoreNotes(mContext, false);
                } else {
                    new AsyncTask<Void, Void, List<QuadInitializer>>() {
                        @Override
                        protected List<QuadInitializer> doInBackground(Void... voids) {

                            if (!photoQuads.isEmpty())
                                return Collections.EMPTY_LIST; //we've already made the image quads

                            List<QuadInitializer> quadInitializers = new ArrayList<QuadInitializer>();
                            Log.d(TAG, "Making image quads");
                            List<Note> notes = EVERNOTE.getNotes();
                            binImagesByPointing(notes);
                            int mosaicCount = 0;
                            for (String photoTitle : notesInScene.keySet()) {
                                Note photo = notesInScene.get(photoTitle);

                                JSONArray model_json = rover.modelJson(photo);
                                if (model_json == null)
                                    continue;

                                mosaicCount++;
                                Model model = CameraModel.getModel(model_json);
                                String imageId = MARS_IMAGES.getMission().getImageID(photo.getResources().get(0));
                                quadInitializers.add(new QuadInitializer(model, imageId, photoTitle));
                            }

                            return quadInitializers;
                        }

                        @Override
                        protected void onPostExecute(final List<QuadInitializer> initializers) {
                            mSurfaceView.queueEvent(new Runnable() {
                                @Override
                                public void run() {
                                    Log.d(TAG, "Adding image quads to scene");
                                    for (QuadInitializer i : initializers) {
                                        ImageQuad quad = new ImageQuad(i.model, qLL, i.imageId);
                                        if (!hasChild(quad)) {
                                            quad.setDrawingMode(GL_LINE_LOOP);
                                            quad.setMaterial(yellowMaterial);
                                            quad.addLight(mLight);
                                            addChild(quad);
                                            photoQuads.put(i.photoTitle, quad);
                                        }
                                    }
                                }
                            });
                        }
                    }.execute();//when there are no more notes returned, we have all images for this location: add them to the scene
                }
            }
        }

        private void binImagesByPointing(List<Note> imagesForRMC) {
            Rover rover = MARS_IMAGES.getMission();
            for (Note prospectiveImage : imagesForRMC) {
                //filter out any images that aren't on the mast i.e. mosaic-able.
                if (!rover.includedInMosaic(prospectiveImage))
                    continue;

                double[] v2 = CameraModel.pointingVector(rover.modelJson(prospectiveImage));
                double angleThreshold = rover.fieldOfView(prospectiveImage)/20; //less overlap than ~5 degrees for Mastcam is problem for memory: see 42-852 looking south for example
                boolean tooCloseToAnotherImage = false;
                for (String title : notesInScene.keySet()) {
                    Note image = notesInScene.get(title);
                    double[] v1 = CameraModel.pointingVector(rover.modelJson(image));
                    if (CameraModel.angularDistance(v1, v2) < angleThreshold &&
                        M.epsilonEquals(rover.fieldOfView(image), rover.fieldOfView(prospectiveImage))) {
                        tooCloseToAnotherImage = true;
                        break;
                    }
                }
                if (!tooCloseToAnotherImage)
                    notesInScene.put(prospectiveImage.getTitle(), prospectiveImage);
            }
        }
    };

    private int computeIdealImageResolution(Note photoNote) {
        int screenWidthPixels = mSurfaceView.getWidth();
        double viewportFovRadians = Math.toRadians(NOMINAL_FOV / mScaleFactor);
//        Log.d(TAG, "viewport FOV:" + Math.toDegrees(viewportFovRadians));
        String imageId = MARS_IMAGES.getMission().getImageID(photoNote.getResources().get(0));
        String cameraId = MARS_IMAGES.getMission().getCameraId(imageId);
        double cameraFovRadians = MARS_IMAGES.getMission().getCameraFOV(cameraId);

        int idealPixelResolution = (int)Math.floor(screenWidthPixels * cameraFovRadians / viewportFovRadians);

        //as much as it pains the scientist in me, cut down the MMM image resolution by half. There're just too darn many of them.
        if (cameraId.charAt(0)=='M') idealPixelResolution /= 2;
//        Log.d(TAG, "Ideal pixel resolution: "+idealPixelResolution);
        return idealPixelResolution;
    }

    private void setCameraLookDirection() {
        if (!gyroEnabled) {
            //set camera rotation and field of view based on gesture input
            rotationX += cameraRelativeXMotion * TOUCH_SCALE_FACTOR;
            rotationY += cameraRelativeYMotion * TOUCH_SCALE_FACTOR;
            cameraRelativeXMotion = cameraRelativeYMotion = 0f;
            rotationY = Math.max(rotationY, Y_ROTATION_LOWER_LIMIT);
            rotationY = Math.min(rotationY, Y_ROTATION_UPPER_LIMIT);
            M.quatva(y_axis, rotationX, rotAz);
            M.quatva(x_axis, rotationY, rotEl);
        } else {
            M.quatva(y_axis, deviceAzimuth, rotAz);
            M.quatva(x_axis, devicePitch, rotEl);
        }
        M.multqv(rotEl, forwardVector, look1);
        M.multqv(rotAz, look1, look2);
        mCamera.setLookAt((float) look2[0], (float) look2[1], (float) look2[2]);
    }

    private void addHoverCompass() {
        int[] maxTextureSize = new int[1];
        GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, maxTextureSize, 0);
        Bitmap bg = null;
        if (maxTextureSize[0] >= 2048)
            bg = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.hover_compass_2k);
        else
            bg = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.hover_compass_1k);

        plane = new Plane(10, 10, 2, 2);
        plane.setPosition(0f,COMPASS_HEIGHT,0f);
        plane.setRotX(-90);
        plane.setMaterial(new SimpleMaterial(AMaterial.ALPHA_MASKING));
        plane.addTexture(mTextureManager.addTexture(bg, TextureManager.TextureType.DIFFUSE, false, true));
        bg.recycle();
        plane.addLight(mLight);
        addChild(plane);
    }

    private static class QuadInitializer {
        public String imageId;
        public Model model;
        public String photoTitle;

        public QuadInitializer(Model model, String imageId, String photoTitle) {
            this.model = model;
            this.imageId = imageId;
            this.photoTitle = photoTitle;
        }
    }
}
