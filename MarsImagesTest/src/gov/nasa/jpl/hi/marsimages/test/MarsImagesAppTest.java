package gov.nasa.jpl.hi.marsimages.test;

import gov.nasa.jpl.hi.marsimages.ImageIDUtils;
import gov.nasa.jpl.hi.marsimages.MarsImagesApp;

import java.io.ByteArrayInputStream;
import java.util.List;

import junit.framework.TestCase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.evernote.edam.type.Note;

public class MarsImagesAppTest extends TestCase {

	private MarsImagesApp app;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		app = new MarsImagesApp();
		app.onCreate();
	}

	public void testGetImageNotes() {
		List<Note> imageNotes = app.getImageNotes(0, 0);
		assertEquals(0, imageNotes.size());
		imageNotes = app.getImageNotes(0, 10);
		assertEquals(10, imageNotes.size());
		List<Note> imageNotesSubset = app.getImageNotes(2,6);
		assertTrue(imageNotes.containsAll(imageNotesSubset));
	}

	public void testGetThumbnailImage() {
		byte[] image = MarsImagesApp.getThumbnailImage("2744", "1F371790450EFFBOJJP1205L0M1");
		assertNotNull(image);
		assertTrue(image.length > 0);
		Bitmap decodeStream = BitmapFactory.decodeStream(new ByteArrayInputStream(image));
		assertNotNull(decodeStream);
	}

	public void testGetListItemLines() {
		MarsImagesApp.setMission(MarsImagesApp.OPPORTUNITY_MISSION);
		String ids[] = new String[] {
				"1N371790719EFFBOJMP0613L0M1",
				"1F371790450EFFBOJJP1205L0M1",
				"1P371790995EFFBOJMP2411L2M1"
		};
		String expected1[] = new String[] {
				"Navcam Left Full Frame",
				"Front Hazcam Left Full Frame",
				"Pancam Left 2 Full Frame"
		};
		for (int i = 0; i < ids.length; i++) {
			String[] listItemLines = ImageIDUtils.getListItemLines("2744", ids[i]);
			assertNotNull(listItemLines);
			assertEquals(2, listItemLines.length);
			assertEquals(expected1[i], listItemLines[0]);
			assertTrue(listItemLines[1].startsWith("Sol 2744"));
		}
	}
	
	public void testGetImageResourceFromNote() {
		Note note = app.getImageNotes(0, 1).get(0);
		byte[] image = app.readImageResourceFromNote(note);
		assertNotNull(image);
		assertTrue(image.length > 0);
		Bitmap bitmap = BitmapFactory.decodeStream(new ByteArrayInputStream(image));
		assertNotNull(bitmap);
	}
	
	public void testSelectedImageBytes() {
		assertNull(app.getSelectedImageBytes());
		app.setSelectedImageBytes(new byte[] {(byte)0, (byte)-1, (byte)2});
		byte[] b = app.getSelectedImageBytes();
		assertEquals(0, b[0]);
		assertEquals(-1, b[1]);
		assertEquals(2, b[2]);
		assertEquals(3, b.length);
	}
	
	public void testGetAnaglyphImageNoteTitle() {
		MarsImagesApp.setMission(MarsImagesApp.OPPORTUNITY_MISSION);
		String rightImageTitle = "Sol 2744 1R371793952EFFBOJMP1312R0M1";
		String leftImageTitle = "Sol 2744 1R371793952EFFBOJMP1312L0M1";
		String companionImageTitle = ImageIDUtils.getCompanionImageTitle(rightImageTitle);
		assertEquals(leftImageTitle, companionImageTitle);
		companionImageTitle = ImageIDUtils.getCompanionImageTitle(leftImageTitle);
		assertEquals(rightImageTitle, companionImageTitle);
	}
}
