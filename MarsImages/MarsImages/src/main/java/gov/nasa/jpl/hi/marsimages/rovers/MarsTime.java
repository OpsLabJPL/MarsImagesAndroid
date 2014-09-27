package gov.nasa.jpl.hi.marsimages.rovers;

import android.util.Log;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by mpowell on 9/21/14.
 */
public class MarsTime {

    public static final double DEG_TO_RAD = Math.PI/180.0;
    public static final double EARTH_SECS_PER_MARS_SEC = 1.027491252;
    public static final float CURIOSITY_WEST_LONGITUDE = 222.6f;

//    i 	Ai 	τi 	φi
//    1 	0.0071 	2.2353 	49.409
//    2 	0.0057 	2.7543 	168.173
//    3 	0.0039 	1.1177 	191.837
//    4 	0.0037 	15.7866 21.736
//    5 	0.0021 	2.1354 	15.704
//    6 	0.0020 	2.4694 	95.528
//    7 	0.0018 	32.8493 49.095
    static float A[] = { 0.0071f, 0.0057f, 0.0039f, 0.0037f, 0.0021f, 0.0020f, 0.0018f };
    static float tau[] = { 2.2353f, 2.7543f, 1.1177f, 15.7866f, 2.1354f, 2.4694f, 32.8493f };
    static float psi[] = { 49.409f, 168.173f, 191.837f, 21.736f, 15.704f, 95.528f, 49.095f };

/* TABLE OF LEAP SECONDS: ftp://maia.usno.navy.mil/ser7/tai-utc.dat
 1961 JAN  1 =JD 2437300.5  TAI-UTC=   1.4228180 S + (MJD - 37300.) X 0.001296 S
 1961 AUG  1 =JD 2437512.5  TAI-UTC=   1.3728180 S + (MJD - 37300.) X 0.001296 S
 1962 JAN  1 =JD 2437665.5  TAI-UTC=   1.8458580 S + (MJD - 37665.) X 0.0011232S
 1963 NOV  1 =JD 2438334.5  TAI-UTC=   1.9458580 S + (MJD - 37665.) X 0.0011232S
 1964 JAN  1 =JD 2438395.5  TAI-UTC=   3.2401300 S + (MJD - 38761.) X 0.001296 S
 1964 APR  1 =JD 2438486.5  TAI-UTC=   3.3401300 S + (MJD - 38761.) X 0.001296 S
 1964 SEP  1 =JD 2438639.5  TAI-UTC=   3.4401300 S + (MJD - 38761.) X 0.001296 S
 1965 JAN  1 =JD 2438761.5  TAI-UTC=   3.5401300 S + (MJD - 38761.) X 0.001296 S
 1965 MAR  1 =JD 2438820.5  TAI-UTC=   3.6401300 S + (MJD - 38761.) X 0.001296 S
 1965 JUL  1 =JD 2438942.5  TAI-UTC=   3.7401300 S + (MJD - 38761.) X 0.001296 S
 1965 SEP  1 =JD 2439004.5  TAI-UTC=   3.8401300 S + (MJD - 38761.) X 0.001296 S
 1966 JAN  1 =JD 2439126.5  TAI-UTC=   4.3131700 S + (MJD - 39126.) X 0.002592 S
 1968 FEB  1 =JD 2439887.5  TAI-UTC=   4.2131700 S + (MJD - 39126.) X 0.002592 S
 1972 JAN  1 =JD 2441317.5  TAI-UTC=  10.0       S + (MJD - 41317.) X 0.0      S
 1972 JUL  1 =JD 2441499.5  TAI-UTC=  11.0       S + (MJD - 41317.) X 0.0      S
 1973 JAN  1 =JD 2441683.5  TAI-UTC=  12.0       S + (MJD - 41317.) X 0.0      S
 1974 JAN  1 =JD 2442048.5  TAI-UTC=  13.0       S + (MJD - 41317.) X 0.0      S
 1975 JAN  1 =JD 2442413.5  TAI-UTC=  14.0       S + (MJD - 41317.) X 0.0      S
 1976 JAN  1 =JD 2442778.5  TAI-UTC=  15.0       S + (MJD - 41317.) X 0.0      S
 1977 JAN  1 =JD 2443144.5  TAI-UTC=  16.0       S + (MJD - 41317.) X 0.0      S
 1978 JAN  1 =JD 2443509.5  TAI-UTC=  17.0       S + (MJD - 41317.) X 0.0      S
 1979 JAN  1 =JD 2443874.5  TAI-UTC=  18.0       S + (MJD - 41317.) X 0.0      S
 1980 JAN  1 =JD 2444239.5  TAI-UTC=  19.0       S + (MJD - 41317.) X 0.0      S
 1981 JUL  1 =JD 2444786.5  TAI-UTC=  20.0       S + (MJD - 41317.) X 0.0      S
 1982 JUL  1 =JD 2445151.5  TAI-UTC=  21.0       S + (MJD - 41317.) X 0.0      S
 1983 JUL  1 =JD 2445516.5  TAI-UTC=  22.0       S + (MJD - 41317.) X 0.0      S
 1985 JUL  1 =JD 2446247.5  TAI-UTC=  23.0       S + (MJD - 41317.) X 0.0      S
 1988 JAN  1 =JD 2447161.5  TAI-UTC=  24.0       S + (MJD - 41317.) X 0.0      S
 1990 JAN  1 =JD 2447892.5  TAI-UTC=  25.0       S + (MJD - 41317.) X 0.0      S
 1991 JAN  1 =JD 2448257.5  TAI-UTC=  26.0       S + (MJD - 41317.) X 0.0      S
 1992 JUL  1 =JD 2448804.5  TAI-UTC=  27.0       S + (MJD - 41317.) X 0.0      S
 1993 JUL  1 =JD 2449169.5  TAI-UTC=  28.0       S + (MJD - 41317.) X 0.0      S
 1994 JUL  1 =JD 2449534.5  TAI-UTC=  29.0       S + (MJD - 41317.) X 0.0      S
 1996 JAN  1 =JD 2450083.5  TAI-UTC=  30.0       S + (MJD - 41317.) X 0.0      S
 1997 JUL  1 =JD 2450630.5  TAI-UTC=  31.0       S + (MJD - 41317.) X 0.0      S
 1999 JAN  1 =JD 2451179.5  TAI-UTC=  32.0       S + (MJD - 41317.) X 0.0      S
 2006 JAN  1 =JD 2453736.5  TAI-UTC=  33.0       S + (MJD - 41317.) X 0.0      S
 2009 JAN  1 =JD 2454832.5  TAI-UTC=  34.0       S + (MJD - 41317.) X 0.0      S
 2012 JUL  1 =JD 2456109.5  TAI-UTC=  35.0       S + (MJD - 41317.) X 0.0      S
 */

/* return the TAI-UTC lookup table value of leap seconds for a given date */
    public static float taiutc(Date date) {
        double julianDate = getJulianDate(date);
        if (julianDate >= 2456109.5)
            return 35.0f;
        else if (julianDate >= 2454832.5)
            return 34.0f;
        else if (julianDate >= 2453736.5)
            return 33.0f;
        else if (julianDate >= 2451179.5)
            return 32.0f;
        else if (julianDate >= 2450630.5)
            return 31.0f;
        else if (julianDate >= 2450083.5)
            return 30.0f;
        else if (julianDate >= 2449534.5)
            return 29.0f;
        else if (julianDate >= 2449169.5)
            return 28.0f;
        else if (julianDate >= 2448804.5)
            return 27.0f;
        else if (julianDate >= 2448257.5)
            return 26.0f;
        else if (julianDate >= 2447892.5)
            return 25.0f;
        else if (julianDate >= 2447161.5)
            return 24.0f;
        else if (julianDate >= 2446247.5)
            return 23.0f;
        else if (julianDate >= 2445516.5)
            return 22.0f;
        else if (julianDate >= 2445151.5)
            return 21.0f;
        else if (julianDate >= 2444786.5)
            return 20.0f;
        else if (julianDate >= 2444239.5)
            return 19.0f;
        else if (julianDate >= 2443874.5)
            return 18.0f;
        else if (julianDate >= 2443509.5)
            return 17.0f;
        else if (julianDate >= 2443144.5)
            return 16.0f;
        else if (julianDate >= 2442778.5)
            return 15.0f;
        else if (julianDate >= 2442413.5)
            return 14.0f;
        else if (julianDate >= 2442048.5)
            return 13.0f;
        else if (julianDate >= 2441683.5)
            return 12.0f;
        else if (julianDate >= 2441499.5)
            return 11.0f;
        else if (julianDate >= 2441317.5)
            return 10.0f;
        else if (julianDate >= 2439887.5)
            return 4.2131700f;
        else if (julianDate >= 2439126.5)
            return 4.3131700f;
        else if (julianDate >= 2439004.5)
            return 3.8401300f;
        else if (julianDate >= 2438942.5)
            return 3.7401300f;
        else if (julianDate >= 2438820.5)
            return 3.6401300f;
        else if (julianDate >= 2438761.5)
            return 3.5401300f;
        else if (julianDate >= 2438639.5)
            return 3.4401300f;
        else if (julianDate >= 2438486.5)
            return 3.3401300f;
        else if (julianDate >= 2438395.5)
            return 3.2401300f;
        else if (julianDate >= 2438334.5)
            return 1.9458580f;
        else if (julianDate >= 2437665.5)
            return 1.8458580f;
        else if (julianDate >= 2437512.5)
            return 1.3728180f;
        else if (julianDate >= 2437300.5)
            return 1.4228180f;

        Log.e("MarsTime", "No lookup table value for date " + date);
        return 0;
    }

