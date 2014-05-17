package gov.nasa.jpl.hi.marsimages.rovers;

import gov.nasa.jpl.hi.marsimages.rovers.Rover;

/**
 * Created by mpowell on 5/3/14.
 */
public class Curiosity extends Rover {

    public String getUser() {
        return "mslmars";
    }

    @Override
    public String getSortableImageFilename(String sourceURL) {
        String tokens[] = sourceURL.split("/");
        String filename = tokens[tokens.length-1];
        return filename;
    }
}
