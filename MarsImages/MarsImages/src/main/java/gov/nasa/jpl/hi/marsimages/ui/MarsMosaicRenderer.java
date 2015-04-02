package gov.nasa.jpl.hi.marsimages.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.opengl.GLSurfaceView;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.evernote.edam.type.Note;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glViewport;
import static gov.nasa.jpl.hi.marsimages.EvernoteMars.EVERNOTE;
import static gov.nasa.jpl.hi.marsimages.MarsImagesApp.MARS_IMAGES;
import static gov.nasa.jpl.hi.marsimages.MarsImagesApp.TAG;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import gov.nasa.jpl.hi.marsimages.EvernoteMars;
import gov.nasa.jpl.hi.marsimages.JsonReader;
import gov.nasa.jpl.hi.marsimages.MarsImagesApp;
import gov.nasa.jpl.hi.marsimages.models.CameraModel;
import gov.nasa.jpl.hi.marsimages.models.M;
import gov.nasa.jpl.hi.marsimages.rovers.Rover;
import rajawali.lights.ALight;
import rajawali.lights.DirectionalLight;
import rajawali.lights.PointLight;
import rajawali.materials.DiffuseMaterial;
import rajawali.primitives.Plane;
import rajawali.primitives.Sphere;
import rajawali.renderer.RajawaliRenderer;

/**
 * Created by mpowell on 3/21/15.
 */
public class MarsMosaicRenderer extends RajawaliRenderer {

    private PointLight mLight;
//    private Sphere mSphere;
    private Plane mPlane;
    private int[] rmc;
    private Map<String, Note> photosInScene;

    public MarsMosaicRenderer(Context context) {
        super(context);
        setFrameRate(60);
        IntentFilter filter = new IntentFilter(EvernoteMars.END_NOTE_LOADING);
        LocalBroadcastManager.getInstance(context).registerReceiver(mMessageReceiver, filter);
    }

    protected void initScene() {
        mLight = new PointLight();
        mLight.setColor(1.0f, 1.0f, 1.0f);
        mLight.setPower(2);

        //    Bitmap bg = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.earthtruecolor_nasa_big);
        DiffuseMaterial material = new DiffuseMaterial();
//        mSphere = new Sphere(1, 18, 18);
//        mSphere.setMaterial(material);
//        mSphere.addLight(mLight);
        //    mSphere.addTexture(mTextureManager.addTexture(bg));
//        addChild(mSphere);
        mPlane = new Plane(2, 2, 1, 1);
        mPlane.setMaterial(material);
        mPlane.addLight(mLight);
        addChild(mPlane);

        mCamera.setZ(4.2f);
        int[] rmc = EVERNOTE.getNearestRMC();
        addImagesToScene(rmc);
    }

    public void onDrawFrame(GL10 glUnused) {
        super.onDrawFrame(glUnused);
        mPlane.setRotY(mPlane.getRotY() + 1);
    }

    public void addImagesToScene(int[] rmc) {
        this.rmc = rmc;
        Rover mission = MARS_IMAGES.getMission();
        int site_index = rmc[0];
        int drive_index = rmc[1];
        double[] qLL = mission.localLevelQuaternion(site_index, drive_index);
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
                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... voids) {
                            List<Note> notes = EVERNOTE.getNotes();
                            photosInScene = new LinkedHashMap<String, Note>();
                            binImagesByPointing(notes);
                            int mosaicCount = 0;
                            for (String photoTitle : photosInScene.keySet()) {
                                Note photo = photosInScene.get(photoTitle);

                                JSONArray model_json = rover.modelJson(photo);
                                if (model_json == null)
                                    continue;

                                mosaicCount++;
//                                id<Model> model = [CameraModel model:model_json];
//                                NSString* imageId = [[MarsImageNotebook instance].mission imageId:photo.resource];
//                                ImageQuad* imageQuad = [[ImageQuad alloc] initWithModel:model qLL:qLL imageID:imageId];
                            }
                            return null;
                        }
                    }.execute();                    //when there are no more notes returned, we have all images for this location: add them to the scene
//                    dispatch_async(downloadQueue, ^{

//                        dispatch_async(dispatch_get_main_queue(), ^{
//                                _photoQuads[photo.note.title] = imageQuad;
//                        });
                }
//                    dispatch_async(dispatch_get_main_queue(), ^{
//                            [((MosaicViewController*)_viewController) hideHud];
//                    });
//                    });
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
}
