/**
 * 
 */
package ca.uwinnipeg.proximitydroid.fragments;

import java.util.ArrayList;
import java.util.List;

import ca.uwinnipeg.proximity.image.Image;
import ca.uwinnipeg.proximitydroid.Region;

/**
 * @author Garrett Smith
 *
 */
// TODO: Make epsilon selectable
public class NeighbourhoodFragment extends RegionShowFragment {
  
  @Override
  protected List<Integer> getRelevantPixels(List<Region> regions, Image image) {
    List<Integer> pixels = new ArrayList<Integer>();
    for (Region r : regions) {
      int center = r.getCenterIndex(image);
      int[] regionPixels = r.getIndices(image);
      List<Integer> nhPixels = image.getHybridNeighbourhoodIndices(center, regionPixels, 0.1);
      pixels.addAll(nhPixels);
    }
    return pixels;
  }
}
