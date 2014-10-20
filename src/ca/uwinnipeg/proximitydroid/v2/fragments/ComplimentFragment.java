/**
 * 
 */
package ca.uwinnipeg.proximitydroid.v2.fragments;

import ca.uwinnipeg.proximitydroid.v2.services.ComplimentService;
import ca.uwinnipeg.proximitydroid.v2.services.NeighbourhoodComplimentService;

/**
 * Displays the results of the {@link ComplimentService}.
 * @author Garrett Smith
 *
 */
public class ComplimentFragment 
  extends UseNeighbourhoodPropertyFragment<ComplimentService, NeighbourhoodComplimentService> {
  
  protected int[] mPoints;
  
  public ComplimentFragment() {
    super(
        ComplimentService.class, 
        ComplimentService.CATEGORY, 
        NeighbourhoodComplimentService.class, 
        NeighbourhoodComplimentService.CATEGORY, 
        ComplimentService.EPSILON_KEY);
  }
  
  @Override
  protected void onPropertyServiceAvailable(ComplimentService service) {
    super.onPropertyServiceAvailable(service);
    mPoints = service.getCompliment();
  }

  @Override
  protected void draw() {
    if (mView != null) {
      super.draw();
      if (mPoints != null) {
        mView.setHighlight(mPoints);
      }
      else {
        mView.clearHighlight();
      }      
    }
  }
  
  @Override
  protected void onValueChanged(int[] points) {
    mPoints = points;
  }
  
  @Override
  protected void onRegionsCleared() {
    mPoints = null;
  }
}
