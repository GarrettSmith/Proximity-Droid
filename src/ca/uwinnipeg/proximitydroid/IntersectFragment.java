/**
 * 
 */
package ca.uwinnipeg.proximitydroid;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import ca.uwinnipeg.proximity.PerceptualSystem;
import ca.uwinnipeg.proximity.image.Image;
import ca.uwinnipeg.proximity.image.Pixel;
import ca.uwinnipeg.proximitydroid.fragments.RegionShowFragment;

/**
 * @author Garrett Smith
 *
 */
// TODO: Make epsilon configurable
public class IntersectFragment extends RegionShowFragment {

  @Override
  protected List<Pixel> getRelevantPixels(
      List<Region> regions, Image image, PerceptualSystem<Pixel> system) {

    List<Pixel> pixels = new ArrayList<Pixel>();
    
    // only bother unless we have at least two regions to use
    if (regions.size() >= 2) {
      List<Set<Pixel>> pixelSets = new ArrayList<Set<Pixel>>();

      for (Region r : regions) {
        pixelSets.add(r.getPixels(image));
      }

      pixels.addAll(system.getHybridIntersectObjects(pixelSets, 0.1));
    }

    return pixels;
  }
}
