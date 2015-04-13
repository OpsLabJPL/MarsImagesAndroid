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
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import com.evernote.edam.type.Note;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageSize;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import com.powellware.marsimages.R;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
import rajawali.materials.SimpleAlphaMaterial;
import rajawali.materials.SimpleMaterial;
import rajawali.materials.TextureInfo;
import rajawali.materials.TextureManager;
import rajawali.primitives.Plane;
import rajawali.renderer.RajawaliRenderer;

import static android.opengl.GLES20.GL_LINE_LOOP;
import static android.opengl.GLES20.GL_TRIANGLE_FAN;
import static gov.nasa.jpl.hi.marsimages.EvernoteMars.EVERNOTE;
import static gov.nasa.jpl.hi.marsimages.MarsImagesApp.MARS_IMAGES;
import static gov.nasa.jpl.hi.marsimages.MarsImagesApp.TAG;

/**
 * Created by mpowell on 3/21/15.
 */
public class MarsMosaicRenderer extends RajawaliRenderer implements View.OnTouchListener {

    public static final float MIN_ZOOM = 0.5f;
    public static final float MAX_ZOOM = 5.0f;
    private static final double Y_ROTATION_UPPER_LIMIT = Math.PI / 2;
    private static final double Y_ROTATION_LOWER_LIMIT = -Math.PI / 2;
    private final float TOUCH_SCALE_FACTOR = .0015f;
    private final float COMPASS_HEIGHT = 0.5f;
    private final double[] x_axis = {1f, 0f, 0f};
    private final double[] y_axis = {0f, 1f, 0f};

    private PointLight mLight;
    private int[] rmc;
    private double[] qLL;
    private Map<String, Note> photosInScene = new ConcurrentHashMap<>();
    private Map<String, Boolean> imagesLoading = new ConcurrentHashMap<>();
    private float mPreviousX = 0f;
    private float mPreviousY = 0f;
    private float cameraRelativeXMotion = 0f;
    private float cameraRelativeYMotion = 0f;
    private final ScaleGestureDetector mScaleDetector;
    private float mScaleFactor = 1f;
    private DiffuseMaterial yellowMaterial;
    private Quad mCompass;
    private Map<String, ImageQuad> photoQuads = new ConcurrentHashMap<>();
    private Map<String, TextureInfo> photoTextures = new ConcurrentHashMap<>();
    private double[] forwardVector = {0, 0, -1};
    private double rotAz[] = new double[4], rotEl[] = new double[4];
    private double look1[] = new double[3], look2[] = new double[3];
    private double rotationX = 0;
    private double rotationY = 0;

