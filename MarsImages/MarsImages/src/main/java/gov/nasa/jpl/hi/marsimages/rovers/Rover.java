package gov.nasa.jpl.hi.marsimages.rovers;

/**
 * Created by mpowell on 4/12/14.
 */
public abstract class Rover {

    public final static String CURIOSITY = "curiosity";
    public final static String OPPORTUNITY = "opportunity";
    public final static String SPIRIT = "spirit";

    public abstract String getUser();

    public abstract String getSortableImageFilename(String sourceURL);

    /**
     * Created by mpowell on 5/3/14.
     */
    public abstract static class MER extends Rover {

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
    }
}
