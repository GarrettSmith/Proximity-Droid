/**
 * 
 */
package ca.uwinnipeg.proximitydroid.fragments;

import ca.uwinnipeg.proximitydroid.R;

/**
 * @author Garrett Smith
 *
 */
// TODO: Look into preference fragments
public class FeatureSelectFragment extends PreferenceListFragment {

  public static String TAG = "ProbeFuncSelectActivity";
    
  public FeatureSelectFragment() {
    super(R.xml.feature_settings, R.layout.probe_func_select);
  }

}
