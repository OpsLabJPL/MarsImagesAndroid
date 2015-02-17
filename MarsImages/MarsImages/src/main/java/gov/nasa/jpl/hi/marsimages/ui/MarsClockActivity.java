package gov.nasa.jpl.hi.marsimages.ui;

import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.powellware.marsimages.R;

import java.text.SimpleDateFormat;
import java.util.Date;

import gov.nasa.jpl.hi.marsimages.rovers.MarsTime;
import gov.nasa.jpl.hi.marsimages.rovers.Opportunity;

import static gov.nasa.jpl.hi.marsimages.rovers.MarsTime.CURIOSITY_WEST_LONGITUDE;
import static gov.nasa.jpl.hi.marsimages.rovers.MarsTime.EARTH_SECS_PER_MARS_SEC;

public class MarsClockActivity extends ActionBarActivity {

    public static final String INTENT_ACTION_MARS_TIME = "gov.nasa.jpl.hi.marsimages.MARS_TIME";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mars_clock);
        final TextView earthText = (TextView) findViewById(R.id.earthTimeText);
        final TextView oppyText = (TextView) findViewById(R.id.oppyTimeText);
        final TextView curioText = (TextView) findViewById(R.id.curioTimeText);
        final Opportunity opportunity = new Opportunity();
        final SimpleDateFormat earthTimeFormat = new SimpleDateFormat("yyyy-DDD'T'hh:mm:ss' UTC'");
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Date today = new Date();
                earthText.setText(earthTimeFormat.format(today));

                long timeDiff = today.getTime()/1000 - opportunity.getEpoch().getTime()/1000;
                timeDiff = (long)(timeDiff / EARTH_SECS_PER_MARS_SEC);
                int sol = (int)(timeDiff / 86400);
                timeDiff -= sol * 86400;
                int hour = (int)(timeDiff / 3600);
                timeDiff -= hour * 3600;
                int minute = (int)(timeDiff / 60);
                int seconds = (int)(timeDiff - minute*60);
                sol += 1; //MER convention of landing day sol 1
                oppyText.setText(String.format("Sol %03d %02d:%02d:%02d", sol, hour, minute, seconds));

                Date curiosityTime = new Date();
                Object[] marsTimes = MarsTime.getMarsTimes(curiosityTime, CURIOSITY_WEST_LONGITUDE);
                Double msd = (Double) marsTimes[10];
                Double mtc = (Double) marsTimes[11];
                sol = (int)(msd - (360-CURIOSITY_WEST_LONGITUDE) / 360) - 49268;
                double mtcInHours = MarsTime.canonicalValue24(mtc - CURIOSITY_WEST_LONGITUDE*24.0/360.0);
                hour = (int) mtcInHours;
                minute = (int) ((mtcInHours-hour)*60.0);
                seconds = (int) ((mtcInHours-hour)*3600 - minute*60);
                curioText.setText(String.format("Sol %03d %02d:%02d:%02d", sol, hour, minute, seconds));

                handler.postDelayed(this, 100);
            }
        }, 100);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.mars_clock, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return item.getItemId() == R.id.action_settings || super.onOptionsItemSelected(item);
    }
}
