package gov.nasa.jpl.hi.marsimages.rovers;

import com.evernote.edam.type.Note;

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

    @Override
    public String getSectionText(Note note) {
        return note.getTitle().substring(0,9);
    }

    @Override
    public String getLabelText(Note note) {
        return "MSL Image"; //TODO
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
