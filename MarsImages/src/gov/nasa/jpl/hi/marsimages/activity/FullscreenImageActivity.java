package gov.nasa.jpl.hi.marsimages.activity;

import static gov.nasa.jpl.hi.marsimages.MarsImagesApp.MARS_IMAGE_FILENAME;
import static gov.nasa.jpl.hi.marsimages.MarsImagesApp.MARS_PLOT_FILENAME;
import static gov.nasa.jpl.hi.marsimages.MarsImagesApp.TAG;
import gov.nasa.jpl.hi.marsimages.MarsImagesApp;
import gov.nasa.jpl.hi.marsimages.ToastServerExceptionRunnable;
import gov.nasa.jpl.hi.marsimages.view.FullscreenImageView;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
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
public class FullscreenImageActivity extends SherlockFragmentActivity {

	private Bitmap bitmap;
	private Bitmap plotMap;
	private Note anaglyphImageNote;
	private AlertDialog alertDialog;
	private ImageViewFragment imageViewFragment;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        setContentView(R.layout.fullscreen_image_activity);
        imageViewFragment = (ImageViewFragment) getSupportFragmentManager().findFragmentById(R.id.imageView);
	}

	@Override
	protected void onStart() {
		super.onStart();

		setSupportProgressBarIndeterminateVisibility(false);
		if (bitmap == null) {
			try {
				File cacheFile = new File(getCacheDir(), MARS_IMAGE_FILENAME);
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
	public void onResume() {
		super.onResume();
		
		reloadImage();
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		//free bitmap memory ASAP
		final FullscreenImageView view = imageViewFragment.getView();
		if (view != null) {
			view.setImage(null);
			view.draw(null);
		}
		
		if (alertDialog != null) {
			alertDialog.dismiss();
		}
	}
	
	@Override
	protected void onRestart() {
		super.onRestart();
		
		reloadImage();
	}
	
	void reloadImage() {
		setProgressBarIndeterminateVisibility(true);
		
		new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void...params) {
				bitmap = BitmapFactory.decodeFile(new File(getCacheDir(), MARS_IMAGE_FILENAME).getAbsolutePath());
				imageViewFragment.getView().setImage(bitmap);
				return null;
			}
			
			@Override
			protected void onPostExecute(Void v) {
				setProgressBarIndeterminateVisibility(false);
			}
		}.execute();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		anaglyphImageNote = ((MarsImagesApp)getApplication()).getAnaglyphImageNote();
		plotMap = ((MarsImagesApp)getApplication()).getPlotMapImage();
		
		if (anaglyphImageNote != null) {
			getSupportMenuInflater().inflate(R.menu.stereo_menu, menu);
			menu.findItem(R.id.stereo).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		} else {
			getSupportMenuInflater().inflate(R.menu.nonstereo_menu, menu);
		}
		if(plotMap != null) {
			getSupportMenuInflater().inflate(R.menu.plot_item_menu, menu);
			menu.findItem(R.id.plot).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		}
		menu.findItem(R.id.info).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		menu.findItem(R.id.share).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		final MarsImagesApp app = (MarsImagesApp)getApplication();
		final FullscreenImageView view = imageViewFragment.getView();
		
		switch (item.getItemId()) {
		case R.id.stereo:

			if (view.getImage2() != null) {
				//toggle back to 2D
				view.setImage2(null);
				view.invalidate();
				return false;
			}

			//go to 3D
			setProgressBarIndeterminateVisibility(true);
			new AsyncTask<Note, Void, byte[]>() {
				@Override
				protected byte[] doInBackground(Note... note) {
					try {
						return app.readImageResourceFromNote(note[0]);
					} catch (RuntimeException e) {
						runOnUiThread(new ToastServerExceptionRunnable(FullscreenImageActivity.this, e));
						return null;
					} catch (OutOfMemoryError e) {
						runOnUiThread(new Runnable() {
							public void run() {
								alertDialog = new AlertDialog.Builder(FullscreenImageActivity.this).create();
								alertDialog.setTitle("Insufficient memory");
								alertDialog.setMessage("Sorry, there is not enough memory to build the 3D image.");
								alertDialog.show();
							}
						});
						return null;
					}
				}

				@Override
				protected void onPostExecute(byte[] result) {
					if (result == null)
						return;

					setProgressBarIndeterminateVisibility(false);
					try {
						Bitmap otherBitmap = BitmapFactory.decodeStream(new ByteArrayInputStream(result));
						Bitmap image1 = null, image2 = null;
						image1 = bitmap;
						image2 = otherBitmap;
						view.setImages(image1, image2);
						view.invalidate();
					} catch (OutOfMemoryError e) {
						alertDialog = new AlertDialog.Builder(FullscreenImageActivity.this).create();
						alertDialog.setTitle("Insufficient memory");
						alertDialog.setMessage("Sorry, there is not enough memory to build the 3D image.");
						alertDialog.show();
					}
				}
			}.execute(anaglyphImageNote);
			break;

		case R.id.info:
			final Note selectedNote = app.getSelectedNote();
			new AsyncTask<Note, Void, String>() {

				/**
				 * Fetch image note content.
				 */
				@Override
				protected String doInBackground(Note... note) {
					try {
						return app.readContentFromNote(note[0]);
					} catch (RuntimeException e) {
						runOnUiThread(new ToastServerExceptionRunnable(FullscreenImageActivity.this, e));
						return null;
					}
				}

				/**
				 * Show image note info in a dialog.
				 */
				@Override
				protected void onPostExecute(String result) {
					if (result == null)
						return;
					result = parseXml(result);
					AlertDialog.Builder builder = new AlertDialog.Builder(FullscreenImageActivity.this);
					builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialog.dismiss();
						}
					});
					AlertDialog alertDialog = builder.create();
					alertDialog.setTitle(selectedNote.getTitle());
					alertDialog.setMessage(result);
					alertDialog.show();
				}
			}.execute(selectedNote);

			break;

		case R.id.share:					    
			/* Create the Intent */
			final Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);

			/* Fill it with Data */
			shareIntent.setType("image/jpeg");
			
			Note note = app.getSelectedNote();
			shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "From Mars Images: " + note.getTitle());
			//an email attachment will only work if the file to attach is on external storage
			File jpegFile = new File(Environment.getExternalStorageDirectory(), MARS_IMAGE_FILENAME);
			File cacheFile = new File(getCacheDir(), MARS_IMAGE_FILENAME);
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

				Bitmap bitmap = BitmapFactory.decodeFile(new File(getCacheDir(), MARS_IMAGE_FILENAME).getAbsolutePath());
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
			
		case R.id.plot:
			setProgressBarIndeterminateVisibility(false);
			
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			plotMap.compress(Bitmap.CompressFormat.PNG, 100, stream);
			byte[] byteArray = stream.toByteArray();
			
			createPlotMapViewActivity(byteArray);
			break;
		}
		return false;
	}


	/**
	 * Start plot map view activity with its Intent.
	 * @param plotMapBytes the plot to view
	 */
	protected void createPlotMapViewActivity(byte[] plotMapBytes) {
		Intent plotIntent = new Intent(CoursePlotActivity.INTENT_ACTION_PLOT_VIEW);
		// write the JPEG image data for the image view to the cache file area 
		try {
			FileOutputStream fileOutputStream = new FileOutputStream(
					new File(getApplication().getCacheDir(), MARS_PLOT_FILENAME));
			fileOutputStream.write(plotMapBytes);
			fileOutputStream.flush();
			fileOutputStream.close();

			startActivity(plotIntent);
		} catch (IOException e) {
			Log.e(TAG, "Unable to write to internal storage to start FullscreenImageView");
		}
	}

	public FullscreenImageView getView() {
		if (imageViewFragment != null) 
			return imageViewFragment.getView();
		return null;
	}

	protected String parseXml(String xml) {
		try {
			SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
			XMLReader reader = parser.getXMLReader();
			MyXMLHandler handler = new MyXMLHandler();
			reader.setContentHandler(handler);
			reader.parse(new InputSource(new StringReader(xml)));
			return handler.getText();
		} catch (SAXException e) {
			Log.e(TAG, "XML Parsing Excpetion = " + e);
		} catch (ParserConfigurationException e) {
			Log.e(TAG, "XML Parsing Excpetion = " + e);
		} catch (IOException e) {
			Log.e(TAG, "XML Parsing Excpetion = " + e);
		}

		return xml;
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
	
	/**
	 * My note formatting is pretty simple...all that is needed is to strip
	 * away the element tags and return the plain text.
	 */
	private static class MyXMLHandler extends DefaultHandler {
		private String text = "";

		public String getText() {
			return text;
		}

		@Override
		public void characters(char[] ch, int start, int length)
				throws SAXException {
			char[] actual = new char[length];
			System.arraycopy(ch, start, actual, 0, length);
			String line = new String(actual);
			// Mission Manager's Report is a note link.
			if (!line.contains("Mission Manager's Report")) 
				text = text + line + "\n";
		}
	}	
}