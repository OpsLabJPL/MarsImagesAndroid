package gov.nasa.jpl.hi.marsimages.models;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by mpowell on 3/22/15.
 */
public abstract class CameraModel implements Model {

    public static final String TAG = "CameraModel";

    private double[] d1 = new double[3], d2 = new double[3];

    public abstract int[] size();

    public static Model getModel(JSONArray modelJSON) {
        Model returnedModel = null;
        JSONArray c, a, h, v, o, r, e;
        int mtype;
        double mparm;
        int width = 0, height = 0;
        String type = null;
        try {
            JSONObject imageDimensions = modelJSON.getJSONObject(0);
            JSONObject geometricModel = modelJSON.getJSONObject(1);
            type = geometricModel.getString("type");
            JSONObject comps = geometricModel.getJSONObject("components");
            JSONArray area = imageDimensions.getJSONArray("area");
            c = comps.getJSONArray("c");
            a = comps.getJSONArray("a");
            h = comps.getJSONArray("h");
            v = comps.getJSONArray("v");
            width = area.getInt(0);
            height = area.getInt(1);

            if ("CAHV".equals(type)) {
                CAHV model = new CAHV();
                model.setC(c.getDouble(0), c.getDouble(1), c.getDouble(2));
                model.setA(a.getDouble(0), a.getDouble(1), a.getDouble(2));
                model.setH(h.getDouble(0), h.getDouble(1), h.getDouble(2));
                model.setV(v.getDouble(0), v.getDouble(1), v.getDouble(2));
                returnedModel = model;
            } else if ("CAHVOR".equals(type)) {
                o = comps.getJSONArray("o");
                r = comps.getJSONArray("r");
                CAHVOR model = new CAHVOR();
                model.setC(c.getDouble(0), c.getDouble(1), c.getDouble(2));
                model.setA(a.getDouble(0), a.getDouble(1), a.getDouble(2));
                model.setH(h.getDouble(0), h.getDouble(1), h.getDouble(2));
                model.setV(v.getDouble(0), v.getDouble(1), v.getDouble(2));
                model.setO(o.getDouble(0), o.getDouble(1), o.getDouble(2));
                model.setR(r.getDouble(0), r.getDouble(1), r.getDouble(2));
                returnedModel = model;
            } else if ("CAHVORE".equals(type)) {
                o = comps.getJSONArray("o");
                r = comps.getJSONArray("r");
                e = comps.getJSONArray("e");
                mtype = comps.getInt("t");
                mparm = comps.getDouble("p");
                CAHVORE model = new CAHVORE();
                model.setC(c.getDouble(0), c.getDouble(1), c.getDouble(2));
                model.setA(a.getDouble(0), a.getDouble(1), a.getDouble(2));
                model.setH(h.getDouble(0), h.getDouble(1), h.getDouble(2));
                model.setV(v.getDouble(0), v.getDouble(1), v.getDouble(2));
                model.setO(o.getDouble(0), o.getDouble(1), o.getDouble(2));
                model.setR(r.getDouble(0), r.getDouble(1), r.getDouble(2));
                model.setE(e.getDouble(0), e.getDouble(1), e.getDouble(2));
                model.setType(mtype);
                model.setP(mparm);
                returnedModel = model;
            }
            else {
                Log.e(TAG, "Unknown camera model type: "+type);
            }
        } catch (JSONException ex) {
            Log.e(TAG, "Error parsing model json ", ex);
        }

        if (returnedModel != null) {
            ((CAHV)returnedModel).setXdim(width);
            ((CAHV)returnedModel).setYdim(height);
        }
        return returnedModel;
    }

    public int[] origin(JSONArray modelJson) {
        try {
            JSONObject imageDimensions = modelJson.getJSONObject(0);
            JSONArray comps = imageDimensions.getJSONArray("origin");
            if (comps != null) {
                int x = comps.getInt(0);
                int y = comps.getInt(1);
                return new int[]{x, y};
            }
            Log.e(TAG, "Brown alert: origin not found in camera model.");
        } catch (JSONException ex) {
            Log.e(TAG, "Error parsing model json", ex);
        }
        return new int[] {0,0};
    }

    public static double[] pointingVector(JSONArray modelJson) {
        try {
            JSONObject cmod = modelJson.getJSONObject(1);
            JSONArray comps = cmod.getJSONArray("camera_vector");
            if (comps != null) {
                double x = comps.getDouble(0);
                double y = comps.getDouble(1);
                double z = comps.getDouble(2);
                return new double[]{x, y, z};
            }
            Log.e(TAG, "Brown alert: camera_vector not found in camera model.");
        } catch (JSONException ex) {
            Log.e(TAG, "error parsing model json", ex);
        }
        return new double[] {};
    }

    public static double angularDistance(double[] v1, double[] v2) {
        if (v1.length==0 || v2.length==0)
            return 0;

        double dot = M.dot(v1, v2);
        return Math.acos(dot);
    }


}
