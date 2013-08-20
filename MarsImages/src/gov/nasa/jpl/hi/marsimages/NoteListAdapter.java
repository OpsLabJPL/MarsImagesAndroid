package gov.nasa.jpl.hi.marsimages;

import static gov.nasa.jpl.hi.marsimages.MarsImagesApp.TAG;

import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.evernote.edam.type.Note;
import com.powellware.marsimages.R;

/**
 * Data provider for image list view. Stores a list of fetched notes and a cache
 * of thumbnail images that are downloaded from a storage service.
 */
public class NoteListAdapter extends android.widget.BaseAdapter {

	private final LinkedHashMap<String, Note> notes;
	private final List<Note> noteList;
	private final Context context;
	private final LruCache<String, Bitmap> cache;

	public NoteListAdapter(Context context) {
		this(context, new LinkedHashMap<String, Note>(), new LruCache<String, Bitmap>(100));
	}

	public NoteListAdapter(Context context, LinkedHashMap<String, Note> notes, LruCache<String, Bitmap> cache) {
		this.context = context;
		this.notes = notes;
		this.cache = cache;
		this.noteList = new LinkedList<Note>();
	}
	
	public Collection<Note> getNoteList() {
		return Collections.unmodifiableList(noteList);
	}

	public LruCache<String, Bitmap> getCache() {
		return cache;
	}

	public int getCount() {
		return noteList.size();
	}

	public Object getItem(int position) {
		if (validNoteIndex(position))
			return noteList.get(position);
		return null;
	}

	public Note getNote(String key) {
		return notes.get(key);
	}
	
	public long getItemId(int position) {
		if (validNoteIndex(position))
			return noteList.get(position).hashCode();
		return 0;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		View view = null;
		if (validNoteIndex(position)) {
			LayoutInflater inflater = ((Activity)context).getLayoutInflater();
			view = (View) inflater.inflate(R.layout.image_list_item_view, parent, false);
			final String title = notes.get(notes.keySet().toArray()[position]).getTitle(); 
			final String[] tokens = title.split(" ");
			final String sol;
			final String imageId;
			if (MarsImagesApp.isMERMission()) {
				if (tokens.length < 3) {
					Log.w(TAG, "note title has bad format: "+title);
					return view;
				}
				sol = tokens[1];
				imageId = tokens[2];
			} else if (MarsImagesApp.getMission().equals(MarsImagesApp.CURIOSITY_MISSION)) {
				if (tokens.length < 3) {
						Log.w(TAG, "note title has bad format: "+title);
						return view;
				}
				sol = tokens[1];
				if (tokens.length == 4)
					imageId = tokens[3];
				else
					imageId = tokens[2];
			} else { 
				Log.w(TAG, "unknown mission"); return view; 
			}

			TextView firstLine = (TextView) view.findViewById(R.id.firstLine);
			TextView secondLine = (TextView) view.findViewById(R.id.secondLine);
			String lines[] = (imageId.equals("Course")) ? ImageIDUtils.getCoursePlotListItemLines(title)
					: ImageIDUtils.getListItemLines(sol, imageId);
			if (lines.length == 2) {
				firstLine.setText(lines[0]);
				secondLine.setText(lines[1]);
			} else {
				Log.w(TAG, "Unable to format image list item text from title: "+title);
			}
			final ImageView thumbnailView = (android.widget.ImageView) view.findViewById(R.id.thumbnailIcon);
			thumbnailView.setAdjustViewBounds(true);
			thumbnailView.setMaxWidth(64);
			thumbnailView.setMaxHeight(64);
			if (thumbnailView.getDrawable() == null) {
				Bitmap cachedImage = cache.get(title);
				if (cachedImage != null) {
					thumbnailView.setImageBitmap(cachedImage);
				}
				else {
					
					/**
					 * It's possible that fetching the thumbnail image over HTTP
					 * will fail due to network unavailability, etc. That's ok,
					 * thumbnail image loading is low priority.
					 */
					new Thread("thumbnail loader") {
						public void run() {
							byte[] jpegBytes = MarsImagesApp.getThumbnailImage(sol, imageId);
							if (jpegBytes != null && jpegBytes.length > 0) {
								try {
								final Bitmap bitmap = BitmapFactory.decodeStream(new ByteArrayInputStream(jpegBytes));
								cache.put(title, bitmap);
								Activity activity = (Activity)NoteListAdapter.this.context;
								activity.runOnUiThread(new Runnable() {
									public void run() {
										thumbnailView.setImageBitmap(bitmap);
									}
								});
								} catch (OutOfMemoryError e) {
									//don't need to inform the user of this: thumbnails are optional
									Log.w(TAG, "Unable to display thumbnail: insufficient memory", e);
								}
							}
						}
					}.start();
				}
			}
		}
		return view;
	}

	public void add(Note note) {
		if (notes != null) {
			notes.put(note.getTitle(), note);
			noteList.add(note);
		}
	}

	public void addAll(Collection<Note> notes) {
		if (notes != null) {
			for (Note note : notes) {
				this.notes.put(note.getTitle(), note);
				noteList.add(note);
			}
		}
	}

	public void clear() {
		notes.clear();
		noteList.clear();
	}

	private boolean validNoteIndex(int position) {
		return position >= 0 && position < notes.keySet().size();
	}
}
