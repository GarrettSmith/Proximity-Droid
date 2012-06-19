/**
 * 
 */
package ca.uwinnipeg.proximitydroid.fragments;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import ca.uwinnipeg.proximitydroid.ProximityService;


/**
 * @author Garrett Smith
 *
 */
public class IntersectionFragment extends RegionShowFragment {
  
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
