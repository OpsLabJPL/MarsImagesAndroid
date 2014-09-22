package gov.nasa.jpl.hi.marsimages.ui;

import com.mapbox.mapboxsdk.geometry.BoundingBox;
import com.mapbox.mapboxsdk.tileprovider.MapTile;
import com.mapbox.mapboxsdk.tileprovider.tilesource.WebSourceTileLayer;

/**
 * Created by mpowell on 9/20/14.
 */
public class MarsWebSourceTileLayer extends WebSourceTileLayer {

    public MarsWebSourceTileLayer(String missionmap, String tileURLPattern, BoundingBox bbox, int minZoom, int maxZoom) {
        super(missionmap, tileURLPattern);
        mBoundingBox = bbox; //override default world
        mCenter = bbox.getCenter(); //override default (0,0)
        mMinimumZoomLevel = minZoom;
        mMaximumZoomLevel = maxZoom;
    }

    @Override
    protected String parseUrlForTile(String url, final MapTile aTile, boolean hdpi) {
        int y =  (int)Math.pow(2, aTile.getZ()) - aTile.getY() - 1;
        return url.replace("{z}", String.valueOf(aTile.getZ()))
                .replace("{x}", String.valueOf(aTile.getX()))
                .replace("{y}", String.valueOf(y))
                .replace("{2x}", hdpi ? "@2x" : "");
    }
}


