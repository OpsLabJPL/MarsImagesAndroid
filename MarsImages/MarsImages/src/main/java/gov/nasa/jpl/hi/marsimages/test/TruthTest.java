package gov.nasa.jpl.hi.marsimages.test;

import android.test.InstrumentationTestCase;

/**
 * Created by mpowell on 9/26/14.
 */
public class TruthTest extends InstrumentationTestCase {
    public void test() throws Exception {
        final int expected = 5;
        final int reality = 5;
        assertEquals(expected, reality);
    }
}
