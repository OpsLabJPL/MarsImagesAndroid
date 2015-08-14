package gov.nasa.jpl.hi.marsimages.models;

import android.graphics.Bitmap;
import android.util.Log;
import android.view.View;

import com.evernote.edam.type.Note;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageSize;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import gov.nasa.jpl.hi.marsimages.rovers.Rover;
import gov.nasa.jpl.hi.marsimages.ui.MarsMosaicRenderer;
import rajawali.Camera;
import rajawali.materials.SimpleMaterial;
import rajawali.materials.TextureInfo;
import rajawali.materials.TextureManager;
import rajawali.math.Number3D;

import static android.opengl.GLES20.GL_TRIANGLE_FAN;
import static gov.nasa.jpl.hi.marsimages.EvernoteMars.EVERNOTE;
import static gov.nasa.jpl.hi.marsimages.MarsImagesApp.MARS_IMAGES;

/**
 * Created by mpowell on 4/5/15.
 */
public class ImageQuad extends Quad {

    static final double x_axis[] = {1,0,0};
    static final double z_axis[] = {0,0,1};
    static final int numberOfPositions = 4;
    private static final String TAG = "ImageQuad";
    private final Number3D sphereVector;
    private final double roverCameraFOVRadians;
    private String cameraId = null;
    private final int layer;

    private double boundingSphereRadius;
    private String imageId;
    private final float[] center = new float[3];
    private final Number3D mBoundsCenter;

    public boolean isLoading() {
        return loading;
    }

    private boolean loading = false;
    private boolean cancelLoadingRequest = false;
    private int originallyRequestedResolution = 0;
    private Model model;

    static final float textureCoords[] = {0.f, 0.f, 0.f, 1.f, 1.f, 0.f, 1.f, 1.f};
    public TextureInfo textureToAdd = null;

    public ImageQuad(Model model, double[] qLL, String imageID) {
        super();
        this.imageId = imageID;
        this.model = model;
        String cameraId = MARS_IMAGES.getMission().getCameraId(imageID);
        this.cameraId = cameraId;
        roverCameraFOVRadians = MARS_IMAGES.getMission().getCameraFOV(cameraId);
        int layer = 5 + MARS_IMAGES.getMission().getLayer(cameraId, imageID);
        this.layer = layer;

        float[][] vertices = new float[4][];
        for (int i = 0; i < 4; i++) { vertices[i] = new float[3]; }
        getImageVertices(model, qLL, vertices, layer);
        System.arraycopy(vertices[0], 0, v0, 0, 3);
        System.arraycopy(vertices[1],0,v1,0,3);
        System.arraycopy(vertices[2],0,v2,0,3);
        System.arraycopy(vertices[3],0,v3,0,3);

        init();

        center[0] = (v0[0]+v2[0])/2;
        center[1] = (v0[1]+v2[1])/2;
        center[2] = (v0[2]+v2[2])/2;
        mBoundsCenter = new Number3D(center[0], center[1], center[2]);
        sphereVector = new Number3D(mBoundsCenter);
        sphereVector.normalize();
        //assign to the radius the distance from the center to the farthest vertex
        double d0 = distanceBetween(center, v0);
        double d1 = distanceBetween(center, v1);
        double d2 = distanceBetween(center, v2);
        double d3 = distanceBetween(center, v3);

        boundingSphereRadius = d0;
        if (d1>boundingSphereRadius) boundingSphereRadius=d1;
        if (d2>boundingSphereRadius) boundingSphereRadius=d2;
        if (d3>boundingSphereRadius) boundingSphereRadius=d3;
    }

    public Number3D getBoundsCenter() {
        return mBoundsCenter;
    }

    public double getBoundsRadius() {
        return boundingSphereRadius;
    }

    private double cameraFOVRadians() {
        if (cameraId != null)
            return MARS_IMAGES.getMission().getCameraFOV(cameraId);
        throw new IllegalStateException("roverCameraFOVRadians called before cameraId initialized");
    }

    private double distanceBetween(float[] pt1, float[] pt2) {
        float dx = pt1[0]-pt2[0];
        float dy = pt1[1]-pt2[1];
        float dz = pt1[2]-pt2[2];
        return Math.sqrt(dx*dx+dy*dy+dz*dz);
    }

