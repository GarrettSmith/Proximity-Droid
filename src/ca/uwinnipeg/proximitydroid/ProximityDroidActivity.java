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
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.MediaStore.Images;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.SpinnerAdapter;
import ca.uwinnipeg.proximity.PerceptualSystem;
import ca.uwinnipeg.proximity.ProbeFunc;
import ca.uwinnipeg.proximity.image.AlphaFunc;
import ca.uwinnipeg.proximity.image.BlueFunc;
import ca.uwinnipeg.proximity.image.GreenFunc;
import ca.uwinnipeg.proximity.image.Image;
import ca.uwinnipeg.proximity.image.Pixel;
import ca.uwinnipeg.proximity.image.RedFunc;
import ca.uwinnipeg.proximitydroid.fragments.DescBasedNeighbourhoodFragment;
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

/**
 * @author Garrett Smith
 *
 */
public class ProximityDroidActivity 
  extends SherlockFragmentActivity 
  implements OnPreferenceAttachedListener, 
    OnPreferenceClickListener, 
    OnPreferenceChangeListener,
    OnRegionSelectedListener,
    RegionProvider,
    OnAddRegionSelecetedListener, 
    OnNavigationListener,
    ListNavigationProvider {

  public static final String TAG = "ProximityDroidActivity";

  // The image we are working on
  protected RotatedBitmap mBitmap;

  // the uri of the image
  protected Uri mUri;

  // Fragments
  private RegionShowFragment mShowFrag;
  private FeatureSelectFragment mProbeFrag;
  
  protected ActionBar mActionBar;
  protected SpinnerAdapter mSpinnerAdapter;
  
  public static final int LIST_SHOW_INDEX = 0;
  public static final int LIST_DESC_NH_INDEX = 1;

  public final String SHOW_TAG = "Show Region";
  public final String SELECT_TAG = "Select Region";

  private ContentResolver mContentResolver;
  private FragmentManager mFragmentManager;

  protected boolean mSmallScreen;

  protected List<Region> mRegions = new ArrayList<Region>();
  
  protected Map<String, ProbeFunc<Pixel>> mProbeFuncs = new HashMap<String, ProbeFunc<Pixel>>();
  
  protected Image mImage;
  protected PerceptualSystem<Pixel> mSystem = new PerceptualSystem<Pixel>();  

  @Override
  public Image getImage() {
    return mImage;
  }

  @Override
  public PerceptualSystem<Pixel> getSystem() {
    return mSystem;
  }

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
  public RotatedBitmap getBitmap() {
    return mBitmap;
  }

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
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

      // hide probe frag on small screens
      if (mSmallScreen) {
        mFragmentManager.beginTransaction()
        .hide(mProbeFrag)
        .commit();
      }

      // restore the bitmap
      mUri = (Uri)state.getParcelable(BUNDLE_KEY_URI);
      if (mUri != null) setupImage();

    }
    // load fragments if we are not using the large tablet display
    // Don't create fragments if we are restoring state
    else if (mSmallScreen) {
      mProbeFrag = new FeatureSelectFragment();

      // add the fragments to the view
//      mFragmentManager.beginTransaction()
//      .add(R.id.fragment_container, mProbeFrag)
//      .hide(mProbeFrag)
//      .commit();
    }
    // Get fragments by their id
    else {
      mProbeFrag = 
          (FeatureSelectFragment) mFragmentManager.findFragmentById(R.id.probe_func_fragment);
    }    

    // request an image
    if (mUri == null) {
      Intent i = new Intent(Intent.ACTION_PICK, Images.Media.INTERNAL_CONTENT_URI);
      startActivityForResult(i, REQUEST_CODE_SELECT_IMAGE);
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle state) {
    // save the fragments  
    if (mProbeFrag.isAdded()) mFragmentManager.putFragment(state, BUNDLE_KEY_PROBE_FRAG, mProbeFrag);
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
      mImage = Util.bitmapToImage(mBitmap.getBitmap());
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
        showShowFragment();
        break;
      case LIST_DESC_NH_INDEX:
        showDescBasedNHFragment();
        break;
    }
    return true;
  }
  
  public void showShowFragment() {
    switchImageFragment(new RegionShowFragment());    
  }
  
  public void showDescBasedNHFragment() {
    switchImageFragment(new DescBasedNeighbourhoodFragment());  
  }
  
  protected void switchImageFragment(RegionShowFragment frag) {
    mShowFrag = frag;
    mFragmentManager.beginTransaction()
      .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
      .replace(R.id.fragment_container, frag, SHOW_TAG)
      .commit();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getSupportMenuInflater();
    inflater.inflate(R.menu.main, menu);
    inflater.inflate(R.menu.features_select, menu);
    updateToggleText(menu.findItem(R.id.menu_features), mProbeFrag.isVisible());
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menu_about:
        showAbout();
        return true;
      case R.id.menu_features:
        toggleFeatures(item, mProbeFrag.isHidden());
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
      item.setTitle(R.string.menu_hide_features);
      transaction.add(R.id.fragment_container, mProbeFrag);
      if (mSmallScreen) {
        transaction.addToBackStack(null);
      }
    }
    else {
      item.setTitle(R.string.menu_show_features);
      if (mSmallScreen) {
        mFragmentManager.popBackStack();
      }
      else {
        transaction.remove(mProbeFrag);
      }
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
  private void showAbout() {
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
    .replace(R.id.fragment_container, new RegionSelectFragment(mBitmap), SELECT_TAG)
    .addToBackStack(null)
    .commit();
  }

  public void onRegionSelected(Region region) { 
    mRegions.add(region);
    // add the region to the perceptual system
    mSystem.addRegion(region.getPixels(mImage));
    onRegionCanceled();
  }

  public void onRegionCanceled() {
    mFragmentManager.popBackStack();
  }

  @Override
  public void onPreferenceAttached(PreferenceScreen root, int xmlId) {
    if(root == null) return; //for whatever reason in very rare cases this is null   

    // Load features
    Map<String, List<ProbeFunc<Pixel>>> features = loadProbeFuncs();

    // Generate preference items from features    
    // generate a category for each given category    
    for (String catStr : features.keySet()) {
      List<ProbeFunc<Pixel>> funcs = features.get(catStr);

      // only add the category if it is non empty
      if (funcs != null && !funcs.isEmpty()) {
        PreferenceCategory category = new PreferenceCategory(this);
        category.setTitle(catStr);
        category.setKey(catStr);
        root.addPreference(category);

        // generate a preference for each probe func
        for (ProbeFunc<Pixel> func : funcs) {
          SwitchPreference pref = new SwitchPreference(this);

          // Set name and key
          String key = catStr + "_" + func.toString();
          pref.setTitle(func.toString());
          pref.setKey(key);

          pref.setOnPreferenceClickListener(this);
          pref.setOnPreferenceChangeListener(this);
          category.addPreference(pref);
          
          // add the ProbeFunc to our map to use later
          mProbeFuncs.put(key, func);
        }
      }
    }
  }

  private Map<String, List<ProbeFunc<Pixel>>> loadProbeFuncs() {
    Map<String, List<ProbeFunc<Pixel>>> features = new HashMap<String, List<ProbeFunc<Pixel>>>();
    // load all the standard probe funcs
    features.put("Colour", new ArrayList<ProbeFunc<Pixel>>());
    List<ProbeFunc<Pixel>> colourFuncs = features.get("Colour");
    colourFuncs.add(new AlphaFunc());
    colourFuncs.add(new RedFunc());
    colourFuncs.add(new GreenFunc());
    colourFuncs.add(new BlueFunc());

    // TODO: load probe funcs from external storage
    return features;
  }

  @Override
  public boolean onPreferenceClick(Preference preference) {
    String key = preference.getKey();
    Log.i(TAG, key + " was pressed.");
    return true;
  }

  @Override
  public boolean onPreferenceChange(Preference preference, Object newValue) {
    if (newValue instanceof Boolean) {
      // add or remove the probe func from the perceptual system
      ProbeFunc<Pixel> func = mProbeFuncs.get(preference.getKey());
      if ((Boolean)newValue) {
        mSystem.addProbeFunc(func);
      }
      else {
        mSystem.removeProbeFunc(func);
      }
    }
    return true;
  }

}
