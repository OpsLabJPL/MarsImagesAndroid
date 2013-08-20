package gov.nasa.jpl.hi.marsimages;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SimpleTimeZone;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Application;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import com.evernote.android.edam.TAndroidHttpClient;
import com.evernote.edam.error.EDAMNotFoundException;
import com.evernote.edam.error.EDAMSystemException;
import com.evernote.edam.error.EDAMUserException;
import com.evernote.edam.notestore.NoteFilter;
import com.evernote.edam.notestore.NoteList;
import com.evernote.edam.notestore.NoteStore;
import com.evernote.edam.type.Note;
import com.evernote.edam.type.NoteSortOrder;
import com.evernote.edam.type.Resource;
import com.evernote.edam.userstore.Constants;
import com.evernote.edam.userstore.PublicUserInfo;
import com.evernote.edam.userstore.UserStore;
import com.evernote.thrift.TException;
import com.evernote.thrift.protocol.TBinaryProtocol;
import com.evernote.thrift.transport.TTransportException;
import com.powellware.marsimages.R;

public class MarsImagesApp extends Application implements IEvernoteServer {

	public static final String OPPORTUNITY_MISSION = "Opportunity";
	public static final String CURIOSITY_MISSION = "Curiosity";
	public static final String SPIRIT_MISSION = "Spirit";

	public static final String TAG = "MarsImages";

	public static final String JPEG_MIME_TYPE = "image/jpeg";
	public static final String PNG_MIME_TYPE = "image/png";
	public static final String MARS_IMAGE_FILENAME = "MarsImage.jpg";
	public static final String MARS_PLOT_FILENAME = "PlotMapImage.jpg";

	private static final String USER_AGENT = "Evernote/MarsImages (Android) " + 
			Constants.EDAM_VERSION_MAJOR + "." + 
			Constants.EDAM_VERSION_MINOR;
	private static final String APP_DATA_PATH = "/Android/data/gov.nasa.jpl.hi.marsimages/files/";
	private static final String EVERNOTE_HOST = "www.evernote.com"; 
	private static final String USERSTORE_URL = "https://" + EVERNOTE_HOST + "/edam/user";
	private static final String NOTESTORE_URL_BASE = "https://" + EVERNOTE_HOST + "/edam/note/";
	private static final String OPPORTUNITY_IMAGES_NOTEBOOK_GUID = "758b3821-6e6d-484d-b297-f4bdfa2aabdc"; //production
	private static final String CURIOSITY_IMAGES_NOTEBOOK_GUID = "c5ddfcb6-6878-4f9f-96ef-04fe50da1c10";   //production
	private static final String SPIRIT_IMAGES_NOTEBOOK_GUID = "7db68065-53c3-4211-be9b-79dfa4a7a2be"; //production

	public static final double EARTH_SECS_PER_MARS_SEC = 1.027491252;
	
	private static String mission;
	public static long pausedTime = 0;
	public static String searchWords = "";
	
	/* singleton instance of this application */

	private static UserStore.Client userStore;
	private static NoteStore.Client noteStore;
	private byte[] selectedImageBytes;
	private Note anaglyph;
	private Note selectedNote;
	private Bitmap plotMap;
	private static String publicUser;
	private static Map<String, Long> missionEpochs;
	
	@Override
	public void onCreate() {
		super.onCreate();
		Log.i(TAG, "starting Mars Images App");
		missionEpochs = new HashMap<String, Long>();
		GregorianCalendar calendar = new GregorianCalendar(new SimpleTimeZone(0, "UTC"));
		calendar.set(2004, Calendar.JANUARY, 3, 13, 26, 15);
		missionEpochs.put(SPIRIT_MISSION, calendar.getTime().getTime());
		calendar.set(2004, Calendar.JANUARY, 24, 15, 8, 59);
		missionEpochs.put(OPPORTUNITY_MISSION, calendar.getTime().getTime());
		calendar.set(2012, Calendar.AUGUST, 6, 6, 30, 0);
		missionEpochs.put(CURIOSITY_MISSION, calendar.getTime().getTime());
	}