    private void getImageVertices(Model model, double[] qLL, float[][] vertices, float distance) {

        Rover mission = MARS_IMAGES.getMission();
        double eye[] = new double[3];
        double pos[] = new double[2], pos3[] = new double[3], vec3[] = new double[3];
        double pos3LL[]= new double[3], pinitial[] = new double[3], pfinal[] = new double[3];
        double xrotq[] = new double[4];
        M.quatva(x_axis, Math.PI/2, xrotq);
        double zrotq[] = new double[4];
        M.quatva(z_axis, -Math.PI/2, zrotq);
        double llRotq[] = new double[4];

        eye[0] = mission.getMastX();
        eye[1] = mission.getMastY();
        eye[2] = mission.getMastZ();

        llRotq[0] = qLL[0];
        llRotq[1] = qLL[1];
        llRotq[2] = qLL[2];
        llRotq[3] = qLL[3];

        //lower left
        pos[0] = 0;
        pos[1] = model.ydim();
        model.cmod_2d_to_3d(pos, pos3, vec3);
        pos3[0] -= eye[0];
        pos3[1] -= eye[1];
        pos3[2] -= eye[2];
        pos3[0] += vec3[0]*distance;
        pos3[1] += vec3[1]*distance;
        pos3[2] += vec3[2]*distance;
        M.multqv(llRotq, pos3, pos3LL);
        M.multqv(zrotq, pos3LL, pinitial);
        M.multqv(xrotq, pinitial, pfinal);
        vertices[0][0] = (float)pfinal[0];
        vertices[0][1] = (float)pfinal[1];
        vertices[0][2] = (float)pfinal[2];

        //upper left
        pos[0] = 0;
        pos[1] = 0;
        model.cmod_2d_to_3d(pos, pos3, vec3);
        pos3[0] -= eye[0];
        pos3[1] -= eye[1];
        pos3[2] -= eye[2];
        pos3[0] += vec3[0]*distance;
        pos3[1] += vec3[1]*distance;
        pos3[2] += vec3[2]*distance;
        M.multqv(llRotq, pos3, pos3LL);
        M.multqv(zrotq, pos3LL, pinitial);
        M.multqv(xrotq, pinitial, pfinal);
        vertices[1][0] = (float)pfinal[0];
        vertices[1][1] = (float)pfinal[1];
        vertices[1][2] = (float)pfinal[2];

        //lower right
        pos[0] = model.xdim();
        pos[1] = model.ydim();
        model.cmod_2d_to_3d(pos, pos3, vec3);
        pos3[0] -= eye[0];
        pos3[1] -= eye[1];
        pos3[2] -= eye[2];
        pos3[0] += vec3[0]*distance;
        pos3[1] += vec3[1]*distance;
        pos3[2] += vec3[2]*distance;
        M.multqv(llRotq, pos3, pos3LL);
        M.multqv(zrotq, pos3LL, pinitial);
        M.multqv(xrotq, pinitial, pfinal);
        vertices[2][0] = (float)pfinal[0];
        vertices[2][1] = (float)pfinal[1];
        vertices[2][2] = (float)pfinal[2];

        //upper right
        pos[0] = model.xdim();
        pos[1] = 0;
        model.cmod_2d_to_3d(pos, pos3, vec3);
        pos3[0] -= eye[0];
        pos3[1] -= eye[1];
        pos3[2] -= eye[2];
        pos3[0] += vec3[0]*distance;
        pos3[1] += vec3[1]*distance;
        pos3[2] += vec3[2]*distance;
        M.multqv(llRotq, pos3, pos3LL);
        M.multqv(zrotq, pos3LL, pinitial);
        M.multqv(xrotq, pinitial, pfinal);
        vertices[3][0] = (float)pfinal[0];
        vertices[3][1] = (float)pfinal[1];
        vertices[3][2] = (float)pfinal[2];
    }

    public boolean cameraIsLookingAtMe(Camera viewPortCamera) {
        Number3D cameraPointing = viewPortCamera.getLookAt();

        double angleBetweenRadians = Math.acos(Number3D.dot(cameraPointing, sphereVector));

//        return angleBetweenRadians - Math.toRadians(camera.getFieldOfView()) - roverCameraFOVRadians <= 0;
        return angleBetweenRadians - roverCameraFOVRadians*.7071 < Math.toRadians(viewPortCamera.getFieldOfView())*.7071;
    }


