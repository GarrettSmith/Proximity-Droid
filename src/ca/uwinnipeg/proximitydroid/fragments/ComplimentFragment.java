/**
 * 
 */
package ca.uwinnipeg.proximitydroid.fragments;

import ca.uwinnipeg.proximitydroid.services.ComplimentService;


/**
 * @author Garrett Smith
 *
 */
// TODO: don't darken unselected area
public class ComplimentFragment extends PropertyFragment<ComplimentService> {
  
  protected int[] mPoints;
  
  public ComplimentFragment() {
    super(ComplimentService.class, ComplimentService.CATEGORY);
  }
  
  @Override
  protected void onPropertyServiceAvailable(ComplimentService service) {
    super.onPropertyServiceAvailable(service);
    mPoints = service.getCompliment();
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
