/**
 * 
 */
package ca.uwinnipeg.proximitydroid.fragments;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import ca.uwinnipeg.proximity.ProbeFunc;
import ca.uwinnipeg.proximitydroid.R;

/**
 * @author Garrett Smith
 *
 */
// TODO: Look into preference fragments
public class ProbeFuncSelectFragment extends PreferenceListFragment {

  public static String TAG = "ProbeFuncSelectActivity";

  // The list of selected probe function
  protected List<ProbeFunc<Integer>> mProbeFuncs = new ArrayList<ProbeFunc<Integer>>();
    
  public ProbeFuncSelectFragment() {
    super(R.xml.feature_settings, R.layout.probe_func_select);
  }

}
