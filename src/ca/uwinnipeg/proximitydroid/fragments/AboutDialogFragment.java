package ca.uwinnipeg.proximitydroid.fragments;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import ca.uwinnipeg.proximitydroid.R;

/**
 * 
 * @author Garrett Smith
 *
 */
public class AboutDialogFragment extends DialogFragment {

  public static final String TAG = "AboutDialog";
  
  protected TextView mVersionTextView;
  
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    // set title
    getDialog().setTitle(R.string.app_about);
    // create the view and set the version name
    View v = inflater.inflate(R.layout.about, container);
    mVersionTextView = (TextView) v.findViewById(R.id.about_app_version);
    mVersionTextView.setText(getVersionName(getActivity()));
    return v;
  }
  
  /**
   * Returns the version name of the app.
   */
  public String getVersionName(Context context) {
    String ver = "";
    try {
      // Ask for version
      ver = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName; 
    }
    catch (NameNotFoundException e) {
      Log.e(TAG, e.getMessage()); // Catch errors
    }
    return ver;
  }

}