	/**
	 * Fetch a thumbnail image JPEG from a storage service.
	 * 
	 * @param sol
	 *            the sol when the image was captured
	 * @param imageId
	 *            the id of the image
	 * @return a thumbnail JPEG in a byte array
	 */
	public static byte[] getThumbnailImage(String sol, String imageId) {
		String host = "";
		StringBuilder path = new StringBuilder();
		if (mission.equals(CURIOSITY_MISSION)) {
			while (sol.length() < 5) { //pad sol to 5 digits
				sol = "0" + sol;
			}
			if (Character.isDigit(imageId.charAt(0))) {
				host = "mars.jpl.nasa.gov";
				path.append("/msl-raw-images/msss/")
				.append(sol)
				.append("/")
				.append(ImageIDUtils.getInstrumentDir(imageId))
				.append('/')
				.append(ImageIDUtils.getThumbnailId(imageId))
				.append(".jpg");				
			}
			else { //MIPL
				host = "mars.jpl.nasa.gov";
				path.append("/msl-raw-images/proj/msl/redops/ods/surface/sol/")
				.append(sol)
				.append("/opgs/edr/")
				.append(ImageIDUtils.getInstrumentDir(imageId))
				.append('/')
				.append(ImageIDUtils.getThumbnailId(imageId))
				.append(".JPG");
			}
		} else if (mission.equals(OPPORTUNITY_MISSION)) {
			if (sol.length() == 4 && sol.charAt(0)=='0') { //pad sol to minimum of 3 digits, more is ok
				sol = sol.substring(1,4);
			}
			if (ImageIDUtils.isColorPancamFromCornellEdu(imageId)) {
				 //http://marswatch.astro.cornell.edu/pancam_instrument/images/False/Sol3112B_P2419_1_False_L257_pos_3_thumb.jpg
				host = "marswatch.astro.cornell.edu";
				path.append("/pancam_instrument/images/False/Sol")
				.append(sol)
				.append("B_")
				.append(ImageIDUtils.getThumbnailId(imageId))
				.append(".jpg");
			} else if (imageId.equals("Course")) {
				host = "merpublic.s3.amazonaws.com";
				path.append("/oss/merb/ops/ops/surface/tactical/sol/")
				.append(sol)
				.append("/sret/mobidd/mot-all-report/cache-mot-all-report/hyperplots/raw_north_vs_raw_east_thumb.png");
			} else { //MIPL-style thumbnail image
				host = "merpublic.s3.amazonaws.com";
				path.append("/oss_maestro/merb/ops/ops/surface/tactical/sol/")
				.append(sol)
				.append("/opgs/edr/")
				.append(ImageIDUtils.getInstrumentDir(imageId))
				.append('/')
				.append(ImageIDUtils.getThumbnailId(imageId))
				.append(".JPG");
			}
		} else if (mission.equals(SPIRIT_MISSION)) {
			if (sol.length() == 4 && sol.charAt(0)=='0') { //pad sol to minimum of 3 digits, more is ok
				sol = sol.substring(1,4);
			}
			if (ImageIDUtils.isColorPancamFromCornellEdu(imageId)) {
				//http://marswatch.astro.cornell.edu/pancam_instrument/images/False/Sol2018A_P2284_1_False_L456_pos_9_thumb.jpg
				host = "marswatch.astro.cornell.edu";
				path.append("/pancam_instrument/images/False/Sol")
				.append(sol)
				.append("A_")
				.append(ImageIDUtils.getThumbnailId(imageId))
				.append(".jpg");
			} else if (imageId.equals("Course")) {
				host = "merpublic.s3.amazonaws.com";
				path.append("/oss/merb/ops/ops/surface/tactical/sol/")
				.append(sol)
				.append("/sret/mobidd/mot-all-report/cache-mot-all-report/hyperplots/raw_north_vs_raw_east_thumb.png");
			} else { //MIPL-style thumbnail image
				host = "merpublic.s3.amazonaws.com";
				path.append("/oss_maestro/mera/ops/ops/surface/tactical/sol/")
				.append(sol)
				.append("/opgs/edr/")
				.append(ImageIDUtils.getInstrumentDir(imageId))
				.append('/')
				.append(ImageIDUtils.getThumbnailId(imageId))
				.append(".JPG");
			}
		} else
			return new byte[] {};
		
		DefaultHttpClient client = new DefaultHttpClient();
		try {
			String thumbPath = path.toString(); // .replaceAll("#", "%23;");
			URI thumbUri = new URI("http", host, thumbPath, null);
			HttpGet get = new HttpGet(thumbUri);
			HttpResponse response = client.execute(get);
			int status = response.getStatusLine().getStatusCode();
			if (status == 200) {
				InputStream inputStream = response.getEntity().getContent();
				byte[] imageBytes = IOUtils.toByteArray(inputStream);
				return imageBytes;
			}
		} catch (IOException e) {
			Log.w(TAG, "Unable to download thumbnail image from server", e);
		} catch (OutOfMemoryError e) {
			Log.w(TAG, "Unable to load thumbnail image into memory", e);
		} catch (URISyntaxException e) {
			Log.w(TAG, "Unable to encode URL: "+path.toString());
		}
		finally {
			client.getConnectionManager().shutdown();
		}
		return new byte[] {};
	}

