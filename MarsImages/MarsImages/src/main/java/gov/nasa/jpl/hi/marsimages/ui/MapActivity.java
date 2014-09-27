package gov.nasa.jpl.hi.marsimages.ui;

import android.app.Activity;
import android.app.Fragment;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.mapbox.mapboxsdk.geometry.BoundingBox;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.overlay.Overlay;
import com.mapbox.mapboxsdk.overlay.TilesOverlay;
import com.mapbox.mapboxsdk.tileprovider.MapTileLayerBase;
import com.mapbox.mapboxsdk.tileprovider.MapTileLayerBasic;
import com.mapbox.mapboxsdk.tileprovider.tilesource.ITileLayer;
import com.mapbox.mapboxsdk.tileprovider.tilesource.WebSourceTileLayer;
import com.mapbox.mapboxsdk.views.MapView;
import com.powellware.marsimages.R;

import org.json.JSONException;
import org.json.JSONObject;

import gov.nasa.jpl.hi.marsimages.JsonReader;
import gov.nasa.jpl.hi.marsimages.MarsImagesApp;

import static gov.nasa.jpl.hi.marsimages.MarsImagesApp.MARS_IMAGES;

public class MapActivity extends Activity {

    public static final String INTENT_ACTION_MAP = "gov.nasa.jpl.hi.marsimages.MAP";
    public static final String MAP = "MissionMap";

    public String tileSet;
    public int minZoom;
    public int maxNativeZoom;
    public int maxZoom;
    public int defaultZoom;
    public double centerLat;
    public double centerLon;
    public double upperLeftLat;
    public double upperLeftLon;
    public double upperRightLat;
    public double upperRightLon;
    public double lowerLeftLat;
    public double lowerLeftLon;
    public double lowerRightLat;
    public double lowerRightLon;
    public int mapPixelWidth;
    public int mapPixelHeight;

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
                        new MarsWebSourceTileLayer("mission map", tileURLPattern, bbox);
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
        lowerRightLat = lowerRight.getDouble("lat");
        lowerRightLon = lowerRight.getDouble("lon");
        JSONObject pixelSize = jsonObject.getJSONObject("pixelSize");
        mapPixelWidth = pixelSize.getInt("width");
        mapPixelHeight = pixelSize.getInt("height");
    }
}
