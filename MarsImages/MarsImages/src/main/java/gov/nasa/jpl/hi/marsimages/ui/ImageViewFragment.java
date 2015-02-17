package gov.nasa.jpl.hi.marsimages.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.evernote.edam.type.Note;
import com.evernote.edam.type.Resource;
import com.google.common.collect.Lists;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import com.powellware.marsimages.R;

import java.util.List;

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
        implements PhotoViewAttacher.OnViewTapListener {

    private static final String IMAGE_DATA_EXTRA = "extra_image_data";
    private static final String ANAGLYPH = "Anaglyph";
    private static final String STATE_IMAGE_NUMBER = "image_number";
    private static final String STATE_RESOURCE_NUMBER = "resource_number";
    private static final String STATE_IMAGE_VIEW_TAG = "image_view_tag";

    private String mImageUrl;
    private boolean reloadImageDueToResultsChange = false;
    private int imageNumber;
    private int resourceNumber;
    private ImageView mImageView;
    private String imageViewTag;
    private PhotoViewAttacher mAttacher;
    private TextView mCaptionView;
    private Button mSelectButton;
    private final PorterDuffXfermode mXferMode = new PorterDuffXfermode(PorterDuff.Mode.LIGHTEN);
    private static final ColorMatrix redMatrix = new ColorMatrix(new float[]{
            1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f});
    private static final ColorMatrixColorFilter redFilter = new ColorMatrixColorFilter(
            redMatrix);
    private static final ColorMatrix blueMatrix = new ColorMatrix(new float[]{
            0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f});
    private static final ColorMatrixColorFilter blueFilter = new ColorMatrixColorFilter(
            blueMatrix);
    private PopupMenu.OnMenuItemClickListener menuItemClickListener;
    private PopupMenu mPopupMenu;

    /**
     * Factory method to generate a new instance of the fragment given an image imageNumber.
     *
     * @param imageUrl The image url to load
     * @return A new instance of ImageDetailFragment with imageNum extras
     */
    public static ImageViewFragment newInstance(String imageUrl, String imageViewTag) {
        final ImageViewFragment f = new ImageViewFragment();
        final Bundle args = new Bundle();
        f.imageNumber = ImageViewActivity.getImageViewFragmentNumber(imageViewTag);
        args.putString(IMAGE_DATA_EXTRA, imageUrl);
        f.setArguments(args);
        f.imageViewTag = imageViewTag;
        return f;
    }

    // Empty constructor, required as per Fragment docs
    public ImageViewFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mImageUrl = getArguments() != null ? getArguments().getString(IMAGE_DATA_EXTRA) : null;
        if (savedInstanceState != null) {
            resourceNumber = savedInstanceState.getInt(STATE_RESOURCE_NUMBER, 0);
            imageNumber = savedInstanceState.getInt(STATE_IMAGE_NUMBER, 0);
            imageViewTag = savedInstanceState.getString(STATE_IMAGE_VIEW_TAG);
            Note note = EVERNOTE.getNote(imageNumber);
            if (note != null && note.getResources().size() > resourceNumber)
                mImageUrl = note.getResources().get(resourceNumber).getAttributes().getSourceURL();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.image_view_fragment, container, false);
        mImageView = (ImageView) v.findViewById(R.id.imageView);
        mImageView.setTag(imageViewTag);
        setupPhotoViewAttacher();

        mCaptionView = (TextView) v.findViewById(R.id.captionView);
        mSelectButton = (Button) v.findViewById(R.id.selectImageButton);

        final Note note = EVERNOTE.getNote(imageNumber);
        if (note != null) {
            setupCaptionAndImageSelectionMenu(note);
        }

        return v;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_RESOURCE_NUMBER, resourceNumber);
        outState.putInt(STATE_IMAGE_NUMBER, imageNumber);
        outState.putString(STATE_IMAGE_VIEW_TAG, imageViewTag);
    }

    private void setupCaptionAndImageSelectionMenu(final Note note) {
        if (getActivity() == null) return;

        final boolean fullscreen = ((ImageViewActivity) getActivity()).isFullscreen();
        mCaptionView.setAlpha(fullscreen ? 0 : 1);
        mSelectButton.setAlpha(fullscreen ? 0 : 1);

        String caption = MARS_IMAGES.getMission().getCaptionText(note);
        mCaptionView.setText(caption);

        if (note.getResources().size() <= 1)
            mSelectButton.setVisibility(View.INVISIBLE);
        else {
            Resource resource = resourceNumber >= note.getResources().size() ? null : note.getResources().get(resourceNumber);
            String buttonText = (resource == null) ? ANAGLYPH : MARS_IMAGES.getMission().getImageName(resource);
            mSelectButton.setText(buttonText);
            final List<String> menuItemNames = Lists.newArrayList();
            for (Resource r : note.getResources()) {
                String imageName = MARS_IMAGES.getMission().getImageName(r);
                menuItemNames.add(imageName);
            }

            final String[] leftAndRight = MARS_IMAGES.getMission().stereoForImages(note);
            if (leftAndRight.length > 0) {
                menuItemNames.add(ANAGLYPH);
            }

            mSelectButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mPopupMenu = new PopupMenu(getActivity(), mSelectButton);
                    for (String menuItemName : menuItemNames) {
                        mPopupMenu.getMenu().add(Menu.NONE, menuItemNames.indexOf(menuItemName), Menu.NONE, menuItemName);
                    }
                    menuItemClickListener = new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem menuItem) {
                            CharSequence title = menuItem.getTitle();
                            mSelectButton.setText(title);
                            resourceNumber = menuItem.getItemId();
                            if (resourceNumber < note.getResources().size()) {
                                String url = note.getResources().get(resourceNumber).getAttributes().getSourceURL();
                                mImageView.setImageDrawable(null);
                                loadImage(url, mImageView, mAttacher);
                            } else { //anaglyph
                                mImageView.setImageDrawable(null);
                                loadAnaglyph(leftAndRight, mImageView, mAttacher);
                            }
                            return false;
                        }
                    };
                    mPopupMenu.setOnMenuItemClickListener(menuItemClickListener);
                    mPopupMenu.show();
                }
            });
        }
    }

    private void loadAnaglyph(final String[] leftAndRight, final ImageView mImageView, final PhotoViewAttacher mAttacher) {
        ImageLoader.getInstance().loadImage(leftAndRight[0], new SimpleImageLoadingListener() {
            @Override
            public void onLoadingComplete(String url, View view, Bitmap bitmap) {
                final Bitmap leftBitmap = bitmap;
                ImageLoader.getInstance().loadImage(leftAndRight[1], new SimpleImageLoadingListener() {
                    @Override
                    public void onLoadingComplete(String url, View view, Bitmap bitmap) {
                        //bitmap is the right eye camera bitmap
                        BitmapDrawable anaglyphImage = new BitmapDrawable(getActivity().getResources(),
                                overlayImages(leftBitmap, bitmap).copy(Bitmap.Config.ARGB_8888, false));
                        mImageView.setImageDrawable(anaglyphImage);
                        mAttacher.update();
                    }
                });
            }
        });
    }

    private void setupPhotoViewAttacher() {
        if (mAttacher == null) {
            mAttacher = new PhotoViewAttacher(mImageView);
            mAttacher.setMinimumScale(0.999f);
            mAttacher.setMediumScale(1.0f);
            mAttacher.setMaximumScale(8.0f);
            mAttacher.setOnViewTapListener(this);
        }
    }

    private void teardownPhotoViewAttacher() {
        if (mAttacher != null) {
            mAttacher.cleanup();
            mAttacher.setOnViewTapListener(null);
            mAttacher = null;
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Note note = EVERNOTE.getNote(imageNumber);
        if (note == null) return;

        if (resourceNumber < note.getResources().size())
            loadImage(mImageUrl, mImageView, mAttacher);
        else {
            final String[] leftAndRight = MARS_IMAGES.getMission().stereoForImages(note);
            loadAnaglyph(leftAndRight, mImageView, mAttacher);
        }

        // Pass clicks on the ImageView to the parent activity to handle
        if (View.OnClickListener.class.isInstance(getActivity()) && Utils.hasHoneycomb()) {
            mImageView.setOnClickListener((View.OnClickListener) getActivity());
        }

        IntentFilter filter = new IntentFilter(MarsImagesApp.MISSION_CHANGED);
        filter.addAction(EvernoteMars.END_NOTE_LOADING);
        filter.addAction(MarsImagesApp.NOTES_CLEARED);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver,
                filter);
    }

    private final BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(MarsImagesApp.MISSION_CHANGED) ||
                    intent.getAction().equals((MarsImagesApp.NOTES_CLEARED))) {
                if (mImageView != null) {
                    mImageView.setImageDrawable(null);
                    reloadImageDueToResultsChange = true;
                    if (mAttacher != null) {
                        mAttacher.update();
                    }
                }
            } else if (intent.getAction().equals(EvernoteMars.END_NOTE_LOADING)) {
                if (reloadImageDueToResultsChange) {
                    if (EVERNOTE.getNotesCount() > imageNumber) {
                        reloadImageDueToResultsChange = false;
                        mImageUrl = EVERNOTE.getNoteUrl(imageNumber);
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
                        final Note note = EVERNOTE.getNote(imageNumber);
                        if (note != null) {
                            setupCaptionAndImageSelectionMenu(note);
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
        if (mAttacher != null) {
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
        if (!Utils.hasKitKat())
            return;

        ImageViewActivity activity = (ImageViewActivity)getActivity();
        activity.setFullscreen(!activity.isFullscreen());
    }

    public void showCaption(boolean visible) {
        if (mCaptionView == null || mSelectButton == null) return;

        if (visible) {
            mCaptionView.setAlpha(1);
            mSelectButton.setAlpha(1);
        } else {
            mCaptionView.setAlpha(0);
            mSelectButton.setAlpha(0);
        }
    }

    protected void fadeOutCaptionView(int delayMillis) {
        AlphaAnimation anim = new AlphaAnimation(1.0f, 0.0f);
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mCaptionView.setAlpha(0);
                mSelectButton.setAlpha(0);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        anim.setStartOffset(delayMillis);
        anim.setDuration(1000);
        mCaptionView.startAnimation(anim);
        mSelectButton.startAnimation(anim);
    }

    Bitmap overlayImages(Bitmap left, Bitmap right) {
        Bitmap bmOverlay = Bitmap.createBitmap(left.getWidth(),
                left.getHeight(), left.getConfig());
        Canvas canvas = new Canvas(bmOverlay);
        Paint paint = new Paint();
        paint.setColorFilter(redFilter);
        canvas.drawBitmap(left.copy(left.getConfig(), true), 0, 0, paint);
        paint.setColorFilter(blueFilter);
        paint.setXfermode(mXferMode);
        canvas.drawBitmap(right.copy(right.getConfig(), true), 0, 0, paint);

        return bmOverlay;
    }
}
