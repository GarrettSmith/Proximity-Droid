/**
 * 
 */
package ca.uwinnipeg.proximitydroid.services;

import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.util.Log;
import ca.uwinnipeg.proximitydroid.MathUtil;
import ca.uwinnipeg.proximitydroid.Region;

/**
 * @author Garrett Smith
 *
 */
public class IntersectionService extends EpsilonPropertyService {
  
  public static final String TAG = "IntersectionService";
  
  public static final String CATEGORY = "Intersection";
  
  public static final String EPSILON_KEY = "Intersection epsilon";
      
  public static final String ACTION_DEGREE_CHANGED = "action.DEGREE_SET";
  
  public static final String DEGREE = "degree";
  
  protected float mDegree = 1;
  
  // The indices of the pixels in the intersection
  protected List<Integer> mIntersection = new ArrayList<Integer>();
  
  // The list of intersection operations that will bring us to our desired result 
  protected IntersectTask mCurrentTask = null;
  
  // The list of regions to be used by intersect tasks
  protected List<Region> mQueue = new ArrayList<Region>(); 
  
  public IntersectionService() {
    super(CATEGORY, EPSILON_KEY);
  }
  
  @Override
  protected void onRegionAdded(Region region) {
    super.onRegionAdded(region);
    addIntersectionTask(region);
  }

  public int[] getIntersection() {
    return indicesToPoints(mIntersection);
  }
  
  protected void setIntersection(List<Integer> indices) {
    // save the new intersection
    mIntersection.clear();
    mIntersection.addAll(indices);
    
    // broadcast the change if we are finished calculating
    if (mQueue.isEmpty()) {
      broadcastValueChanged(mIntersection);
    }
  }
  
  public float getDegree() {
    return mDegree;
  }
  
  protected void setDegree(float degree) {
    mDegree = degree;
    
    // broadcast the change if we are finished calculating
    if (mQueue.isEmpty()) {
      Intent intent = new Intent(ACTION_DEGREE_CHANGED);
      intent.putExtra(DEGREE, degree);
      mBroadcastManager.sendBroadcast(intent);
    }
  }

  @Override
  public int getProgress() {
    return (super.getProgress() / mQueue.size());
  }

  protected void invalidate() {
    // stop all intersection tasks
    // cancel running task
    if (mCurrentTask != null && mCurrentTask.isRunning()) mCurrentTask.cancel(false);
    // clear upcoming tasks
    mQueue.clear();
    // clear calculated intersection
    mIntersection.clear();    
    // reset the intersection degree
    setDegree(1);
    
    // add all regions to the queue to be recalculated
    for (Region r : mRegions) {
      mQueue.add(r);
    }    
    // start running intersection tasks
    runNextIntersectionTask();
  }
  

  protected void addIntersectionTask(Region region) {
    // add the region to the queue
    mQueue.add(region);
    
    // run if this is the only region in the queue
    if (mQueue.size() == 1) {
      runNextIntersectionTask();
    }
      
  }

  protected void runNextIntersectionTask() {
    // only run if there are regions in the queue
    if (!mQueue.isEmpty()) {
      
      // run the task on the next region
      mCurrentTask = new IntersectTask();
      Region region = mQueue.remove(0);
  
      // start the task
      mCurrentTask.execute(region);
    }
  }

  // Tasking 
  
  private class IntersectTask extends PropertyTask {
  
    @Override
    protected List<Integer> doInBackground(Region... params) {
      mRegion = params[0];
  
      // check if we should stop because the task was cancelled
      if (isCancelled()) return null;
      
      List<Integer> indices = new ArrayList<Integer>();
      
      // check if this is the only region
      if (mIntersection.isEmpty()) {
        indices = mRegion.getIndicesList();
      }
      // else take the intersection of the region and the current intersection
      else {
        long startTime = System.currentTimeMillis();
        indices = mImage.getHybridIntersectIndices(
            mIntersection, 
            mRegion.getIndicesList(),
            getEpsilon(), 
            this);
        Log.i(TAG, "Intersection took " + (System.currentTimeMillis() - startTime)/1000f + " seconds");
      }
  
      return indices;
      
    }
    
    @Override
    protected void onPostExecute(List<Integer> result) {
      super.onPostExecute(result);
      
      // store the new degree 
      float intSize = result.size();
      float unionSize = MathUtil.union(mIntersection, mRegion.getIndicesList()).size();
      float degree = 1 - (intSize / unionSize);
      setDegree(degree);
      
      // store result as the new intersection
      setIntersection(result);
      
      // run the next intersection task if there is one
      runNextIntersectionTask();
    }
    
  }

}
