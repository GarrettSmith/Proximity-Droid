/**
 * 
 */
package ca.uwinnipeg.proximitydroid.fragments;

import java.util.ArrayList;
import java.util.List;

import ca.uwinnipeg.proximity.ProbeFunc;
import ca.uwinnipeg.proximitydroid.R;

/**
 * @author Garrett Smith
 *
 */
// TODO: Look into preference fragments
public class FeatureSelectFragment extends PreferenceListFragment {

  public static String TAG = "ProbeFuncSelectActivity";

  // The list of selected probe function
  protected List<ProbeFunc<Integer>> mProbeFuncs = new ArrayList<ProbeFunc<Integer>>();
    
  public FeatureSelectFragment() {
    super(R.xml.feature_settings, R.layout.probe_func_select);
  }

}
