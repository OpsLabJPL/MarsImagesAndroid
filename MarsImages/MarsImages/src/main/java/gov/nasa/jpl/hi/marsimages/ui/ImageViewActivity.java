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

public class ImageViewActivity extends ActionBarActivity
        implements OnNavigationListener {

    private static final String STATE_PAGE_NUMBER = "page_number";
    private static final String STATE_DRAWER_OPEN = "drawer_open";
    private static final String STATE_FULLSCREEN = "fullscreen";

    private ImagePagerAdapter mAdapter;
    private ViewPager mPager;
    private boolean needToSetViewPagerToPageZero = false;
    private SearchView searchView;
    private MenuItem mSearchItem;
    private DrawerLayout mDrawer;
    private ActionBarDrawerToggle mDrawerToggle;
    private boolean fullscreen;
    private final WifiStateReceiver mWifiStateReceiver = new WifiStateReceiver();
    private MenuItem mMapMenuItem;
    private MenuItem mMosaicMenuItem;

    @Override
    public void onCreate(Bundle savedInstanceState) {
//        Utils.enableStrictMode();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        IntentFilter filter = new IntentFilter(EvernoteMars.END_NOTE_LOADING);
        filter.addAction(MarsImagesApp.MISSION_CHANGED);
        filter.addAction(MarsImagesApp.IMAGE_SELECTED);
        filter.addAction(MarsImagesApp.NOTES_CLEARED);
        filter.addAction(MarsImagesApp.LOCATIONS_LOADED);
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, filter);

        mDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawer.setScrimColor(Color.TRANSPARENT);

        // Set up ViewPager and backing adapter
        mAdapter = new ImagePagerAdapter(getSupportFragmentManager());
        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);
        mPager.setPageMargin((int) getResources().getDimension(R.dimen.horizontal_page_margin));
        mPager.setOffscreenPageLimit(1);
        mPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);

                //send image selected event
                Intent intent = new Intent(MarsImagesApp.IMAGE_SELECTED);
                intent.putExtra(MarsImagesApp.IMAGE_INDEX, position);
                intent.putExtra(MarsImagesApp.SELECTION_SOURCE, MarsImagesApp.VIEW_PAGER_SOURCE);
                LocalBroadcastManager.getInstance(ImageViewActivity.this).sendBroadcast(intent);

                //try to load more notes when the last image view page is selected
                int pageCount = mAdapter.getCount();
                if (position >= pageCount - mPager.getOffscreenPageLimit() - 1)
                    loadMoreImages();
            }
        });

        ImageListFragment mListFragment = new ImageListFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.image_list_frame, mListFragment)
                .commit();



        mDrawerToggle = new ActionBarDrawerToggle(
                this,
                mDrawer,
                R.string.drawer_open,
                R.string.drawer_close
        ) { /* default implementation */ };

        mDrawer.setDrawerListener(mDrawerToggle);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        int v =  View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        getWindow().getDecorView().setSystemUiVisibility(v);
        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                boolean fullscreenVisibility = (visibility & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) != 0;
                if (fullscreenVisibility != isFullscreen())
                    setFullscreen(fullscreenVisibility);
            }
        });
        ArrayAdapter<CharSequence> mSpinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.missions, android.R.layout.simple_spinner_dropdown_item);
        String[] missions = getResources().getStringArray(R.array.missions);
        List<String> missionList = Lists.newArrayList(missions);
        CharSequence missionName = MARS_IMAGES.getMissionName();
        int missionIndex = missionList.indexOf(missionName);
        getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        getSupportActionBar().setListNavigationCallbacks(mSpinnerAdapter, this);
        getSupportActionBar().setSelectedNavigationItem(missionIndex);

        if (savedInstanceState != null) {
            int selectedPage = savedInstanceState.getInt(STATE_PAGE_NUMBER, -1);
            if (selectedPage > -1) {
                mAdapter.setCount(EVERNOTE.getNotesCount());
                mAdapter.notifyDataSetChanged();
                mPager.setCurrentItem(selectedPage);
            }
            if (savedInstanceState.getBoolean(STATE_DRAWER_OPEN, true))
                mDrawer.openDrawer(GravityCompat.START);
            else
                mDrawer.closeDrawer(GravityCompat.START);

            if (savedInstanceState.getBoolean(STATE_FULLSCREEN, false))
                setFullscreen(true);
        }

        loadMoreImages();
    }

    void loadMoreImages() {
        loadMoreImages(false);
    }

    private void loadMoreImages(boolean clearFirst) {
        ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        if (!isConnected) {
            IntentFilter intentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
            intentFilter.setPriority(100);
            LocalBroadcastManager.getInstance(this).registerReceiver(mWifiStateReceiver, intentFilter);
            Toast.makeText(this, "Unable to connect to the network.", Toast.LENGTH_SHORT).show();
            return;
        }
        EVERNOTE.loadMoreNotes(this, clearFirst);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_PAGE_NUMBER, mPager.getCurrentItem());
        outState.putBoolean(STATE_DRAWER_OPEN, mDrawer.isDrawerOpen(GravityCompat.START));
        outState.putBoolean(STATE_FULLSCREEN, isFullscreen());
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


    private final BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() == null) return;

            if (intent.getAction().equals(EvernoteMars.END_NOTE_LOADING)) {
                Integer notesReturned = intent.getIntExtra(EvernoteMars.NUM_NOTES_RETURNED, 0);
                Log.d("receiver", "Notes returned: " + notesReturned);
                mAdapter.setCount(EVERNOTE.getNotesCount());
                mAdapter.notifyDataSetChanged();
                if (needToSetViewPagerToPageZero) {
                    needToSetViewPagerToPageZero = false;
                    mPager.setCurrentItem(0);
                }
            } else if (intent.getAction().equals(MarsImagesApp.MISSION_CHANGED) ||
                    intent.getAction().equals(MarsImagesApp.NOTES_CLEARED)) {
                mAdapter.setCount(0);
                mAdapter.notifyDataSetChanged();
                needToSetViewPagerToPageZero = true;
            } else if (intent.getAction().equals(MarsImagesApp.IMAGE_SELECTED)) {
                Integer imageIndex = intent.getIntExtra(MarsImagesApp.IMAGE_INDEX, 0);
                String selectionSource = intent.getStringExtra(MarsImagesApp.SELECTION_SOURCE);
                if (selectionSource != null && !selectionSource.equals(VIEW_PAGER_SOURCE)) {
                    mPager.setCurrentItem(imageIndex);
                }
            } else if (intent.getAction().equals(MarsImagesApp.LOCATIONS_LOADED)) {
                MarsImagesApp.enableMenuItem(mMosaicMenuItem);
                MarsImagesApp.enableMenuItem(mMapMenuItem);
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        long pauseTimeMillis = MARS_IMAGES.getPauseTimestamp();
        ImageLoader.getInstance().resume();
        if (pauseTimeMillis > 0 && System.currentTimeMillis() - pauseTimeMillis > 30 * 60 * 1000)
            loadMoreImages(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        ImageLoader.getInstance().pause();
        MARS_IMAGES.setPauseTimestamp(System.currentTimeMillis());
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mWifiStateReceiver);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.image_view, menu);

        mMapMenuItem = menu.findItem(R.id.map);
        mMosaicMenuItem = menu.findItem(R.id.mosaic);
        if (!MARS_IMAGES.hasLocations()) {
            MarsImagesApp.disableMenuItem(mMapMenuItem);
            MarsImagesApp.disableMenuItem(mMosaicMenuItem);
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    MARS_IMAGES.getLocations(ImageViewActivity.this);
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
                            EvernoteMars.setSearchWords(null, ImageViewActivity.this);
                            mPager.setCurrentItem(0);
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
                        LocalBroadcastManager.getInstance(ImageViewActivity.this).sendBroadcast(intent);
                        EvernoteMars.setSearchWords(query, ImageViewActivity.this);
                        Log.d("search query", "user entered query " + query);
                        return false;
                    }

                    @SuppressLint("NewApi")
                    @Override
                    public boolean onQueryTextChange(String query) {
                        if (query == null || query.isEmpty()) {
                            EvernoteMars.setSearchWords(null, ImageViewActivity.this);
                        }
                        return false;
                    }
                });
            }
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        switch (item.getItemId()) {
            case R.id.about:
                createAboutThisAppActivity();
                return true;
            case R.id.share:
                shareImage();
                return true;
            case R.id.save:
                saveImageToGallery();
                return true;
            case R.id.clock:
                createMarsTimeActivity();
                return true;
            case R.id.map:
                createMapActivity();
                return true;
            case R.id.mosaic:
                createMosaicActivity();
                return true;
        }
        return super.onOptionsItemSelected(item);
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

    private void shareImage() {
        Note thisNote = EVERNOTE.getNote(mPager.getCurrentItem());
        final String imageSubject = R.string.share_subject + thisNote.getTitle();
        new AsyncTask<Object, Void, File>() {
            @Override
            protected File doInBackground(Object... params) {
                if (!(params[0] instanceof Note) || !(params[1] instanceof Integer))
                    return null;
                return saveImageToExternalStorage((Note) params[0], (Integer) params[1]);
            }

            @Override
            protected void onPostExecute(File jpegFile) {
                if (jpegFile == null) {
                    CharSequence text = "Error sharing Mars image, please try again.";
                    Toast.makeText(ImageViewActivity.this, text, Toast.LENGTH_SHORT).show();
                    return;
                }

                final Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
                shareIntent.setType("image/jpeg");
                shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, imageSubject);
                shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(jpegFile.getAbsolutePath())));
                startActivity(Intent.createChooser(shareIntent, "Share"));
            }
        }.execute(thisNote, mPager.getCurrentItem());
    }

    private void saveImageToGallery() {
        Note thisNote = EVERNOTE.getNote(mPager.getCurrentItem());
        new AsyncTask<Object, Void, Void>() {
            @Override
            protected Void doInBackground(Object... params) {
                if (!(params[0] instanceof Note) || !(params[1] instanceof Integer))
                    return null;
                saveImageToExternalStorage((Note) params[0], (Integer) params[1]);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                CharSequence text = getString(R.string.gallery_saved);
                Toast.makeText(ImageViewActivity.this, text, Toast.LENGTH_SHORT).show();
            }
        }.execute(thisNote, mPager.getCurrentItem());
    }

    private File saveImageToExternalStorage(Note thisNote, Integer pageNumber) {
        String imageURL = thisNote.getResources().get(0).getAttributes().getSourceURL();
        ImageView imageView = (ImageView) mPager.findViewWithTag(getImageViewTag(pageNumber));
        final ImageViewActivity activity = ImageViewActivity.this;

        if (imageView == null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    CharSequence text = "Error saving Mars image to gallery, please try again.";
                    Toast.makeText(activity, text, Toast.LENGTH_SHORT).show();
                }
            });
            return null;
        }
        BitmapDrawable bitmapDrawable = (BitmapDrawable) imageView.getDrawable();
        if (bitmapDrawable == null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    CharSequence text = "Error saving Mars image to gallery, please try again.";
                    Toast.makeText(activity, text, Toast.LENGTH_SHORT).show();
                }
            });
            return null;
        }
        Bitmap bitmap = bitmapDrawable.getBitmap();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] imageInByte = stream.toByteArray();
        File jpegFile = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), MARS_IMAGES.getMission().getSortableImageFilename(imageURL));
        try {
            FileOutputStream fos = new FileOutputStream(jpegFile);
            fos.write(imageInByte);
            fos.flush();
            fos.close();
        } catch (IOException e) {
            Log.e("share image", "Unable to create image file on external storage for email.");
        }
        Log.d("share image", "JPEG File to email: " + jpegFile);
        MediaScannerConnection.scanFile(getApplicationContext(), new String[]{jpegFile.toString()}, null, null);

        return jpegFile;
    }

    private static String getImageViewTag(int number) {
        return "imageview" + number;
    }

    public static int getImageViewFragmentNumber(String tag) {
        return Integer.parseInt(tag.substring(9));
    }

    public boolean isFullscreen() {
        return fullscreen;
    }

    public void setFullscreen(boolean fullscreen) {
        this.fullscreen = fullscreen;
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        int v = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        if (fullscreen) {
            v |= View.SYSTEM_UI_FLAG_FULLSCREEN;
            v |= View.SYSTEM_UI_FLAG_IMMERSIVE;
            v |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        }
        getWindow().getDecorView().setSystemUiVisibility(v);

        for (Fragment fragment : fragments) {
            if (fragment instanceof ImageViewFragment) {
                ImageViewFragment imageview = (ImageViewFragment)fragment;
                imageview.showCaption(!isFullscreen());
            }
        }
    }

    public static class ImagePagerAdapter extends FragmentStatePagerAdapter {

        private int mPageCount = 0;

        public ImagePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        public void setCount(int count) {
            mPageCount = count;
        }

        @Override
        public int getCount() {
            return mPageCount;
        }

        @Override
        public Fragment getItem(int position) {
            Log.d("getitem", "Getting fragment for index: " + position);
            String imageUrl = EVERNOTE.getNoteUrl(position);
            return ImageViewFragment.newInstance(imageUrl, ImageViewActivity.getImageViewTag(position));
        }
    }

    public static class WifiStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager cm =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
            if (isConnected) {
                Log.d("wifi state connected", "Wifi reconnected.");
                LocalBroadcastManager.getInstance(context).unregisterReceiver(this);
                EVERNOTE.hasNotesRemaining = true;
                EVERNOTE.loadMoreNotes(context, false);
            }
        }
    }
}
