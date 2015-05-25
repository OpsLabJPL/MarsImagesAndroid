package gov.nasa.jpl.hi.marsimages.rovers;

import android.util.Log;

import com.evernote.edam.type.Note;
import com.evernote.edam.type.Resource;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gov.nasa.jpl.hi.marsimages.models.CameraModel;
import gov.nasa.jpl.hi.marsimages.models.M;

import static gov.nasa.jpl.hi.marsimages.MarsImagesApp.TAG;

/**
 * Created by mpowell on 4/12/14.
 */
public abstract class Rover {

    public static final String CURIOSITY = "Curiosity";
    public static final String OPPORTUNITY = "Opportunity";
    public static final String SPIRIT = "Spirit";

    public static final String TAG = "Rover";

    static final String SOL = "Sol";
    static final String LTST = "LTST";
    static final String RMC = "RMC";

    private static final double EARTH_SECS_PER_MARS_SEC = 1.027491252;
    private static final DateFormat DATE_FORMAT = SimpleDateFormat.getDateInstance();
    static final String EPOCH_FORMAT = "yyyyMMddhh:mm:sszzz";

    int instrumentIndex;
    int eyeIndex;
    int sampleTypeIndex;

    Map<Integer, List<CSVRecord>> locationsBySite = new HashMap<Integer, List<CSVRecord>>();

    final Set<String> stereoInstruments = new HashSet<>();

    public abstract String getUser();

    public abstract String getSortableImageFilename(String sourceURL);

    public abstract String getLabelText(Note note);

    public String getSectionText(Note note) {
        int sol = getSol(note);
        double interval = sol * 24 * 60 * 60 * EARTH_SECS_PER_MARS_SEC;
        GregorianCalendar calendar = new GregorianCalendar();
        Date epoch = getEpoch();
        calendar.setTime(epoch);
        calendar.add(Calendar.SECOND, (int) interval);
        String formattedDate = DATE_FORMAT.format(calendar.getTime());
        return "Sol " + sol + "  " + formattedDate;
    }

    public abstract int getSol(Note note);

    protected abstract Date getEpoch();

    public abstract String getURLPrefix();

    public abstract String getDetailText(Note note);

    public abstract String getCaptionText(Note note);

    public abstract String getImageName(Resource resource);

    public abstract double getCameraFOV(String cameraId);

    public abstract String getCameraId(String imageID);

    public abstract float getMastX();

    public abstract float getMastY();

    public abstract float getMastZ();

    public String getImageID(Resource resource) {
        String url = resource.getAttributes().getSourceURL();
        String[] tokens = url.split("[\\./]");
        int numTokens = tokens.length;
        return tokens[numTokens - 2];
    }

    public abstract String[] stereoForImages(Note note);

    public JSONArray modelJson(Note note) {
        String cameraModelJson = note.getResources().get(0).getAttributes().getCameraModel();
        if (cameraModelJson != null && cameraModelJson.length() > 0) {
            try {
                return new JSONArray(cameraModelJson);
            } catch (JSONException e) {
                Log.e(TAG, "Bad json in image note " + note.getTitle() + " " + e.getMessage());
            }
        }
        return null;
    }

    public double fieldOfView(Note image) {
        String imageID = getImageID(image.getResources().get(0));
        String cameraId = getCameraId(imageID);
        return getCameraFOV(cameraId);
    }

    public static boolean includedInMosaic(Note note) {
        String title = note.getTitle();
        return title.contains("Navcam") ||
                title.contains("Mastcam Left") ||
                title.contains("Pancam");
    }

