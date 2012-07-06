/**
 * 
 */
package ca.uwinnipeg.proximitydroid.fragments;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import ca.uwinnipeg.proximitydroid.Region;
import ca.uwinnipeg.proximitydroid.services.PropertyService;
import ca.uwinnipeg.proximitydroid.services.ProximityService;

/**
 * A fragment that displays the results from a {@link PropertyService}.
 * @author Garrett Smith
 *
 */
public abstract class PropertyFragment<S extends PropertyService> extends RegionFragment {
  
  // the class of the property service
  protected Class<S> mServiceClass;
  
  // the category of broadcasts we are interested in
  protected String mCategory;
  
  // receives broadcasts from the property service we are interseted in about progress changes and 
  // results
  protected BroadcastReceiver mPropertyReciever = new PropertyFragmentReceiver();
  
  // the intent filter used to filter broadcasts we are interseted in
  protected IntentFilter mFilter;
  
  // the current progress of the service's calculation
  protected int mProgress;
  
  // the attached propertyService
  protected S mPropertyService;
  
  /**
   * Creates a new property fragment.
   * @param clazz the class of {@link PropertyService} we are interested in
   * @param category the category of broadcasts we are interested in
   */
  public PropertyFragment(Class<S> clazz, String category) {
    mServiceClass = clazz;
    mCategory = category;
    mFilter = new IntentFilter();
  }
  
  /**
   * Creates a new property fragment with a custom filter.
   * @param clazz the class of {@link PropertyService} we are interested in
   * @param category the category of broadcasts we are interested in
   * @param filter the filter containing the custom filtering rules
   */
  public PropertyFragment(Class<S> clazz, String category, IntentFilter filter) {
    mServiceClass = clazz;
    mCategory = category;
    mFilter = filter;
  }
  
  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    IntentFilter filter = new IntentFilter(mFilter);
    filter.addAction(PropertyService.ACTION_PROGRESS_CHANGED);
    filter.addAction(PropertyService.ACTION_VALUE_CHANGED);    
    filter.addAction(ProximityService.ACTION_REGIONS_CLEARED);    
    filter.addCategory(mCategory);
    mBroadcastManager.registerReceiver(mPropertyReciever, filter);
  }
  
  @Override
  protected void draw() {
    // don't fade unselected areas
    mView.setDim(false);
    super.draw();
  }
  
  @Override
  public void onStart() {
    super.onStart();
    // write the current progress
    updateProgress(getActivity());
  }
  
  @Override
  public void onDestroy() {
    mBroadcastManager.unregisterReceiver(mPropertyReciever);
    mPropertyService = null;
    super.onDestroy();
  }
  
  @SuppressWarnings("unchecked")
  @Override
  protected void onServiceAttached(ProximityService service) {
    super.onServiceAttached(service);
    mPropertyService = (S) service.getPropertyService(mServiceClass);
    onPropertyServiceAvailable(mPropertyService);
  }
  
  /**
   * Callback for when the property service we are interested in available to us.
   * @param service
   */
  protected void onPropertyServiceAvailable(S service) {
    // get the current progress
    setProgress(service.getProgress());
  }
  
  /**
   * Returns the attached {@link PropertyService}.
   * @return
   */
  public S getPropertyService() {
    return mPropertyService;
  }
  
  /**
   * Sets the current progress.
   * @param progress
   */
  protected void setProgress(int progress) {
    mProgress = progress;
    Activity activity = getActivity();
    updateProgress(activity);
  }
  
  /**
   * Updates the progress display of the attached activity.
   * @param activity
   */
  protected void updateProgress(Activity activity) {
    if (activity != null && isVisible()) {
      activity.setProgressBarVisibility(true);
      activity.setProgress(mProgress);
    }    
  }

  /**
   * Receives broadcasts about progress changes and results.
   * @author Garrett Smith
   *
   */
  protected class PropertyFragmentReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
      onRecieve(context, intent);
    }
      
  }
  
  // callbacks
  /**
   * Handles receiving broadcasts from the {@link PropertyService} and passes the relevant 
   * information to the callbacks.
   * @param context
   * @param intent
   */
  protected void onRecieve(Context context, Intent intent) {
    String action = intent.getAction();
    if (action.equals(PropertyService.ACTION_VALUE_CHANGED)) {
      Region reg = intent.getParcelableExtra(PropertyService.REGION);
      int[] points = intent.getIntArrayExtra(PropertyService.POINTS);
      if (reg == null) {
        onValueChanged(points);
      }
      else {
        onValueChanged(reg, points);
      }
      invalidate();
    }
    else if (action.equals(PropertyService.ACTION_PROGRESS_CHANGED)) {
      int progress = intent.getIntExtra(PropertyService.PROGRESS, 0);
      onProgressChanged(progress);
    }
    else if (action.equals(ProximityService.ACTION_REGIONS_CLEARED)) {
      onRegionsCleared();
      invalidate();
    }
  }
  
  /**
   * Called when the value for a particular region has changed.
   * @param region
   * @param points
   */
  protected void onValueChanged(Region region, int[] points) {} 
  
  /**
   * Called when the general value for the property has changed.
   * @param points
   */
  protected void onValueChanged(int[] points) {};
  
  /**
   * Called when the regions have been cleared.
   */
  protected void onRegionsCleared() {};
  
  /**
   * Called when the current progress has changed.
   * @param progress
   */
  protected void onProgressChanged(int progress) {
    setProgress(progress);
  }
  
}
