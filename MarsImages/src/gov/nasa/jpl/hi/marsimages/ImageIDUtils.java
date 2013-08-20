package gov.nasa.jpl.hi.marsimages;

import static gov.nasa.jpl.hi.marsimages.MarsImagesApp.TAG;

import java.text.DateFormat;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;

import android.util.Log;

public class ImageIDUtils {

	public static final String DOWNSAMPLED = "Downsampled";
	public static final String SUBFRAMED = "Subframed";
	public static final String SUBFRAMED_AND_DOWNSAMPLED = "Subframed+Downsampled";
	public static final String FULL_FRAME = "Full Frame";
	public static final String FALSE_COLOR = "False Color";
	public static final String MI = "mi";
	public static final String PCAM = "pcam";
	public static final String RCAM = "rcam";
	public static final String FCAM = "fcam";
	public static final String NCAM = "ncam";
	public static final String MCAM = "mcam";
	public static final String MHLI = "mhli";
	public static final String MRDI = "mrdi";
	public static final String RIGHT = "Right";
	public static final String LEFT = "Left";
	public static final String MICROSCOPIC_IMAGER = "Microscopic Imager";
	public static final String PANCAM = "Pancam";
	public static final String NAVCAM = "Navcam";
	public static final String REAR_HAZCAM = "Rear Hazcam";
	public static final String FRONT_HAZCAM = "Front Hazcam";
	public static final String MASTCAM_LEFT = "Mastcam Left 34";
	public static final String MASTCAM_RIGHT = "Mastcam Right 100";
	public static final String MAHLI = "Mars Hand Lens Imager";
	public static final String MARDI = "Mars Descent Imager";

	
	private static DateFormat dateFormatter = DateFormat.getDateInstance();
	
	/**
	 * Return full name of an instrument given its one character id
	 * 
	 * @param imageId
	 *            the instrument id (only the first character is important)
	 * @return full instrument name or empty string if unknown
	 */
	public static String getPrettyInstrumentName(String imageId) {
		if (MarsImagesApp.isMERMission()) {
			if (isColorPancamFromCornellEdu(imageId)) {
				return PANCAM;
			}
			//try MIPL style image ID
			switch (imageId.charAt(1)) {
			case 'F':
				return FRONT_HAZCAM;
			case 'R':
				return REAR_HAZCAM;
			case 'N':
				return NAVCAM;
			case 'P':
				return PANCAM;
			case 'M':
				return MICROSCOPIC_IMAGER;
			}
			//fall back to unknown
			Log.w(TAG, "Unknown instrument for image "+imageId);
			return "";
		}
		else if (MarsImagesApp.getMission().equals(MarsImagesApp.CURIOSITY_MISSION)) {
			char char0 = imageId.charAt(0);
			if (Character.isDigit(char0)) { //MSSS
				switch (imageId.charAt(5)) {
				case 'L':
					return MASTCAM_LEFT;
				case 'R':
					return MASTCAM_RIGHT;
				case 'H':
					return MAHLI;
				case 'D':
					return MARDI;
				}
			} else { //MIPL
				switch (char0) {
				case 'F':
					return FRONT_HAZCAM;
				case 'R':
					return REAR_HAZCAM;
				case 'N':
					return NAVCAM;
				}
			}
		}
		Log.w(TAG, "ImageID too short: "+imageId);
		return "";

	}

	/**
	 * Return LEFT or RIGHT as appropriate for an imageID
	 * 
	 * @param imageId
	 * @return LEFT or RIGHT or empty string if unknown.
	 */
	public static String getEye(String imageId) {
		if (MarsImagesApp.isMERMission()) {
			if (imageId.charAt(1) == 'M') { //Microscopic Imager, eye is irrelevant
				return "";
			}
			if (isColorPancamFromCornellEdu(imageId)) {
				return LEFT;
			}
			switch (imageId.charAt(23)) {
			case 'L':
				return LEFT;
			case 'R':
				return RIGHT;
			}
			Log.w(TAG, "Unknown eye for image "+imageId);
			return "";
		} else if (MarsImagesApp.getMission().equals(MarsImagesApp.CURIOSITY_MISSION)) {
			if (Character.isDigit(imageId.charAt(0))) //MSSS
				return "";
			switch (imageId.charAt(1)) { //MIPL
			case 'L':
				return LEFT;
			case 'R':
				return RIGHT;
			}
		}
		Log.w(TAG, "ImageID too short: "+imageId);
		return "";

	}

