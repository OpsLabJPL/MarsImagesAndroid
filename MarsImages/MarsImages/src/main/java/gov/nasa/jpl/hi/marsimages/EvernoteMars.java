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
import com.evernote.edam.type.NoteSortOrder;
import com.evernote.edam.type.Resource;
import com.evernote.edam.userstore.PublicUserInfo;
import com.evernote.edam.userstore.UserStore;
import com.evernote.edam.type.Note;
import com.evernote.thrift.TException;
import com.evernote.thrift.protocol.TBinaryProtocol;
import com.evernote.thrift.transport.THttpClient;
import com.evernote.thrift.transport.TTransportException;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import gov.nasa.jpl.hi.marsimages.rovers.Curiosity;
import gov.nasa.jpl.hi.marsimages.rovers.Opportunity;
import gov.nasa.jpl.hi.marsimages.rovers.Rover;
import gov.nasa.jpl.hi.marsimages.rovers.Spirit;

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
            notesArray.clear();
            EvernoteMars.reloadNotes(context);
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
        new NoteLoaderTask().execute(context, notesArray.size());
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

    private class NoteLoaderTask extends AsyncTask<Object, Void, Void> {

        @Override
        protected Void doInBackground(Object... params) {
            if (!(params[0] instanceof Context) && (params[1] instanceof Integer)) {
                Log.e(getClass().toString(), "Unexpected input parameters");
            }
            Context context = (Context)params[0];
            Integer startIndex = (Integer)params[1];

            int notesReturned = 0;

            try {
                if (noteStore == null || userStore == null)
                    connect();

                Log.d("TAG", "Connected to evernote as " + userInfo.getUsername());
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
                if (searchWords != null && searchWords.length()>0) {
                    filter.setWords(formatSearch(searchWords));
                }
                NoteList notelist = noteStore.findNotes(null, filter, startIndex, NOTE_PAGE_SIZE);
                notesReturned = notelist.getNotes().size();

                for (Note note : notelist.getNotes()) {
                  Note orderedNote = reorderResources(note);
                  notesArray.add(orderedNote);

//                NSNumber* sol = [NSNumber numberWithInt:[self.mission sol:note]];
//                int lastSolIndex = _sols.count-1;
//                if (lastSolIndex < 0 || ![[_sols objectAtIndex:lastSolIndex] isEqualToNumber: sol])
//                [(NSMutableArray*)_sols addObject:sol];
//                NSMutableArray* notesForSol = [_notes objectForKey:sol];
//                if (!notesForSol)
//                    notesForSol = [[NSMutableArray alloc] init];
//                [notesForSol addObject:note];
//                [(NSMutableDictionary*)_notes setObject:notesForSol forKey:sol];

//                MarsPhoto* photo = [self getNotePhoto:j+startIndex withIndex:0];
//                [(NSMutableArray*)_notePhotosArray addObject:photo];

//                [(NSMutableDictionary*)_sections removeObjectForKey:sol];
//                [(NSMutableDictionary*)_sections setObject:[NSNumber numberWithInt:_sections.count] forKey:sol];
//                if (_sections.count != _sols.count) {
//                    NSLog(@"Brown alert: sections and sols counts don't match each other.");
//                }
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

//        EDAMNoteList* notelist = [[EDAMNoteList alloc] init];
//        @try {
//            [(NSMutableArray*)_notesArray addObjectsFromArray: notelist.notes];
//            for (int j = 0; j < notelist.notes.count; j++) {
//                EDAMNote* note = [notelist.notes objectAtIndex:j];
//                note = [MarsImageNotebook reorderResources:note];
//                NSNumber* sol = [NSNumber numberWithInt:[self.mission sol:note]];
//                int lastSolIndex = _sols.count-1;
//                if (lastSolIndex < 0 || ![[_sols objectAtIndex:lastSolIndex] isEqualToNumber: sol])
//                [(NSMutableArray*)_sols addObject:sol];
//                NSMutableArray* notesForSol = [_notes objectForKey:sol];
//                if (!notesForSol)
//                    notesForSol = [[NSMutableArray alloc] init];
//                [notesForSol addObject:note];
//                [(NSMutableDictionary*)_notes setObject:notesForSol forKey:sol];
//                MarsPhoto* photo = [self getNotePhoto:j+startIndex withIndex:0];
//                [(NSMutableArray*)_notePhotosArray addObject:photo];
//                [(NSMutableDictionary*)_sections removeObjectForKey:sol];
//                [(NSMutableDictionary*)_sections setObject:[NSNumber numberWithInt:_sections.count] forKey:sol];
//                if (_sections.count != _sols.count) {
//                    NSLog(@"Brown alert: sections and sols counts don't match each other.");
//                }
//            }

//            [MarsImageNotebook notifyNotesReturned:notelist.notes.count];

//        } @catch (NSException *e) {
//            NSLog(@"Exception listing notes: %@ %@", e.name, e.description);
//            [[Evernote instance] setNoteStore: nil];
//            dispatch_async(dispatch_get_main_queue(), ^{
//            if (!_serviceAlert.visible) {
//                [_serviceAlert show];
//            }
//            });
//            [MarsImageNotebook notifyNotesReturned:0];
//            return;
//        }

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

    private static void reloadNotes(Context context) {
        notesArray.clear();
        EVERNOTE.noteStore = null;
        EVERNOTE.userStore = null;
        EVERNOTE.userInfo = null;
        EVERNOTE.uriPrefix = null;
        EVERNOTE.loadMoreNotes(context);
    }

    private String formatSearch(String text) {
//        NSArray* words = [text componentsSeparatedByCharactersInSet:[NSCharacterSet whitespaceAndNewlineCharacterSet]];
//        NSMutableString* formattedText = [[NSMutableString alloc] init];
//        for (NSString* w in words) {
//            NSString* word = [NSString stringWithString:w];
//
//            if ([word intValue] > 0 && ![word hasSuffix:@"*"])
//            word = [NSString stringWithFormat:@"\"Sol %05d\"", [word intValue]];
//
//            if (formattedText.length > 0)
//            [formattedText appendString:@" "];
//
//            [formattedText appendString: [NSString stringWithFormat:@"intitle:%@", word]];
//        }
//        NSLog(@"formatted text: %@", formattedText);
//        return formattedText;
        return text; //TODO
    }

}




