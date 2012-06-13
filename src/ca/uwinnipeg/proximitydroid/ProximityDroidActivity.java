/**
 * 
 */
package ca.uwinnipeg.proximitydroid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.MediaStore.Images;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.SpinnerAdapter;
import ca.uwinnipeg.proximity.ProbeFunc;
import ca.uwinnipeg.proximity.image.AlphaFunc;
import ca.uwinnipeg.proximity.image.BlueFunc;
import ca.uwinnipeg.proximity.image.GreenFunc;
import ca.uwinnipeg.proximity.image.Image;
import ca.uwinnipeg.proximity.image.RedFunc;
import ca.uwinnipeg.proximitydroid.fragments.FeatureSelectFragment;
import ca.uwinnipeg.proximitydroid.fragments.PreferenceListFragment.OnPreferenceAttachedListener;
import ca.uwinnipeg.proximitydroid.fragments.RegionSelectFragment;
import ca.uwinnipeg.proximitydroid.fragments.RegionSelectFragment.ListNavigationProvider;
import ca.uwinnipeg.proximitydroid.fragments.RegionSelectFragment.OnRegionSelectedListener;
import ca.uwinnipeg.proximitydroid.fragments.RegionShowFragment;
import ca.uwinnipeg.proximitydroid.fragments.RegionShowFragment.OnAddRegionSelecetedListener;
import ca.uwinnipeg.proximitydroid.fragments.RegionShowFragment.RegionProvider;

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
// TODO: add enabled probe funcs
public class ProximityDroidActivity 
  extends SherlockFragmentActivity 
  implements OnPreferenceAttachedListener,
    OnPreferenceChangeListener,
    OnRegionSelectedListener,
    RegionProvider,
    OnAddRegionSelecetedListener, 
    OnNavigationListener,
    ListNavigationProvider {

  public static final String TAG = "ProximityDroidActivity";
  
  private ContentResolver mContentResolver;
  private FragmentManager mFragmentManager;
  protected ActionBar mActionBar;
  protected SpinnerAdapter mSpinnerAdapter;

  // The image we are working on
  protected RotatedBitmap mBitmap;

  // the uri of the image
  protected Uri mUri;
  
  // The image perceptual system used for the system logic
  

  // Fragments
  private RegionShowFragment mShowFrag;
  private FeatureSelectFragment mProbeFrag;  
  
  // Used to read the positions of the view mode spinner
  public static final int LIST_SHOW_INDEX = 0;
  public static final int LIST_NEIGHBOURHOOD_INDEX = 1;
  public static final int LIST_INTERSECT_INDEX = 2;

  // gets set to true if we are on a small screen device like a phone
  protected boolean mSmallScreen;

  protected List<Region> mRegions = new ArrayList<Region>();
  
  protected Map<String, ProbeFunc<Integer>> mProbeFuncs = new HashMap<String, ProbeFunc<Integer>>();
  
  // Stores the points to be highlighted for different view (neighbourhoods, intersection)
  protected Map<String, List<Integer>> mPoints = new HashMap<String, List<Integer>>();  

  // The current status of different operations
  protected Map<String, Boolean> mLoading = new HashMap<String, Boolean>();

  // calculates the new views
  protected Map<Region, NeighbourhoodTask> mNeighbourhoodTasks = 
      new HashMap<Region, NeighbourhoodTask>();
  
  protected IntersectTask mIntersectTask;
  
  // The current view mode
  protected String mViewMode;
  
  // All the view modes
  public static final String SHOW_KEY = "Show";
  public static final String SELECT_KEY = "Select";
  public static final String NEIGHBOURHOOD_KEY = "Neighbourhood";
  public static final String INTERSECT_KEY = "Intersection";  
  public static final String FEATURE_KEY = "Features";

  // Intents
  private static final int REQUEST_CODE_SELECT_IMAGE = 0;

  // bundle keys
  protected static final String BUNDLE_KEY_URI = "Uri";
  protected static final String BUNDLE_KEY_SHOW_FRAG = "Show Fragment";
  protected static final String BUNDLE_KEY_PROBE_FRAG = "Probe Fragment";

  @Override
  public List<Region> getRegions() {
    return mRegions;
  }

  @Override
  public Image getImage() {
    return mImage;
  }

  @Override
  public List<Integer> getIndices() {
    return mPoints.get(mViewMode);
  }

  @Override
  public RotatedBitmap getBitmap() {
    return mBitmap;
  }

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    
    // to display progress
    requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
    
    setContentView(R.layout.main);

    mContentResolver = getContentResolver();
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
      mShowFrag = 
          (RegionShowFragment) mFragmentManager.getFragment(state, BUNDLE_KEY_SHOW_FRAG);

      // hide probe frag on small screens
      if (mSmallScreen) {
        mFragmentManager.beginTransaction()
        .add(R.id.fragment_container, mProbeFrag)
        .hide(mProbeFrag)
        .commit();
      }

      // restore the bitmap
      mUri = (Uri)state.getParcelable(BUNDLE_KEY_URI);
      if (mUri != null) setupImage();

    }
    // else we are starting for the first time
    else {

      // create the image fragment
      mShowFrag = new RegionShowFragment();

      // add the fragments to the view
      mFragmentManager.beginTransaction()
      .add(R.id.fragment_container, mShowFrag)
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

    // request an image
    if (mUri == null) {
      Intent i = new Intent(Intent.ACTION_PICK, Images.Media.INTERNAL_CONTENT_URI);
      startActivityForResult(i, REQUEST_CODE_SELECT_IMAGE);
    }
  }
  
  @Override
  protected void onStart() {
    super.onStart();    
    // add enabled probe funcs
    SharedPreferences settings = null;
    for (String key : mProbeFuncs.keySet()) {
      // get the settings object the first time through
      if (settings == null) {
        settings = mProbeFrag.findPreference(key).getSharedPreferences();
      }
      // add enabled funcs
      if (settings.getBoolean(key, false)) {
        mImage.addProbeFunc(mProbeFuncs.get(key));
      }
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle state) {
    // save the fragments  
    if (mProbeFrag.isAdded()) mFragmentManager.putFragment(state, BUNDLE_KEY_PROBE_FRAG, mProbeFrag);
    if (mShowFrag.isAdded()) mFragmentManager.putFragment(state, BUNDLE_KEY_SHOW_FRAG, mShowFrag);
    // Save the bitmap
    if (mUri != null) {
      state.putParcelable(BUNDLE_KEY_URI, mUri);
    }
    super.onSaveInstanceState(state);
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    switch(requestCode) {
      case REQUEST_CODE_SELECT_IMAGE:
        if (resultCode == Activity.RESULT_OK) {
          mUri = data.getData();
          setupImage();
        }
        else {
          finish();
        }
        break;
    }
  }

  /**
   * Sets the image to be the selected image
   * @param data
   */
  private void setupImage() {    
    mBitmap = Util.loadImage(mUri, mContentResolver, getWindowManager());

    // make sure the image was loaded properly
    if (mBitmap != null) {
      Util.setImage(mImage, mBitmap.getBitmap());
    }
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
    
    // update the points if the mode changed
    if (changed) {
      mShowFrag.setPoints(getIndices());
    }    
    
    // update loading spinner
    Boolean loading = mLoading.get(mViewMode);
    if (loading == null) {
      mLoading.put(mViewMode, false);
      loading = false;
    }
    setProgressBarIndeterminateVisibility(loading);
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
    .replace(R.id.fragment_container, new RegionSelectFragment(mBitmap), SELECT_KEY)
    .addToBackStack(null)
    .commit();
  }

  public void onRegionSelected(Region region) { 
    mRegions.add(region);
    runUpdate();
    onRegionCanceled();
  }

  public void onRegionCanceled() {
    mFragmentManager.popBackStack();
  }

  @Override
  public void onPreferenceAttached(PreferenceScreen root, int xmlId) {
    if(root == null) return; //for whatever reason in very rare cases this is null   

    // Load features
    Map<String, List<ProbeFunc<Integer>>> features = loadProbeFuncs();

    // Generate preference items from features    
    // generate a category for each given category    
    for (String catStr : features.keySet()) {
      List<ProbeFunc<Integer>> funcs = features.get(catStr);

      // only add the category if it is non empty
      if (funcs != null && !funcs.isEmpty()) {
        PreferenceCategory category = new PreferenceCategory(this);
        category.setTitle(catStr);
        category.setKey(catStr);
        root.addPreference(category);

        // generate a preference for each probe func
        for (ProbeFunc<Integer> func : funcs) {
          
          Preference pref;
          
          // Use switches when supported
          if (android.os.Build.VERSION.SDK_INT >= 14) {
            pref = new SwitchPreference(this);
          }
          else {
            pref = new CheckBoxPreference(this);
          }

          // Set name and key
          String key = catStr + "_" + func.toString();
          pref.setTitle(func.toString());
          pref.setKey(key);

          pref.setOnPreferenceChangeListener(this);
          category.addPreference(pref);
          
          // add the ProbeFunc to our map to use later
          mProbeFuncs.put(key, func);
        }
      }
    }
  }

  private Map<String, List<ProbeFunc<Integer>>> loadProbeFuncs() {
    Map<String, List<ProbeFunc<Integer>>> features = new HashMap<String, List<ProbeFunc<Integer>>>();
    // load all the standard probe funcs
    features.put("Colour", new ArrayList<ProbeFunc<Integer>>());
    List<ProbeFunc<Integer>> colourFuncs = features.get("Colour");
    colourFuncs.add(new AlphaFunc());
    colourFuncs.add(new RedFunc());
    colourFuncs.add(new GreenFunc());
    colourFuncs.add(new BlueFunc());

    // TODO: load probe funcs from external storage
    return features;
  }

  @Override
  public boolean onPreferenceChange(Preference preference, Object newValue) {
    if (newValue instanceof Boolean) {
      // add or remove the probe func from the perceptual system
      ProbeFunc<Integer> func = mProbeFuncs.get(preference.getKey());
      if ((Boolean)newValue) {
        mImage.addProbeFunc(func);
      }
      else {
        mImage.removeProbeFunc(func);
      }
      runUpdate();
    }
    return true;
  }
  
  protected void runUpdate() {
    
    for (Region r : mRegions) {
      NeighbourhoodTask task = mNeighbourhoodTasks.get(r);
      if (task == null) {
        task = new NeighbourhoodTask();
        mNeighbourhoodTasks.put(r, task);
        task.execute(r);
      }
    }

    if (mIntersectTask != null && mIntersectTask.isRunning()) {
      mIntersectTask.cancel(true);
    }
    
    mIntersectTask = new IntersectTask();
    mIntersectTask.execute();
    
  }
  
  protected void setLoading(String key, boolean loading) {
    mLoading.put(key, loading);
    if (key == mViewMode) {
      setProgressBarIndeterminateVisibility(loading);
    }
  }
  
  private abstract class PointsTask<T> extends AsyncTask<T, Void, List<Integer>> {
    
    public boolean isRunning() {
      return getStatus() == Status.RUNNING;
    }
    
    protected abstract String key();

    @Override
    protected void onPreExecute() {
      super.onPreExecute();
      setLoading(key(), true);
    }

    @Override
    protected void onPostExecute(List<Integer> result) {
      setLoading(key(), false);
      mPoints.put(key(), result);
      if (mViewMode == key() && mShowFrag != null) {
        mShowFrag.setPoints(getIndices());
      }
    }

  }

  private class NeighbourhoodTask extends PointsTask<Region> {
    
    protected Region mRegion;

    @Override
    protected String key() {
      return NEIGHBOURHOOD_KEY;
    }

    @Override
    protected List<Integer> doInBackground(Region... params) {
      mRegion = params[0];
      
      // check if we should stop because the task was cancelled
      if (isCancelled()) {
        return null;
      }

      int center = mRegion.getCenterIndex(mImage);
      int[] regionPixels = mRegion.getIndices(mImage);
      
      return mImage.getHybridNeighbourhoodIndices(center, regionPixels, 0.1);
    }
    
    @Override
    protected void onPostExecute(List<Integer> result) {
      setLoading(key(), false);
      List<Integer> points = mPoints.get(key());
      points.addAll(result);
      if (mViewMode == key() && mShowFrag != null) {
        mShowFrag.setPoints(getIndices());
      }
    }
    
  }
  
  private class IntersectTask extends PointsTask<Void> {

    @Override
    protected String key() {
      return INTERSECT_KEY;
    }

    @Override
    protected List<Integer> doInBackground(Void... params) {
      List<Integer> pixels = new ArrayList<Integer>();
      
      // only bother unless we have at least two regions to use
      if (mRegions.size() >= 2) {
        List<int[]> regionsIndices = new ArrayList<int[]>();

        for (Region r : mRegions) {
          regionsIndices.add(r.getIndices(mImage));
        }
        
        // check if we should stop because the task was cancelled
        if (isCancelled()) return null;

        //pixels.addAll(system.getHybridIntersectObjects(pixelSets, 0.1));
        long startTime = System.currentTimeMillis();
        pixels = (mImage.getDescriptionBasedIntersectIndices(regionsIndices.get(0), regionsIndices.get(1)));
        Log.i("IntersectionFragment", "Intersection took " + ((System.currentTimeMillis() - startTime) / 1000.0) + " seconds");
      }

      return pixels;
    }
    
  }

}