	/**
	 * Return the sample type description for an imageID
	 * 
	 * @param imageId
	 * @return FULL_FRAME, SUBFRAMED, DOWNSAMPLED, or empty string if unknown.
	 */
	public static String getSampleType(String imageId) {
		if (MarsImagesApp.isMERMission()) {
			if (isColorPancamFromCornellEdu(imageId))
				return FALSE_COLOR;
			//MIPL-style image id
			char sampleType = imageId.charAt(12);
			switch (sampleType) {
			case 'F':
				return FULL_FRAME;
			case 'S':
				return SUBFRAMED;
			case 'D':
				return DOWNSAMPLED;
			}
			Log.w(TAG, "Unknown sample type for image "+imageId);
			return "";
		} else if (MarsImagesApp.getMission().equals(MarsImagesApp.CURIOSITY_MISSION)) {
			if (Character.isDigit(imageId.charAt(0))) //MSSS
				return "";
			//MIPL
			char sampleType = imageId.charAt(17);
			switch (sampleType) {
			case 'F':
				return FULL_FRAME;
			case 'S':
				return SUBFRAMED;
			case 'D':
				return DOWNSAMPLED;
			case 'M':
				return SUBFRAMED_AND_DOWNSAMPLED;
			}
		}
		Log.w(TAG, "ImageID too short: "+imageId);
		return "";
	}

	/**
	 * Return the thumbnail imageID for an imageID.
	 * 
	 * @param imageId
	 * @return the corresponding thumbnail imageID, substituting "TH" in the
	 *         product type
	 */
	public static String getThumbnailId(String imageId) {
		if (MarsImagesApp.isMERMission()) { 
			if (isColorPancamFromCornellEdu(imageId)) {
				return imageId+"_thumb";
			}
			//MIPL-style image id
			StringBuffer id = new StringBuffer(imageId);
			id.replace(12, 14, "TH");
			return id.toString();
		} else if (MarsImagesApp.getMission().equals(MarsImagesApp.CURIOSITY_MISSION)) {
			if (Character.isDigit(imageId.charAt(0))) { //MSSS
				return imageId+"-thm";
			}
			else { //MIPL
				StringBuffer id = new StringBuffer(imageId);
				id.replace(17, 18, "T");
				return id.toString();
			}
		}
		Log.w(TAG, "Mission not recognized for image: "+imageId);
		return "";
	}

	/**
	 * Return the instrument directory containing an imageID.
	 * 
	 * @param imageId
	 * @return the instrument directory name containing the image file
	 */
	public static String getInstrumentDir(String imageId) {
		if (MarsImagesApp.isMERMission()) {
			//color Pancam from cornell.edu
			if (isColorPancamFromCornellEdu(imageId)) {
				return "";
			}
			switch (imageId.charAt(1)) {
			case 'N':
				return NCAM;
			case 'F':
				return FCAM;
			case 'R':
				return RCAM;
			case 'P':
				return PCAM;
			case 'M':
				return MI;
			}
			Log.w(TAG, "Unknown instrument for image "+imageId);
			return "unknown";
		} else if (MarsImagesApp.getMission().equals(MarsImagesApp.CURIOSITY_MISSION)) {
			if (Character.isDigit(imageId.charAt(0))) { //MSSS
				switch (imageId.charAt(5)) {
				case 'L':
				case 'R':
					return MCAM;
				case 'D':
					return MRDI;
				case 'H':
					return MHLI;
				}
			}
			else { //MIPL
				switch (imageId.charAt(0)) {
				case 'N':
					return NCAM;
				case 'F':
					return FCAM;
				case 'R':
					return RCAM;
				}
			}
			Log.w(TAG, "Unknown instrument for image "+imageId);
			return "unknown";
		}
		Log.w(TAG, "Mission not recognized for image: "+imageId);
		return "";
	}

