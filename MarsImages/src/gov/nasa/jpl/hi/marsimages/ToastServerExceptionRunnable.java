package gov.nasa.jpl.hi.marsimages;

import android.app.Activity;
import android.widget.Toast;

import com.evernote.thrift.TException;

public class ToastServerExceptionRunnable implements Runnable {

	private Exception serverException;
	private final Activity activity;

	public ToastServerExceptionRunnable(Activity activity, Exception e) {
		this.activity = activity;
		serverException = e;
	}
	
	/**
	 * Give error feedback to the user and stop the progress bar. 
	 */
	public void run() {
		if (serverException.getCause() instanceof TException) {
			Toast.makeText(activity, "Unable to connect to the network", Toast.LENGTH_LONG).show();
		} else {
			Toast.makeText(activity, "Unable to connect to the Mars Images server", Toast.LENGTH_LONG).show();
		}
		activity.setProgressBarIndeterminateVisibility(false);
	}

}