    public void loadImageAndTexture(final Note photo, final String title, final int resolution, final MarsMosaicRenderer renderer) {
        if (photo == null) {
            Log.e(TAG, "No photo in scene with title " + title);
            return;
        }

        synchronized (this) {
            cancelLoadingRequest = false;
            if (loading) {
                return;
            }
            loading = true;
            final int bestTextureResolution = computeBestTextureResolution(resolution);
//            Log.d(TAG, "Best texture resolution: "+bestTextureResolution);
            originallyRequestedResolution = bestTextureResolution;
        }

        ImageLoader.getInstance().resume(); //in case the image loader engine is currently paused
        final String uri = EVERNOTE.getUri(photo.getResources().get(0));
        ImageLoader.getInstance().loadImage(uri, new ImageSize(originallyRequestedResolution, originallyRequestedResolution), new SimpleImageLoadingListener() {
            @Override
            public void onLoadingComplete(String imageUri, View view, final Bitmap loadedImage) {

                //if this image is no longer in the viewport, early out
                synchronized (ImageQuad.this) {
                    if (!loading) {
                        return;
                    }

                    if (cancelLoadingRequest) {
                        loading = false;
                        cancelLoadingRequest = false;
                        return;
                    }

                    int width = loadedImage.getWidth();
                    int height = loadedImage.getHeight();

                    if (!getTextureInfoList().isEmpty()) {
                        int testWidth = width, testHeight = height;
                        if (testWidth != originallyRequestedResolution || testHeight != originallyRequestedResolution) {
                            testWidth = originallyRequestedResolution; testHeight = originallyRequestedResolution;
                        }
                        TextureInfo info = getTextureInfoList().get(0);
                        if (testWidth == info.getWidth() || testHeight == info.getHeight()) {
                            loading = false;
                            return;
                        }
                    }

//                    Log.d(TAG, "Loaded image size: " + width + "x" + height + " for image " + title);
                    final Bitmap texture = (width != originallyRequestedResolution || height != originallyRequestedResolution) ?
                            Bitmap.createScaledBitmap(loadedImage, originallyRequestedResolution, originallyRequestedResolution, true) : loadedImage;
                    if (!loading) return;

                    int bestTextureResolution = computeBestTextureResolution(resolution);
                    if (bestTextureResolution != originallyRequestedResolution) {
                        Log.d(TAG, "Texture resolution mismatch: "+bestTextureResolution + " vs. "+originallyRequestedResolution);
                        loading = false;
                        return;
                    }

                    renderer.getSurfaceView().queueEvent(new Runnable() {
                        @Override
                        public void run() {

                            Runnable runnableForGL = new Runnable() {
                                @Override
                                public void run() {
                                    renderer.getTextureManager().removeTextures(getTextureInfoList());
                                    for (TextureInfo textureInfo : getTextureInfoList())
                                        removeTexture(textureInfo);
                                    getTextureInfoList().clear();
                                    final TextureInfo textureInfo = renderer.getTextureManager().addTexture(texture, TextureManager.TextureType.DIFFUSE, false, false);

                                    setDrawingMode(GL_TRIANGLE_FAN);
                                    setMaterial(new SimpleMaterial());
                                    addTexture(textureInfo);
                                    loading = false;
                                }
                            };
                            renderer.glRunnables.add(runnableForGL);
                        }
                    });
                }
            }

            @Override
            public void onLoadingCancelled(String imageUri, View view) {
                Log.e(TAG, "Loading cancelled for "+imageUri);
                loading = false;
                super.onLoadingCancelled(imageUri, view);
            }

            @Override
            public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                Log.e(TAG, "Loading failed for "+imageUri);
                loading = false;
                super.onLoadingFailed(imageUri, view, failReason);
            }
        });
    }

    private int computeBestTextureResolution(int resolution) {
        int largestImageDimension = (int)Math.max(model.xdim(), model.ydim());
        int bestImageResolution = Math.min(largestImageDimension, resolution);
//        Log.d(TAG, "Texture res: " + M.floorPowerOfTwo(bestImageResolution));
        return M.floorPowerOfTwo(bestImageResolution);
    }

    public void stopLoading() {
        synchronized (this) {
            cancelLoadingRequest = true;
        }
    }
}
