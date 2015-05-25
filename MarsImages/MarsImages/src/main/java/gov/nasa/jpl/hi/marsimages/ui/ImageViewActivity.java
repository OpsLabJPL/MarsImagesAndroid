package gov.nasa.jpl.hi.marsimages.ui;


import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaScannerConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Toast;

import com.evernote.edam.type.Note;
import com.google.common.collect.Lists;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.powellware.marsimages.R;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import gov.nasa.jpl.hi.marsimages.EvernoteMars;
import gov.nasa.jpl.hi.marsimages.MarsImagesApp;
import gov.nasa.jpl.hi.marsimages.Utils;

import static android.support.v7.app.ActionBar.OnNavigationListener;
import static gov.nasa.jpl.hi.marsimages.EvernoteMars.EVERNOTE;
import static gov.nasa.jpl.hi.marsimages.MarsImagesApp.MARS_IMAGES;
import static gov.nasa.jpl.hi.marsimages.MarsImagesApp.MARS_IMAGES_PREFERENCES_KEY;
import static gov.nasa.jpl.hi.marsimages.MarsImagesApp.MISSION_NAME_PREFERENCE;
import static gov.nasa.jpl.hi.marsimages.MarsImagesApp.VIEW_PAGER_SOURCE;

public class ImageViewActivity extends ActionBarActivity {

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
