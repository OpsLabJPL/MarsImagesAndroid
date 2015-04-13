package gov.nasa.jpl.hi.marsimages.models;

import gov.nasa.jpl.hi.marsimages.EvernoteMars;
import gov.nasa.jpl.hi.marsimages.MarsImagesApp;
import gov.nasa.jpl.hi.marsimages.rovers.Rover;
import rajawali.math.Quaternion;

import static gov.nasa.jpl.hi.marsimages.MarsImagesApp.MARS_IMAGES;

/**
 * Created by mpowell on 4/5/15.
 */
public class ImageQuad extends Quad {

    static final double x_axis[] = {1,0,0};
    static final double z_axis[] = {0,0,1};
    static final int numberOfPositions = 4;
    private String cameraId = null;
    private final int layer;

    private double boundingSphereRadius;
    private String imageId;
    public final float[] center = new float[3];

    static final float textureCoords[] = {0.f, 0.f, 0.f, 1.f, 1.f, 0.f, 1.f, 1.f};

    public ImageQuad(Model model, double[] qLL, String imageID) {
        super();
        this.imageId = imageID;
        String cameraId = MARS_IMAGES.getMission().getCameraId(imageID);
        this.cameraId = cameraId;
        int layer = MARS_IMAGES.getMission().getLayer(cameraId, imageID);
        this.layer = layer;

        float[][] vertices = new float[4][];
        for (int i = 0; i < 4; i++) { vertices[i] = new float[3]; }
        getImageVertices(model, qLL, vertices, layer);
        System.arraycopy(vertices[0],0,v0,0,3);
        System.arraycopy(vertices[1],0,v1,0,3);
        System.arraycopy(vertices[2],0,v2,0,3);
        System.arraycopy(vertices[3],0,v3,0,3);

        center[0] = (v0[0]+v2[0])/2;
        center[1] = (v0[1]+v2[1])/2;
        center[2] = (v0[2]+v2[2])/2;

        //assign to the radius the distance from the center to the farthest vertex
        double d0 = distanceBetween(center, v0);
        double d1 = distanceBetween(center, v1);
        double d2 = distanceBetween(center, v2);
        double d3 = distanceBetween(center, v3);

        boundingSphereRadius = d0;
        if (d1>boundingSphereRadius) boundingSphereRadius=d1;
        if (d2>boundingSphereRadius) boundingSphereRadius=d2;
        if (d3>boundingSphereRadius) boundingSphereRadius=d3;

        init();
    }


    private double cameraFOVRadians() {
        if (cameraId != null)
            return MARS_IMAGES.getMission().getCameraFOV(cameraId);
        throw new IllegalStateException("cameraFOVRadians called before cameraId initialized");
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
}
