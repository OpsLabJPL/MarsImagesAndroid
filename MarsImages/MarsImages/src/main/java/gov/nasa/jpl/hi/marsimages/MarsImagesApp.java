package gov.nasa.jpl.hi.marsimages;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.common.collect.Maps;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import gov.nasa.jpl.hi.marsimages.rovers.Curiosity;
import gov.nasa.jpl.hi.marsimages.rovers.Opportunity;
import gov.nasa.jpl.hi.marsimages.rovers.Rover;
import gov.nasa.jpl.hi.marsimages.rovers.Spirit;

/**
 * Created by mpowell on 5/21/14.
 */
public class MarsImagesApp extends Application {

    public static final String NOTES_CLEARED = "notesCleared";
    public static final String MISSION_CHANGED = "missionChanged";
    public static final String IMAGE_SELECTED = "imageSelected";
    public static final String IMAGE_INDEX = "imageIndex";
    public static final String SELECTION_SOURCE = "selectionSource";
    public static final String VIEW_PAGER_SOURCE = "viewPagerSource";
    public static final String LIST_SOURCE = "listSource";
    public static final String MARS_IMAGES_PREFERENCES_KEY = "com.powellware.marsimages.MARS_IMAGES";
    public static final String MISSION_NAME_PREFERENCE = "mission_name_preference";
    private static final String CURIOSITY_MISSION_NAME = "Curiosity";
    public static final String TAG = "MarsImagesApplication";
    public static MarsImagesApp MARS_IMAGES;
    private String missionName;
    private final Map<String, Rover> missions = Maps.newHashMap();
    private long pauseTimestamp;
    private List<int[]> locations; //lazily initialized

    public MarsImagesApp() {
        MARS_IMAGES = this;
        missions.put(Rover.CURIOSITY, new Curiosity());
        missions.put(Rover.OPPORTUNITY, new Opportunity());
        missions.put(Rover.SPIRIT, new Spirit());
    }

    @Override
    public void onCreate() {
        super.onCreate();

        SharedPreferences sharedPreferences = getSharedPreferences(MARS_IMAGES_PREFERENCES_KEY, MODE_PRIVATE);
        missionName = sharedPreferences.getString(MISSION_NAME_PREFERENCE, CURIOSITY_MISSION_NAME);
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

    public void setMission(final String newMissionName, Context context) {
        if (!this.missionName.equals(newMissionName)) {
            this.missionName = newMissionName;
            Intent intent = new Intent(MISSION_CHANGED);
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            locations = null;
        }
    }

    public void setPauseTimestamp(long pauseTimestamp) {
        this.pauseTimestamp = pauseTimestamp;
    }

    public long getPauseTimestamp() {
        return pauseTimestamp;
    }

    public List<int[]> getLocations() {

        if (locations != null) return locations;

        String urlPrefix = getMission().getURLPrefix();
        URL locationsURL = null;
        try {
            locationsURL = new URL(urlPrefix+"/locations/location_manifest.csv");
            Log.d(TAG, "location url: %@" + locationsURL);

            final Reader reader = new InputStreamReader(locationsURL.openStream());
            final CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT);
            try {
                locations = new ArrayList<int[]>();
                for (final CSVRecord record : parser) {
                    final int siteIndex = Integer.parseInt(record.get(0));
                    final int driveIndex = Integer.parseInt(record.get(1));
                    locations.add(new int[] {siteIndex, driveIndex});
                }
            } finally {
                parser.close();
                reader.close();
            }
        } catch (MalformedURLException e) {
            Log.e(TAG, "Badly formatted URL for location manifest: "+locationsURL);
            locations = Collections.EMPTY_LIST;
        } catch (IOException e) {
            Log.e(TAG, "Error reading from location manifest URL: "+locationsURL);
            locations = Collections.EMPTY_LIST;
        }

        return locations;
    }

}

