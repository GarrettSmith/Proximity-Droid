/**
 * 
 */
package ca.uwinnipeg.proximitydroid.v2.services;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;
import ca.uwinnipeg.proximity.PerceptualSystem.PerceptualSystemSubscriber;
import ca.uwinnipeg.proximitydroid.v2.Region;

/**
 * A service that calculates the compliment of a {@link Region}. Every additional region is used to
 * take the difference from the original compliment.
 * @author Garrett Smith
 *
 */
public class ComplimentService extends EpsilonLinearService {
  
  public static final String TAG = "PropertyService";
  
  public static final String CATEGORY = "Compliment";
  
  public static final String EPSILON_KEY = "Compliment epsilon";
  
  public ComplimentService() {
    super(CATEGORY, EPSILON_KEY);
  }
  
  protected ComplimentService(String category) {
    super(category, EPSILON_KEY);
  }
  
  /**
   * Returns the points within the most recently calculated compliment.
   * @return
   */
  public int[] getCompliment() {
    return indicesToPoints(getValue());
  }

  @Override
  protected List<Integer> calculateProperty(Region region, PerceptualSystemSubscriber sub) {
    List<Integer> indices = new ArrayList<Integer>();

    // check if we should stop because the task was cancelled
    if (sub.isCancelled()) {
      indices = null;
    }
    else {
      // take the initial compliment
      long startTime = System.currentTimeMillis();
      if (mRegions.get(0) == region) {
        indices = mImage.hybridCompliment(getIndices(region), getEpsilon(), sub);
      }
      // take the difference of with the next object
      else {  
        indices = mImage.hybridDifference(mValue, getIndices(region), getEpsilon(), sub);
      }
      Log.i(TAG, "Compliment took " + (System.currentTimeMillis() - startTime)/1000f + " seconds");
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

}
