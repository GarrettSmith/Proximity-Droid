/**
 * 
 */
package ca.uwinnipeg.proximitydroid;

import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import ca.uwinnipeg.proximitydroid.fragments.RegionShowFragment;

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

  // broadcasts
  
  protected NeighbourhoodReceiver  mNeighbourhoodReceiver 
    = new NeighbourhoodReceiver ();
  
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
    }
    
  }
}
