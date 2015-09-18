package gov.nasa.jpl.hi.marsimages.ui;


import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.powellware.marsimages.R;

import static gov.nasa.jpl.hi.marsimages.EvernoteMars.EVERNOTE;
import static gov.nasa.jpl.hi.marsimages.MarsImagesApp.MARS_IMAGES;

public class ImageViewActivity extends AppCompatActivity {

    private ImageViewPagerFragment imageViewPagerFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
//        Utils.enableStrictMode();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_view);
        int selectedPage = getIntent().getIntExtra(ImageViewPagerFragment.STATE_PAGE_NUMBER, 0);
        imageViewPagerFragment = (ImageViewPagerFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_image_view_pager);
        imageViewPagerFragment.setUpViewPager(selectedPage);
    }

    @Override
    public void onResume() {
        super.onResume();
        long pauseTimeMillis = MARS_IMAGES.getPauseTimestamp();
        ImageLoader.getInstance().resume();
        if (pauseTimeMillis > 0 && System.currentTimeMillis() - pauseTimeMillis > 30 * 60 * 1000)
            EVERNOTE.loadMoreImages(this, true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        ImageLoader.getInstance().pause();
        MARS_IMAGES.setPauseTimestamp(System.currentTimeMillis());
    }

    @Override
    public void finish() {
        Intent data = getIntent();
        data.putExtra(ImageViewPagerFragment.STATE_PAGE_NUMBER, imageViewPagerFragment.getViewPager().getCurrentItem());
        setResult(RESULT_OK, data);
        super.finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.image_view, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final ImageViewPagerFragment imageViewPagerFragment = (ImageViewPagerFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_image_view_pager);
        switch (item.getItemId()) {
            case R.id.share:
                imageViewPagerFragment.shareImage();
                return true;
            case R.id.save:
                imageViewPagerFragment.saveImageToGallery();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
