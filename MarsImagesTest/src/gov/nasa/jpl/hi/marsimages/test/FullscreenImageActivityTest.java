package gov.nasa.jpl.hi.marsimages.test;

import gov.nasa.jpl.hi.marsimages.activity.FullscreenImageActivity;
import android.test.ActivityInstrumentationTestCase2;

public class FullscreenImageActivityTest extends
		ActivityInstrumentationTestCase2<FullscreenImageActivity> {

	private FullscreenImageActivity mActivity;

	public FullscreenImageActivityTest() {
		super("com.powellware.marsimages.imageview", FullscreenImageActivity.class);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		mActivity = getActivity();
	}
	
	public void testView() {
		assertNotNull("FullscreenImageView may not be null", mActivity.getView());
	}
	
}
