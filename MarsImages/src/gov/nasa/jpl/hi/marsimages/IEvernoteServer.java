package gov.nasa.jpl.hi.marsimages;

import java.util.List;

import com.evernote.edam.type.Note;

/**
 * For mocking the server in test 
 */
public interface IEvernoteServer {
	byte[] readImageResourceFromNote(Note note);

	List<Note> getImageNotes(int offset, int noteBatchSize);
	
}
