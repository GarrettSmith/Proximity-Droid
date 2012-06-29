/**
 * 
 */
package ca.uwinnipeg.proximitydroid.services;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;
import ca.uwinnipeg.proximity.PerceptualSystem.PerceptualSystemSubscriber;
import ca.uwinnipeg.proximitydroid.Region;

/**
 * @author Garrett Smith
 *
 */
public class DifferenceService extends EpsilonLinearService {
  
  public static final String TAG = "DifferenceService";
  
  public static final String CATEGORY = "Difference";
  
  public static final String EPSILON_KEY = "Difference Epsilon";

  public DifferenceService() {
    super(CATEGORY, EPSILON_KEY);
  }
  
  public int[] getDifference() {
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
      if (mValue.isEmpty()) {
        indices = region.getIndicesList();
      }
      // take the difference of with the next object
      else {  
        indices = mImage.hybridDifference(mValue, region.getIndicesList(), getEpsilon(), sub);
      }
      Log.i(TAG, "Difference took " + (System.currentTimeMillis() - startTime)/1000f + " seconds");
    }

    return indices;
  }

}
