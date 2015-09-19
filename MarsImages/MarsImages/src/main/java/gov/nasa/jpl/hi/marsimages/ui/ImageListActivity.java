package gov.nasa.jpl.hi.marsimages.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;

import com.google.common.collect.Lists;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.powellware.marsimages.R;

import java.util.List;

import gov.nasa.jpl.hi.marsimages.EvernoteMars;
import gov.nasa.jpl.hi.marsimages.MarsImagesApp;
import gov.nasa.jpl.hi.marsimages.Utils;

import static gov.nasa.jpl.hi.marsimages.EvernoteMars.EVERNOTE;
import static gov.nasa.jpl.hi.marsimages.MarsImagesApp.MARS_IMAGES;
import static gov.nasa.jpl.hi.marsimages.MarsImagesApp.MARS_IMAGES_PREFERENCES_KEY;
import static gov.nasa.jpl.hi.marsimages.MarsImagesApp.MISSION_NAME_PREFERENCE;

public class ImageListActivity extends AppCompatActivity implements ImageListFragment.Callbacks,
        ActionBar.OnNavigationListener {

    public static final String STATE_HIDDEN_LIST = "hidden_list";
    public static final int IMAGE_LIST_REQUEST_CODE = 105;
    private SearchView searchView;
    private MenuItem mSearchItem;

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;
    private final int mRequestCode = IMAGE_LIST_REQUEST_CODE;
    private int imageViewPageNumber = 0;

//    MenuItem mMapMenuItem;
    MenuItem mMosaicMenuItem;
    MenuItem mHideShowListMenuItem;
    ImageListFragment mListFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_list);

        mListFragment = (ImageListFragment)
                getSupportFragmentManager().findFragmentById(R.id.image_list);
        if (imageViewPageNumber > 0) { //returning here after dismissing an ImageViewActivity
            mListFragment.mStickyList.setSelection(imageViewPageNumber);
        }
        if (savedInstanceState != null) {
            boolean hiddenList = savedInstanceState.getBoolean(STATE_HIDDEN_LIST);
            if (hiddenList) {
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.hide(mListFragment);
                ft.commit();
            }
        }

        if (findViewById(R.id.fragment_image_view_pager) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-large and
            // res/values-sw600dp). If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;
        }

        ArrayAdapter<CharSequence> mSpinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.missions, android.R.layout.simple_spinner_dropdown_item);
        String[] missions = getResources().getStringArray(R.array.missions);
        List<String> missionList = Lists.newArrayList(missions);
        CharSequence missionName = MARS_IMAGES.getMissionName();
        int missionIndex = missionList.indexOf(missionName);
        getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        getSupportActionBar().setListNavigationCallbacks(mSpinnerAdapter, this);
        getSupportActionBar().setSelectedNavigationItem(missionIndex);

        EVERNOTE.loadMoreImages(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        ImageLoader.getInstance().pause();
        MARS_IMAGES.setPauseTimestamp(System.currentTimeMillis());
    }

    @Override
    protected void onResume() {
        super.onResume();
        long pauseTimeMillis = MARS_IMAGES.getPauseTimestamp();
        ImageLoader.getInstance().resume();
        if (pauseTimeMillis > 0 && System.currentTimeMillis() - pauseTimeMillis > 30 * 60 * 1000)
            EVERNOTE.loadMoreImages(this, true);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_HIDDEN_LIST, mListFragment.isHidden());
    }
    /**
     * Callback method from {@link ImageListFragment.Callbacks}
     * indicating that the item with the given ID was selected.
     */
    @Override
    public void onItemSelected(int imageIndex) {
        if (mTwoPane) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            Intent intent = new Intent(MarsImagesApp.IMAGE_SELECTED);
            intent.putExtra(MarsImagesApp.IMAGE_INDEX, imageIndex);
            intent.putExtra(MarsImagesApp.SELECTION_SOURCE, MarsImagesApp.LIST_SOURCE);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        } else {
            // In single-pane mode, simply start the Image View Activity
            // for the selected item ID.
            Intent imageViewIntent = new Intent(this, ImageViewActivity.class);
            imageViewIntent.putExtra(ImageViewPagerFragment.STATE_PAGE_NUMBER, imageIndex);
            startActivityForResult(imageViewIntent, mRequestCode);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        imageViewPageNumber = data.getIntExtra(ImageViewPagerFragment.STATE_PAGE_NUMBER, 0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_image_list, menu);
        if (mTwoPane)
            getMenuInflater().inflate(R.menu.image_view, menu);


//        mMapMenuItem = menu.findItem(R.id.map);
        mMosaicMenuItem = menu.findItem(R.id.mosaic);
        mHideShowListMenuItem = menu.findItem(R.id.hideShowList);
        if (mHideShowListMenuItem != null && !mTwoPane) {
            mHideShowListMenuItem.setVisible(false);
        }
        else if (mListFragment.isHidden()) {
            mHideShowListMenuItem.setTitle("Show List");
            mHideShowListMenuItem.setIcon(R.drawable.ic_action_return_from_full_screen);
        }

        if (!MARS_IMAGES.hasLocations()) {
//            MarsImagesApp.disableMenuItem(mMapMenuItem);
            MarsImagesApp.disableMenuItem(mMosaicMenuItem);
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    MARS_IMAGES.getLocations(ImageListActivity.this);
                    return null;
                }
            }.execute();
        }

        mSearchItem = menu.findItem(R.id.action_search);
        if (mSearchItem != null) {
            searchView = (SearchView) mSearchItem.getActionView();
            if (searchView != null) {
                searchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
                    @SuppressLint("NewApi")
                    @Override
                    public void onFocusChange(View v, boolean hasFocus) {
                        if (!hasFocus) {
                            if (Utils.hasIceCreamSandwich()) mSearchItem.collapseActionView();
                            searchView.setQuery("", false);
                        } else {
                            EvernoteMars.setSearchWords(null, ImageListActivity.this);
//                            mPager.setCurrentItem(0); //search is broken, fix this...
                        }
                    }
                });

                searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        if (searchView != null) {
                            searchView.setVisibility(View.INVISIBLE);
                            searchView.setVisibility(View.VISIBLE);
                        }
                        Intent intent = new Intent(MarsImagesApp.NOTES_CLEARED);
                        LocalBroadcastManager.getInstance(ImageListActivity.this).sendBroadcast(intent);
                        EvernoteMars.setSearchWords(query, ImageListActivity.this);
                        Log.d("search query", "user entered query " + query);
                        return false;
                    }

                    @SuppressLint("NewApi")
                    @Override
                    public boolean onQueryTextChange(String query) {
                        if (query == null || query.isEmpty()) {
                            EvernoteMars.setSearchWords(null, ImageListActivity.this);
                        }
                        return false;
                    }
                });
            }
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        ImageViewPagerFragment imageViewPagerFragment = null;
        int id = item.getItemId();
        switch (id) {
            case R.id.about:
                createAboutThisAppActivity();
                return true;
            case R.id.clock:
                createMarsTimeActivity();
                return true;
//            case R.id.map:
//                createMapActivity();
//                return true;
            case R.id.hideShowList:
                if (mListFragment != null) {
                    FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                    ft.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_left);
                    if (mListFragment.isHidden()) {
                        ft.show(mListFragment);
                        mHideShowListMenuItem.setTitle("Hide List");
                        mHideShowListMenuItem.setIcon(R.drawable.ic_action_full_screen);
                    } else {
                        ft.hide(mListFragment);
                        mHideShowListMenuItem.setTitle("Show List");
                        mHideShowListMenuItem.setIcon(R.drawable.ic_action_return_from_full_screen);
                    }
                    ft.commit();
                }
                return true;
            case R.id.mosaic:
                createMosaicActivity();
                return true;
            case R.id.share:
                imageViewPagerFragment = (ImageViewPagerFragment)
                        getSupportFragmentManager().findFragmentById(R.id.fragment_image_view_pager);
                imageViewPagerFragment.shareImage();
                return true;
            case R.id.save:
                imageViewPagerFragment = (ImageViewPagerFragment)
                        getSupportFragmentManager().findFragmentById(R.id.fragment_image_view_pager);
                imageViewPagerFragment.saveImageToGallery();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(int position, long itemId) {
        String[] missionNames = getResources().getStringArray(R.array.missions);
        final String missionName = missionNames[position];
        MARS_IMAGES.setMission(missionName, this);
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                SharedPreferences sharedPreferences = getSharedPreferences(MARS_IMAGES_PREFERENCES_KEY, MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(MISSION_NAME_PREFERENCE, missionName);
                editor.commit();
                return null;
            }
        }.execute();
        return true;
    }

    private void createMarsTimeActivity() {
        Intent marsTimeIntent = new Intent(MarsClockActivity.INTENT_ACTION_MARS_TIME);
        startActivity(marsTimeIntent);
    }

    private void createMapActivity() {
        Intent mapIntent = new Intent(MapActivity.INTENT_ACTION_MAP);
        startActivity(mapIntent);
    }

    private void createAboutThisAppActivity() {
        Intent aboutThisAppIntent = new Intent(AboutThisAppActivity.INTENT_ACTION_ABOUT_THIS_APP);
        startActivity(aboutThisAppIntent);
    }

    private void createMosaicActivity() {
        Intent mosaicIntent = new Intent(MosaicActivity.INTENT_ACTION_MOSAIC);
        startActivity(mosaicIntent);
    }

}
