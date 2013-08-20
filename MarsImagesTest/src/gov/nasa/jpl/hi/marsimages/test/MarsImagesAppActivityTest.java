package gov.nasa.jpl.hi.marsimages.test;

import gov.nasa.jpl.hi.marsimages.MarsImagesActivity;
import gov.nasa.jpl.hi.marsimages.MarsImagesActivity.ImageListFragment;
import android.test.ActivityInstrumentationTestCase2;
import android.widget.ListView;

public class MarsImagesAppActivityTest extends
		ActivityInstrumentationTestCase2<MarsImagesActivity> {

	private MarsImagesActivity activity;

	public MarsImagesAppActivityTest() {
		super("com.powellware.marsimages", MarsImagesActivity.class);
	}
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		activity = getActivity();
	}
	
	public void testViews() {
		ImageListFragment imageListFragment = activity.getImageListFragment();
		assertNotNull(imageListFragment);
		ListView listView = imageListFragment.getListView();
		assertNotNull(listView);
		assertEquals(1, listView.getFooterViewsCount());
	}
	
}
