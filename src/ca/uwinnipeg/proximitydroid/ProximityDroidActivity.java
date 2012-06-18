/**
 * 
 */
package ca.uwinnipeg.proximitydroid;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceScreen;
import android.provider.MediaStore.Images;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.SpinnerAdapter;
import ca.uwinnipeg.proximitydroid.fragments.ImageFragment.ProximityServiceProvider;
import ca.uwinnipeg.proximitydroid.fragments.IntersectionFragment;
import ca.uwinnipeg.proximitydroid.fragments.PreferenceListFragment.OnPreferenceAttachedListener;
import ca.uwinnipeg.proximitydroid.fragments.NeighbourhoodFragment;
import ca.uwinnipeg.proximitydroid.fragments.RegionSelectFragment;
import ca.uwinnipeg.proximitydroid.fragments.RegionSelectFragment.ListNavigationProvider;
import ca.uwinnipeg.proximitydroid.fragments.RegionSelectFragment.OnClosedListener;
import ca.uwinnipeg.proximitydroid.fragments.RegionShowFragment;
import ca.uwinnipeg.proximitydroid.fragments.RegionShowFragment.OnAddRegionSelectedListener;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.OnNavigationListener;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;

/**
 * @author Garrett Smith
 *
 */

//TODO: find more efficient way to draw points
//TODO: make progress output scale with multiple tasks
//TODO: clear progress bar when switching views
public class ProximityDroidActivity 
  extends SherlockFragmentActivity 
  implements OnPreferenceAttachedListener,
             OnAddRegionSelectedListener, 
             OnNavigationListener,
             ListNavigationProvider,
             OnClosedListener,
             ProximityServiceProvider {

  public static final String TAG = "ProximityDroidActivity";
  
  // UI
  private FragmentManager mFragmentManager;
  protected ActionBar mActionBar;
  protected SpinnerAdapter mSpinnerAdapter;  
  protected PreferenceScreen mPreferenceScreen;

  // true if we are on a small screen devices
  protected boolean mSmallScreen;
  
  // All the view modes
  public enum ViewMode { 
    VIEW_REGIONS, 
    VIEW_NEIGHBOURHOODS, 
    VIEW_INTERSECTION, 
    SELECT_REGION, 
    EDIT_REGION };
    
  // The current mode
  protected ViewMode mMode;
  
  // Constants
  
  // Used to read the positions of the view mode spinner
  public static final int LIST_SHOW_INDEX = 0;
  public static final int LIST_NEIGHBOURHOOD_INDEX = 1;
  public static final int LIST_INTERSECTION_INDEX = 2;

  // bundle keys
  protected static final String BUNDLE_KEY_MODE = "Mode";
  
  // Service connection
  
  // The service we are bound to
  private ProximityService mService;
  
  @Override
  public ProximityService getService() {
    return mService;
  }

  // Whether we are currently bound
  protected boolean mBound = false;
  
  /**
   * Callbacks for binding the service.
   */
  private ServiceConnection mConnection = new ServiceConnection() {
    
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
      @SuppressWarnings("unchecked")
      LocalBinder<ProximityService> binder = (LocalBinder<ProximityService>) service;
      mService = binder.getService();
      mBound = true;
      
      Log.i(TAG, "Binding service");
      
      // check if we need to start an activity to load a bitmap
      if (!mService.hasBitmap()) {
        Intent i = new Intent(Intent.ACTION_PICK, Images.Media.INTERNAL_CONTENT_URI);
        startActivityForResult(i, REQUEST_CODE_SELECT_IMAGE);
      }
      
      // Check if we need to populate the preference screen
      if (mPreferenceScreen != null) {
        mService.populatePreferences(mPreferenceScreen);
      }
    }
    
    @Override
    public void onServiceDisconnected(ComponentName name) {
      mBound = false;
    }
  };
  
  // Intents
  private static final int REQUEST_CODE_SELECT_IMAGE = 0;

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch(requestCode) {
      case REQUEST_CODE_SELECT_IMAGE:
        if (resultCode == Activity.RESULT_OK) {
          mService.setBitmap(data.getData());
        }
    }
  }

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);    
    
    // to display progress
    requestWindowFeature(Window.FEATURE_PROGRESS);    
    
    setContentView(R.layout.main);
    
    mFragmentManager = getSupportFragmentManager();

    mSmallScreen = findViewById(R.id.main_layout) == null;
    
    // setup the spinner
    mActionBar = getSupportActionBar();
    mSpinnerAdapter = ArrayAdapter.createFromResource(
        this, 
        R.array.operations_list, 
        R.layout.sherlock_spinner_dropdown_item);

    mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);   
    resetListNavigationCallbacks();
    
    // hide features if we are on a small screen
    if (mSmallScreen) {
      Fragment frag = mFragmentManager.findFragmentById(R.id.feature_fragment);
      mFragmentManager.beginTransaction()
      .hide(frag)
      .commit();
    }    

    // bind to service
    Intent intent = new Intent(this, ProximityService.class);
    bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
  }
  
  @Override
  protected void onDestroy() {
    // release the service
    if (mBound) {
      unbindService(mConnection);
      mBound = false;
    }
    super.onDestroy();
  }
  
  @Override
  protected void onSaveInstanceState(Bundle state) {
    // save the current view mode
    if (mMode != null) {
      state.putString(BUNDLE_KEY_MODE, mMode.name());
    }
    super.onSaveInstanceState(state);
  }
  
  @Override
  protected void onRestoreInstanceState(Bundle savedInstanceState) {
    super.onRestoreInstanceState(savedInstanceState);
    //restore the view mode
    String modeName = savedInstanceState.getString(BUNDLE_KEY_MODE);
    setViewMode(ViewMode.valueOf(modeName));
  }
  
  // the previous position in the navigation list, used to restore view after adding a region
  protected int mPreviousPosition = LIST_SHOW_INDEX;

  @Override
  public void setListNavigationCallbacks(
      SpinnerAdapter adapter,
      OnNavigationListener listener) {
    mActionBar.setListNavigationCallbacks(adapter, listener);
  }

  @Override
  public void resetListNavigationCallbacks() {
    mActionBar.setListNavigationCallbacks(mSpinnerAdapter, this);
    mActionBar.setSelectedNavigationItem(mPreviousPosition);
  }

  @Override
  public boolean onNavigationItemSelected(int itemPosition, long itemId) {
    switch(itemPosition) {
      case LIST_SHOW_INDEX:
        setViewMode(ViewMode.VIEW_REGIONS);
        break;
        
      case LIST_NEIGHBOURHOOD_INDEX:
        setViewMode(ViewMode.VIEW_NEIGHBOURHOODS);
        break;

      case LIST_INTERSECTION_INDEX:
        setViewMode(ViewMode.VIEW_INTERSECTION);
        break;
    }
    return true;
  }
  
  protected void setViewMode(ViewMode newMode) {
    boolean changed = mMode != newMode;
    mMode = newMode;
    
    if (changed) {
      
      FragmentTransaction trans = mFragmentManager.beginTransaction();

      Fragment frag;
      switch(mMode) {
        case VIEW_INTERSECTION:
          frag = new IntersectionFragment();
          break;
        case VIEW_NEIGHBOURHOODS:
          frag = new NeighbourhoodFragment();
          break;
        default:
          frag = new RegionShowFragment();
          break;
      }

      trans.replace(R.id.image_fragment_container, frag);
      trans.commit();
    }
    
    // TODO: update loading spinner
    //setProgressBarVisibility(isLoading());
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getSupportMenuInflater();
    inflater.inflate(R.menu.main, menu);
    inflater.inflate(R.menu.features_select, menu);
    //updateToggleText(menu.findItem(R.id.menu_features), mProbeFrag.isVisible());
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menu_about:
        showAbout();
        return true;
      case R.id.menu_features:
        toggleFeatures(item);
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }
  
  private void toggleFeatures(MenuItem item) {
    
    // TODO: disable buttons on small screen
    
    Fragment fragment = mFragmentManager.findFragmentById(R.id.feature_fragment);
    
    boolean show = fragment.isHidden();
    
    FragmentTransaction transaction = mFragmentManager.beginTransaction();
    transaction.setCustomAnimations(
        R.anim.slide_in, 
        R.anim.slide_out, 
        R.anim.slide_in, 
        R.anim.slide_out);
    // toggle hiding probe frag
    if (show) {
      if (mSmallScreen) {
        transaction.addToBackStack(null);
      }
      transaction.show(fragment);
    }
    else {      
      if (mSmallScreen) {
      }      
      transaction.hide(fragment);      
    }
    transaction.commit();
    updateToggleText(item, show);
  }

  private void updateToggleText(MenuItem item, Boolean shown) {
    int text = shown ? R.string.menu_hide_features : R.string.menu_show_features;
    item.setTitle(text);    
  }

  /**
   * Displays the about dialog.
   */
  public void showAbout() {
    Intent i = new Intent(this, AboutActivity.class);
    startActivity(i);
  }

  /**
   * Change to the select region fragment
   */
  public void onAddRegionSelected() {

    // we will need the options menu redrawn
    invalidateOptionsMenu();    

    // to restore to the proper view later
    mPreviousPosition = mActionBar.getSelectedNavigationIndex();

    // swap the select fragment in
    mFragmentManager.beginTransaction()
    .replace(R.id.image_fragment_container, new RegionSelectFragment())
    .addToBackStack(null)
    .commit();
  }

  @Override
  public void onClosed() {
    mFragmentManager.popBackStack();
  }
  
  protected boolean isLoading() {
    boolean loading = false;    
    return loading;
  }
  
  @Override
  public void onPreferenceAttached(PreferenceScreen root, int xmlId) {
    if(root == null) return; //for whatever reason in very rare cases this is null   
    
    // store the preference screen so we can add to it when the service is bound
    mPreferenceScreen = root;
  }

}
