/**
 * 
 */
package ca.uwinnipeg.proximitydroid.v2.fragments;

import ca.uwinnipeg.proximitydroid.v2.services.DifferenceService;

/**
 * Displays the results of the {@link DifferenceService}.
 * @author Garrett Smith
 *
 */
public class DifferenceFragment extends EpsilonPropertyFragment<DifferenceService> {
  
  protected int[] mPoints;

  public DifferenceFragment() {
    super(DifferenceService.class, DifferenceService.CATEGORY, DifferenceService.EPSILON_KEY);
  }
  
  @Override
  protected void onPropertyServiceAvailable(DifferenceService service) {
    super.onPropertyServiceAvailable(service);
    mPoints = service.getDifference();
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
