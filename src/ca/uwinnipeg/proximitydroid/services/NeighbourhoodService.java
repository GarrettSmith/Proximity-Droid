/**
 * 
 */
package ca.uwinnipeg.proximitydroid.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.util.Log;
import ca.uwinnipeg.proximitydroid.Region;

/**
 * @author Garrett Smith
 *
 */
public class NeighbourhoodService extends EpsilonPropertyService {  
  
  public static final String TAG = "NeighbourhoodService";
  
  public static final String CATEGORY = "Neighbourhood";
  
  public static final String EPSILON_KEY = "Intersection epsilon";

  // The map of regions to the indices of the pixels in their neighbourhoods
  protected Map<Region, List<Integer>> mNeighbourhoods = new HashMap<Region, List<Integer>>();

  // Map each region to the task generating it's neighbourhood
  protected Map<Region, NeighbourhoodTask> mTasks = new HashMap<Region, NeighbourhoodTask>();
  
  public NeighbourhoodService() {
    super(CATEGORY, EPSILON_KEY);
  }
  
  @Override
  protected void onRegionAdded(Region region) {
    super.onRegionAdded(region);
    invalidate(region);
  }
  
  @Override
  protected void onRegionRemoved(Region region) {
    super.onRegionRemoved(region);
    setNeighbourhood(region, null);
  }
  
  public Map<Region, int[]> getNeighbourhoods() {
    Map<Region, int[]> nhs = new HashMap<Region, int[]>();
    for (Region reg : mNeighbourhoods.keySet()) {
      List<Integer> indices = mNeighbourhoods.get(reg);
      nhs.put(reg, indicesToPoints(indices));
    }
    return nhs;
  }
  
  protected void setNeighbourhood(Region region, List<Integer> indices) {
    // save the change
    if (indices != null) {
      mNeighbourhoods.put(region, indices);
    }
    else {
      mNeighbourhoods.get(region).clear();
    }
    
    // broadcast
    indices = mNeighbourhoods.get(region);
    broadcastValueChanged(indices, region);
  }

  // Tasking 
  
  @Override
  public int getProgress() {
    // count the number of tasks running
    int runningTasks = 0;
    for (NeighbourhoodTask task : mTasks.values()) {
      if (task.isRunning()) {
        runningTasks++;
      }
    }
    return super.getProgress() / runningTasks;
  }

  @Override
  protected void invalidate() {
    //clear all neighbourhoods
    mNeighbourhoods.clear();
    // update all neighbourhoods
    for (Region r : mRegions) {
      invalidate(r);
    } 
  }
  
  protected void invalidate(Region region) {
    // clear old points
    mNeighbourhoods.put(region, new ArrayList<Integer>());
    
    // Cancel running task
    NeighbourhoodTask task = mTasks.get(region);
    if (task != null && task.isRunning()) {
      task.cancel(false);
    }
  
    // run the new task
    task = new NeighbourhoodTask();
    mTasks.put(region, task);
    task.execute(region);
  }

  // Tasking 
  
  private class NeighbourhoodTask extends PropertyTask {
  
    @Override
    protected List<Integer> doInBackground(Region... params) {
      mRegion = params[0];
  
      // check if we should stop because the task was cancelled
      if (isCancelled()) return null;
  
      int center = mRegion.getCenterIndex();
      List<Integer> regionPixels = mRegion.getIndicesList();
  
      long startTime = System.currentTimeMillis();
      List<Integer> rtn = mImage.hybridNeighbourhood(
          center, 
          regionPixels, 
          getEpsilon(), 
          this);
      Log.i(TAG, "Neighbourhood took " + (System.currentTimeMillis() - startTime)/1000f + " seconds");
      
      return rtn;
    }
  
    @Override
    protected void onPostExecute(List<Integer> result) {      
      super.onPostExecute(result);
      // save the result
      setNeighbourhood(mRegion, result);
    }
  
  }

}
