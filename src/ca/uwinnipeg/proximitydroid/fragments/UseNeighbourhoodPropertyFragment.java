/**
 * 
 */
package ca.uwinnipeg.proximitydroid.fragments;

import android.content.IntentFilter;
import ca.uwinnipeg.proximitydroid.R;
import ca.uwinnipeg.proximitydroid.services.PropertyService;
import ca.uwinnipeg.proximitydroid.services.ProximityService;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

/**
 * A fragment that uses to different {@link PropertyService}s. One that uses neighbourhoods to 
 * calculate the result and another that does not.
 * @author Garrett Smith
 *
 */
public abstract class UseNeighbourhoodPropertyFragment<S extends PropertyService, N extends S> 
  extends EpsilonPropertyFragment<S> {
  
  //true if we are using neighbourhoods to calculate 
  protected boolean mUseNeighbourhoods = false;
  
  protected Class<N> mWithClass;
  protected Class<S> mWithoutClass;
  
  protected String mWithCategory;
  protected String mWithoutCategory;

  public UseNeighbourhoodPropertyFragment(
      Class<S> withoutClass, 
      String withoutCategory, 
      Class<N> withClass,
      String withCategory,
      IntentFilter filter,
      String epsilonKey) {
    super(withoutClass, withoutCategory, filter, epsilonKey);
    mWithClass = withClass;
    mWithCategory = withCategory;
    mWithoutClass = withoutClass;
    mWithoutCategory = withoutCategory;
  } 
  
  public UseNeighbourhoodPropertyFragment(
      Class<S> withoutClass, 
      String withoutCategory, 
      Class<N> withClass,
      String withCategory,
      String epsilonKey) {
    super(withoutClass, withoutCategory, epsilonKey);
    mWithClass = withClass;
    mWithCategory = withCategory;
    mWithoutClass = withoutClass;
    mWithoutCategory = withoutCategory;
  } 
  
  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.use_neighbourhood, menu);
    // set the checked status
    MenuItem item = menu.findItem(R.id.menu_use_neighbourhood);
    item.setChecked(mUseNeighbourhoods);
    super.onCreateOptionsMenu(menu, inflater);
  }
  
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == R.id.menu_use_neighbourhood) {
      boolean status = !mUseNeighbourhoods;
      item.setChecked(status);
      useNeighbourhoods(status);
      return true;
    }
    else {
      return super.onOptionsItemSelected(item);
    }
  }
  
  /**
   * Sets whether we should display the results of using neighbourhods to create the intersections 
   * or not.
   * @param enabled
   */
  public void useNeighbourhoods(boolean enabled) {
    // only switch if we are given a different value.
    if (mUseNeighbourhoods != enabled) {
      
      mUseNeighbourhoods = enabled;
      
      if (enabled) {
        mCategory = mWithCategory; 
        mServiceClass = mWithClass;
      }
      else {
        mCategory = mWithoutCategory;    
        mServiceClass = mWithoutClass;    
      }

      // unregister receiver
      mBroadcastManager.unregisterReceiver(mPropertyReciever);

      // register receiver with new filter
      IntentFilter filter = new IntentFilter(mBaseFilter);
      filter.addAction(PropertyService.ACTION_PROGRESS_CHANGED);
      filter.addAction(PropertyService.ACTION_VALUE_CHANGED);    
      filter.addAction(ProximityService.ACTION_REGIONS_CLEARED);    
      filter.addCategory(mCategory);
      mBroadcastManager.registerReceiver(mPropertyReciever, filter);

      // setup with new service
      onServiceAttached(getService());
      
      // redraw
      invalidate();
    }
  }
  
  /**
   * Returns whether we are displaying results using the neighbourhoods.
   * @return
   */
  public boolean isUsingNeighbourhoods() {
    return mUseNeighbourhoods;
  }

}