    public MarsMosaicRenderer(Context context) {
        super(context);

        //framework places camera at Z=4 by default. Fix that.
        mEyeZ = 0f;
        mCamera.setZ(mEyeZ);

        setFrameRate(60);
        IntentFilter filter = new IntentFilter(EvernoteMars.END_NOTE_LOADING);
        LocalBroadcastManager.getInstance(context).registerReceiver(mMessageReceiver, filter);
        mScaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float scaleFactor = detector.getScaleFactor();
//                Log.d(TAG, "scale factor: "+scaleFactor);
                mScaleFactor *= scaleFactor;
                // Don't let the object get too small or too large.
                mScaleFactor = Math.max(MIN_ZOOM, Math.min(mScaleFactor, MAX_ZOOM));
                return true;
            }
        });
    }

    protected void initScene() {
        mLight = new PointLight();
        mLight.setColor(1.0f, 1.0f, 1.0f);

        yellowMaterial = new DiffuseMaterial();
        yellowMaterial.setAmbientColor(1f, 1f, 0f, 1f);

        addHoverCompass();

        int[] rmc = EVERNOTE.getNearestRMC();
        addImagesToScene(rmc);
    }

    private void addHoverCompass() {
        int[] maxTextureSize = new int[1];
        GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, maxTextureSize, 0);
        Bitmap bg = null;
        if (maxTextureSize[0] >= 2048)
            bg = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.hover_compass_2k);
        else
            bg = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.hover_compass_1k);

        Plane plane = new Plane(10, 10, 2, 2);
        plane.setPosition(0f,COMPASS_HEIGHT,0f);
        plane.setRotX(-90);
        plane.setMaterial(new SimpleMaterial(AMaterial.ALPHA_MASKING));
        plane.addTexture(mTextureManager.addTexture(bg, TextureManager.TextureType.DIFFUSE, false, true));
        bg.recycle();
        plane.addLight(mLight);
        addChild(plane);
    }

    @Override
    public void onDrawFrame(GL10 glUnused) {

        //set camera rotation and field of view based on gesture input
        rotationX += cameraRelativeXMotion * TOUCH_SCALE_FACTOR;
        rotationY += cameraRelativeYMotion * TOUCH_SCALE_FACTOR;
        cameraRelativeXMotion = cameraRelativeYMotion = 0f;

        rotationY = Math.max(rotationY, Y_ROTATION_LOWER_LIMIT);
        rotationY = Math.min(rotationY, Y_ROTATION_UPPER_LIMIT);

        M.quatva(y_axis, rotationX, rotAz);
        M.quatva(x_axis, rotationY, rotEl);
        M.multqv(rotEl, forwardVector, look1);
        M.multqv(rotAz, look1, look2);
        mCamera.setLookAt((float)look2[0], (float)look2[1], (float)look2[2]);
        mCamera.setFieldOfView(45/mScaleFactor);
        mCamera.setFarPlane(12.0f);
        mCamera.setNearPlane(0.1f);
        mCamera.setProjectionMatrix(mSurfaceView.getWidth(), mSurfaceView.getHeight());

        //update imagequads to use image texture or line mode depending on visibility and if the image is loaded yet into memory
        prepareImageQuads();

        super.onDrawFrame(glUnused);
    }

    private void prepareImageQuads() {

        int skippedImages = 0;

        for (String title: photoQuads.keySet()) {
            ImageQuad imageQuad = photoQuads.get(title);
//            //frustum culling: don't draw if the bounding sphere of the image quad isn't in the camera frustum
//            AGLKFrustum frustum = ((MosaicViewController*)_viewController).frustum;
//            if (AGLKFrustumCompareSphere(&frustum, imageQuad.center, imageQuad.boundingSphereRadius) == AGLKFrustumOut) {
//                skippedImages++;
//                [self deleteImageAndTexture: title];
//                continue;
//            }
            if (imageQuad.getTextureInfoList().size() == 0)
                prepareImageQuad(imageQuad, title);
        }
    }

    private void prepareImageQuad(ImageQuad imageQuad, String title) {
        TextureInfo textureInfo = photoTextures.get(title);
        if (textureInfo != null) {
            if (!imageQuad.getTextureInfoList().contains(textureInfo)) {
                imageQuad.setDrawingMode(GL_TRIANGLE_FAN);
                imageQuad.setMaterial(new SimpleMaterial());
                imageQuad.addTexture(textureInfo);
            }
        }
        else {
            synchronized (this){
                loadImageAndTexture(title);
            }
        }
    }

    private void loadImageAndTexture(final String title) {
        final Note photo = photosInScene.get(title);
        if (photo == null) {
            Log.w(TAG, "No photo in scene with title "+title);
            return;
        }

        //TODO get the image size right
        Boolean isLoading = imagesLoading.get(title);
        if (isLoading == null || isLoading == false) {
            imagesLoading.put(title, true);
            ImageLoader.getInstance().resume(); //in case the image loader engine is currently paused
            final String uri = EVERNOTE.getUri(photo.getResources().get(0));
            ImageLoader.getInstance().loadImage(uri, new ImageSize(512, 512), new SimpleImageLoadingListener() {
                @Override
                public void onLoadingComplete(String imageUri, View view, final Bitmap loadedImage) {
                    int width = loadedImage.getWidth();
                    int height = loadedImage.getHeight();
                    Log.d(TAG, "Loaded image size: "+ width +"x"+ height +" for image " + uri);
                    final Bitmap texture = (width != 512 || height != 512) ? Bitmap.createScaledBitmap(loadedImage, 512, 512, true) : loadedImage;
                    mSurfaceView.queueEvent(new Runnable() {
                        @Override
                        public void run() {
                            TextureInfo textureInfo = mTextureManager.addTexture(texture, TextureManager.TextureType.DIFFUSE, false, false);
                            photoTextures.put(title, textureInfo);
                            imagesLoading.put(title, false);
                        }
                    });
                }
            });
        }
//        UIImage* image = [photo underlyingImage];
//        if (!image) {
//            if (!photo.isLoading) {
//                [photo performLoadUnderlyingImageAndNotify];
//            }
//        } else {
//            [self makeTexture:image withTitle:title grayscale:[photo isGrayscale]];
//            [photo unloadUnderlyingImage]; //improves memory management quite significantly
//        }
    }

    public void addImagesToScene(int[] rmc) {
        this.rmc = rmc;
        Rover mission = MARS_IMAGES.getMission();
        int site_index = rmc[0];
        int drive_index = rmc[1];
        qLL = mission.localLevelQuaternion(site_index, drive_index);
        EVERNOTE.setSearchWords(String.format("RMC %06d-%06d", site_index, drive_index), mContext);
        EVERNOTE.reloadNotes(mContext); //rely on the resultant note load end broadcast receiver to populate images in the scene
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
                            for (String photoTitle : photosInScene.keySet()) {
                                Note photo = photosInScene.get(photoTitle);

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
                double angleThreshold = rover.fieldOfView(prospectiveImage)/10; //less overlap than ~5 degrees for Mastcam is problem for memory: see 42-852 looking south for example
                boolean tooCloseToAnotherImage = false;
                for (String title : photosInScene.keySet()) {
                    Note image = photosInScene.get(title);
                    double[] v1 = CameraModel.pointingVector(rover.modelJson(image));
                    if (CameraModel.angularDistance(v1, v2) < angleThreshold &&
                        M.epsilonEquals(rover.fieldOfView(image), rover.fieldOfView(prospectiveImage))) {
                        tooCloseToAnotherImage = true;
                        break;
                    }
                }
                if (!tooCloseToAnotherImage)
                    photosInScene.put(prospectiveImage.getTitle(), prospectiveImage);
            }
        }
    };

    @Override
    public boolean onTouch(View v, MotionEvent e) {
        // MotionEvent reports input details from the touch screen
        // and other input controls. In this case, you are only
        // interested in events where the touch position changed.

        // Let the ScaleGestureDetector inspect all events.
        mScaleDetector.onTouchEvent(e);

        float x = e.getX();
        float y = e.getY();
//        Log.d(TAG, "Motion event: "+x+", "+y);
        switch (e.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                mPreviousX = e.getX();
                mPreviousY = e.getY();
                break;

            case MotionEvent.ACTION_MOVE:
                cameraRelativeXMotion += x - mPreviousX;
                cameraRelativeYMotion += y - mPreviousY;

                mPreviousX = x;
                mPreviousY = y;
                break;

            case MotionEvent.ACTION_UP:
                mPreviousX = 0f;
                mPreviousY = 0f;
                break;
        }
        return true;
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