	/**
	 * Fetch some Evernote Mars image notes.
	 * 
	 * @param offset
	 *            the starting offset in the search results
	 * @param total
	 *            the number of notes to request
	 * @return a list of image notes of size 0 to total, inclusive
	 */
	public synchronized List<Note> getImageNotes(int offset, int total) {
		try {
			return getImageNotesImpl(offset, total);
		} catch (Exception e) {
			Log.d(TAG, "Reauthenticating/connecting with server");
			connectToEvernote(); //try to reauthenticate with the server
			return getImageNotesImpl(offset, total); 
			//let a RuntimeException be thrown here to be caught by the caller
		}
	}

	/**
	 * Fetch the first JPEG resources from a note.
	 * 
	 * @param note
	 *            the note to fetch a JPEG from
	 * @return a byte array of JPEG encoded image data
	 */
	public synchronized byte[] readImageResourceFromNote(Note note) {
		try {
			return readImageResourceFromNoteImpl(note);
		} catch (RuntimeException e) {
			if (e.getCause() instanceof EDAMUserException) {
				Log.d(TAG, "Reauthenticating with server");
				connectToEvernote(); //try to reauthenticate with the server
				return readImageResourceFromNoteImpl(note); 
				//let a RuntimeException be thrown here to be caught by the caller
			} else {
				throw e;
			}
		}
	}

	/**
	 * Return the note content for a note
	 * 
	 * @param note
	 *            the note to fetch content for
	 * @return the note content (ENML format)
	 */
	public synchronized String readContentFromNote(Note note) {
		try {
			return readContentFromNoteImpl(note);
		} catch (RuntimeException e) {
			if (e.getCause() instanceof EDAMUserException) {
				Log.d(TAG, "Reauthenticating with server");
				connectToEvernote(); //try to reauthenticate with the server
				return readContentFromNoteImpl(note); 
				//let a RuntimeException be thrown here to be caught by the caller
			}
			else {
				throw e;
			}
		}
	}

	/**
	 * Retain the selected Mars image for the image preview fragment
	 * @param selectedImageBytes
	 */
	public void setSelectedImageBytes(byte[] selectedImageBytes) {
		this.selectedImageBytes = selectedImageBytes;
	}

	public byte[] getSelectedImageBytes() {
		return selectedImageBytes;
	}

	/**
	 * Set the note of the image that is the stereo pair of the selected image.
	 * FullscreenImageView will load the JPEG image from this note on demand.
	 * 
	 * @param note
	 *            the complement image of the selected image, together these
	 *            make a stereo pair
	 */
	public void setAnaglyphImageNote(Note note) {
		this.anaglyph = note;
	}

	public Note getAnaglyphImageNote() {
		return anaglyph;
	}
	
	/**
	 * Set the associated plot of the image.
	 * FullscreenImageView will load this image on demand.
	 * 
	 * @param plotImage
	 *            the plot map image associated with the image
	 */
	public void setPlotMapImage(Bitmap plotImage) {
		this.plotMap = plotImage;
	}

	public Bitmap getPlotMapImage() {
		return plotMap;
	}

