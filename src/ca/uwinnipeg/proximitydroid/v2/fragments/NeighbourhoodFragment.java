/**
 * 
 */
package ca.uwinnipeg.proximitydroid.v2.fragments;

import java.util.HashMap;
import java.util.Map;

import ca.uwinnipeg.proximitydroid.v2.Region;
import ca.uwinnipeg.proximitydroid.v2.services.NeighbourhoodService;

/**
 * Displays the results of {@link NeighbourhoodService}.
 * @author Garrett Smith
 *
 */
public class NeighbourhoodFragment extends EpsilonPropertyFragment<NeighbourhoodService> {

  protected Map<Region, int[]> mNeighbourhoods = new HashMap<Region, int[]>();
  
  public NeighbourhoodFragment() {
    super(
        NeighbourhoodService.class, 
        NeighbourhoodService.CATEGORY, 
        NeighbourhoodService.EPSILON_KEY);
  }
  
  @Override
  protected void onPropertyServiceAvailable(NeighbourhoodService service) {
    mNeighbourhoods = service.getNeighbourhoods();
  }
  
  @Override
  protected void draw() {
    if (mView != null) {
      super.draw();
      
      mView.clearHighlight();
      for (Region reg : mNeighbourhoods.keySet()) {
        int[] points = mNeighbourhoods.get(reg);
        if (points != null) {
          mView.addHighlight(points);
        }
      }
    }
  }
  
  @Override
  public void invalidate() {
    if (mView != null) mView.setDrawCenterPoint(true);
    super.invalidate();
  }
  
  @Override
  protected void onValueChanged(Region region, int[] points) {
    mNeighbourhoods.put(region, points);
  }
  
  @Override
  protected void onRegionsCleared() {
    mNeighbourhoods.clear();
  }
}
