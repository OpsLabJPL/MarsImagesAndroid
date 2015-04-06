package gov.nasa.jpl.hi.marsimages.models;

/**
 * Created by mpowell on 3/22/15.
 */
public class CAHV extends CameraModel {

    protected int xdim;
    protected int ydim;
    protected double c[] = new double[3];
    protected double a[] = new double[3];
    protected double h[] = new double[3];
    protected double v[] = new double[3];

    protected double f[] = new double[3];
    protected double g[] = new double[3];
    protected double t[] = new double[3];
    protected double d[] = new double[3];

    @Override
    public int[] size() {
        return new int[0]; //TODO
    }

    public void setC(double c0, double c1, double c2) {
        c[0] = c0; c[1] = c1; c[2] = c2;
    }

    public void setA(double a0, double a1, double a2) {
        a[0] = a0; a[1] = a1; a[2] = a2;
    }

    public void setH(double h0, double h1, double h2) {
        h[0] = h0; h[1] = h1; h[2] = h2;
    }

    public void setV(double v0, double v1, double v2) {
        v[0] = v0; v[1] = v1; v[2] = v2;
    }

    public void setXdim(int width) { xdim = width; }
    public void setYdim(int height) { ydim = height; }
    @Override
    public double xdim() { return xdim; }
    @Override
    public double ydim() { return ydim; }


    @Override
    public void cmod_2d_to_3d(double[] pos2, double[] pos3, double[] uvec3) {
        double magi;
        double sgn;

    /* The projection point is merely the C of the camera model */
        M.copy(c,pos3);

    /* Calculate the projection ray assuming normal vector directions */
        M.scale(pos2[1],a,f);
        M.sub(v,f,f);
        M.scale(pos2[0],a,g);
        M.sub(h,g,g);
        M.cross(f,g,uvec3);
        magi = M.mag(uvec3);
        magi = 1.0/magi;
        M.scale(magi,uvec3,uvec3);

    /* Check and optionally correct for vector directions */
        M.cross(v,h,t);
        if (M.dot(t,a) < 0) {
            M.scale(-1.0,uvec3,uvec3);
        }
    }

    @Override
    public void cmod_3d_to_2d(double[] pos3, double[] range, double[] pos2) {
        double r_1;

    /* Calculate the projection */
        M.sub(pos3,c,d);
        range[0] = M.dot(d,a);
        r_1 = 1.0 / range[0];
        pos2[0] = M.dot(d,h) * r_1;
        pos2[1] = M.dot(d,v) * r_1;
    }
}
