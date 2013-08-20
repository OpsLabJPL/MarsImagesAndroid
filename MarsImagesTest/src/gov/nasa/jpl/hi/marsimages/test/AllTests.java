package gov.nasa.jpl.hi.marsimages.test;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite(AllTests.class.getName());
		//$JUnit-BEGIN$
		suite.addTestSuite(NoteListAdapterTest.class);
		suite.addTestSuite(FullscreenImageActivityTest.class);
		suite.addTestSuite(MarsImagesAppActivityTest.class);
		suite.addTestSuite(MarsImagesAppTest.class);
		//$JUnit-END$
		return suite;
	}

}
