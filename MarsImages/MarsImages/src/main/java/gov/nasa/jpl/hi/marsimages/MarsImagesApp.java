package gov.nasa.jpl.hi.marsimages;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.google.common.collect.Maps;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import java.util.Map;

import gov.nasa.jpl.hi.marsimages.rovers.Curiosity;
import gov.nasa.jpl.hi.marsimages.rovers.Opportunity;
import gov.nasa.jpl.hi.marsimages.rovers.Rover;
import gov.nasa.jpl.hi.marsimages.rovers.Spirit;

/**
 * Created by mpowell on 5/21/14.
 */
public class MarsImagesApp extends Application {

    public static final String IMAGE_CACHE_DIR = "images";
    public static final String NOTES_CLEARED = "notesCleared";
    public static MarsImagesApp MARS_IMAGES;
    public static final String MISSION_CHANGED = "missionChanged";
    public static final String IMAGE_SELECTED = "imageSelected";
    public static final String IMAGE_INDEX = "imageIndex";
    public static final String SELECTION_SOURCE = "selectionSource";
    public static final String VIEW_PAGER_SOURCE = "viewPagerSource";
    public static final String LIST_SOURCE = "listSource";

    private String missionName = Rover.CURIOSITY;
    private Map<String, Rover> missions = Maps.newHashMap();

    public MarsImagesApp() {
        MARS_IMAGES = this;
        missions.put(Rover.CURIOSITY, new Curiosity());
        missions.put(Rover.OPPORTUNITY, new Opportunity());
        missions.put(Rover.SPIRIT, new Spirit());
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // Create global configuration and initialize ImageLoader with this configuration
        DisplayImageOptions defaultOptions = new DisplayImageOptions.Builder()
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .build();
        ImageLoaderConfiguration config =
                new ImageLoaderConfiguration.Builder(getApplicationContext())
                        .defaultDisplayImageOptions(defaultOptions)
                        .build();
        ImageLoader.getInstance().init(config);
    }

    @Override
    public void onLowMemory() {
        ImageLoader.getInstance().clearMemoryCache();
        super.onLowMemory();
    }

    @Override
    public void onTerminate() {
        ImageLoader.getInstance().destroy();
        super.onTerminate();
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