	/**
	 * Return the two lines for a list view item for an imageID.
	 * 
	 * @param sol
	 *            the sol of the image
	 * @param imageId
	 *            the id of the image
	 * @return array of two strings containing the formatted description of the
	 *         image
	 */
	public static String[] getListItemLines(String sol, String imageId) {
		String prettyInstrumentName = getPrettyInstrumentName(imageId);
		String eye = getEye(imageId);

		StringBuilder firstLine = new StringBuilder()
		.append(prettyInstrumentName);

		if (!StringUtils.isEmpty(eye)) {
			firstLine.append(' ').append(eye);
		}

		if (PANCAM.equals(prettyInstrumentName) && !ImageIDUtils.isColorPancamFromCornellEdu(imageId)) {
			firstLine.append(' ').append(imageId.charAt(24));
		}

		firstLine.append(' ').append(getSampleType(imageId));

		StringBuilder secondLine = new StringBuilder()
		.append("Sol ")
		.append(sol);

	    long interval = (long)(Integer.parseInt(sol)*24*60*60*MarsImagesApp.EARTH_SECS_PER_MARS_SEC);
	    Date imageDate = new Date(interval*1000 + MarsImagesApp.getMissionEpochMillis());
	    secondLine.append("  ");
	    secondLine.append(dateFormatter.format(imageDate));
		return new String[] { firstLine.toString(), secondLine.toString() };
	}

	@SuppressWarnings("unused")
	private static String getSCLK(String imageId) {
		if (MarsImagesApp.isMERMission()) {
			//color Pancam from cornell.edu
			if (isColorPancamFromCornellEdu(imageId)) {
				return "";
			}
			return imageId.substring(2, 11);
		}
		else if (MarsImagesApp.getMission().equals(MarsImagesApp.CURIOSITY_MISSION))
			return imageId.substring(4, 13);
		Log.w(TAG, "Unknown mission");
		return "";
	}

	@SuppressWarnings("unused")
	private static String getSeqID(String imageId) {
		if (MarsImagesApp.isMERMission()) {
			if (isColorPancamFromCornellEdu(imageId)) {
				return imageId.substring(0,5);
			}
			return imageId.substring(18, 23);
		}
		else if (MarsImagesApp.getMission().equals(MarsImagesApp.CURIOSITY_MISSION)) {
			return imageId.substring(25, 34);
		}
		Log.w(TAG, "Unknown mission");
		return "";
	}

	static boolean isColorPancamFromCornellEdu(String imageId) {
		return imageId.charAt(0) == 'P';
	}

	/**
	 * Get the imageID of the companion stereo image for a given image
	 * 
	 * @param title
	 *            note title of the image to match i.e. Sol 2000
	 *            2F123456789EFF0000P1234L0M1
	 * @return the companion image note title to the image (right eye image for
	 *         a left eye image, or vice versa.) Empty string if unknown.
	 */
	public static String getCompanionImageTitle(String title) {
		StringBuffer otherTitle = new StringBuffer(title);
		int eyePos = 0;
		//TODO early out here for all MI images. They will not anaglyph.
		if (MarsImagesApp.isMERMission()) {
			if (isColorPancamFromCornellEdu(title))
				return "";
			eyePos = otherTitle.length()-4;
		}
		else if (MarsImagesApp.getMission().equals(MarsImagesApp.CURIOSITY_MISSION))
			//TODO early out here for other than Hazcam and Navcam. MASTCAM/MAHLI will not anaglyph.
			eyePos = 20; //20th character in the entire title
		else {
			Log.w(TAG, "Unknown mission");
			return "";
		}
		
		char eyeChar = otherTitle.charAt(eyePos);
		switch (eyeChar) {
		case 'L':
			return otherTitle.replace(eyePos, eyePos+1, "R").toString();
		case 'R':
			return otherTitle.replace(eyePos, eyePos+1, "L").toString();
		default:
			Log.e(MarsImagesApp.TAG, "Unexpected eye character in image note title: "+eyeChar);
		}
		return "";
	}

	public static String[] getCoursePlotListItemLines(String title) {
		final String[] tokens = title.split(" ");
		String line1 = "Drive for " +tokens[4]+ " meters";
		String line2 = "Sol " + tokens[1];
		return new String[] { line1, line2 };
	}

}
