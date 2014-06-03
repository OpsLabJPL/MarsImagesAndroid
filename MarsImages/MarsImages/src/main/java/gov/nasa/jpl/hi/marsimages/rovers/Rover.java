package gov.nasa.jpl.hi.marsimages.rovers;

import android.util.Log;

import com.evernote.edam.type.Note;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Created by mpowell on 4/12/14.
 */
public abstract class Rover {

    public static final String CURIOSITY = "curiosity";
    public static final String OPPORTUNITY = "opportunity";
    public static final String SPIRIT = "spirit";

    public static final String SOL = "Sol";
    public static final String LTST = "LTST";
    public static final String RMC = "RMC";

    public static final double EARTH_SECS_PER_MARS_SEC = 1.027491252;
    private static final DateFormat DATE_FORMAT = SimpleDateFormat.getDateInstance();
    protected static final String EPOCH_FORMAT = "yyyyMMddhh:mm:sszzz";

    public abstract String getUser();

    public abstract String getSortableImageFilename(String sourceURL);

    public abstract String getLabelText(Note note);

    public String getSectionText(Note note) {
        int sol = getSol(note);
        double interval = sol*24*60*60*EARTH_SECS_PER_MARS_SEC;
        GregorianCalendar calendar = new GregorianCalendar();
        Date epoch = getEpoch();
        calendar.setTime(epoch);
        calendar.add(Calendar.SECOND, (int)interval);
        String formattedDate = DATE_FORMAT.format(calendar.getTime());
        return "Sol "+sol+"  "+formattedDate;
    }

    public abstract int getSol(Note note);

    public abstract Date getEpoch();

    public abstract String getDetailText(Note note);

    public abstract String getCaptionText(Note note);

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
        public String getSortableImageFilename(String sourceURL) {
            String[] tokens = sourceURL.split("/");
            String filename = tokens[tokens.length-1];
            if (filename.startsWith("Sol"))
                return "0"; //sort Cornell Pancam images first
            else if ((filename.startsWith("1") || filename.startsWith("2")) && filename.length() == 31)
                return filename.substring(23);

            return filename;
        }

        @Override
        public String getCaptionText(Note note) {
            MERTitle title = tokenize(note.getTitle());
            if (title.distance == 0.0f)
                return String.format("%s image taken on Sol %d.", title.instrumentName, title.sol);
            else
                return String.format("Drive for %.2f meters on Sol %d.", title.distance, title.sol);
        }

        @Override
        public String getLabelText(Note note) {
            MERTitle merTitle = tokenize(note.getTitle());
            if (merTitle.distance == 0f)
                return merTitle.instrumentName;
            else
                return String.format("Drive for %.2f meters", merTitle.distance);
        }

        @Override
        public String getDetailText(Note note) {
            String marstime = tokenize(note.getTitle()).marsLocalTime;
            return (marstime != null) ? marstime+" LST" : "";
        }

        @Override
        public int getSol(Note note) {
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
                if (word.equals(SOL)) {
                    state = TitleState.SOL_NUMBER;
                    continue;
                }
                else if (word.equals(LTST)) {
                    state = TitleState.MARS_LOCAL_TIME;
                    continue;
                }
                else if (word.equals(RMC)) {
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
                            mer.instrumentName += " "+word;
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
                        Log.w("mer title","Unexpected state in parsing image title: "+state);
                        break;
                }
            }
            return mer;
        }

        public MERTitle parseCoursePlotTitle(String title, MERTitle mer) {
            String[] tokens = title.split(" ");
            TitleState state = TitleState.START;
            for (String word : tokens) {
                if (word.equals(COURSE)) {
                    mer.instrumentName = "Course Plot";
                } else if (word.equals("Distance")) {
                    state = TitleState.DISTANCE;
                    continue;
                } else if (word.equals("yaw")) {
                    state = TitleState.YAW;
                    continue;
                } else if (word.equals("pitch")) {
                    state = TitleState.PITCH;
                    continue;
                } else if (word.equals("roll")) {
                    state = TitleState.ROLL;
                    continue;
                } else if (word.equals("tilt")) {
                    state = TitleState.TILT;
                    continue;
                } else if (word.equals("RMC")) {
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
                        Log.w("mer title","Unexpected state in parsing course plot title: "+state);
                        break;
                }
            }
            return mer;
        }

    }
}
