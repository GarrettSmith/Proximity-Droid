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
  public static final String INTERSECTION_SIZE = "intersection size";
  public static final String UNION_SIZE = "union size";
  
  // the current degree of nearness, 1 is the maximum and means completely dissimilar
  protected float mDegree = 1;
  
  // the number of pixels in the union
  protected int mUnionSize = 0;
  
  // the number of pixels in the intersections
  protected int mIntersectionSize = 0;
  
  public IntersectionService() {
    super(CATEGORY, EPSILON_KEY);
  }
  
  protected IntersectionService(String category) {
    super(category, EPSILON_KEY);
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
  protected void setDegree(float degree, int unionSize, int intSize) {
    mDegree = degree;
    mUnionSize = unionSize;
    mIntersectionSize = intSize;
    
    // broadcast the change if we are finished calculating
    if (mQueue.isEmpty()) {
      Intent intent = new Intent(ACTION_DEGREE_CHANGED);
      intent.addCategory(CATEGORY);
      intent.putExtra(DEGREE, degree);
      intent.putExtra(UNION_SIZE, mUnionSize);
      intent.putExtra(INTERSECTION_SIZE, mIntersectionSize);
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
      indices = getIndices(region);
    }
    // else take the intersection of the region and the current intersection
    else {
      long startTime = System.currentTimeMillis();
      indices = mImage.hybridIntersection(
          mValue, 
          getIndices(region),
          getEpsilon(), 
          sub);
      Log.i(TAG, "Intersection took " + (System.currentTimeMillis() - startTime)/1000f + " seconds");
    }

    return indices;
  }
  
  /**
   * Returns the indices associated with the given region.
   * <p>
   * We use this so we can override it for the other intersection services.
   * @param region
   * @return
   */
  protected List<Integer> getIndices(Region region) {
    return region.getIndicesList();
  }
  
  @Override
  protected void setResult(List<Integer> intersection, Region region) {  
    // size of the intersection
    int intSize = intersection.size();    
    // union of indices in the current intersection and in the added region
    int unionSize = MathUtil.union(mValue, getIndices(region)).size();
    float degree = 1 - ((float) intSize / unionSize);
    
    setDegree(degree, unionSize, intSize);    
    super.setValue(intersection);
  }

}
