package gov.nasa.jpl.hi.marsimages.ui;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class AboutThisAppActivity extends Activity {

    public static final String INTENT_ACTION_ABOUT_THIS_APP = "gov.nasa.jpl.hi.marsimages.ABOUT_THIS_APP";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WebView view = new WebView(this);
        view.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(view);

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            DisplayMetrics dm = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(dm);
            if (dm.widthPixels < 360) {
                view.setInitialScale((int) (dm.widthPixels / 360.0 * 100));
            }
        }

        view.loadUrl("http://www.powellware.net/MarsImagesAndroid.html");
        view.setWebViewClient(new MarsImagesWebViewClient());
    }

    private class MarsImagesWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            //keep only my web content in this view, let external links go to system browser
            if (view != null && url.startsWith("http://www.powellware.net")) {
                view.loadUrl(url);
                return true;
            }
            return false;
        }
    }
}
