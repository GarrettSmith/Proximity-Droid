package ca.uwinnipeg.proximitydroid.v2.services;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;
import ca.uwinnipeg.proximity.PerceptualSystem.PerceptualSystemSubscriber;
import ca.uwinnipeg.proximitydroid.v2.Region;

public class UpperApproxService extends LinearService {
	  
	  public static final String TAG = "UpperApproxService";
	  
	  public static final String CATEGORY = "UpperApprox";

	  public UpperApproxService() {
	    super(CATEGORY);
	  }

	  /**
	   * Returns the points of the pixels left after the upper approx operations.
	   * @return
	   */
	  public int[] getUpperApprox() {
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
		        indices = flatten(mImage.equivalenceClasses(region.getIndicesList(), sub));
		      }
		      // take the difference of with the next object
		      else {  
//		        indices = mImage.hybridDifference(mValue, getIndices(region), getEpsilon(), sub);
		      }
		      Log.i(TAG, "Compliment took " + (System.currentTimeMillis() - startTime)/1000f + " seconds");
		    }

		    return indices;
	  }
	  
	  private List<Integer> flatten(List<List<Integer>> orig) {
		  List<Integer> result = new ArrayList<Integer>();
		  for (List<Integer> l : orig) {
			  result.add(-1);
			  result.addAll(l);
		  }
		  return result;
	  }
}
