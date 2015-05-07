package gov.nasa.jpl.hi.marsimages.ui;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.evernote.edam.type.Note;
import com.powellware.marsimages.R;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import gov.nasa.jpl.hi.marsimages.EvernoteMars;
import gov.nasa.jpl.hi.marsimages.MarsImagesApp;

import static gov.nasa.jpl.hi.marsimages.EvernoteMars.EVERNOTE;
import static gov.nasa.jpl.hi.marsimages.MarsImagesApp.MARS_IMAGES;
import static gov.nasa.jpl.hi.marsimages.MarsImagesApp.VIEW_PAGER_SOURCE;

/**
 */
public class ImageViewPagerFragment extends Fragment {

    public static final String STATE_PAGE_NUMBER = "page_number";

    private ImagePagerAdapter mAdapter;
    private ViewPager mPager;
    private boolean needToSetViewPagerToPageZero = false;

    public ImageViewPagerFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        IntentFilter filter = new IntentFilter(EvernoteMars.END_NOTE_LOADING);
        filter.addAction(MarsImagesApp.MISSION_CHANGED);
        filter.addAction(MarsImagesApp.IMAGE_SELECTED);
        filter.addAction(MarsImagesApp.NOTES_CLEARED);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, filter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View parentView = inflater.inflate(R.layout.fragment_image_view_pager, container, false);

        // Set up ViewPager and backing adapter
        mAdapter = new ImagePagerAdapter(getActivity().getSupportFragmentManager());
        mPager = (ViewPager) parentView.findViewById(R.id.pager);
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
                LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);

                //try to load more notes when the last image view page is selected
                int pageCount = mAdapter.getCount();
                if (position >= pageCount - mPager.getOffscreenPageLimit() - 1) {
                    EVERNOTE.loadMoreImages(getActivity());
                }
            }
        });

        if (savedInstanceState != null) {
            int selectedPage = savedInstanceState.getInt(STATE_PAGE_NUMBER, -1);
            if (selectedPage > -1) {
                setUpViewPager(selectedPage);
            }
        }

        return parentView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_PAGE_NUMBER, mPager.getCurrentItem());
    }

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
        super.onDestroy();
    }

    public void setUpViewPager(int selectedPage) {
        mAdapter.setCount(EVERNOTE.getNotesCount());
        mAdapter.notifyDataSetChanged();
        mPager.setCurrentItem(selectedPage);
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
            }
        }
    };

    public void shareImage() {
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
                    Toast.makeText(getActivity(), text, Toast.LENGTH_SHORT).show();
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

    public void saveImageToGallery() {
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
                Toast.makeText(getActivity(), text, Toast.LENGTH_SHORT).show();
            }
        }.execute(thisNote, mPager.getCurrentItem());
    }

    private File saveImageToExternalStorage(Note thisNote, Integer pageNumber) {
        String imageURL = thisNote.getResources().get(0).getAttributes().getSourceURL();
        ImageView imageView = (ImageView) mPager.findViewWithTag(getImageViewTag(pageNumber));
        final Activity activity = getActivity();

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
        MediaScannerConnection.scanFile(getActivity().getApplicationContext(), new String[]{jpegFile.toString()}, null, null);

        return jpegFile;
    }

    public static int getImageViewFragmentNumber(String tag) {
        return Integer.parseInt(tag.substring(9));
    }

    private static String getImageViewTag(int number) {
        return "imageview" + number;
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
        public android.support.v4.app.Fragment getItem(int position) {
            Log.d("getitem", "Getting fragment for index: " + position);
            String imageUrl = EVERNOTE.getNoteUrl(position);
            return ImageViewFragment.newInstance(imageUrl, getImageViewTag(position));
        }
    }
}
