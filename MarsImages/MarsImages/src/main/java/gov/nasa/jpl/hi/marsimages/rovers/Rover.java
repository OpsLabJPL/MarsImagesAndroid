package gov.nasa.jpl.hi.marsimages.rovers;

import com.evernote.edam.type.Note;

/**
 * Created by mpowell on 4/12/14.
 */
public abstract class Rover {

    public final static String CURIOSITY = "curiosity";
    public final static String OPPORTUNITY = "opportunity";
    public final static String SPIRIT = "spirit";

    public abstract String getUser();

    public abstract String getSortableImageFilename(String sourceURL);

    public abstract String getLabelText(Note note);

    public abstract String getSectionText(Note note);

    public abstract int getSol(Note note);

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

        @Override
        public String getLabelText(Note note) {
            return "MER Image"; //TODO
        }

        @Override
        public String getSectionText(Note note) {
            return note.getTitle().substring(0, 9);
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
    }
}
