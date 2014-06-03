package gov.nasa.jpl.hi.marsimages.rovers;

import android.util.Log;

import com.evernote.edam.type.Note;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by mpowell on 5/3/14.
 */
public class Curiosity extends Rover {

    enum TitleState {
        START,
        SOL_NUMBER,
        IMAGESET_ID,
        INSTRUMENT_NAME,
        MARS_LOCAL_TIME,
        ROVER_MOTION_COUNTER
    }

    public String getUser() {
        return "mslmars";
    }

    @Override
    public String getSortableImageFilename(String sourceURL) {
        String tokens[] = sourceURL.split("/");
        return tokens[tokens.length-1];
    }

    @Override
    public String getLabelText(Note note) {
        Title mslTitle = tokenize(note.getTitle());
        return mslTitle.instrumentName;
    }

    @Override
    public String getDetailText(Note note) {
        String marstime = tokenize(note.getTitle()).marsLocalTime;
        return (marstime != null) ? marstime+" LST" : "";
    }

    @Override
    public String getCaptionText(Note note) {
        Title title = tokenize(note.getTitle());
        return String.format("%s image taken on Sol %d.", title.instrumentName, title.sol);
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

    @Override
    public Date getEpoch() {
        try {
            return new SimpleDateFormat(EPOCH_FORMAT).parse("2012080606:30:00GMT");
        } catch (ParseException e) {
            Log.e("epoch date error", e.toString());
        }
        return null;
    }

    public Title tokenize(String noteTitle) {
        Title msl = new Title();
        String[] tokens = noteTitle.split(" ");
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
                    msl.sol = Integer.parseInt(word);
                    state = TitleState.IMAGESET_ID;
                    break;
                case IMAGESET_ID:
                    msl.imageSetID = word;
                    state = TitleState.INSTRUMENT_NAME;
                    break;
                case INSTRUMENT_NAME:
                    if (msl.instrumentName == null) {
                        msl.instrumentName = word;
                    } else {
                        msl.instrumentName += " "+word;
                    }
                    break;
                case MARS_LOCAL_TIME:
                    msl.marsLocalTime = word;
                    break;
                case ROVER_MOTION_COUNTER:
                    indices = word.split("-");
                    msl.siteIndex = Integer.parseInt(indices[0]);
                    msl.driveIndex = Integer.parseInt(indices[1]);
                    break;
                default:
                    Log.w("MSL Title", "Unexpected state in parsing image title: "+ state);
                    break;
            }
        }
        return msl;
    }

}