	/**
	 * Return whether the anaglyph image is left-eye or right-eye.
	 * 
	 * @return true for left-eye, false for right-eye
	 */
	public boolean isAnaglyphImageLeft() {
		if (anaglyph != null) {
			String title = anaglyph.getTitle();
			
			boolean isMER = isMERMission();
			
			if (isMER && title.length() >= 4 && title.charAt(title.length()-4) == 'L') {
				return true;
			} else if (MarsImagesApp.getMission().equals(MarsImagesApp.CURIOSITY_MISSION)) {
				String[] tokens = title.split(" ");
				if (tokens.length == 4 && tokens[3].charAt(1) == 'L')
					return true;
			}
		}
		return false;
	}

	public static boolean isMERMission() {
		return MarsImagesApp.getMission().equals(MarsImagesApp.OPPORTUNITY_MISSION) 
				|| MarsImagesApp.getMission().equals(MarsImagesApp.SPIRIT_MISSION);
	}

	/**
	 * Retain the selected image note from the ImageListFragment.
	 * 
	 * @param note
	 *            the selected image note.
	 */
	public void setSelectedNote(Note note) {
		selectedNote = note;
	}

	public Note getSelectedNote() {
		return selectedNote;
	}

	public static String getMission() {
		return mission;
	}
	
	public static long getMissionEpochMillis() {
		return missionEpochs.get(getMission());
	}

	public static void setMission(String mission) {
		MarsImagesApp.mission = mission;
		if (getMission().equals(SPIRIT_MISSION))
			publicUser = "spiritrover";
		else if (getMission().equals(CURIOSITY_MISSION))
			publicUser = "curiosityrover";
		else //OPPY
			publicUser = "marsrovers";
		
		//reconnect to Evernote with the new public user info
		userStore = null;
		noteStore = null;
	}

	private synchronized List<Note> getImageNotesImpl(int offset, int total) {
		List<Note> notes = new ArrayList<Note>();
		try {
		
			Log.i(TAG, "fetching notes with offset: "+offset);
			if (isConnectedToEvernote()) {
				connectToEvernote();
			}

			NoteFilter filter = new NoteFilter();
			if (searchWords != null && searchWords.length() > 0)
				filter.setWords(searchWords);
			filter.setNotebookGuid(getCurrentNotebookGUID());
			filter.setOrder(NoteSortOrder.TITLE.getValue());
			filter.setAscending(false);
			NoteList noteList = noteStore.findNotes("", filter, offset, total);
			notes.addAll(noteList.getNotes());

		} catch (EDAMUserException e) {
			Log.e(TAG, "User exception searching notes", e);
			throw new RuntimeException("User exception searching notes", e);
		} catch (EDAMSystemException e) {
			Log.e(TAG, "System exception searching notes", e);
			throw new RuntimeException("System exception searching notes", e);
		} catch (EDAMNotFoundException e) {
			Log.e(TAG, "Note not found while searching notes", e);
			throw new RuntimeException("Note not found while searching notes", e);
		} catch (TException e) {
			Log.e(TAG, "Network error searching notes", e);
			throw new RuntimeException("Network error searching notes", e);
		}
		return notes;
	}

	private String getCurrentNotebookGUID() {
		if (mission.equals(OPPORTUNITY_MISSION))
			return OPPORTUNITY_IMAGES_NOTEBOOK_GUID;
		else if (mission.equals(CURIOSITY_MISSION))
			return CURIOSITY_IMAGES_NOTEBOOK_GUID;
		else if (mission.equals(SPIRIT_MISSION))
			return SPIRIT_IMAGES_NOTEBOOK_GUID;
		else {
			Log.w(TAG, "Unknown mission: "+mission);
			return "";
		}
	}

	private synchronized byte[] readImageResourceFromNoteImpl(Note note) {
		try {

			if (isConnectedToEvernote()) {
				connectToEvernote();
			}

			if (note != null) {
				for (Resource resource : note.getResources()) {
					if (resource.getMime().equals(MarsImagesApp.JPEG_MIME_TYPE) || resource.getMime().equals(MarsImagesApp.PNG_MIME_TYPE)) {
						return noteStore.getResourceData("", resource.getGuid());
					}
				}
			} else {
				Log.e(TAG, "readImageResourceFromNote called with null argument");
			}

		} catch (EDAMUserException e) {
			Log.e(TAG, "User exception fetching image resource", e);
			throw new RuntimeException("User exception fetching image resource", e);
		} catch (EDAMSystemException e) {
			Log.e(TAG, "System exception fetching image resource", e);
			throw new RuntimeException("System exception fetching image resource", e);
		} catch (EDAMNotFoundException e) {
			Log.e(TAG, "GUID not found fetching image resource", e);
			throw new RuntimeException("GUID not found fetching image resource", e);
		} catch (TException e) {
			Log.e(TAG, "Network error fetching image resource", e);
			throw new RuntimeException("network error fetching image resource", e);
		}
		return new byte[] {};
	}

