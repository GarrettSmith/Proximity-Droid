/**
 * 
 */
package ca.uwinnipeg.proximitydroid.fragments;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import ca.uwinnipeg.proximity.PerceptualSystem;
import ca.uwinnipeg.proximity.image.Image;
import ca.uwinnipeg.proximity.image.Pixel;
import ca.uwinnipeg.proximitydroid.Region;

/**
 * @author Garrett Smith
 *
 */
public class DescBasedNeighbourhoodFragment extends RegionShowFragment {
  
  @Override
  protected List<Pixel> getRelevantPixels(
      List<Region> regions, Image image, PerceptualSystem<Pixel> system) {
    List<Pixel> pixels = new ArrayList<Pixel>();
    for (Region r : regions) {
      Pixel center = r.getCenterPixel(image);
      Set<Pixel> regionPixels = r.getPixels(image);
      Set<Pixel> neighbourHood = system.getDescriptionBasedNeighbourhood(center, regionPixels);
      pixels.addAll(neighbourHood);
    }
    return pixels;
  }
}
