/**
 * 
 */
package ca.uwinnipeg.proximitydroid;

import java.util.List;
import java.util.Map;

import android.content.ContentResolver;
import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.widget.ArrayAdapter;
import android.widget.SpinnerAdapter;
import ca.uwinnipeg.proximity.ProbeFunc;
import ca.uwinnipeg.proximitydroid.fragments.FeatureSelectFragment;
import ca.uwinnipeg.proximitydroid.fragments.PreferenceListFragment.OnPreferenceAttachedListener;
import ca.uwinnipeg.proximitydroid.fragments.RegionSelectFragment.ListNavigationProvider;
import ca.uwinnipeg.proximitydroid.fragments.RegionShowFragment;
import ca.uwinnipeg.proximitydroid.fragments.RegionShowFragment.OnAddRegionSelecetedListener;

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
             OnAddRegionSelecetedListener, 
             OnNavigationListener,
             ListNavigationProvider {

  public static final String TAG = "ProximityDroidActivity";
  
  // UI
  private FragmentManager mFragmentManager;
  protected ActionBar mActionBar;
  protected SpinnerAdapter mSpinnerAdapter;

  // Fragments
  private FeatureSelectFragment mProbeFrag;  
  
  // The current view mode
  protected String mViewMode;
  
  // System

  // gets set to true if we are on a small screen device like a phone
  protected boolean mSmallScreen;  
  
  // Constants
  
  // Used to read the positions of the view mode spinner
  public static final int LIST_SHOW_INDEX = 0;
  public static final int LIST_NEIGHBOURHOOD_INDEX = 1;
  public static final int LIST_INTERSECT_INDEX = 2;
  
  // All the view modes
  public enum ViewMode { 
    VIEW_REGIONS, 
    VIEW_NEIGHBOURHOODS, 
    VIEW_INTERSECTION, 
    SELECT_REGION, 
    EDIT_REGION };
    
  public static final String SHOW_KEY = "Show";
  public static final String SELECT_KEY = "Select";
  public static final String NEIGHBOURHOOD_KEY = "Neighbourhood";
  public static final String INTERSECT_KEY = "Intersection";  
  public static final String FEATURE_KEY = "Features";

  // bundle keys
  protected static final String BUNDLE_KEY_PROBE_FRAG = "Probe Fragment";
  protected static final String BUNDLE_KEY_MODE = "Mode";

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
        android.R.layout.simple_spinner_dropdown_item);

    mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);   
    resetListNavigationCallbacks();

    // restore previous state
    if (state != null) {
      // restore fragments
      mProbeFrag = 
          (FeatureSelectFragment) mFragmentManager.getFragment(state, BUNDLE_KEY_PROBE_FRAG);

      // hide probe frag on small screens
//      if (mSmallScreen) {
//        mFragmentManager.beginTransaction()
//        .add(R.id.fragment_container, mProbeFrag)
//        .hide(mProbeFrag)
//        .commit();
//      }

    }
    // else we are starting for the first time
    else {

      // create the image fragment

      // add the fragments to the view
      mFragmentManager.beginTransaction()
      //.add(R.id.fragment_container, new RegionShowFragment())
      //.hide(mProbeFrag)
      .commit();
      
      // create for find the feature fragment based on screen size
      if (mSmallScreen) {
        mProbeFrag = new FeatureSelectFragment();
      }
      // Get fragments by their id
      else {
        mProbeFrag = 
            (FeatureSelectFragment) mFragmentManager.findFragmentById(R.id.probe_func_fragment);
      }    
    }
  }
  
  @Override
  protected void onSaveInstanceState(Bundle state) {
    // save the fragments  
    if (mProbeFrag.isAdded()) mFragmentManager.putFragment(state, BUNDLE_KEY_PROBE_FRAG, mProbeFrag);
    state.putString(BUNDLE_KEY_MODE, mViewMode);
    super.onSaveInstanceState(state);
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
        setViewMode(SHOW_KEY);
        break;
        
      case LIST_NEIGHBOURHOOD_INDEX:
        setViewMode(NEIGHBOURHOOD_KEY);
        break;

      case LIST_INTERSECT_INDEX:
        setViewMode(INTERSECT_KEY);
        break;
    }
    return true;
  }
  
  protected void setViewMode(String newMode) {
    boolean changed = mViewMode != newMode;
    mViewMode = newMode;
    
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
        toggleFeatures(item, (mProbeFrag.isHidden() || !mProbeFrag.isAdded()));
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }
  
  public void toggleFeatures(MenuItem item, boolean show) {
    FragmentTransaction transaction = mFragmentManager.beginTransaction();
    transaction.setCustomAnimations(
        R.anim.slide_in, 
        R.anim.slide_out, 
        R.anim.slide_in, 
        R.anim.slide_out);
    // toggle hiding probe frag
    if (show) {

      if (mSmallScreen) {
        transaction.add(R.id.fragment_container, mProbeFrag);
        transaction.addToBackStack(null);
      }
      transaction.show(mProbeFrag);

    }
    else {
      
      if (mSmallScreen) {
        transaction.remove(mProbeFrag);
      }
      transaction.hide(mProbeFrag);
      
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

    invalidateOptionsMenu();

    // swap the select fragment in
    FragmentTransaction transaction = mFragmentManager.beginTransaction();

    // slide if we are showing the features list with a small screen
    if (mSmallScreen) {
      transaction.remove(mProbeFrag);
      if (mProbeFrag.isVisible()) {
        transaction.setCustomAnimations(
            0, 
            R.anim.slide_out, 
            R.anim.slide_in, 
            android.R.anim.fade_out);
      }
      else {
        transaction.setCustomAnimations(
            android.R.anim.fade_in, 
            android.R.anim.fade_out, 
            android.R.anim.fade_in, 
            android.R.anim.fade_out);
      }
    }

    transaction
    // TODO: .replace(R.id.fragment_container, new RegionSelectFragment(), SELECT_KEY)
    .addToBackStack(null)
    .commit();
  }

  public void onRegionCanceled() {
    mFragmentManager.popBackStack();
  }
  
  protected boolean isLoading() {
    boolean loading = false;    
    return loading;
  }
  
  @Override
  public void onPreferenceAttached(PreferenceScreen root, int xmlId) {
    if(root == null) return; //for whatever reason in very rare cases this is null   

    // Load features
    Map<String, List<ProbeFunc<Integer>>> features;//TODO: = loadProbeFuncs();

    // Generate preference items from features    
    // generate a category for each given category    
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
//
//          // TODO: pref.setOnPreferenceChangeListener(this);
//          category.addPreference(pref);
//          
//          // add the ProbeFunc to our map to use later
//          // TODO: mProbeFuncs.put(key, func);
//        }
//      }
//    }
  }

}
