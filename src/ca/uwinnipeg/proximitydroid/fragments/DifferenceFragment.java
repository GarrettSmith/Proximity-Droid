/**
 * 
 */
package ca.uwinnipeg.proximitydroid.fragments;

import ca.uwinnipeg.proximitydroid.services.DifferenceService;

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
    super.draw();
    if (mPoints != null) {
      mView.setHighlight(mPoints);
    }
    else {
      mView.clearHighlight();
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
