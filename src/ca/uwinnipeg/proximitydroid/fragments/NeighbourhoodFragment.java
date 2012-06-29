/**
 * 
 */
package ca.uwinnipeg.proximitydroid.fragments;

import java.util.HashMap;
import java.util.Map;

import ca.uwinnipeg.proximitydroid.Region;
import ca.uwinnipeg.proximitydroid.services.NeighbourhoodService;

/**
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
    super.draw();
    
    mView.clearHighlight();
    for (Region reg : mNeighbourhoods.keySet()) {
      int[] points = mNeighbourhoods.get(reg);
      if (points != null) {
        mView.addHighlight(points);
      }
    }
  }
  
  @Override
  public void invalidate() {
    mView.setDrawCenterPoint(true);
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
