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
import ca.uwinnipeg.proximitydroid.fragments.PreferenceListFragment.OnPreferenceAttachedListener;
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
             OnClosedListener {

  public static final String TAG = "ProximityDroidActivity";
  
  // UI
  private FragmentManager mFragmentManager;
  protected ActionBar mActionBar;
  protected SpinnerAdapter mSpinnerAdapter;  

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
    // TODO: figure out proper flag to use
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
    state.putString(BUNDLE_KEY_MODE, mMode.name());
    super.onSaveInstanceState(state);
  }
  
  @Override
  protected void onRestoreInstanceState(Bundle savedInstanceState) {
    super.onRestoreInstanceState(savedInstanceState);
    //restore the view mode
    String modeName = savedInstanceState.getString(BUNDLE_KEY_MODE);
    setViewMode(ViewMode.valueOf(modeName));
  }

  @Override
  public void setListNavigationCallbacks(
      SpinnerAdapter adapter,
      OnNavigationListener listener) {
    mActionBar.setListNavigationCallbacks(adapter, listener);
    
  }

  @Override
  public void resetListNavigationCallbacks() {
    mActionBar.setListNavigationCallbacks(mSpinnerAdapter, this);
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

      Fragment frag = new RegionShowFragment(mService.getRegions(), mService.getBitmap());
      switch(mMode) {
        case VIEW_REGIONS:
          frag = new RegionShowFragment(mService.getRegions(), mService.getBitmap());
          break;
        case VIEW_INTERSECTION:
          break;
        case VIEW_NEIGHBOURHOODS:
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
   * Creates a new region and adds it to the image
   */
  public void onAddRegionSelected() {

    // we will need the options menu redrawn
    invalidateOptionsMenu();

    // swap the select fragment in
    mFragmentManager.beginTransaction()
    .replace(R.id.image_fragment_container, new RegionSelectFragment(mService.getBitmap()))
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
//
//    // Load features
//    Map<String, List<ProbeFunc<Integer>>> features = mService.getFeatures();//TODO: = loadProbeFuncs();
//
//    // Generate preference items from features    
//    // generate a category for each given category    
//    for (String catStr : features.keySet()) {
//      List<ProbeFunc<Integer>> funcs = features.get(catStr);
//
//      // only add the category if it is non empty
//      if (funcs != null && !funcs.isEmpty()) {
//        PreferenceCategory category = new PreferenceCategory(this);
//        category.setTitle(catStr);
//        category.setKey(catStr);
//        root.addPreference(category);
//
//        // generate a preference for each probe func
//        for (ProbeFunc<Integer> func : funcs) {
//          
//          Preference pref;
//          
//          // Use switches when supported
//          if (android.os.Build.VERSION.SDK_INT >= 14) {
//            pref = new SwitchPreference(this);
//          }
//          else {
//            pref = new CheckBoxPreference(this);
//          }
//
//          // Set name and key
//          String key = catStr + "_" + func.toString();
//          pref.setTitle(func.toString());
//          pref.setKey(key);
//          category.addPreference(pref);
//          
//          // register the service as a preference listener
//          pref.setOnPreferenceChangeListener(mService);
//          
//          // add the ProbeFunc to our map to use later
//          // TODO: mProbeFuncs.put(key, func);
//        }
//      }
//    }
  }

}
