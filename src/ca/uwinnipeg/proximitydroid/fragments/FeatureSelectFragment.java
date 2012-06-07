/**
 * 
 */
package ca.uwinnipeg.proximitydroid.fragments;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import ca.uwinnipeg.proximity.ProbeFunc;
import ca.uwinnipeg.proximitydroid.R;
import ca.uwinnipeg.proximitydroid.fragments.RegionSelectFragment.OnRegionSelectedListener;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

/**
 * @author Garrett Smith
 *
 */
// TODO: Look into preference fragments
public class FeatureSelectFragment extends PreferenceListFragment {

  public static String TAG = "ProbeFuncSelectActivity";

  // The list of selected probe function
  protected List<ProbeFunc<Integer>> mProbeFuncs = new ArrayList<ProbeFunc<Integer>>();
  
  private OnToggleSelectedListener mListener;
  
  public interface OnToggleSelectedListener {
    public void onToggleSelected(MenuItem item);
  }
    
  public FeatureSelectFragment() {
    super(R.xml.feature_settings, R.layout.probe_func_select);
  }
  
  @Override
  public void onCreate(Bundle b) {
    super.onCreate(b);
    setHasOptionsMenu(true);
  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    try {
      mListener = (OnToggleSelectedListener) activity;
    } catch (ClassCastException e) {
      throw new ClassCastException(activity.toString() + 
          " must implement OnToggleSelectedListener");
    }
  }
  
  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    super.onCreateOptionsMenu(menu, inflater);
    inflater.inflate(R.menu.features_select, menu);
    // setup toggle button text
    int text = isHidden() ? R.string.menu_show_features : R.string.menu_hide_features;
    menu.findItem(R.id.menu_features).setTitle(text);
  }
  
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menu_features:
        mListener.onToggleSelected(item);
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

}
