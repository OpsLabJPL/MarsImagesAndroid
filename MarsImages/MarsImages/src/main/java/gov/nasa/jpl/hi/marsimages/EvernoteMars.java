package gov.nasa.jpl.hi.marsimages;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

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
import com.evernote.thrift.transport.TTransportException;
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

    public static EvernoteMars EVERNOTE = new EvernoteMars();

    public static int TIMEOUT = 15;

    public static final String NOTES_LOADED = "notesLoaded";

    private NoteStore.Client noteStore;
    private UserStore.Client userStore;
    private PublicUserInfo userInfo;
    private String uriPrefix;

    private static List<Note> notesArray = Lists.newArrayList();

    private static int NOTE_PAGE_SIZE = 15;

    private static final String OPPY_NOTEBOOK_ID   = "a7271bf8-0b06-495a-bb48-7c0c7af29f70";
    private static final String MSL_NOTEBOOK_ID    = "0296f732-694d-4ccd-9f5b-5983dc98b9e0";
    private static final String SPIRIT_NOTEBOOK_ID = "f1a72415-56e7-4244-8e12-def9be9c512b";

    public static final String BEGIN_NOTE_LOADING = "beginNoteLoading";
    public static final String END_NOTE_LOADING   = "endNoteLoading";
    public static final String NUM_NOTES_RETURNED = "numNotesReturned";

    private static final Map<String, String> notebookIDs = Maps.newHashMap();

    private static String searchWords = null;

    static {
        notebookIDs.put(Rover.CURIOSITY, MSL_NOTEBOOK_ID);
        notebookIDs.put(Rover.OPPORTUNITY, OPPY_NOTEBOOK_ID);
        notebookIDs.put(Rover.SPIRIT, SPIRIT_NOTEBOOK_ID);
    }

    private EvernoteMars() {
        LocalBroadcastManager.getInstance(MARS_IMAGES.getApplicationContext()).registerReceiver(mMessageReceiver,
                new IntentFilter(MarsImagesApp.MISSION_CHANGED));
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //intent action: mission changed
            searchWords = null;
            EvernoteMars.reloadNotes(context, true); //reset connection for new mission notebook
        }
    };

    public int getNotesCount() {
        return notesArray.size();
    }

    public Note getNote(int noteIndex) {
        if (noteIndex >= 0 && noteIndex < getNotesCount())
            return notesArray.get(noteIndex);
        else
            return null;
    }

    public void loadMoreNotes(Context context) {
        loadMoreNotes(context, false);
    }

    public void loadMoreNotes(Context context, boolean clearNotes) {
        new NoteLoaderTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, context, getNotesCount(), clearNotes);
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
            uriPrefix = userStore.getPublicUserInfo(user).getWebApiUrlPrefix();
            String noteStoreUrl = userInfo.getNoteStoreUrl();
            final String agent = "Mars Images/2.0;Android";
            THttpClient noteStoreHttpClient = new THttpClient(noteStoreUrl);
            TBinaryProtocol noteStoreProtocol = new TBinaryProtocol(noteStoreHttpClient);
            noteStore = new NoteStore.Client(noteStoreProtocol, noteStoreProtocol);
            Log.d("evernote connect", "Time to connect to Evernote: "+ watch.elapsed(TimeUnit.MILLISECONDS)+" ms");
        }
    }

    public String getNoteUrl(int position) {
        String imageUrl = null;
        if (position >= 0 && position < notesArray.size()) {
            Note note = notesArray.get(position);
            imageUrl = EVERNOTE.getUriPrefix() + "res/"+note.getResources().get(0).getGuid();
        }
        return imageUrl;
    }

    public String getUriPrefix() {
       return uriPrefix;
    }

    public String getThumbnailURL(int position, int thumbnailSize) {
        String thumbnailUrl = null;
        if (position >= 0 && position < notesArray.size()) {
            Note note = notesArray.get(position);
            if (note.getResources().isEmpty())
                return null;
            String guid = note.getResources().get(0).getGuid();
            thumbnailUrl = getUriPrefix()+"thm/res/"+guid+"?size="+thumbnailSize;
        }
        return thumbnailUrl;
    }

    private class NoteLoaderTask extends AsyncTask<Object, Void, Void> {

        @Override
        protected Void doInBackground(Object... params) {
            if (!(params[0] instanceof Context) || !(params[1] instanceof Integer) || !(params[2] instanceof Boolean)) {
                Log.e(getClass().toString(), "Unexpected input parameters");
            }
            Context context = (Context)params[0];
            Integer startIndex = (Integer)params[1];
            Boolean clearNotes = (Boolean)params[2];

            if (clearNotes) {
                notesArray.clear();
                startIndex = 0;
            }

            int notesReturned = 0;

            try {
                if (noteStore == null || userStore == null)
                    connect();
            } catch (TTransportException tte) {
                //FIXME
            } catch (TException te) {
                //FIXME
            } catch (EDAMSystemException ese) {
                //FIXME
            } catch (EDAMNotFoundException enfe) {
                //FIXME
            } catch (EDAMUserException eue) {
                //FIXME
            }

            //TODO ConnectivityManager
//        if (_internetReachable.currentReachabilityStatus == NotReachable) {
//            if (!_networkAlert.visible) {
//                [_networkAlert show];
//            }
//            [MarsImageNotebook notifyNotesReturned:0];
//            return;
//        }

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

                for (Note note : notelist.getNotes()) {
                    Note orderedNote = reorderResources(note);
                    notesArray.add(orderedNote);
                }
            } catch (EDAMSystemException ese) {
                //FIXME
            } catch (EDAMNotFoundException enfe) {
                //FIXME
            } catch (EDAMUserException eue) {
                //FIXME
            } catch (TException te) {
                //FIXME
            }

            Intent intent = new Intent(END_NOTE_LOADING);
            intent.putExtra(NUM_NOTES_RETURNED, notesReturned);
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            return null;
        }
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

    private static void reloadNotes(Context context, boolean resetConnection) {
        if (resetConnection) {
            EVERNOTE.noteStore = null;
            EVERNOTE.userStore = null;
            EVERNOTE.userInfo = null;
            EVERNOTE.uriPrefix = null;
        }
        EVERNOTE.loadMoreNotes(context, true);
    }

    public static void setSearchWords(String searchWords, Context context) {
        EVERNOTE.searchWords = searchWords;
        EVERNOTE.reloadNotes(context);
    }

    private String formatSearch(String text) {
        String[] words = text.split("\\s+");
        StringBuffer formattedText = new StringBuffer();
        for (String w : words) {
            String word = w;
            int value = 0;
            try { value = Integer.parseInt(word); } catch (NumberFormatException e) {}
            if (value > 0 && !word.endsWith("*")){
                word = String.format("\"Sol %05d\"", value);
            }

            if (formattedText.length() > 0) {
                formattedText.append(" ");
            }

            formattedText.append(String.format("intitle:%s",word));
        }
        Log.d("search words", "formatted text: "+formattedText);
        return formattedText.toString();
    }

}




