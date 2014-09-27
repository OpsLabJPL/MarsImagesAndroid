package gov.nasa.jpl.hi.marsimages.test;

import android.test.InstrumentationTestCase;

import java.util.Date;

import gov.nasa.jpl.hi.marsimages.rovers.MarsTime;
import gov.nasa.jpl.hi.marsimages.rovers.Spirit;

/**
 * Created by mpowell on 9/26/14.
 */
public class MarsTimeTest extends InstrumentationTestCase {

    public void testMarsTime() throws Exception {
        Date spiritLanding = new Spirit().getEpoch();
        double spiritJulian = MarsTime.getJulianDate(spiritLanding);
        assertEquals("Spirit landing julian date has invalid value.", 2453008.07397, spiritJulian, 0.01);

        Object[] marsTimes = MarsTime.getMarsTimes(new Spirit().getEpoch(), Spirit.SPIRIT_WEST_LONGITUDE);
        assertNotNull("MarsTimes should not be null", marsTimes);
        assertEquals("MarsTimes should have 14 elements.", 14, marsTimes.length);

        double jdut = (Double)marsTimes[0];
        assertEquals("Spirit landing jdut invalid", 2453008.07397, jdut, 0.01);

        float tt_utc_diff = (Float)marsTimes[1];
        assertEquals("Spirit landing TT - UTC invalid", 64.184f, tt_utc_diff, 0.01f);

        double jdtt = (Double)marsTimes[2];
        assertEquals("Spirit landing jdtt invalid", 2453008.07471, jdtt, 0.01);

        double deltaJ2000 = (Double)marsTimes[3];
        assertEquals("Spirit landing deltaJ2000 invalid", 1463.07471, deltaJ2000, 0.01);

        double marsMeanAnomaly = (Double)marsTimes[4];
        assertEquals("Spirit mars mean anomaly invalid", 786.06851, marsMeanAnomaly, 0.01);

        double angleFictitiousMeanSun = (Double)marsTimes[5];
        assertEquals("Spirit angleFictitiousMeanSun invalid", 1037.09363, angleFictitiousMeanSun, 0.01);

        double pbs = (Double)marsTimes[6];
        assertEquals("Spirit pbs invalid", 0.01614, pbs, 0.01);

        double v_M_diff = (Double)marsTimes[7];
        assertEquals("Spirit v - M invalid", 10.22959, v_M_diff, 0.01);

        double ls = (Double)marsTimes[8];
        assertEquals("Spirit ls invalid", 1047.32322, ls, 0.01);

        double eot = (Double)marsTimes[9];
        assertEquals("Spirit EOT invalid", -12.77557, eot, 0.01);
    }
}
