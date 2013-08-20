package gov.nasa.jpl.hi.marsimages.activity;

import static gov.nasa.jpl.hi.marsimages.MarsImagesApp.MARS_PLOT_FILENAME;
import static gov.nasa.jpl.hi.marsimages.MarsImagesApp.TAG;
import gov.nasa.jpl.hi.marsimages.MarsImagesApp;
import gov.nasa.jpl.hi.marsimages.view.FullscreenImageView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.evernote.edam.type.Note;
import com.powellware.marsimages.R;

/**
 * View an image in a full-screen interactive image view.
 * Menu items provide additional actions:
 * <ul>
 * <li>load red-blue stereo anaglyph for 3D viewing
 * <li>display image note ancillary information
 * <li>email this image to someone
 * </ul> 
 */
public class CoursePlotActivity extends SherlockFragmentActivity {
	private Bitmap bitmap;
	private AlertDialog alertDialog;
	private ImageViewFragment imageViewFragment; 
	
	public static final String INTENT_ACTION_PLOT_VIEW = "gov.nasa.jpl.hi.marsimages.view.PLOT_VIEW";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        setContentView(R.layout.course_plot_activity);
        imageViewFragment = (ImageViewFragment) getSupportFragmentManager().findFragmentById(R.id.imageView);
	}

	@Override
	protected void onStart() {
		super.onStart();
		if (bitmap == null) { 
			try {				
				File cacheFile = new File(getCacheDir(), MARS_PLOT_FILENAME);
				FileInputStream fileInputStream = new FileInputStream(cacheFile);
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				IOUtils.copy(fileInputStream, bos);
				fileInputStream.close();
				byte[] imageBytes = bos.toByteArray();
				Options options = new BitmapFactory.Options();
				options.inScaled = false;
				bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, options);
				imageViewFragment.getView().setImage(bitmap);				
			} catch (IOException e) {
				Log.e(TAG, "Unable to read from internal storage to start FullscreenImageView");
			} catch (OutOfMemoryError e) {
				alertDialog = new AlertDialog.Builder(this).create();
				alertDialog.setTitle("Insufficient memory");
				alertDialog.setMessage("Sorry, there is not enough memory to display the image.");
				alertDialog.show();
			}
			finally {
				setProgressBarIndeterminateVisibility(false);
			}
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.plot_menu, menu);

		menu.findItem(R.id.share).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		menu.findItem(R.id.save).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		final MarsImagesApp app = (MarsImagesApp)getApplication();
		
		switch (item.getItemId()) {

		case R.id.share:					    
			/* Create the Intent */
			final Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);

			/* Fill it with Data */
			shareIntent.setType("image/jpeg");
			
			Note note = app.getSelectedNote();
			shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "From Mars Images: " + note.getTitle());
			//an email attachment will only work if the file to attach is on external storage
			File jpegFile = new File(Environment.getExternalStorageDirectory(), MARS_PLOT_FILENAME);
			File cacheFile = new File(getCacheDir(), MARS_PLOT_FILENAME);
			FileInputStream fis;
			try {
				fis = new FileInputStream(cacheFile);
				FileOutputStream fos = new FileOutputStream(jpegFile);
				IOUtils.copy(fis, fos);
				fos.flush();
				fos.close();
				fis.close();
			} catch (IOException e) {
				Log.e(TAG, "Unable to create image file on external storage for email.");
				break;
			}
			Log.d(TAG, "JPEG File to email: " + jpegFile);			
			shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(jpegFile.getAbsolutePath())));

			/* Send it off to the Activity-Chooser */
			startActivity(Intent.createChooser(shareIntent, "Share"));
			break;

		case R.id.save:
			Note imageNote = app.getSelectedNote();

			String folder = "/Mars Images";
			String path = Environment.getExternalStorageDirectory().getAbsolutePath() + folder;
			
			File dir = new File(path);
			if(!dir.exists()) {
				dir.mkdirs();
			}
			
			File file = new File(dir, imageNote.getTitle() + ".jpg");
			
			try {
				FileOutputStream fOut = new FileOutputStream(file);

				Bitmap bitmap = BitmapFactory.decodeFile(new File(getCacheDir(), MARS_PLOT_FILENAME).getAbsolutePath());
				bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fOut);
				
				fOut.flush();
				fOut.close();
				
				MediaScannerConnection.scanFile(getApplicationContext(), new String[] { file.toString() }, null, new MediaScannerConnection.OnScanCompletedListener() {
					public void onScanCompleted(String path, Uri uri) {
					}				
				});
				Context context = getApplicationContext();
				CharSequence text = "The image has been saved to your gallery!";
				int duration = Toast.LENGTH_SHORT;

				Toast toast = Toast.makeText(context, text, duration);
				toast.show();
				
			} catch (FileNotFoundException e) {
				Log.e(TAG, e.toString());
			} catch (IOException e) {
				Log.e(TAG, e.toString());
			}
			break;
		}
		return false;
	}

	@Override
	protected void onStop() {
		super.onStop();
		FullscreenImageView view = imageViewFragment.getView();
		//free bitmap memory ASAP
		if (view != null) {
			view.setImage(null);
		}
		
		if (alertDialog != null) {
			alertDialog.dismiss();
		}
	}
	
	/**
	 * Reusable image preview fragment. Only shown if there is enough screen area.
	 * Creates an image view with a Progress indicator for image fetching feedback.
	 * If you select it, it will open a full-screen interactive image view.  
	 */
	public static class ImageViewFragment extends SherlockFragment {
		
		private FullscreenImageView view;
		
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			
			setRetainInstance(true);
			
			FrameLayout layout = new FrameLayout(getActivity());
			view = new FullscreenImageView(getActivity());
			view.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.MATCH_PARENT));
			layout.addView(view);
			return layout;
		}

		public FullscreenImageView getView() {
			return view;
		}
	}

}
