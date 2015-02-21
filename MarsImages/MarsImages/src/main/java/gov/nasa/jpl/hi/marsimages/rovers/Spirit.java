package gov.nasa.jpl.hi.marsimages.rovers;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

/**
 * Created by mpowell on 5/3/14.
 */
public class Spirit extends Rover.MER {

    public static final float SPIRIT_WEST_LONGITUDE                   = 184.702f;
    public static final long  SPIRIT_LANDING_SECONDS_SINCE_1970_EPOCH = 1073137591;

    public Spirit() {
        eyeIndex = 23;
        instrumentIndex = 1;
        sampleTypeIndex = 12;
        stereoInstruments.addAll(Arrays.asList("F", "R", "N", "P"));
    }

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

    @Override
    public String getURLPrefix() {
        return "http://merpublic.s3.amazonaws.com";
    }
}
