/**
 * 
 */
package ca.uwinnipeg.proximitydroid.fragments;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;
import ca.uwinnipeg.proximity.image.Image;
import ca.uwinnipeg.proximitydroid.Region;

/**
 * @author Garrett Smith
 *
 */
// TODO: Make epsilon configurable
public class IntersectFragment extends RegionShowFragment {

  @Override
  protected List<Integer> getRelevantPixels(List<Region> regions, Image image) {

    List<Integer> pixels = new ArrayList<Integer>();
    
    // only bother unless we have at least two regions to use
    if (regions.size() >= 2) {
      List<int[]> regionsIndices = new ArrayList<int[]>();

      for (Region r : regions) {
        regionsIndices.add(r.getIndices(image));
      }

      //pixels.addAll(system.getHybridIntersectObjects(pixelSets, 0.1));
      long startTime = System.currentTimeMillis();
      pixels = (image.getDescriptionBasedIntersectIndices(regionsIndices.get(0), regionsIndices.get(1)));
      Log.i("IntersectionFragment", "Intersection took " + ((System.currentTimeMillis() - startTime) / 1000.0) + " seconds");
    }

    return pixels;
  }
}
