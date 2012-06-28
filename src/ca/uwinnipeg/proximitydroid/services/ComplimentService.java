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
// TODO: take compliment of all regions
public class ComplimentService extends LinearService {
  
  public static final String TAG = "PropertyService";
  
  public static final String CATEGORY = "Compliment";
  
  public ComplimentService() {
    super(CATEGORY);
  }
  
  public int[] getCompliment() {
    return indicesToPoints(getValue());
  }

  @Override
  protected List<Integer> calculateProperty(Region region, PerceptualSystemSubscriber sub) {
    // check if we should stop because the task was cancelled
    if (sub.isCancelled()) return null;

    List<Integer> indices = new ArrayList<Integer>();
    long startTime = System.currentTimeMillis();
    indices = mImage.getDescriptiveComplimentIndices(region.getIndicesList(), sub);
    Log.i(TAG, "Compliment took " + (System.currentTimeMillis() - startTime)/1000f + " seconds");

    return indices;
  }

}