    public List<CSVRecord> siteLocationData(int siteIndex) {
        //return cached results if we have them
        //TODO invalidate cache when needed (after a day?)
        List<CSVRecord> cachedLocations = locationsBySite.get(siteIndex);
        if (cachedLocations != null) return cachedLocations;

        List<CSVRecord> locations = new ArrayList<>();
        URL locationsURL = null;

        try {
            String sixDigitSite = String.format("%06d", siteIndex);
            locationsURL = new URL(getURLPrefix() + "/locations/site_"+sixDigitSite+".csv");
            Log.d(TAG, "location url: %@" + locationsURL);

            final Reader reader = new InputStreamReader(locationsURL.openStream());
            final CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT);
            try {
                for (final CSVRecord record : parser) {
                    locations.add(record);
                }
                locationsBySite.put(siteIndex, locations);
            } finally {
                parser.close();
                reader.close();
            }
        } catch (MalformedURLException e) {
            Log.e(TAG, "Badly formatted URL for location manifest: " + locationsURL);
        } catch (IOException e) {
            Log.e(TAG, "Error reading from location manifest URL: " + locationsURL);
        }
        return locations;
    }

    public double[] localLevelQuaternion(int siteIndex, int driveIndex) {
        List<CSVRecord> locationData = siteLocationData(siteIndex);
        double[] q = new double[4];
        for (CSVRecord location : locationData) {
            if (location.size() >= 5 && driveIndex == Integer.parseInt(location.get(0))) {
                q[0] = Double.parseDouble(location.get(1));
                q[1] = Double.parseDouble(location.get(2));
                q[2] = Double.parseDouble(location.get(3));
                q[3] = Double.parseDouble(location.get(4));
                break;
            }
        }
        return q;
    }

    public abstract int getLayer(String cameraId, String imageId);

    /**
     * Created by mpowell on 5/3/14.
     */
    public abstract static class MER extends Rover {

        public static final String COURSE = "Course";

        enum TitleState {
            START,
            SOL_NUMBER,
            IMAGESET_ID,
            INSTRUMENT_NAME,
            MARS_LOCAL_TIME,
            DISTANCE,
            YAW,
            PITCH,
            ROLL,
            TILT,
            ROVER_MOTION_COUNTER
        }

        @Override
        public String getCameraId(String imageId) {
            if (imageId.contains("Sol")) { //Color Pancam image IDs begin with "Sol"...
                return "P";
            }
            return imageId.substring(1,2);
        }

        public int getLayer(String cameraId, String imageId) {
            if (cameraId.charAt(0) == 'N') {
                return 3;
            } else if (!imageId.contains("Sol")) {
                return 2;
            }
            return 1;
        }

        @Override
        public double getCameraFOV(String cameraId) {
            char camera = cameraId.charAt(0);
            if (camera == 'N')
                return 0.78539816;
            else if (camera == 'P')
                return 0.27925268;
            Log.e(TAG, "Unexpected camera id for FOV check: "+cameraId);
            return 0;
        }

        public float getMastX() {
            return 0.456f;
        }

        public float getMastY() {
            return 0.026f;
        }

        public float getMastZ() {
            return -1.0969f;
        }

        @Override
        public String getSortableImageFilename(String sourceURL) {
            String[] tokens = sourceURL.split("/");
            String filename = tokens[tokens.length - 1];
            if (filename.startsWith("Sol"))
                return "0"; //sort Cornell Pancam images first
            else if ((filename.startsWith("1") || filename.startsWith("2")) && filename.length() == 31)
                return filename.substring(23);

            return filename;
        }

        @Override
        public String getCaptionText(Note note) {
            if (note == null) return "";
            MERTitle title = tokenize(note.getTitle());
            if (title.distance == 0.0f)
                return String.format("%s image taken on Sol %d.", title.instrumentName, title.sol);
            else
                return String.format("Drive for %.2f meters on Sol %d.", title.distance, title.sol);
        }

        @Override
        public String getLabelText(Note note) {
            if (note == null) return "";
            MERTitle merTitle = tokenize(note.getTitle());
            if (merTitle.distance == 0f)
                return merTitle.instrumentName;
            else
                return String.format("Drive for %.2f meters", merTitle.distance);
        }

        @Override
        public String getDetailText(Note note) {
            if (note == null) return "";
            String marstime = tokenize(note.getTitle()).marsLocalTime;
            return (marstime != null) ? marstime + " LST" : "";
        }

        @Override
        public int getSol(Note note) {
            if (note == null) return 0;
            String title = note.getTitle();
            String tokens[] = title.split(" ");
            if (tokens.length >= 2) {
                return Integer.parseInt(tokens[1]);
            }
            return 0;
        }

        public MERTitle tokenize(String title) {
            MERTitle mer = new MERTitle();
            String[] tokens = title.split(" ");
            TitleState state = TitleState.START;
            for (String word : tokens) {
                switch (word) {
                    case SOL:
                        state = TitleState.SOL_NUMBER;
                        continue;
                    case LTST:
                        state = TitleState.MARS_LOCAL_TIME;
                        continue;
                    case RMC:
                        state = TitleState.ROVER_MOTION_COUNTER;
                        continue;
                }
                String[] indices;
                switch (state) {
                    case START:
                        break;
                    case SOL_NUMBER:
                        mer.sol = Integer.parseInt(word);
                        state = TitleState.IMAGESET_ID;
                        break;
                    case IMAGESET_ID:
                        if (word.equals(COURSE)) {
                            mer = parseCoursePlotTitle(title, mer);
                            return mer;
                        } else {
                            mer.imageSetID = word;
                        }
                        state = TitleState.INSTRUMENT_NAME;
                        break;
                    case INSTRUMENT_NAME:
                        if (mer.instrumentName == null) {
                            mer.instrumentName = word;
                        } else {
                            mer.instrumentName += " " + word;
                        }
                        break;
                    case MARS_LOCAL_TIME:
                        mer.marsLocalTime = word;
                        break;
                    case ROVER_MOTION_COUNTER:
                        indices = word.split("-");
                        mer.siteIndex = Integer.parseInt(indices[0]);
                        mer.driveIndex = Integer.parseInt(indices[1]);
                        break;
                    default:
                        Log.w("mer title", "Unexpected state in parsing image title: " + state);
                        break;
                }
            }
            return mer;
        }

        public MERTitle parseCoursePlotTitle(String title, MERTitle mer) {
            String[] tokens = title.split(" ");
            TitleState state = TitleState.START;
            for (String word : tokens) {
                switch (word) {
                    case COURSE:
                        mer.instrumentName = "Course Plot";
                        break;
                    case "Distance":
                        state = TitleState.DISTANCE;
                        continue;
                    case "yaw":
                        state = TitleState.YAW;
                        continue;
                    case "pitch":
                        state = TitleState.PITCH;
                        continue;
                    case "roll":
                        state = TitleState.ROLL;
                        continue;
                    case "tilt":
                        state = TitleState.TILT;
                        continue;
                    case "RMC":
                        state = TitleState.ROVER_MOTION_COUNTER;
                        continue;
                }
                String[] indices;
                switch (state) {
                    case START:
                        break;
                    case DISTANCE:
                        mer.distance = Float.parseFloat(word);
                        break;
                    case YAW:
                        mer.yaw = Float.parseFloat(word);
                        break;
                    case PITCH:
                        mer.pitch = Float.parseFloat(word);
                        break;
                    case ROLL:
                        mer.roll = Float.parseFloat(word);
                        break;
                    case TILT:
                        mer.tilt = Float.parseFloat(word);
                        break;
                    case ROVER_MOTION_COUNTER:
                        indices = word.split("-");
                        mer.siteIndex = Integer.parseInt(indices[0]);
                        mer.driveIndex = Integer.parseInt(indices[1]);
                        break;
                    default:
                        Log.w("mer title", "Unexpected state in parsing course plot title: " + state);
                        break;
                }
            }
            return mer;
        }

        @Override
        public String getImageName(Resource resource) {
            String imageid = getImageID(resource);

            if (resource.getAttributes().getSourceURL().contains("False"))
                return "Color";

            String instrument = imageid.substring(instrumentIndex, instrumentIndex + 1);
            if (instrument.equals("N") || instrument.equals("F") || instrument.equals("R")) {
                String eye = imageid.substring(eyeIndex, eyeIndex + 1);
                if (eye.equals("L"))
                    return "Left";
                else
                    return "Right";
            } else if (instrument.equals("P")) {
                return imageid.substring(eyeIndex, eyeIndex + 2);
            }

            return "";
        }

        public String[] stereoForImages(Note note) {
            if (note == null || note.getResources().size() == 0)
                return new String[0];
            String imageid = getImageID(note.getResources().get(0));
            String instrument = imageid.substring(instrumentIndex, instrumentIndex + 1);
            if (!stereoInstruments.contains(instrument) && !imageid.startsWith("Sol"))
                return new String[0];

            int leftImageIndex = -1;
            int rightImageIndex = -1;
            int index = 0;
            for (Resource resource : note.getResources()) {
                imageid = getImageID(resource);
                String eye = imageid.substring(eyeIndex, eyeIndex + 1);
                if (leftImageIndex == -1 && eye.equals("L") && !imageid.startsWith("Sol"))
                    leftImageIndex = index;
                if (rightImageIndex == -1 && eye.equals("R"))
                    rightImageIndex = index;
                index += 1;
            }
            if (leftImageIndex >= 0 && rightImageIndex >= 0) {
                return new String[]{
                        note.getResources().get(leftImageIndex).getAttributes().getSourceURL(),
                        note.getResources().get(rightImageIndex).getAttributes().getSourceURL()};
            }
            return new String[0];
        }
    }
}
