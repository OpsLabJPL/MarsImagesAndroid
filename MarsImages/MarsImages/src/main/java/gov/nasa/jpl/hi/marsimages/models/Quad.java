package gov.nasa.jpl.hi.marsimages.models;

/**
 * Created by mpowell on 4/4/15.
 */

import rajawali.BaseObject3D;

public class Quad extends BaseObject3D {


    protected final float[] v0 = new float[3];
    protected final float[] v1 = new float[3];
    protected final float[] v2 = new float[3];
    protected final float[] v3 = new float[3];
    public static final int NUM_VERTICES = 4;

    public Quad() {
        super();
    }

    public Quad(float[] v0, float[] v1, float[] v2, float[] v3) {
        super();
        System.arraycopy(v0, 0, this.v0, 0, 3);
        System.arraycopy(v1, 0, this.v1, 0, 3);
        System.arraycopy(v2, 0, this.v2, 0, 3);
        System.arraycopy(v3,0,this.v3,0,3);

        init();
    }

    public Quad(float[] v0, float[] v1, float[] v2, float[] v3, float[] normal) {
        this(v0, v1, v2, v3);
        float[] normals = new float[NUM_VERTICES*3];
        for (int i = 0; i < NUM_VERTICES; i++)
            System.arraycopy(normal, 0, normals, i*3, 3);
        mGeometry.setNormals(normals);
    }

    protected void init() {
        float[] vertices = new float[NUM_VERTICES * 3];
        float[] textureCoords = new float[NUM_VERTICES * 2];
        float[] normals = new float[NUM_VERTICES * 3];
        float[] colors = new float[NUM_VERTICES * 4];
        int[] indices = new int[4];

        System.arraycopy(v0, 0, vertices, 0, 3);
        System.arraycopy(v1, 0, vertices, 3, 3);
        System.arraycopy(v2, 0, vertices, 6, 3);
        System.arraycopy(v3, 0, vertices, 9, 3);

        textureCoords[0] = 0.0f;
        textureCoords[1] = 1.0f;
        textureCoords[2] = 0.0f;
        textureCoords[3] = 0.0f;
        textureCoords[4] = 1.0f;
        textureCoords[5] = 1.0f;
        textureCoords[6] = 1.0f;
        textureCoords[7] = 0.0f;

        //give every vertex a Z=1 normal
        for (int i = 0; i < NUM_VERTICES; i++) {
            normals[i*3] = 0;
            normals[i*3 + 1] = 0;
            normals[i*3 + 2] = 1;
        }

        int ul = 0;
        int ll = 1;
        int ur = 2;
        int lr = 3;

        indices[0] = (int) ll;
        indices[1] = (int) ul;
        indices[2] = (int) ur;
        indices[3] = (int) lr;

        int numColors = NUM_VERTICES * 4;
        for (int j = 0; j < numColors; j += 4) {
            colors[j] = 1.0f;
            colors[j + 1] = 1.0f;
            colors[j + 2] = 1.0f;
            colors[j + 3] = 1.0f;
        }

        setData(vertices, normals, textureCoords, colors, indices);
    }
}
