/**
 * 
 */
package ca.uwinnipeg.proximitydroid.services;

import java.util.ArrayList;
import java.util.List;

import ca.uwinnipeg.proximity.PerceptualSystem.PerceptualSystemSubscriber;
import ca.uwinnipeg.proximitydroid.Region;

/**
 * A service that uses a linear list of tasks to calculate the property for each subsequent region.
 * @author Garrett Smith
 *
 */
public abstract class LinearService extends PropertyService {
  
  public static final String TAG = "PropertyService";

  // The indices of the pixels in the intersection
  protected List<Integer> mValue = new ArrayList<Integer>();
  
  // The list of intersection operations that will bring us to our desired result 
  protected LinearTask mCurrentTask = null;
  
  // The list of regions to be used by intersect tasks
  protected List<Region> mQueue = new ArrayList<Region>(); 
  
  /**
   * Creates a new service that broadcasts using the given category.
   * @param category
   */
  public LinearService(String category) {
    super(category);
  }
  
  @Override
  protected void onRegionAdded(Region region) {
    super.onRegionAdded(region);
    addTask(region);
  }
  
  /**
   * Returns the current list of indices calculated to form the property.
   * @return
   */
  protected List<Integer> getValue() {
    return mValue;
  }
  
  /**
   * Sets and broadcasts the current general value calculated for the property.
   * @param indices
   */
  protected void setValue(List<Integer> indices) {
    // save the new intersection
    mValue.clear();
    if (indices != null) mValue.addAll(indices);
    
    // broadcast the change if we are finished calculating
    if (mQueue.isEmpty()) {
      broadcastValueChanged(mValue);
    }
  }

  @Override
  public int getProgress() {
    int tasks = mQueue.size();
    
    // add the current task to the count if it is running
    if (mCurrentTask != null && mCurrentTask.isRunning()) {
      tasks++;
    }
    
    if (tasks > 0) {
      return (super.getProgress() / tasks);
    }
    else {
      return MAX_PROGRESS;
    }
  }

  /**
   * Recalculates the property starting from the first region added.
   */
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
  

  /**
   * Adds a task to the queue for the given region.
   * @param region
   */
  protected void addTask(Region region) {
    // add the region to the queue
    mQueue.add(region);
    
    // run if this is the only region in the queue
    if (mQueue.size() == 1) {
      runNextTask();
    }
      
  }

  /**
   * Runs the next task in the queue if it is non empty.
   */
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
  
  /**
   * A task that calculates a value and runs the next task in the queue.
   * @author Garrett Smith
   *
   */
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
  
  /**
   * This method should be overridden by subclasses to calculate their specific property.
   * @param region
   * @param sub
   * @return
   */
  protected abstract List<Integer> calculateProperty(Region region, PerceptualSystemSubscriber sub);
  
  /**
   * Sets the result of calculating using the given region to the given value.
   * @param result
   * @param region
   */
  protected void setResult(List<Integer> result, Region region) {    
    // store result as the new value
    setValue(result);
  }

}