    public static double canonicalValue24(double hours) {
        if (hours < 0)
            return 24 + hours;
        else if (hours > 24)
            return hours - 24;
        return hours;
    }

    public static Object[] getMarsTimes(Date date, float longitude) {
        //A-1 millis since Jan 1 1970
//    NSTimeInterval millis = 1000 * [date timeIntervalSince1970];

        //A-2 convert to Julian date: JDUT = 2440587.5 + (millis / 8.64×107 ms/day)
        double jdut = getJulianDate(date);

        //A-3 Determine time offset from J2000 epoch: T = (JDUT - 2451545.0) / 36525.
//    double t = (jdut - 2451545.0) / 36525.0;

        //A-4 Determine UTC to TT conversion (consult table of leap seconds) To obtain the TT-UTC difference, add 32.184 seconds to the value of TAI-UTC
        float tt_utc_diff = 32.184f + taiutc(date);

        //A-5 Determine Julian Date: JDTT = JDUT + [(TT - UTC) / 86400 s·day-1]
        double jdtt = jdut + tt_utc_diff / 86400.0;

        //A-6 Determine time offset from J2000 epoch (TT). (AM2000, eq. 15): ΔtJ2000 = JDTT - 2451545.0
        double deltaJ2000 = jdtt - 2451545.0;

        //B-1 Determine Mars mean anomaly. (AM2000, eq. 16): M = 19.3870° + 0.52402075° ΔtJ2000
        double marsMeanAnomaly = 19.3870 + 0.52402075 * deltaJ2000;

        //B-2 Determine angle of Fiction Mean Sun. (AM2000, eq. 17): αFMS = 270.3863° + 0.52403840° ΔtJ2000
        double angleFictiousMeanSun = 270.3863 + 0.52403840 * deltaJ2000;

        //B-3 PBS = Σ(i=1,7) Ai cos [ (0.985626° ΔtJ2000 / τi) + φi]
        //    where 0.985626° = 360° / 365.25, and
        //    i 	Ai 	τi 	φi
        //    1 	0.0071 	2.2353 	49.409
        //    2 	0.0057 	2.7543 	168.173
        //    3 	0.0039 	1.1177 	191.837
        //    4 	0.0037 	15.7866 21.736
        //    5 	0.0021 	2.1354 	15.704
        //    6 	0.0020 	2.4694 	95.528
        //    7 	0.0018 	32.8493 49.095
        double pbs = 0.0;
        for (int i = 0; i < 7; i++) {
            pbs += A[i] * Math.cos((0.985626 * deltaJ2000 / tau[i] + psi[i]) * DEG_TO_RAD);
        }

        //B-4 Determine Equation of Center. (Bracketed term in AM2000, eqs. 19 and 20)
        //The equation of center is the true anomaly minus mean anomaly.
        //ν - M = (10.691° + 3.0° × 10-7 ΔtJ2000) sin M + 0.623° sin 2M + 0.050° sin 3M + 0.005° sin 4M + 0.0005° sin 5M + PBS
        double v_M_diff = (10.691 + .0000003 * deltaJ2000) * Math.sin(marsMeanAnomaly * DEG_TO_RAD) +
                0.623 * Math.sin(2 * marsMeanAnomaly * DEG_TO_RAD) + 0.050 * Math.sin(3 * marsMeanAnomaly * DEG_TO_RAD) +
                0.005 * Math.sin(4 * marsMeanAnomaly * DEG_TO_RAD) + 0.0005 * Math.sin(5 * marsMeanAnomaly * DEG_TO_RAD) + pbs;

        //B-5 Determine areocentric solar longitude. (AM2000, eq. 19): Ls = αFMS + (ν - M)
        double ls = angleFictiousMeanSun + v_M_diff;

        //C-1 Determine equation of time: EOT = 2.861° sin 2Ls - 0.071° sin 4Ls + 0.002° sin 6Ls - (ν - M)
        double eot = 2.861 * Math.sin(2 * ls * DEG_TO_RAD) - 0.071 * Math.sin(4 * ls * DEG_TO_RAD) + 0.002 * Math.sin(6 * ls * DEG_TO_RAD) - v_M_diff;

        //C-2 Determine Coordinated Mars Time. (AM2000, eq. 22, modified): MTC = mod24 { 24 h × ( [(JDTT - 2451549.5) / 1.027491252] + 44796.0 - 0.00096 ) }
        double msd = ((jdtt - 2451549.5) / 1.027491252) + 44796.0 - 0.00096;
        double mtc = 24 * msd % 24.0; //fmod in C...float mod % operator in Java

        //C-3. Determine Local Mean Solar Time.
        //The Local Mean Solar Time for a given planetographic longitude, Λ, in degrees west, is easily determined by offsetting from the mean solar time on the prime meridian.
        //LMST = MTC - Λ (24 h / 360°) = MTC - Λ (1 h / 15°)
        double lmst = canonicalValue24(mtc - longitude / 15.0);

        //C-4. Determine Local True Solar Time. (AM2000, eq. 23)
        //LTST = LMST + EOT (24 h / 360°) = LMST + EOT (1 h / 15°)
        double ltst = lmst + eot / 15.0;

        ArrayList<Object> times = new ArrayList<Object>();

        times.add(jdut);
        times.add(tt_utc_diff);
        times.add(jdtt);
        times.add(deltaJ2000);
        times.add(marsMeanAnomaly);
        times.add(angleFictiousMeanSun);
        times.add(pbs);
        times.add(v_M_diff);
        times.add(ls);
        times.add(eot);
        times.add(msd);
        times.add(mtc);
        times.add(lmst);
        times.add(ltst);
        return times.toArray();
    }

    public static double getJulianDate(Date date) {
        return date.getTime() / 1000 / 86400.0 + 2440587.5;
    }

}
