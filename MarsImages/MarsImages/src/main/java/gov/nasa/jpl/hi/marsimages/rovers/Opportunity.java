package gov.nasa.jpl.hi.marsimages.rovers;

import com.evernote.edam.type.Note;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by mpowell on 5/3/14.
 */
public class Opportunity extends Rover.MER {

    @Override
    public String getUser() {
        return "opportunitymars";
    }

    @Override
    public Date getEpoch() {
        try {
            return new SimpleDateFormat(EPOCH_FORMAT).parse("2004012415:08:59GMT");
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }
}
