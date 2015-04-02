package gov.nasa.jpl.hi.marsimages.models;

/**
 * Created by mpowell on 3/22/15.
 */
public class CAHVOR extends CAHV {

    public static final double EPSILON = 1e-15;
    public static final int MAXITER = 20;
    public static final double CONV = 1.0e-8;

    protected double[] o = new double[3];
    protected double[] r = new double[3];

    protected double[] p_c = new double[3], pp = new double[3], pp_c = new double[3], wo = new double[3], lambda = new double[3], rr = new double[3];


    public void setO(double o0, double o1, double o2) {
        o[0] = o0; o[1] = o1; o[2] = o2;
    }

    public void setR(double r0, double r1, double r2) {
        r[0] = r0; r[1] = r1; r[2] = r2;
    }

    @Override
    public void cmod_2d_to_3d(double[] pos2, double[] pos3, double[] uvec3) {
        int i;
        double deriv;
        double du;
        double k1;
        double k3;
        double k5;
        double magi;
        double magv;
        double mu;
        double omega;
        double omega_2;
        double poly;
        double sgn;
        double tau;
        double u;
        double u_2;

    /* The projection point is merely the C of the camera model. */
        M.copy(c,pos3);

    /* Calculate the projection ray assuming normal vector directions, */
    /* neglecting distortion.                                          */
        M.scale(pos2[1],a,f);
        M.sub(v,f,f);
        M.scale(pos2[0],a,g);
        M.sub(h,g,g);
        M.cross(f,g,rr);
        magi = M.mag(rr);
        magi = 1.0/magi;
        M.scale(magi,rr,rr);

    /* Check and optionally correct for vector directions. */
        sgn = 1;
        M.cross(v,h,t);
        if (M.dot(t,a) < 0) {
            M.scale(-1.0,rr,rr);
            sgn = -1;
        }

    /* Remove the radial lens distortion.  Preliminary values of omega,  */
    /* lambda, and tau are computed from the rr vector including         */
    /* distortion, in order to obtain the coefficients of the equation   */
    /* k5*u^5 + k3*u^3 + k1*u = 1, which is solved for u by means of     */
    /* Newton's method.  This value is used to compute the corrected rr. */
        omega = M.dot(rr,o);
        omega_2 = omega * omega;
        M.scale(omega,o,wo);
        M.sub(rr,wo,lambda);
        tau = M.dot(lambda,lambda) / omega_2;
        k1 = 1 + r[0];		/*  1 + rho0 */
        k3 = r[1] * tau;		/*  rho1*tau  */
        k5 = r[2] * tau*tau;	/*  rho2*tau^2  */
        mu = r[0] + k3 + k5;
        u = 1.0 - mu;	/* initial approximation for iterations */
        for (i=0; i<MAXITER; i++) {
            u_2 = u*u;
            poly  =  ((k5*u_2  +  k3)*u_2 + k1)*u - 1;
            deriv = (5*k5*u_2 + 3*k3)*u_2 + k1;
            if (deriv <= EPSILON) {
                break;
            }
            else {
                du = poly/deriv;
                u -= du;
                if (Math.abs(du) < CONV) {
                    break;
                }
            }
        }
        mu = 1 - u;
        M.scale(mu,lambda,pp);
        M.sub(rr,pp,uvec3);
        magv = M.mag(uvec3);

        M.scale(1.0/magv,uvec3,uvec3);
    }

    @Override
    public void cmod_3d_to_2d(double[] pos3, double[] range, double[] pos2) {
        double alpha, beta, gamma, xh, yh;
        double omega, omega_2, tau, mu;

    /* Calculate p' and other necessary quantities */
        M.sub(pos3,c,p_c);
        omega = M.dot(p_c,o);
        omega_2 = omega * omega;
        M.scale(omega,o,wo);
        M.sub(p_c,wo,lambda);
        tau = M.dot(lambda,lambda) / omega_2;
        mu = r[0] + (r[1] * tau) + (r[2] * tau * tau);
        M.scale(mu,lambda,pp);
        M.add(pos3,pp,pp);

    /* Calculate alpha, beta, gamma, which are (p' - c) */
    /* dotted with a, h, v, respectively                */
        M.sub(pp,c,pp_c);
        alpha  = M.dot(pp_c,a);
        beta   = M.dot(pp_c,h);
        gamma  = M.dot(pp_c,v);

    /* Calculate the projection */
        pos2[0] = xh = beta  / alpha;
        pos2[1] = yh = gamma / alpha;
        range[0] = alpha;
    }
}
