/**
 * 
 */
package ca.uwinnipeg.proximitydroid.services;

import java.util.ArrayList;
import java.util.List;

import ca.uwinnipeg.proximity.PerceptualSystem.PerceptualSystemSubscriber;
import ca.uwinnipeg.proximitydroid.Region;

/**
 * @author Garrett Smith
 *
 */
public abstract class LinearService extends PropertyService {
  
  public static final String TAG = "PropertyService";
  
  public LinearService(String category) {
    super(category);
  }

  // The indices of the pixels in the intersection
  protected List<Integer> mValue = new ArrayList<Integer>();
  
  // The list of intersection operations that will bring us to our desired result 
  protected LinearTask mCurrentTask = null;
  
  // The list of regions to be used by intersect tasks
  protected List<Region> mQueue = new ArrayList<Region>(); 

  
  @Override
  protected void onRegionAdded(Region region) {
    super.onRegionAdded(region);
    addTask(region);
  }
  
  protected List<Integer> getValue() {
    return mValue;
  }
  
  protected void setValue(List<Integer> indices) {
    // save the new intersection
    mValue.clear();
    mValue.addAll(indices);
    
    // broadcast the change if we are finished calculating
    if (mQueue.isEmpty()) {
      broadcastValueChanged(mValue);
    }
  }

  @Override
  public int getProgress() {
    return (super.getProgress() / mQueue.size());
  }

  protected void invalidate() {
    // stop all tasks
    // cancel running task
    if (mCurrentTask != null && mCurrentTask.isRunning()) mCurrentTask.cancel(false);
    // clear upcoming tasks
    mQueue.clear();
    // clear calculated value
    mValue.clear();
    
    // add all regions to the queue to be recalculated
    for (Region r : mRegions) {
      mQueue.add(r);
    }    
    // start running tasks
    runNextTask();
  }
  

  protected void addTask(Region region) {
    // add the region to the queue
    mQueue.add(region);
    
    // run if this is the only region in the queue
    if (mQueue.size() == 1) {
      runNextTask();
    }
      
  }

  protected void runNextTask() {
    // only run if there are regions in the queue
    if (!mQueue.isEmpty()) {
      
      // run the task on the next region
      mCurrentTask = new LinearTask();
      Region region = mQueue.remove(0);
  
      // start the task
      mCurrentTask.execute(region);
    }
  }
  
  protected class LinearTask extends PropertyTask {
    
    protected Region mRegion;

    @Override
    protected void onPostExecute(List<Integer> result) {
      super.onPostExecute(result);      
      setResult(result, mRegion);
      // run the next task if there is one
      runNextTask();
    }

    @Override
    protected List<Integer> doInBackground(Region... params) {
      mRegion = params[0];
      return calculateProperty(mRegion, this);
    }
    
  }
  
  protected abstract List<Integer> calculateProperty(Region region, PerceptualSystemSubscriber sub);
  
  protected void setResult(List<Integer> result, Region region) {    
    // store result as the new value
    setValue(result);
  }

}
