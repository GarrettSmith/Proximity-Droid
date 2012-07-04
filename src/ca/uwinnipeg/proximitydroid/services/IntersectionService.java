/**
 * 
 */
package ca.uwinnipeg.proximitydroid.services;

import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.util.Log;
import ca.uwinnipeg.proximity.PerceptualSystem.PerceptualSystemSubscriber;
import ca.uwinnipeg.proximitydroid.MathUtil;
import ca.uwinnipeg.proximitydroid.Region;

/**
 * A service that calculates the intersection.
 * @author Garrett Smith
 *
 */
public class IntersectionService extends EpsilonLinearService {
  
  public static final String TAG = "IntersectionService";
  
  public static final String CATEGORY = "Intersection";
  
  public static final String EPSILON_KEY = "Intersection epsilon";
      
  public static final String ACTION_DEGREE_CHANGED = "action.DEGREE_SET";
  
  public static final String DEGREE = "degree";
  
  // the current degree of nearness, 1 is the maximum and means completely dissimilar
  protected float mDegree = 1;
  
  public IntersectionService() {
    super(CATEGORY, EPSILON_KEY);
  }
  
  /**
   * Returns the points of the pixels within the calculated intersection.
   * @return
   */
  public int[] getIntersection() {
    return indicesToPoints(getValue());
  }
  
  /**
   * Returns the degree of nearness of the current intersection.
   * @return
   */
  public float getDegree() {
    return mDegree;
  }
  
  /**
   * Updates and broadcasts the current degree of nearness.
   * @param degree
   */
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
  protected List<Integer> calculateProperty(Region region, PerceptualSystemSubscriber sub) {
    // check if we should stop because the task was cancelled
    if (sub.isCancelled()) return null;

    List<Integer> indices = new ArrayList<Integer>();

    // check if this is the only region
    if (mValue.isEmpty()) {
      indices = region.getIndicesList();
    }
    // else take the intersection of the region and the current intersection
    else {
      long startTime = System.currentTimeMillis();
      indices = mImage.hybridIntersection(
          mValue, 
          region.getIndicesList(),
          getEpsilon(), 
          sub);
      Log.i(TAG, "Intersection took " + (System.currentTimeMillis() - startTime)/1000f + " seconds");
    }

    return indices;
  }
  
  @Override
  protected void setResult(List<Integer> indices, Region region) {
    // store the new degree 
    float intSize = indices.size();
    float unionSize = MathUtil.union(mValue, region.getIndicesList()).size();
    float degree = 1 - (intSize / unionSize);
    setDegree(degree);
    
    super.setValue(indices);
  }

}
