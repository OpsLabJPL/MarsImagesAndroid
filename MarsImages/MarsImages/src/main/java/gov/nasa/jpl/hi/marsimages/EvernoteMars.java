package gov.nasa.jpl.hi.marsimages;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.evernote.edam.error.EDAMNotFoundException;
import com.evernote.edam.error.EDAMSystemException;
import com.evernote.edam.error.EDAMUserException;
import com.evernote.edam.notestore.NoteFilter;
import com.evernote.edam.notestore.NoteList;
import com.evernote.edam.notestore.NoteStore;
import com.evernote.edam.type.Note;
import com.evernote.edam.type.NoteSortOrder;
import com.evernote.edam.type.Resource;
import com.evernote.edam.userstore.PublicUserInfo;
import com.evernote.edam.userstore.UserStore;
import com.evernote.thrift.TException;
import com.evernote.thrift.protocol.TBinaryProtocol;
import com.evernote.thrift.transport.THttpClient;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import gov.nasa.jpl.hi.marsimages.rovers.Rover;

import static gov.nasa.jpl.hi.marsimages.MarsImagesApp.MARS_IMAGES;

/**
 * Created by mpowell on 4/12/14.
 */
public class EvernoteMars {

    public static final EvernoteMars EVERNOTE = new EvernoteMars();

    private final WifiStateReceiver mWifiStateReceiver = new WifiStateReceiver();

    private static final int TIMEOUT = 15000;

    private static final String BEGIN_NOTE_LOADING = "beginNoteLoading";
    public static final String END_NOTE_LOADING = "endNoteLoading";
    public static final String NUM_NOTES_RETURNED = "numNotesReturned";

    private static final String OPPY_NOTEBOOK_ID = "a7271bf8-0b06-495a-bb48-7c0c7af29f70";
    private static final String MSL_NOTEBOOK_ID = "0296f732-694d-4ccd-9f5b-5983dc98b9e0";
    private static final String SPIRIT_NOTEBOOK_ID = "f1a72415-56e7-4244-8e12-def9be9c512b";

    public static final int NOTE_PAGE_SIZE = 15;

    private NoteStore.Client noteStore;
    private UserStore.Client userStore;
    private PublicUserInfo userInfo;
    private String uriPrefix;

    private static final List<Note> notesArray = Lists.newArrayList();

    private static final Map<String, String> notebookIDs = Maps.newHashMap();

    private static String searchWords = null;

    static {
        notebookIDs.put(Rover.CURIOSITY, MSL_NOTEBOOK_ID);
        notebookIDs.put(Rover.OPPORTUNITY, OPPY_NOTEBOOK_ID);
        notebookIDs.put(Rover.SPIRIT, SPIRIT_NOTEBOOK_ID);
    }

    public boolean hasNotesRemaining = true;

