/**
 * 
 */
package ca.uwinnipeg.proximitydroid.fragments;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;
import ca.uwinnipeg.proximity.PerceptualSystem;
import ca.uwinnipeg.proximity.image.Image;
import ca.uwinnipeg.proximity.image.Pixel;
import ca.uwinnipeg.proximitydroid.Region;

/**
 * @author Garrett Smith
 *
 */
// TODO: Make epsilon configurable
public class IntersectFragment extends RegionShowFragment {

  @Override
  protected List<Pixel> getRelevantPixels(List<Region> regions, Image image) {

    List<Pixel> pixels = new ArrayList<Pixel>();
    
    // only bother unless we have at least two regions to use
    if (regions.size() >= 2) {
      List<int[]> pixelSets = new ArrayList<int[]>();

      for (Region r : regions) {
        pixelSets.add(r.getIndices(image));
      }

      //pixels.addAll(system.getHybridIntersectObjects(pixelSets, 0.1));
      long startTime = System.currentTimeMillis();
      pixels.addAll(image.getDescriptionBasedIntersectObjects(pixelSets.get(0), pixelSets.get(1)));
      Log.i("IntersectionFragment", "Intersection took " + ((System.currentTimeMillis() - startTime) / 1000.0) + " seconds");
    }

    return pixels;
  }
}
