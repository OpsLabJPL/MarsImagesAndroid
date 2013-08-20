package gov.nasa.jpl.hi.marsimages.test;

import gov.nasa.jpl.hi.marsimages.NoteListAdapter;

import java.util.Collection;

import junit.framework.TestCase;

import com.evernote.edam.type.Note;

public class NoteListAdapterTest extends TestCase {

	private Note note0;
	private Note note1;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		note0 = new Note();
		note0.setTitle("note0");
		note1 = new Note();
		note1.setTitle("note1");
	}
	
	public void testAdapter() {
		NoteListAdapter noteListAdapter = new NoteListAdapter(null);
		Collection<Note> noteList = noteListAdapter.getNoteList();
		assertFalse(noteList.contains(note1));
		assertFalse(noteList.contains(note0));
		assertEquals(0, noteList.size());
		
		noteListAdapter.add(note0);
		noteListAdapter.add(note1);
		
		assertEquals(2, noteListAdapter.getCount());
		assertEquals(note0, noteListAdapter.getItem(0));
		assertEquals(note1, noteListAdapter.getItem(1));
		
		noteList = noteListAdapter.getNoteList();
		assertTrue(noteList.contains(note1));
		assertTrue(noteList.contains(note0));
		assertEquals(2, noteList.size());
	}
	
}
