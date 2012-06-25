/**
 * 
 */
package ca.uwinnipeg.proximitydroid.fragments;

import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import ca.uwinnipeg.proximitydroid.ProximityService;
import ca.uwinnipeg.proximitydroid.R;
import ca.uwinnipeg.proximitydroid.Region;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

/**
 * @author Garrett Smith
 *
 */
public class NeighbourhoodFragment extends RegionShowFragment {
  
  protected Map<Region, int[]> mNeighbourhoods = new HashMap<Region, int[]>();
  
  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    IntentFilter filter = new IntentFilter(ProximityService.ACTION_NEIGHBOURHOOD_SET);
    filter.addAction(ProximityService.ACTION_NEIGHBOURHOOD_PROGRESS);
    filter.addAction(ProximityService.ACTION_REGIONS_CLEARED);
    mBroadcastManager.registerReceiver(mNeighbourhoodReceiver, filter);
  }
  
  @Override
  public void onDestroy() {
    mBroadcastManager.unregisterReceiver(mNeighbourhoodReceiver);
    super.onDestroy();
  }
  
  @Override
  protected void setupView() {
    super.setupView();
    
    mNeighbourhoods = getService().getNeighbourhoods();
    
    mView.clearHighlight();
    for (Region reg : mNeighbourhoods.keySet()) {
      int[] points = mNeighbourhoods.get(reg);
      if (points != null) {
        mView.addHighlight(points);
      }
    }
  } 
  
  // options menu
  
  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    super.onCreateOptionsMenu(menu, inflater);
    inflater.inflate(R.menu.epsilon, menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == R.id.menu_epsilon) {
      FragmentManager fm = getActivity().getSupportFragmentManager();
      FragmentTransaction transaction = fm.beginTransaction();
      Fragment prev = fm.findFragmentByTag("dialog");
      if (prev != null) {
        transaction.remove(prev);
      }
      transaction.addToBackStack(null);
      
      DialogFragment newFragment = 
          EpsilonDialogFragment.newInstance(ProximityService.NEIGHBOURHOOD_EPSILON_SETTING);
      newFragment.show(transaction, "dialog");
      return true;
    }
    else {
      return super.onOptionsItemSelected(item);
    }
  }

  // broadcasts
  
  protected NeighbourhoodReceiver  mNeighbourhoodReceiver = new NeighbourhoodReceiver ();
  
  public class NeighbourhoodReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();
      if (action.equals(ProximityService.ACTION_NEIGHBOURHOOD_SET)) {
        Region reg = intent.getParcelableExtra(ProximityService.REGION);
        int[] points = intent.getIntArrayExtra(ProximityService.POINTS);
        mNeighbourhoods.put(reg, points);
        invalidate();
      }
      else if (action.equals(ProximityService.ACTION_NEIGHBOURHOOD_PROGRESS)) {
        int progress = intent.getIntExtra(ProximityService.PROGRESS, 0);
        setProgress(progress);
      }
      else if (action.equals(ProximityService.ACTION_REGIONS_CLEARED)) {
        mNeighbourhoods.clear();
        invalidate();
      }
    }
    
  }
}
