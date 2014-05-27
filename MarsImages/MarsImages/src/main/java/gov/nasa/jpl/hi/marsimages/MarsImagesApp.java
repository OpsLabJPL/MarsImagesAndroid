package gov.nasa.jpl.hi.marsimages;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.google.common.collect.Maps;

import java.util.Map;

import gov.nasa.jpl.hi.marsimages.rovers.Curiosity;
import gov.nasa.jpl.hi.marsimages.rovers.Opportunity;
import gov.nasa.jpl.hi.marsimages.rovers.Rover;
import gov.nasa.jpl.hi.marsimages.rovers.Spirit;

/**
 * Created by mpowell on 5/21/14.
 */
public class MarsImagesApp extends Application {

    public static MarsImagesApp MARS_IMAGES;
    public static final String MISSION_CHANGED = "missionChanged";

    private String missionName = Rover.CURIOSITY;
    private Map<String, Rover> missions = Maps.newHashMap();

    public MarsImagesApp() {
        MARS_IMAGES = this;
        missions.put(Rover.CURIOSITY, new Curiosity());
        missions.put(Rover.OPPORTUNITY, new Opportunity());
        missions.put(Rover.SPIRIT, new Spirit());
    }

    public Rover getMission() {
        return missions.get(missionName);
    }

    public String getMissionName() {
        return missionName;
    }

    public void setMission(String newMissionName, Context context) {
        String newMission = newMissionName.toLowerCase().trim();
        if (!this.missionName.equals(newMission)) {
            this.missionName = newMission;
            Intent intent = new Intent(MISSION_CHANGED);
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        }
    }
}

