/**
 * 
 */
package ca.uwinnipeg.proximitydroid.fragments;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import android.util.Log;
import ca.uwinnipeg.proximity.PerceptualSystem;
import ca.uwinnipeg.proximity.image.Image;
import ca.uwinnipeg.proximity.image.Pixel;
import ca.uwinnipeg.proximitydroid.Region;

/**
 * @author Garrett Smith
 *
 */
public class HybridNeighbourhoodFragment extends RegionShowFragment {
  
  @Override
  protected List<Pixel> getRelevantPixels(
      List<Region> regions, Image image, PerceptualSystem<Pixel> system) {
    List<Pixel> pixels = new ArrayList<Pixel>();
    for (Region r : regions) {
      Pixel center = r.getCenterPixel(image);
      Set<Pixel> regionPixels = r.getPixels(image);
      Set<Pixel> nhPixels = system.getHybridNeighbourhood(center, regionPixels, 0.1);
      Log.i("Hello!", "Count: " + nhPixels.size());
      pixels.addAll(nhPixels);
    }
    return pixels;
  }
}
