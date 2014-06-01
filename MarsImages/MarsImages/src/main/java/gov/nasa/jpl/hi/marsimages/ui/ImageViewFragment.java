package gov.nasa.jpl.hi.marsimages.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.common.base.Stopwatch;
import com.powellware.marsimages.R;

import java.util.concurrent.TimeUnit;

import gov.nasa.jpl.hi.marsimages.EvernoteMars;
import gov.nasa.jpl.hi.marsimages.MarsImagesApp;
import gov.nasa.jpl.hi.marsimages.Utils;
import gov.nasa.jpl.hi.marsimages.image.ImageFetcher;
import gov.nasa.jpl.hi.marsimages.image.ImageWorker;
import uk.co.senab.photoview.PhotoViewAttacher;

import static gov.nasa.jpl.hi.marsimages.EvernoteMars.EVERNOTE;

/**
 * Created by mpowell on 5/4/14.
 */
public class ImageViewFragment extends Fragment {
    private static final String IMAGE_DATA_EXTRA = "extra_image_data";

    private String mImageUrl;
    private boolean reloadImageDueToMissionChange = false;
    private int number;
    private ImageView mImageView;
    private ImageFetcher mImageFetcher;
    private String imageViewTag;
    private PhotoViewAttacher mAttacher;

    /**
     * Factory method to generate a new instance of the fragment given an image number.
     *
     * @param imageUrl The image url to load
     * @return A new instance of ImageDetailFragment with imageNum extras
     */
    public static ImageViewFragment newInstance(String imageUrl, String imageViewTag) {
        final ImageViewFragment f = new ImageViewFragment();
        final Bundle args = new Bundle();
        f.number = ImageViewActivity.getImageViewFragmentNumber(imageViewTag);
        args.putString(IMAGE_DATA_EXTRA, imageUrl);
        f.setArguments(args);
        f.imageViewTag = imageViewTag;
        return f;
    }

    // Empty constructor, required as per Fragment docs
    public ImageViewFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mImageUrl = getArguments() != null ? getArguments().getString(IMAGE_DATA_EXTRA) : null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // image_detail_fragment.xml contains just an ImageView
        final View v = inflater.inflate(R.layout.image_view_fragment, container, false);
        mImageView = (ImageView) v.findViewById(R.id.imageView);
        mImageView.setTag(imageViewTag);
        setupPhotoViewAttacher();
        return v;
    }

    private void setupPhotoViewAttacher() {
        if (mAttacher == null) {
            mAttacher = new MyPhotoViewAttacher(mImageView);
            mAttacher.setMinimumScale(0.999f);
            mAttacher.setMediumScale(1.0f);
            mAttacher.setMaximumScale(8.0f);
        }
    }

    private void teardownPhotoViewAttacher() {
        if (mAttacher != null) {
            mAttacher.cleanup();
            mAttacher = null;
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Use the parent activity to load the image asynchronously into the ImageView (so a single
        // cache can be used over all pages in the ViewPager)
        if (ImageViewActivity.class.isInstance(getActivity())) {
            mImageFetcher = ((ImageViewActivity) getActivity()).getImageFetcher();
            mImageFetcher.loadImage(mImageUrl, mImageView, mAttacher);
        }

        // Pass clicks on the ImageView to the parent activity to handle
        if (View.OnClickListener.class.isInstance(getActivity()) && Utils.hasHoneycomb()) {
            mImageView.setOnClickListener((View.OnClickListener) getActivity());
        }

        IntentFilter filter = new IntentFilter(MarsImagesApp.MISSION_CHANGED);
        filter.addAction(EvernoteMars.END_NOTE_LOADING);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver,
                filter);
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //intent action: mission changed
            if (intent.getAction().equals(MarsImagesApp.MISSION_CHANGED)) {
                if (mImageView != null) {
                    mImageView.setImageDrawable(null);
                    reloadImageDueToMissionChange = true;
                    if (mAttacher != null) {
                        mAttacher.update();
                    }
                }
            }
            else if (intent.getAction().equals(EvernoteMars.END_NOTE_LOADING)) {
                if (reloadImageDueToMissionChange) {
                    if (EVERNOTE.getNotesCount() > number) {
                        reloadImageDueToMissionChange = false;
                        mImageUrl = EVERNOTE.getNoteUrl(number);
                        mImageFetcher.loadImage(mImageUrl, mImageView, mAttacher);
                    }
                }
            }
        }
    };

    @Override
    public void onPause() {
        super.onPause();
        teardownPhotoViewAttacher();
    }

    @Override
    public void onResume() {
        super.onResume();
//        setupPhotoViewAttacher();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        teardownPhotoViewAttacher();
        if (mImageView != null) {
            // Cancel any pending image work
            ImageWorker.cancelWork(mImageView);
            mImageView.setImageDrawable(null);
        }
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
    }
}