	public static boolean isConnectedToEvernote() {
		return userStore != null && noteStore != null;
	}

	private synchronized String readContentFromNoteImpl(Note note) {
		try {
			if (isConnectedToEvernote()) {
				connectToEvernote();
			}
			if (note == null) {
				return "";
			}
			return noteStore.getNoteContent("", note.getGuid());

		} catch (EDAMUserException e) {
			Log.e(TAG, "User exception fetching note content", e);
			throw new RuntimeException("User exception fetching note content", e);
		} catch (EDAMSystemException e) {
			Log.e(TAG, "System exception fetching content", e);
			throw new RuntimeException("System exception fetching note content", e);
		} catch (EDAMNotFoundException e) {
			Log.e(TAG, "Note not found error fetching note content", e);
			throw new RuntimeException("Note not found error fetching content", e);
		} catch (TException e) {
			Log.e(TAG, "Network error fetching note content", e);
			throw new RuntimeException("Network error fetching note content", e);
		}
	}

	/**
	 * Set up communications with the Evernote web service API, including
	 * authenticating the user.
	 */
	private synchronized void connectToEvernote() {
		try {
			// You can also use EDAMUtil.getUserStoreClient() to build a UserStore.client
			TAndroidHttpClient userStoreTrans = 
					new TAndroidHttpClient(USERSTORE_URL, USER_AGENT, getTempDir());
			TBinaryProtocol userStoreProt = new TBinaryProtocol(userStoreTrans);
			userStore = new UserStore.Client(userStoreProt, userStoreProt);

			// Verify that the Evernote API version we're using is compatible with the server
			boolean versionOk = userStore.checkVersion("Mars Images",
					com.evernote.edam.userstore.Constants.EDAM_VERSION_MAJOR,
					com.evernote.edam.userstore.Constants.EDAM_VERSION_MINOR);
			if (!versionOk) {
				Log.e(TAG, getString(R.string.err_protocol_version));
				throw new RuntimeException(getString(R.string.err_protocol_version));
			}
			PublicUserInfo user = userStore.getPublicUserInfo(publicUser);
			// After successful authentication, configure a connection to the NoteStore
			String noteStoreUrl = NOTESTORE_URL_BASE + user.getShardId();
			TAndroidHttpClient noteStoreTrans = 
					new TAndroidHttpClient(noteStoreUrl, USER_AGENT, getTempDir());
			TBinaryProtocol noteStoreProt = new TBinaryProtocol(noteStoreTrans);
			noteStore = new NoteStore.Client(noteStoreProt, noteStoreProt);

		} catch (EDAMSystemException e) {
			Log.e(TAG, "System exception connecting to Evernote", e);
			throw new RuntimeException("System exception connecting to Evernote", e);
		} catch (TTransportException e) {
			Log.e(TAG, "Transport exception connecting to Evernote.", e);
			throw new RuntimeException("Transport exception connecting to Evernote.", e);
		} catch (TException e) {
			Log.e(TAG, "Network exception connecting to Evernote.", e);
			throw new RuntimeException("Network exception connecting to Evernote.", e);
		} catch (EDAMNotFoundException e) {
			Log.e(TAG, "Network exception connecting to Evernote.", e);
			throw new RuntimeException("Network exception connecting to Evernote.", e);
		} catch (EDAMUserException e) {
			Log.e(TAG, "Network exception connecting to Evernote.", e);
			throw new RuntimeException("Network exception connecting to Evernote.", e);
		}
	}

	private static File getTempDir() {
		return new File(Environment.getExternalStorageDirectory(), APP_DATA_PATH);
	}

}
