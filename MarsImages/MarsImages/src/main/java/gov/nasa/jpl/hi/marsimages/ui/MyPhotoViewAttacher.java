package gov.nasa.jpl.hi.marsimages.ui;

import android.util.Log;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import uk.co.senab.photoview.PhotoViewAttacher;

/**
 * Created by mpowell on 5/21/14.
 */
public class MyPhotoViewAttacher extends PhotoViewAttacher {
    public MyPhotoViewAttacher(ImageView mImageView) {
        super(mImageView);
    }

    @Override
    public ImageView getImageView() {
        ImageView imageView = super.getImageView();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream str = new PrintStream(baos);
        if (imageView == null) {
            new Exception().printStackTrace(str);
            Log.d("attacher with null imageview", new String(baos.toByteArray()));
        }
        return imageView;
    }
}