    private EvernoteMars() {
        IntentFilter mIntentFilter = new IntentFilter(MarsImagesApp.MISSION_CHANGED);
        BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(MarsImagesApp.MISSION_CHANGED)) {
                    searchWords = null;
                    EvernoteMars.reloadNotes(context, true); //reset connection for new mission notebook
                }
            }
        };
        LocalBroadcastManager.getInstance(MARS_IMAGES.getApplicationContext()).registerReceiver(mMessageReceiver,
                mIntentFilter);
    }

    public void loadMoreImages(Context context) {
        loadMoreImages(context, false);
    }

    public void loadMoreImages(Context context, boolean clearFirst) {
        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        if (!isConnected) {
            IntentFilter intentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
            intentFilter.setPriority(100);
            LocalBroadcastManager.getInstance(MARS_IMAGES.getApplicationContext()).registerReceiver(mWifiStateReceiver, intentFilter);
            Toast.makeText(context, "Unable to connect to the network.", Toast.LENGTH_SHORT).show();
            return;
        }
        EVERNOTE.loadMoreNotes(context, clearFirst);
    }

    public int getNotesCount() {
        return notesArray.size();
    }

    public Note getNote(int noteIndex) {
        if (noteIndex >= 0 && noteIndex < getNotesCount())
            return notesArray.get(noteIndex);
        else
            return null;
    }

    public List<Note> getNotes() { return notesArray; }

    public void loadMoreNotes(Context context, boolean clearNotes) {
        new NoteLoaderTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, context, getNotesCount(), clearNotes);
    }

    public int[] getNearestRMC() {
        int user_site = 0;
        int user_drive = 0;

        //find the RMC of the newest image/note
        for (Note note : notesArray) {
            String title = note.getTitle();
            if (!title.contains("RMC")) {
                continue;
            }
            String rmcString = title.substring(title.length()-13);
            String siteString = rmcString.substring(0,6);
            String driveString = rmcString.substring(7,13);
            user_site = Integer.parseInt(siteString);
            user_drive = Integer.parseInt(driveString);
            break;
        }

        //find the RMC in locations closest to the RMC of the newest image
        List<int[]> locations = MARS_IMAGES.getLocations();
        int[] lastLocation = locations.get(locations.size()-1);
        int siteIndex = lastLocation[0];
        int driveIndex = lastLocation[1];

        if (user_site != 0 || user_drive != 0) {
            for (int i = locations.size()-1; i >=0; i--) {
                int[] location = locations.get(i);
                int aSiteIndex = location[0];
                int aDriveIndex = location[1];
                if (aSiteIndex*100000+aDriveIndex < user_site*100000+user_drive)
                    break;
                siteIndex = aSiteIndex;
                driveIndex = aDriveIndex;
            }
        }

        return new int[] { siteIndex, driveIndex };
    }


    private void connect() throws TException, EDAMSystemException, EDAMUserException, EDAMNotFoundException {
        if (noteStore == null) {
            Stopwatch watch = Stopwatch.createStarted();
            String userStoreUrl = "https://www.evernote.com/edam/user";
            THttpClient userStoreHttpClient = new THttpClient(userStoreUrl);
            TBinaryProtocol userStoreProtocol = new TBinaryProtocol(userStoreHttpClient);
            userStore = new UserStore.Client(userStoreProtocol, userStoreProtocol);
            String user = MARS_IMAGES.getMission().getUser();
            userInfo = userStore.getPublicUserInfo(user);
            uriPrefix = userInfo.getWebApiUrlPrefix();
            String noteStoreUrl = userInfo.getNoteStoreUrl();
            THttpClient noteStoreHttpClient = new THttpClient(noteStoreUrl);
            noteStoreHttpClient.setConnectTimeout(TIMEOUT);
            TBinaryProtocol noteStoreProtocol = new TBinaryProtocol(noteStoreHttpClient);
            noteStore = new NoteStore.Client(noteStoreProtocol, noteStoreProtocol);
            Log.d("evernote connect", "Time to connect to Evernote: " + watch.elapsed(TimeUnit.MILLISECONDS) + " ms");
        }
    }

    public String getNoteUrl(int position) {
        String imageUrl = null;
        if (position >= 0 && position < notesArray.size()) {
            Note note = notesArray.get(position);
            imageUrl = getUri(note.getResources().get(0));
        }
        return imageUrl;
    }

    public String getUri(Resource resource) {
        return EVERNOTE.getUriPrefix() + "res/" + resource.getGuid();
    }

    String getUriPrefix() {
        return uriPrefix;
    }

    public String getThumbnailURL(int position) {
        String thumbnailUrl = null;
        if (position >= 0 && position < notesArray.size()) {
            Note note = notesArray.get(position);
            if (note.getResources().isEmpty())
                return null;
            String guid = note.getResources().get(0).getGuid();
            thumbnailUrl = getUriPrefix() + "thm/res/" + guid + "?size=" + gov.nasa.jpl.hi.marsimages.ui.ImageListFragment.THUMBNAIL_IMAGE_WIDTH;
        }
        return thumbnailUrl;
    }

    public class NoteLoaderTask extends AsyncTask<Object, Void, String> {

        private Context context;

        @Override
        protected String doInBackground(Object... params) {
            if (!(params[0] instanceof Context) ||
                    !(params[1] instanceof Integer) ||
                    !(params[2] instanceof Boolean)) {
                Log.e(getClass().toString(), "Unexpected input parameters");
            }
            context = (Context) params[0];
            Integer startIndex = (Integer) params[1];
            final Boolean clearNotes = (Boolean) params[2];

            if (clearNotes) {
                notesArray.clear();
                startIndex = 0;
                hasNotesRemaining = true;
                Intent intent = new Intent(MarsImagesApp.NOTES_CLEARED);
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            }

            if (!hasNotesRemaining) {
                Intent intent = new Intent(END_NOTE_LOADING);
                intent.putExtra(NUM_NOTES_RETURNED, 0);
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                return null;
            }

            ConnectivityManager cm =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            boolean isConnected = activeNetwork != null &&
                    activeNetwork.isConnectedOrConnecting();
            if (!isConnected) {
                hasNotesRemaining = false;
                Intent intent = new Intent(END_NOTE_LOADING);
                intent.putExtra(NUM_NOTES_RETURNED, 0);
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                return "Unable to connect to the network.";
            }

            int notesReturned = 0;

            try {
                if (noteStore == null || userStore == null)
                    connect();
            } catch (Exception e) {
                Log.w("service error", "Error connecting to Evernote: " + e);
                suspendEvernoteQueries();
                Intent intent = new Intent(END_NOTE_LOADING);
                intent.putExtra(NUM_NOTES_RETURNED, 0);
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                return "The Mars image service is currently unavailable. Please try again later.";
            }

            if (notesArray.size() > startIndex) {
                return null;
            }

            try {
                Intent intent = new Intent(BEGIN_NOTE_LOADING);
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

                NoteFilter filter = new NoteFilter();
                filter.setNotebookGuid(notebookIDs.get(MARS_IMAGES.getMissionName()));
                filter.setOrder(NoteSortOrder.TITLE.getValue());
                filter.setAscending(false);
                if (searchWords != null && !searchWords.isEmpty()) {
                    filter.setWords(formatSearch(searchWords));
                }
                NoteList notelist = noteStore.findNotes(null, filter, startIndex, NOTE_PAGE_SIZE);
                notesReturned = notelist.getNotes().size();
                hasNotesRemaining = notelist.getTotalNotes() - (startIndex + notesReturned) > 0;
                for (Note note : notelist.getNotes()) {
                    Note orderedNote = reorderResources(note);
                    notesArray.add(orderedNote);
                }
            } catch (Exception e) {
                Log.w("service error", "Error querying Evernote: " + e);
                suspendEvernoteQueries();
                Intent intent = new Intent(END_NOTE_LOADING);
                intent.putExtra(NUM_NOTES_RETURNED, notesReturned);
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                return "The Mars image service is currently unavailable. Please try again later.";
            }

            Intent intent = new Intent(END_NOTE_LOADING);
            intent.putExtra(NUM_NOTES_RETURNED, notesReturned);
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            return null;
        }

        @Override
        protected void onPostExecute(String message) {
            if (message != null && context instanceof Activity) {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void suspendEvernoteQueries() {
        hasNotesRemaining = false;
        new Thread() {
            @Override
            public void run() {
                try {
                    sleep(30 * 1000);
                } catch (InterruptedException e) {
                    //no need to handle
                } finally {
                    hasNotesRemaining = true;
                }
            }
        }.start();
    }

    private Note reorderResources(Note note) {
        List<String> resourceFilenames = Lists.newArrayList();
        Map<String, Resource> resourcesByFile = Maps.newHashMap();
        for (Resource resource : note.getResources()) {
            Rover mission = MARS_IMAGES.getMission();
            String filename = mission.getSortableImageFilename(resource.getAttributes().getSourceURL());
            resourceFilenames.add(filename);
            resourcesByFile.put(filename, resource);
        }
        Collections.sort(resourceFilenames);
        List<Resource> sortedResources = Lists.newArrayList();
        for (String resourceFilename : resourceFilenames) {
            sortedResources.add(resourcesByFile.get(resourceFilename));
        }
        note.setResources(sortedResources);
        return note;
    }

    public static void reloadNotes(Context context) {
        reloadNotes(context, false);
    }

    public static void reloadNotes(Context context, boolean resetConnection) {
        EVERNOTE.hasNotesRemaining = true;
        if (resetConnection) {
            EVERNOTE.noteStore = null;
            EVERNOTE.userStore = null;
            EVERNOTE.userInfo = null;
            EVERNOTE.uriPrefix = null;
        }
        EVERNOTE.loadMoreNotes(context, true);
    }

    public static void setSearchWords(String searchWords, Context context) {
        EvernoteMars.searchWords = searchWords;
        EvernoteMars.reloadNotes(context);
    }

    private String formatSearch(String text) {
        String[] words = text.split("\\s+");
        StringBuilder formattedText = new StringBuilder();
        for (String w : words) {
            String word = w;
            int value = 0;
            try {
                value = Integer.parseInt(word);
            } catch (NumberFormatException e) {
                Log.w("search words", "Expected an integer value instead of "+word);
            }
            if (value > 0 && !word.endsWith("*")) {
                word = String.format("\"Sol %05d\"", value);
            }

            if (formattedText.length() > 0) {
                formattedText.append(" ");
            }

            formattedText.append(String.format("intitle:%s", word));
        }
        Log.d("search words", "formatted text: " + formattedText);
        return formattedText.toString();
    }

    public AlertDialog getAlertDialog(Context context, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(message)
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
        return builder.create();
    }

    public static class WifiStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager cm =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
            if (isConnected) {
                Log.d("wifi state connected", "Wifi reconnected.");
                LocalBroadcastManager.getInstance(context).unregisterReceiver(this);
                EVERNOTE.hasNotesRemaining = true;
                EVERNOTE.loadMoreNotes(context, false);
            }
        }
    }
}




