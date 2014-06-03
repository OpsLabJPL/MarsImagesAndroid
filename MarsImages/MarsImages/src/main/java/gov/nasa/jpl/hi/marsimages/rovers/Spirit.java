package gov.nasa.jpl.hi.marsimages.rovers;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import gov.nasa.jpl.hi.marsimages.rovers.Rover;

/**
 * Created by mpowell on 5/3/14.
 */
public class Spirit extends Rover.MER {

    @Override
    public String getUser() {
        return "spiritmars";
    }

    @Override
    public Date getEpoch() {
        try {
            return new SimpleDateFormat(EPOCH_FORMAT).parse("2004010313:36:15GMT");
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }
}
