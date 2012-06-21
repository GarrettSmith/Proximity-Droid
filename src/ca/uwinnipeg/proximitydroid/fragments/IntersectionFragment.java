/**
 * 
 */
package ca.uwinnipeg.proximitydroid.fragments;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import ca.uwinnipeg.proximitydroid.ProximityService;
import ca.uwinnipeg.proximitydroid.R;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;


/**
 * @author Garrett Smith
 *
 */
// TODO: remove intersection and neighbourhood fragment duplication
public class IntersectionFragment extends RegionShowFragment {
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);
  }
  
  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    IntentFilter filter = new IntentFilter(ProximityService.ACTION_INTERSECTION_SET);
    filter.addAction(ProximityService.ACTION_INTERSECTION_PROGRESS);
    filter.addAction(ProximityService.ACTION_REGIONS_CLEARED);
    mBroadcastManager.registerReceiver(mIntersectionReceiver, filter);
  }
  
  @Override
  public void onDestroy() {
    mBroadcastManager.unregisterReceiver(mIntersectionReceiver);
    super.onDestroy();
  }
  
  @Override
  protected void setupView() {
    super.setupView();
    
    int[] points = getService().getIntersection();
    if (points != null) {
      mView.addHighlight(points);
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
      // TODO: get current epsilon
      
      DialogFragment newFragment = 
          EpsilonDialogFragment.newInstance(ProximityService.INTERSECTION_EPSILON_SETTING);
      newFragment.show(transaction, "dialog");
      return true;
    }
    else {
      return super.onOptionsItemSelected(item);
    }
  }
  
  // broadcasts
  
  protected IntersectionReceiver  mIntersectionReceiver = new IntersectionReceiver ();
  
  public class IntersectionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();
      if (action.equals(ProximityService.ACTION_INTERSECTION_SET)) {
        int[] points = intent.getIntArrayExtra(ProximityService.POINTS);
        mView.setHighlight(points);
      }
      else if (action.equals(ProximityService.ACTION_INTERSECTION_PROGRESS)) {
        int progress = intent.getIntExtra(ProximityService.PROGRESS, 0);
        setProgress(progress);
      }
      else if (action.equals(ProximityService.ACTION_REGIONS_CLEARED)) {
        mView.clearHighlight();
      }
    }
    
  }
}
