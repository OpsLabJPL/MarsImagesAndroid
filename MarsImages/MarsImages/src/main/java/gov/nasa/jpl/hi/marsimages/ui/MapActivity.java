package gov.nasa.jpl.hi.marsimages.ui;

import android.app.Activity;
import android.graphics.PointF;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;

import com.mapbox.mapboxsdk.geometry.BoundingBox;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.overlay.Marker;
import com.mapbox.mapboxsdk.views.MapView;
import com.powellware.marsimages.R;

import org.apache.commons.csv.CSVRecord;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gov.nasa.jpl.hi.marsimages.JsonReader;

import static gov.nasa.jpl.hi.marsimages.MarsImagesApp.MARS_IMAGES;

public class MapActivity extends Activity {

    public static final String INTENT_ACTION_MAP = "gov.nasa.jpl.hi.marsimages.MAP";
    private static final String MAP = "MissionMap";

    private String tileSet;
    private int minZoom;
    private int maxZoom;
    private int maxNativeZoom;
    private int defaultZoom;
    private double centerLat;
    private double centerLon;
    private double upperLeftLat;
    private double upperLeftLon;
    private double upperRightLat;
    private double upperRightLon;
    private double lowerLeftLat;
    private double lowerLeftLon;
    private int mapPixelWidth;
    private int mapPixelHeight;

    private int latestSiteIndex;
    private MapView mapView;
    private ArrayList<LatLng> points;
    private HashMap<Integer, int[]> rmcsForPoints;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        final FrameLayout frameLayout = (FrameLayout) findViewById(R.id.mapContainer);
        String missionName = MARS_IMAGES.getMissionName();
        new AsyncTask<String, Void, Void>() {
            @Override
            protected Void doInBackground(String... params) {
                String missionName = params[0];
                try {
                    String url = "http://merpublic.s3.amazonaws.com/maps/" + missionName + "Map.json";
                    JSONObject jsonObject = JsonReader.readJsonFromUrl(url);
                    parseMapMetadata(jsonObject);

                } catch (Exception e) {
                    Log.e(MAP, e.toString());
                }
                return null;
            }
            @Override
            protected void onPostExecute(Void aVoid) {
                BoundingBox bbox = new BoundingBox(
                        new LatLng(upperRightLat, upperRightLon), new LatLng(lowerLeftLat, lowerLeftLon));
                String tileURLPattern = tileSet+"/{z}/{x}/{y}.png";
                mapView = new MapView(MapActivity.this);
                MarsWebSourceTileLayer ws =
                        new MarsWebSourceTileLayer(tileURLPattern, bbox);
                ws.setName("Mission Map")
                        .setAttribution("NASA/JPL")
                        .setMinimumZoomLevel(minZoom)
                        .setMaximumZoomLevel(maxNativeZoom);
                mapView.setTileSource(ws);
                mapView.setCenter(new LatLng(centerLat, centerLon));
                mapView.setMinZoomLevel(minZoom);
                mapView.setMaxZoomLevel(maxZoom);
                mapView.setZoom(maxZoom-4);
                frameLayout.addView(mapView);

                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        loadLatestTraversePath();
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        mapView.getController().goTo(points.get(0), new PointF(0,0));

                        for (LatLng point : points) {
                            mapView.addMarker(new Marker(mapView, "", "", point));
                        }
                    }
                }.execute();
            }
        }.execute(missionName);
    }

    private void parseMapMetadata(JSONObject jsonObject) throws JSONException {
        tileSet = jsonObject.getString("tileSet");
        minZoom = jsonObject.getInt("minZoom");
        maxNativeZoom = jsonObject.getInt("maxNativeZoom");
        maxZoom = jsonObject.getInt("maxZoom");
        defaultZoom = jsonObject.getInt("defaultZoom");
        JSONObject center = jsonObject.getJSONObject("center");
        centerLat = center.getDouble("lat");
        centerLon = center.getDouble("lon");
        JSONObject upperLeft = jsonObject.getJSONObject("upperLeft");
        upperLeftLat = upperLeft.getDouble("lat");
        upperLeftLon = upperLeft.getDouble("lon");
        JSONObject upperRight = jsonObject.getJSONObject("upperRight");
        upperRightLat = upperRight.getDouble("lat");
        upperRightLon = upperRight.getDouble("lon");
        JSONObject lowerLeft = jsonObject.getJSONObject("lowerLeft");
        lowerLeftLat = lowerLeft.getDouble("lat");
        lowerLeftLon = lowerLeft.getDouble("lon");
        JSONObject lowerRight = jsonObject.getJSONObject("lowerRight");
        double lowerRightLat = lowerRight.getDouble("lat");
        double lowerRightLon = lowerRight.getDouble("lon");
        JSONObject pixelSize = jsonObject.getJSONObject("pixelSize");
        mapPixelWidth = pixelSize.getInt("width");
        mapPixelHeight = pixelSize.getInt("height");
    }

    private void loadLatestTraversePath() {
        points = new ArrayList<LatLng>();
        rmcsForPoints = new HashMap<Integer, int[]>();
        List<int[]> locationManifest = MARS_IMAGES.getLocations(); //write this
        int locationCount = locationManifest.size();
        if (locationCount > 0) {
            latestSiteIndex = locationManifest.get(locationCount-1)[0];
            int i = 0;
            do {
                List<CSVRecord> locations = MARS_IMAGES.getMission().siteLocationData(latestSiteIndex); //write this
                for (CSVRecord location : locations) {
                    if (location.size() >= 7) {
                        int driveIndex = Integer.parseInt(location.get(0));
                        double mapPixelH = Double.parseDouble(location.get(5));
                        double mapPixelV = Double.parseDouble(location.get(6));
                        points.add(new LatLng(
                                upperLeftLat + (mapPixelV / mapPixelHeight) * (lowerLeftLat - upperLeftLat),
                                upperLeftLon + (mapPixelH / mapPixelWidth) * (upperRightLon - upperLeftLon)));
                        int[] rmc = new int[] {latestSiteIndex, driveIndex};
                        rmcsForPoints.put(i, rmc);
                        i += 1;
                    }
                }

                latestSiteIndex -= 1;
            } while (points.size() == 0); //as soon as there are any points from the most recent location that has some, stop
        }
    }

}
