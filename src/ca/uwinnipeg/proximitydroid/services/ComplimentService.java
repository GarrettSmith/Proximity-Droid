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
public class ComplimentService extends EpsilonLinearService {
  
  public static final String TAG = "PropertyService";
  
  public static final String CATEGORY = "Compliment";
  
  public static final String EPSILON_KEY = "Compliment epsilon";
  
  public ComplimentService() {
    super(CATEGORY, EPSILON_KEY);
  }
  
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
      if (mValue.isEmpty()) {
        indices = mImage.hybridCompliment(region.getIndicesList(), getEpsilon(), sub);
      }
      // take the difference of with the next object
      else {  
        indices = mImage.hybridDifference(mValue, region.getIndicesList(), getEpsilon(), sub);
      }
      Log.i(TAG, "Compliment took " + (System.currentTimeMillis() - startTime)/1000f + " seconds");
    }

    return indices;
  }

}
