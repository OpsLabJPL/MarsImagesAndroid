package gov.nasa.jpl.hi.marsimages.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.TextView;

import com.evernote.edam.type.Note;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import com.powellware.marsimages.R;

import gov.nasa.jpl.hi.marsimages.EvernoteMars;
import gov.nasa.jpl.hi.marsimages.MarsImagesApp;
import gov.nasa.jpl.hi.marsimages.Utils;
import uk.co.senab.photoview.PhotoViewAttacher;

import static gov.nasa.jpl.hi.marsimages.EvernoteMars.EVERNOTE;
import static gov.nasa.jpl.hi.marsimages.MarsImagesApp.MARS_IMAGES;

/**
 * Created by mpowell on 5/4/14.
 */
public class ImageViewFragment extends Fragment
        implements HackyTouchListener, PhotoViewAttacher.OnViewTapListener {

    private static final String IMAGE_DATA_EXTRA = "extra_image_data";

    private String mImageUrl;
    private boolean reloadImageDueToMissionChange = false;
    private int number;
    private ImageView mImageView;
    private String imageViewTag;
    private PhotoViewAttacher mAttacher;
    private TextView mCaptionView;
    private static boolean captionVisible = true;
    private HackySlidingPaneLayout mLayout;

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
        final View v = inflater.inflate(R.layout.image_view_fragment, container, false);
        mImageView = (ImageView) v.findViewById(R.id.imageView);
        mImageView.setTag(imageViewTag);
        setupPhotoViewAttacher();
        mAttacher.setOnViewTapListener(this);

        mCaptionView = (TextView) v.findViewById(R.id.captionView);
        Note note = EVERNOTE.getNote(number);
        String caption = MARS_IMAGES.getMission().getCaptionText(note);
        mCaptionView.setText(caption);
        mCaptionView.setAlpha(captionVisible ? 1 : 0);
        mLayout = (HackySlidingPaneLayout) getActivity().findViewById(R.id.main_layout);
        mLayout.addHackyTouchListener(this);

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

        loadImage(mImageUrl, mImageView, mAttacher);

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
                        loadImage(mImageUrl, mImageView, mAttacher);
                    }
                }
            }
        }
    };

    private void loadImage(String url, ImageView imageView, final PhotoViewAttacher attacher) {
        ImageLoader.getInstance().displayImage(url, imageView,
                new SimpleImageLoadingListener() {
                    @Override
                    public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                        if (attacher != null) {
                            attacher.update();
                        }
                    }
                }
        );
    }

    @Override
    public void onPause() {
        super.onPause();
        teardownPhotoViewAttacher();
    }

    @Override
    public void onResume() {
        super.onResume();
        setupPhotoViewAttacher();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mLayout.removeHackyTouchListener(this);
        if (mAttacher != null) {
            mAttacher.setOnPhotoTapListener(null);
            teardownPhotoViewAttacher();
        }
        if (mImageView != null) {
            // Cancel any pending image work
            ImageLoader.getInstance().cancelDisplayTask(mImageView);
            mImageView.setImageDrawable(null);
        }
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
    }

    @Override
    public void onViewTap(View view, float v, float v2) {
        if (captionVisible) {
            captionVisible = false;
            fadeOutCaptionView(0);
        } else {
            captionVisible = true;
            mCaptionView.setAlpha(1);
            //schedule a 3 second delay and then animate fade out to zero alpha
            fadeOutCaptionView(3000);
        }
    }

    private void fadeOutCaptionView(int delayMillis) {
        AlphaAnimation anim = new AlphaAnimation(1.0f, 0.0f);
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}
            @Override
            public void onAnimationEnd(Animation animation) {
                mCaptionView.setAlpha(0);
            }
            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
        anim.setStartOffset(delayMillis);
        anim.setDuration(1000);
        mCaptionView.startAnimation(anim);
    }

    @Override
    public void moved() {
        captionVisible = false;
        mCaptionView.setAlpha(0);
    }
}
