package gov.nasa.jpl.hi.marsimages.ui;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;

import com.mapbox.mapboxsdk.geometry.BoundingBox;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.views.MapView;
import com.powellware.marsimages.R;

import org.json.JSONException;
import org.json.JSONObject;

import gov.nasa.jpl.hi.marsimages.JsonReader;

import static gov.nasa.jpl.hi.marsimages.MarsImagesApp.MARS_IMAGES;

public class MapActivity extends Activity {

    public static final String INTENT_ACTION_MAP = "gov.nasa.jpl.hi.marsimages.MAP";
    private static final String MAP = "MissionMap";

    private String tileSet;
    private int minZoom;
    private int maxNativeZoom;
    private int defaultZoom;
    private double centerLat;
    private double centerLon;
    private double upperRightLat;
    private double upperRightLon;
    private double lowerLeftLat;
    private double lowerLeftLon;

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
                MapView mapView = new MapView(MapActivity.this);
                MarsWebSourceTileLayer ws =
                        new MarsWebSourceTileLayer(tileURLPattern, bbox);
                ws.setName("Mission Map")
                        .setAttribution("NASA/JPL")
                        .setMinimumZoomLevel(minZoom)
                        .setMaximumZoomLevel(maxNativeZoom);
                mapView.setTileSource(ws);
                mapView.setCenter(new LatLng(centerLat, centerLon));
                mapView.setZoom(defaultZoom);
                frameLayout.addView(mapView);
            }
        }.execute(missionName);
    }

    private void parseMapMetadata(JSONObject jsonObject) throws JSONException {
        tileSet = jsonObject.getString("tileSet");
        minZoom = jsonObject.getInt("minZoom");
        maxNativeZoom = jsonObject.getInt("maxNativeZoom");
        int maxZoom = jsonObject.getInt("maxZoom");
        defaultZoom = jsonObject.getInt("defaultZoom");
        JSONObject center = jsonObject.getJSONObject("center");
        centerLat = center.getDouble("lat");
        centerLon = center.getDouble("lon");
        JSONObject upperLeft = jsonObject.getJSONObject("upperLeft");
        double upperLeftLat = upperLeft.getDouble("lat");
        double upperLeftLon = upperLeft.getDouble("lon");
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
        int mapPixelWidth = pixelSize.getInt("width");
        int mapPixelHeight = pixelSize.getInt("height");
    }
}
