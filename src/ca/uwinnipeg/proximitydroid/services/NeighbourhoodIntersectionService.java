/**
 * 
 */
package ca.uwinnipeg.proximitydroid.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import ca.uwinnipeg.proximitydroid.Region;

/**
 * A service that calculates the intersection of the neighbourhoods within regions.
 * @author Garrett Smith
 *
 */
public class NeighbourhoodIntersectionService extends IntersectionService {
  
  public static final String TAG = "NeighbourhoodIntersectionService";
  
  public static final String CATEGORY = "Neighbourhood Intersection";
  
  // the map of regions to their neighbourhoods
  protected Map<Region, List<Integer>> mNeighbourhoods = new HashMap<Region, List<Integer>>();
  
  // receives when neighbourhoods have been calculated
  protected BroadcastReceiver mNeighbourhoodReceiver = new mNeighbourhoodReceiver();
  
  public NeighbourhoodIntersectionService() {
    super(CATEGORY);
  }

  @Override
  public void onCreate() {
    super.onCreate();
    // register to watch for changes in neighbourhoods
    IntentFilter filter = new IntentFilter();
    filter.addAction(PropertyService.ACTION_VALUE_CHANGED);
    filter.addCategory(NeighbourhoodService.CATEGORY);
    mBroadcastManager.registerReceiver(mNeighbourhoodReceiver, filter);
  }
  
  @Override
  protected void onRegionAdded(Region region) {
    // Do nothing, instead we listen for neighbourhoods to be calculated
  }
  
  @Override
  protected List<Integer> getIndices(Region region) {
    return mNeighbourhoods.get(region);
  }
  
  protected class mNeighbourhoodReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();
      if (action.equals(PropertyService.ACTION_VALUE_CHANGED)) {
        // add the region and neighbourhood to the map and add a task for them
        Region region = intent.getParcelableExtra(REGION);
        List<Integer> indices = intent.getIntegerArrayListExtra(INDICES);
        mNeighbourhoods.put(region, indices);
        addTask(region);
      }
    }
    
  }

}
