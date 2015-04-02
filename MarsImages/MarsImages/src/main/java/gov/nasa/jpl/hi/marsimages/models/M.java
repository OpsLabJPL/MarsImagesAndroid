package gov.nasa.jpl.hi.marsimages.models;

/**
 * Created by mpowell on 3/22/15.
 */
public class M {
    public static final double MAT3_EPSILON = 1e-7;
    public static final double LOG2 = Math.log(2);

    public static boolean epsilonEquals(double a, double b) {
        return Math.abs(a - b) <= 0.001;
    }

    public static boolean isPowerOfTwo(int value) {
        return ((value & -value) == value);
    }

    public static int ceilingPowerOfTwo(double x) {
        double y = Math.ceil(Math.log(x)/LOG2);
        return (int)Math.pow(2, y);
    }

    public static int floorPowerOfTwo(double x) {
        double y = Math.floor(Math.log(x)/LOG2);
        return (int)Math.pow(2, y);
    }

    public static int nextHighestPowerOfTwo(int n) {
        double y = Math.floor(Math.log(n)/LOG2);
        return (int)Math.pow(2, y + 1);
    }

    public static int nextLowestPowerOfTwo(int n) {
        double y = Math.floor(Math.log(n)/LOG2);
        return (int)Math.pow(2, y - 1);
    }

    public static void copy(final double[] a, double[] b) {
        b[0] = a[0];
        b[1] = a[1];
        b[2] = a[2];
    }

    public static void scale(final double s, final double[] a, double[] b) {
        b[0] = s*a[0];
        b[1] = s*a[1];
        b[2] = s*a[2];
    }

    public static void add(final double[] a, final double[] b, double[] c) {
        c[0] = a[0]+b[0];
        c[1] = a[1]+b[1];
        c[2] = a[2]+b[2];
    }

    public static void sub(final double[] a, final double[] b, double[] c) {
        c[0] = a[0]-b[0];
        c[1] = a[1]-b[1];
        c[2] = a[2]-b[2];
    }

    public static void cross(final double[] a, final double[] b, double[] c) {
        c[0]  =  a[1] * b[2] - a[2] * b[1];
        c[1]  =  a[2] * b[0] - a[0] * b[2];
        c[2]  =  a[0] * b[1] - a[1] * b[0];
    }

    public static double dot(final double[] a, final double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    public static double mag(final double[] a) {
        return Math.sqrt(a[0] * a[0] + a[1] * a[1] + a[2] * a[2]);
    }

    public static void unit(final double[] a, double[] b) {
        double mag = Math.sqrt(a[0] * a[0]  +  a[1] * a[1]  +  a[2] * a[2]);
        b[0] = a[0] / mag;
        b[1] = a[1] / mag;
        b[2] = a[2] / mag;
    }

    public static void quatva(final double[] v, final double a, double[] q) {
        double c;
        double s;
        double vmag;

    /* Precompute some needed quantities */
        vmag = Math.sqrt(v[0] * v[0]  +  v[1] * v[1]  +  v[2] * v[2]);
        if (vmag < MAT3_EPSILON) {
            return;
        }
        c = Math.cos(a/2);
        s = Math.sin(a/2);

    /* Construct the quaternion */
        q[0] = c;
        q[1] = s * v[0] / vmag;
        q[2] = s * v[1] / vmag;
        q[3] = s * v[2] / vmag;
    }

    public static void multqv(final double[] q, final double[] v, double[] u) {
        double q0;
        double q1;
        double q2;
        double q3;
        double q0q0;
        double q0q1;
        double q0q2;
        double q0q3;
        double q1q1;
        double q1q2;
        double q1q3;
        double q2q2;
        double q2q3;
        double q3q3;

    /* Perform the multiplication */
        q0 = q[0];
        q1 = q[1];
        q2 = q[2];
        q3 = q[3];
        q0q0 = q0 * q0;
        q0q1 = q0 * q1;
        q0q2 = q0 * q2;
        q0q3 = q0 * q3;
        q1q1 = q1 * q1;
        q1q2 = q1 * q2;
        q1q3 = q1 * q3;
        q2q2 = q2 * q2;
        q2q3 = q2 * q3;
        q3q3 = q3 * q3;
        u[0] = v[0]*(q0q0+q1q1-q2q2-q3q3) + 2*v[1]*(q1q2-q0q3) + 2*v[2]*(q0q2+q1q3);
        u[1] = 2*v[0]*(q0q3+q1q2) + v[1]*(q0q0-q1q1+q2q2-q3q3) + 2*v[2]*(q2q3-q0q1);
        u[2] = 2*v[0]*(q1q3-q0q2) + 2*v[1]*(q0q1+q2q3) + v[2]*(q0q0-q1q1-q2q2+q3q3);
    }

/*
 * Convert spherical coordinates to cartesian.
 * The azimuth will range between 0 to 2*PI, measured from the positive x axis,
 * increasing towards the positive y axis.
 * The declination will range between 0 and PI, measured from the positive Z axis
 * (assumed to be down), increasing towards the xy plane.
 */
    public static void sphericalToCartesian(double az, double dec, double radius, double[] xyz) {
        double rsinDec = radius * Math.sin(dec);
        xyz[0] = rsinDec * Math.cos(az);
        xyz[1] = rsinDec * Math.sin(az);
        xyz[2] = radius * Math.cos(dec);
    }

    public static void cartesianToSpherical(final double[] xyz, double[] azDecR) {
        double x = xyz[0], y = xyz[1], z = xyz[2];
        double radius = Math.sqrt(x * x + y * y + z * z);
        double dec = Math.acos(z / radius);
        double az = Math.atan2(y, x);
        if (az < 0) az += 2*Math.PI;
        azDecR[0] = az;
        azDecR[1] = dec;
        azDecR[2] = radius;
    }

}
