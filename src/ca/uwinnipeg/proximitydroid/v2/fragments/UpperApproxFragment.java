package ca.uwinnipeg.proximitydroid.v2.fragments;

import ca.uwinnipeg.proximitydroid.v2.services.UpperApproxService;

public class UpperApproxFragment extends PropertyFragment<UpperApproxService> {
	  
	  protected int[] mPoints;

	  public UpperApproxFragment() {
	    super(
	    	UpperApproxService.class, 
	    	UpperApproxService.CATEGORY);
	  }
	  
	  @Override
	  protected void onPropertyServiceAvailable(UpperApproxService service) {
	    super.onPropertyServiceAvailable(service);
	    mPoints = service.getUpperApprox();
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
