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
 * @author Garrett Smith
 *
 */
public abstract class PropertyFragment<S extends PropertyService> extends RegionFragment {
  
  protected Class<S> mClass;
  
  protected String mCategory;
  
  protected BroadcastReceiver mPropertyReciever = new PropertyFragmentReceiver();
  
  protected IntentFilter mFilter;
  
  public PropertyFragment(Class<S> clazz, String category) {
    mClass = clazz;
    mCategory = category;
    mFilter = new IntentFilter();
  }
  
  public PropertyFragment(Class<S> clazz, String category, IntentFilter filter) {
    mClass = clazz;
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
  public void onDestroy() {
    mBroadcastManager.unregisterReceiver(mPropertyReciever);
    super.onDestroy();
  }
  
  @SuppressWarnings("unchecked")
  @Override
  protected void onServiceAttached(ProximityService service) {
    super.onServiceAttached(service);
    onPropertyServiceAvailable((S) service.getPropertyService(mClass));
  }
  
  protected void onPropertyServiceAvailable(S service) {}
  
  protected void setProgress(int progress) {
    Activity activity = getActivity();
    if (activity != null && isVisible()) {
      activity.setProgressBarVisibility(true);
      activity.setProgress(progress);
    }
  }

  protected class PropertyFragmentReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
      onRecieve(context, intent);
    }
      
  }
  
  // callbacks
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
  
  protected void onValueChanged(Region region, int[] points) {}
  protected void onValueChanged(int[] points) {};
  protected void onRegionsCleared() {};
  
  protected void onProgressChanged(int progress) {
    setProgress(progress);
  }
  
}
