/**
 * 
 */
package ca.uwinnipeg.proximitydroid.v2.fragments;

import ca.uwinnipeg.proximitydroid.v2.R;

/**
 * A simple fragment that displays the shared preferences used to select the current features.
 * @author Garrett Smith
 *
 */
public class FeatureFragment extends PreferenceListFragment {

  public static String TAG = "ProbeFuncSelectActivity";
    
  /**
   * Creates a new feature fragment.
   */
  public FeatureFragment() {
    super(R.xml.feature_settings, R.layout.feature_select);
  }

}
