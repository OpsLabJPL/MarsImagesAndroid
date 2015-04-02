package gov.nasa.jpl.hi.marsimages.models;

/**
 * Created by mpowell on 3/22/15.
 */
public class CAHVORE extends CAHVOR {

    public static final int MAX_NEWTON = 100;
    public static final double EPSILON = 1e-15;

    private double[] e = new double[3];
    private int mType;
    private double mParm;

    private double[] cp = new double[3], lambdap3 = new double[3], ri = new double[3], rp = new double[3], u3 = new double[3], v3 = new double[3], w3 = new double[3];

    public void setE(double e0, double e1, double e2) {
        e[0] = e0; e[1] = e1; e[2] = e2;
    }

    public void setType(int mtype) {
        this.mType = mtype;
    }

    public void setP(double mparm) {
        this.mParm = mparm;
    }

    @Override
    public void cmod_2d_to_3d(double[] pos2, double[] pos3, double[] uvec3) {
        double avh1;
        double chi;
        double chi2;
        double chi3;
        double chi4;
        double chi5;
        double chip;
        double lambdap;
        double linchi;
        double theta;
        double theta2;
        double theta3;
        double theta4;
        double zetap;
        double linearity = 0;

    /* In the following there is a mixture of nomenclature from several */
    /* versions of Gennery's write-ups and Litwin's software. Beware!   */

        chi = 0;
        chi3 = 0;
        theta = 0;
        theta2 = 0;
        theta3 = 0;
        theta4 = 0;

    /* Calculate initial terms */

        M.scale(pos2[1],a,u3);
        M.sub(v,u3,u3);
        M.scale(pos2[0],a,v3);
        M.sub(h,v3,v3);
        M.cross(u3,v3,w3);
        M.cross(v,h,u3);
        avh1 = M.dot(a,u3);
        avh1 = 1/avh1;
        M.scale(avh1,w3,rp);

        zetap = M.dot(rp,o);

        M.scale(zetap,o,u3);
        M.sub(rp,u3,lambdap3);

        lambdap = M.mag(lambdap3);

        chip = lambdap / zetap;

    /* Approximations for small angles */
        if (chip < 1e-8) {
            M.copy(c,cp);
            M.copy(o,ri);
        }

    /* Full calculations */
        else {
            int n;
            double dchi;
            double s;

        /* Calculate chi using Newton's Method */
            n = 0;
            chi = chip;
            dchi = 1;
            for (;;) {
                double deriv;

            /* Make sure we don't iterate forever */
                if (++n > MAX_NEWTON) {
                    break;
                }

            /* Compute terms from the current value of chi */
                chi2 = chi * chi;
                chi3 = chi * chi2;
                chi4 = chi * chi3;
                chi5 = chi * chi4;

            /* Check exit criterion from last update */
                if (Math.abs(dchi) < 1e-8) {
                    break;
                }

            /* Update chi */
                deriv = (1 + r[0]) + 3*r[1]*chi2 + 5*r[2]*chi4;
                dchi = ((1 + r[0])*chi + r[1]*chi3 + r[2]*chi5 - chip) / deriv;
                chi -= dchi;
            }

        /* Compute the incoming ray's angle */
            linchi = linearity * chi;
            theta = chi;

            theta2 = theta * theta;
            theta3 = theta * theta2;
            theta4 = theta * theta3;

        /* Compute the shift of the entrance pupil */
            s = Math.sin(theta);
            s = (theta/s - 1) * (e[0] + e[1]*theta2 + e[2]*theta4);

        /* The position of the entrance pupil */
            M.scale(s,o,cp);
            M.add(c,cp,cp);

        /* The unit vector along the ray */
            M.unit(lambdap3,u3);
            M.scale(Math.sin(theta),u3,u3);
            M.scale(Math.cos(theta),o,v3);
            M.add(u3,v3,ri);
        }

        M.copy(cp,pos3);
        M.copy(ri,uvec3);
    }

}
