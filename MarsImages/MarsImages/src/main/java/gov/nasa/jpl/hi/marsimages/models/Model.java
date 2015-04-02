package gov.nasa.jpl.hi.marsimages.models;

/**
 * Created by mpowell on 3/22/15.
 */
public interface Model {

    double xdim();
    double ydim();

    void cmod_2d_to_3d(double pos2[], double pos3[], double uvec3[]);
    void cmod_3d_to_2d(double pos3[], double range[], double pos2[]);

}
