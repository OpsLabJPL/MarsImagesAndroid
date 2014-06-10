package gov.nasa.jpl.hi.marsimages.ui;


import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SearchView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
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
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

import static gov.nasa.jpl.hi.marsimages.EvernoteMars.EVERNOTE;
import static gov.nasa.jpl.hi.marsimages.MarsImagesApp.MARS_IMAGES;
import static gov.nasa.jpl.hi.marsimages.MarsImagesApp.MARS_IMAGES_PREFERENCES_KEY;
import static gov.nasa.jpl.hi.marsimages.MarsImagesApp.MISSION_NAME_PREFERENCE;
import static gov.nasa.jpl.hi.marsimages.MarsImagesApp.VIEW_PAGER_SOURCE;

public class ImageViewActivity extends ActionBarActivity
        implements ActionBar.OnNavigationListener {

    private static final String STATE_PAGE_NUMBER = "page_number";

    private HackySlidingPaneLayout mSlidingPane;
    private ImagePagerAdapter mAdapter;
    private ViewPager mPager;
    private ArrayAdapter<CharSequence> mSpinnerAdapter;
    private boolean needToSetViewPagerToPageZero = false;
    private ActionBarHelper mActionBar;
    private StickyListHeadersListView mList;
    private SearchView searchView;
    private MenuItem mSearchItem;

    @SuppressLint("NewApi")
    @Override
    public void onCreate(Bundle savedInstanceState) {
//        Utils.enableStrictMode();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        IntentFilter filter = new IntentFilter(EvernoteMars.END_NOTE_LOADING);
        filter.addAction(MarsImagesApp.MISSION_CHANGED);
        filter.addAction(MarsImagesApp.IMAGE_SELECTED);
        filter.addAction(MarsImagesApp.NOTES_CLEARED);
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, filter);

        mSlidingPane = (HackySlidingPaneLayout) findViewById(R.id.main_layout);
        mSlidingPane.setCoveredFadeColor(0x00000000);

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
                    EVERNOTE.loadMoreNotes(ImageViewActivity.this);
            }
        });

        mList = (StickyListHeadersListView) findViewById(R.id.image_list_view);

        mSpinnerAdapter = ArrayAdapter.createFromResource(this, R.array.missions, android.R.layout.simple_spinner_dropdown_item);
        String[] missions = getResources().getStringArray(R.array.missions);
        List<String> missionList = Lists.newArrayList(missions);
        CharSequence missionName = MARS_IMAGES.getMissionName();
        int missionIndex = missionList.indexOf(missionName);
        getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        getActionBar().setListNavigationCallbacks(mSpinnerAdapter, this);
        getSupportActionBar().setSelectedNavigationItem(missionIndex);

        // Set up activity to go full screen
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Enable some additional newer visibility and ActionBar features to create a more
        // immersive photo viewing experience
        if (Utils.hasHoneycomb()) {
            final ActionBar actionBar = getActionBar();

            // Hide and show the ActionBar as the visibility changes
//            mPager.setOnSystemUiVisibilityChangeListener(
//                    new View.OnSystemUiVisibilityChangeListener() {
//                        @Override
//                        public void onSystemUiVisibilityChange(int vis) {
//                            if ((vis & View.SYSTEM_UI_FLAG_LOW_PROFILE) != 0) {
//                                actionBar.hide();
//                            } else {
//                                actionBar.show();
//                            }
//                        }
//                    });

            // Start low profile mode and hide ActionBar
//            mPager.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
//            actionBar.hide();
        }

        if (savedInstanceState != null) {
            int selectedPage = savedInstanceState.getInt(STATE_PAGE_NUMBER, -1);
            if (selectedPage > -1) {
                mAdapter.setCount(EVERNOTE.getNotesCount());
                mAdapter.notifyDataSetChanged();
                mPager.setCurrentItem(selectedPage);
            }
        }

        if (Utils.hasJellyBeanMR2()) {
            getActionBar().setHomeAsUpIndicator(
                    getResources().getDrawable(R.drawable.ic_drawer));
        }
        mActionBar = createActionBarHelper();
        mActionBar.init();
        SliderListener sliderListener = new SliderListener();
        mSlidingPane.setPanelSlideListener(sliderListener);
        if (mList.isShown()) {
            sliderListener.onPanelOpened(null);
            resetLayoutParams(mList.getWidth());
        } else {
            sliderListener.onPanelClosed(null);
            resetLayoutParams(0);
        }

        EVERNOTE.loadMoreNotes(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_PAGE_NUMBER, mPager.getCurrentItem());
    }

    @Override
    public boolean onNavigationItemSelected(int position, long itemId) {
        String[] missionNames = getResources().getStringArray(R.array.missions);
        String missionName = missionNames[position];
        MARS_IMAGES.setMission(missionName, this);
        SharedPreferences sharedPreferences = getSharedPreferences(MARS_IMAGES_PREFERENCES_KEY, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(MISSION_NAME_PREFERENCE, missionName);
        editor.commit();
        return true;
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
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
                if (!selectionSource.equals(VIEW_PAGER_SOURCE)) {
                    mPager.setCurrentItem(imageIndex);
                }
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        long pauseTimeMillis = MARS_IMAGES.getPauseTimestamp();
        ImageLoader.getInstance().resume();
        if (pauseTimeMillis > 0 && System.currentTimeMillis() - pauseTimeMillis > 30 * 60 * 1000)
            EVERNOTE.loadMoreNotes(this, true);
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
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.image_view, menu);

        mSearchItem = menu.findItem(R.id.action_search);
        searchView = (SearchView) mSearchItem.getActionView();
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
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
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
            case android.R.id.home:
                Log.d("home", "Home touched. slidable: " + mSlidingPane.isSlideable());
                if (mSlidingPane.isOpen())
                    mSlidingPane.closePane();
                else
                    mSlidingPane.openPane();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void resetLayoutParams(int bottomPaneWidth) {
        ViewGroup.LayoutParams layoutParams = mPager.getLayoutParams();
        final DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        final int width = displayMetrics.widthPixels;
        layoutParams.width = width - bottomPaneWidth + 1;
        Log.d("new-width", "setting width to " + layoutParams.width);
        mPager.setLayoutParams(layoutParams);
    }

    private void createAboutThisAppActivity() {
        Intent aboutThisAppIntent = new Intent(AboutThisAppActivity.INTENT_ACTION_ABOUT_THIS_APP);
        startActivity(aboutThisAppIntent);
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
                Context context = getApplicationContext();
                CharSequence text = getString(R.string.gallery_saved);
                int duration = Toast.LENGTH_SHORT;
                Toast toast = Toast.makeText(context, text, duration);
                toast.show();
            }
        }.execute(thisNote, mPager.getCurrentItem());
    }

    private File saveImageToExternalStorage(Note thisNote, Integer pageNumber) {
        String imageURL = thisNote.getResources().get(0).getAttributes().getSourceURL();
        ImageView imageView = (ImageView) mPager.findViewWithTag(getImageViewTag(pageNumber));
        BitmapDrawable bitmapDrawable = (BitmapDrawable) imageView.getDrawable();
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

    /**
     * Create a compatible helper that will manipulate the action bar if
     * available.
     */
    private ActionBarHelper createActionBarHelper() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            return new ActionBarHelperICS();
        } else {
            return new ActionBarHelper();
        }
    }

    /**
     * Stub action bar helper; this does nothing.
     */
    private class ActionBarHelper {
        public void init() {
        }

        @SuppressLint("NewApi")
        public void onPanelClosed() {
            if (Utils.hasJellyBeanMR2()) {
                getActionBar().setHomeAsUpIndicator(
                        getResources().getDrawable(R.drawable.ic_drawer));
            }
        }

        @SuppressLint("NewApi")
        public void onPanelOpened() {
            if (Utils.hasJellyBeanMR2()) {
                getActionBar().setHomeAsUpIndicator(
                        getResources().getDrawable(R.drawable.abc_ic_ab_back_holo_dark));
            }
        }
    }

    /**
     * Action bar helper for use on ICS and newer devices.
     */
    private class ActionBarHelperICS extends ActionBarHelper {
        private final ActionBar mActionBar;

        ActionBarHelperICS() {
            mActionBar = getActionBar();
        }

        @SuppressLint("NewApi")
        @Override
        public void init() {
            mActionBar.setDisplayHomeAsUpEnabled(true);
            mActionBar.setHomeButtonEnabled(true);
        }
    }

    /**
     * This panel slide listener updates the action bar accordingly for each
     * panel state.
     */
    private class SliderListener extends
            HackySlidingPaneLayout.SimplePanelSlideListener {
        @Override
        public void onPanelOpened(View panel) {
            mActionBar.onPanelOpened();
            resetLayoutParams(mList.getWidth());
            mPager.setCurrentItem(mPager.getCurrentItem());
        }

        @Override
        public void onPanelClosed(View panel) {
            mActionBar.onPanelClosed();
            resetLayoutParams(0);
            mPager.setCurrentItem(mPager.getCurrentItem());
        }

        @Override
        public void onPanelSlide(View panel, float slideOffset) {
            resetLayoutParams((int) (mList.getWidth() * slideOffset));
        }
    }
}
