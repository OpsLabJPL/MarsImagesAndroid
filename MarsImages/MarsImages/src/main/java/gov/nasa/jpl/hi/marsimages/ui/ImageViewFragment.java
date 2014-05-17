package gov.nasa.jpl.hi.marsimages.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.common.base.Stopwatch;
import com.powellware.marsimages.R;

import java.util.concurrent.TimeUnit;

import gov.nasa.jpl.hi.marsimages.Utils;
import gov.nasa.jpl.hi.marsimages.image.ImageFetcher;
import gov.nasa.jpl.hi.marsimages.image.ImageWorker;
import uk.co.senab.photoview.PhotoViewAttacher;

/**
 * Created by mpowell on 5/4/14.
 */
public class ImageViewFragment extends Fragment {
    private static final String IMAGE_DATA_EXTRA = "extra_image_data";

    private String mImageUrl;
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
        mAttacher = new PhotoViewAttacher(mImageView);
        mAttacher.setMinimumScale(0.999f);
        mAttacher.setMediumScale(1.0f);
        mAttacher.setMaximumScale(8.0f);
        return v;
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
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mAttacher != null)
            mAttacher.cleanup();
        if (mImageView != null) {
            // Cancel any pending image work
            ImageWorker.cancelWork(mImageView);
            mImageView.setImageDrawable(null);
        }
    }
}
