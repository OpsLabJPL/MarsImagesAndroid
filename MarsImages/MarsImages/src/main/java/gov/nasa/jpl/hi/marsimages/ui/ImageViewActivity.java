package gov.nasa.jpl.hi.marsimages.ui;

import android.app.ActionBar;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import gov.nasa.jpl.hi.marsimages.EvernoteMars;
import gov.nasa.jpl.hi.marsimages.Utils;
import gov.nasa.jpl.hi.marsimages.image.ImageCache;
import gov.nasa.jpl.hi.marsimages.image.ImageFetcher;

import com.evernote.edam.type.Note;
import com.powellware.marsimages.R;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import static gov.nasa.jpl.hi.marsimages.EvernoteMars.EVERNOTE;

public class ImageViewActivity extends ActionBarActivity
        implements ImageListDrawerFragment.NavigationDrawerCallbacks {

    private static final String IMAGE_CACHE_DIR = "images";
    public static final String EXTRA_IMAGE = "extra_image";

    private ImageListDrawerFragment mImageListDrawerFragment;
    private ImagePagerAdapter mAdapter;
    private ViewPager mPager;
    private ImageFetcher mImageFetcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Utils.enableStrictMode();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Fetch screen height and width, to use as our max size when loading images as this
        // activity runs full screen
        final DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        final int height = displayMetrics.heightPixels;
        final int width = displayMetrics.widthPixels;

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter(EvernoteMars.END_NOTE_LOADING));

        mImageListDrawerFragment = (ImageListDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.image_list_drawer);

        // Set up the drawer.
        mImageListDrawerFragment.setUp(
                R.id.image_list_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        final int longest = (height > width ? height : width) / 2;

        ImageCache.ImageCacheParams cacheParams =
                new ImageCache.ImageCacheParams(this, IMAGE_CACHE_DIR);
        cacheParams.setMemCacheSizePercent(0.25f); // Set memory cache to 25% of app memory

        // The ImageFetcher takes care of loading images into our ImageView children asynchronously
        mImageFetcher = new ImageFetcher(this, longest);
        mImageFetcher.setLoadingImage(R.drawable.empty_photo);
        mImageFetcher.addImageCache(getSupportFragmentManager(), cacheParams);
        mImageFetcher.setImageFadeIn(false);

        // Set up ViewPager and backing adapter
        mAdapter = new ImagePagerAdapter(getSupportFragmentManager());
        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);
        mPager.setPageMargin((int) getResources().getDimension(R.dimen.horizontal_page_margin));
        mPager.setOffscreenPageLimit(2);
        mPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                int noteCount = EVERNOTE.getNotesCount();
                if (position >= noteCount - mPager.getOffscreenPageLimit()-1)
                    EVERNOTE.loadMoreNotes(ImageViewActivity.this);
            }
        });

        // Set up activity to go full screen
//        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Enable some additional newer visibility and ActionBar features to create a more
        // immersive photo viewing experience
        if (Utils.hasHoneycomb()) {
            final ActionBar actionBar = getActionBar();

            // Hide title text and set home as up
//            actionBar.setDisplayShowTitleEnabled(false);
//            actionBar.setDisplayHomeAsUpEnabled(true);

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

        // Set the current item based on the extra passed in to this activity
        final int extraCurrentItem = getIntent().getIntExtra(EXTRA_IMAGE, -1);
        if (extraCurrentItem != -1) {
            mPager.setCurrentItem(extraCurrentItem);
        }

        EVERNOTE.loadMoreNotes(this);
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
//        FragmentManager fragmentManager = getSupportFragmentManager();
//        fragmentManager.beginTransaction()
//                .replace(R.id.container, PlaceholderFragment.newInstance(position + 1))
//                .commit();
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            Integer notesReturned = intent.getIntExtra(EvernoteMars.NUM_NOTES_RETURNED, 0);
            Log.d("receiver", "Notes returned: " + notesReturned);
            mAdapter.setCount(EVERNOTE.getNotesCount());
            mAdapter.notifyDataSetChanged();
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        mImageFetcher.setExitTasksEarly(false);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mImageFetcher.setExitTasksEarly(true);
        mImageFetcher.flushCache();
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        mImageFetcher.closeCache();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.image_view, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
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
        }
        return super.onOptionsItemSelected(item);
    }

    public ImageFetcher getImageFetcher() {
        return mImageFetcher;
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
                return saveImageToExternalStorage((Note)params[0], (Integer)params[1]);
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
                saveImageToExternalStorage((Note)params[0], (Integer)params[1]);
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
        MarsImageView imageView = (MarsImageView) mPager.findViewWithTag(getImageViewTag(pageNumber));
        BitmapDrawable bitmapDrawable = (BitmapDrawable) imageView.getDrawable();
        Bitmap bitmap = bitmapDrawable.getBitmap();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] imageInByte = stream.toByteArray();
        File jpegFile = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), EVERNOTE.getMission().getSortableImageFilename(imageURL));
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
        return "imageview"+number;
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
            String imageUrl = EVERNOTE.getNoteUrl(position);
            return ImageViewFragment.newInstance(imageUrl, ImageViewActivity.getImageViewTag(position));
        }

        @Override
        public void notifyDataSetChanged() {
            super.notifyDataSetChanged();
        }
    }
}
