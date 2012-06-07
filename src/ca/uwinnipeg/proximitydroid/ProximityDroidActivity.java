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
import android.graphics.Bitmap;
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
import ca.uwinnipeg.proximity.ProbeFunc;
import ca.uwinnipeg.proximity.image.AlphaFunc;
import ca.uwinnipeg.proximity.image.BlueFunc;
import ca.uwinnipeg.proximity.image.GreenFunc;
import ca.uwinnipeg.proximity.image.RedFunc;
import ca.uwinnipeg.proximitydroid.fragments.PreferenceListFragment.OnPreferenceAttachedListener;
import ca.uwinnipeg.proximitydroid.fragments.FeatureSelectFragment;
import ca.uwinnipeg.proximitydroid.fragments.FeatureSelectFragment.OnToggleSelectedListener;
import ca.uwinnipeg.proximitydroid.fragments.RegionSelectFragment;
import ca.uwinnipeg.proximitydroid.fragments.RegionSelectFragment.OnRegionSelectedListener;
import ca.uwinnipeg.proximitydroid.fragments.RegionShowFragment;
import ca.uwinnipeg.proximitydroid.fragments.RegionShowFragment.OnAddRegionListener;
import ca.uwinnipeg.proximitydroid.fragments.RegionShowFragment.RegionProvider;

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
    OnAddRegionListener,
    OnToggleSelectedListener {

  public static final String TAG = "ProximityDroidActivity";

  // The image we are working on
  protected RotatedBitmap mBitmap;

  // the uri of the image
  protected Uri mUri;

  // the map of categories to lists of probe functions
  // TODO: Load features dynamically
  protected Map<String, List<ProbeFunc<Integer>>> mFeatures = 
      new HashMap<String, List<ProbeFunc<Integer>>>();

  // Fragments
  private RegionShowFragment mShowFrag;
  private FeatureSelectFragment mProbeFrag;

  public final String SELECT_TAG = "Select RegionView";

  private ContentResolver mContentResolver;
  private FragmentManager mFragmentManager;

  protected boolean mSmallScreen;
  
  public enum Mode {SHOW, SELECT};
  
  protected Mode mMode = Mode.SHOW;

  protected List<Region> mRegions = new ArrayList<Region>();

  // Intents
  private static final int REQUEST_CODE_SELECT_IMAGE = 0;

  // bundle keys
  protected static final String BUNDLE_KEY_BITMAP = "Bitmap";
  protected static final String BUNDLE_KEY_ORIENTATION = "Orientation";
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

    // restore previous state
    if (state != null) {
      // restore fragments
      mShowFrag = 
          (RegionShowFragment) mFragmentManager.getFragment(state, BUNDLE_KEY_SHOW_FRAG);
      mProbeFrag = 
          (FeatureSelectFragment) mFragmentManager.getFragment(state, BUNDLE_KEY_PROBE_FRAG);

      // hide probe frag on small screens
      if (mSmallScreen) {
        mFragmentManager.beginTransaction()
        .hide(mProbeFrag)
        .commit();
      }

      // restore the bitmap
      Bitmap bm = (Bitmap) state.getParcelable(BUNDLE_KEY_BITMAP);
      int orientation = state.getInt(BUNDLE_KEY_ORIENTATION);
      if (bm != null) {
        mBitmap = new RotatedBitmap(bm , orientation);
      }
      mUri = (Uri)state.getParcelable(BUNDLE_KEY_URI);

    }
    // load fragments if we are not using the large tablet display
    // Don't create fragments if we are restoring state
    else if (mSmallScreen) {
      mShowFrag = new RegionShowFragment();
      mProbeFrag = new FeatureSelectFragment();

      // add the fragments to the view
      mFragmentManager.beginTransaction()
      .add(R.id.fragment_container, mShowFrag)
      .add(R.id.fragment_container, mProbeFrag)
      .hide(mProbeFrag)
      .commit();
    }
    // Get fragments by their id
    else {
      mShowFrag = new RegionShowFragment();

      // add the fragment to the view
      mFragmentManager.beginTransaction()
      .add(R.id.fragment_container, mShowFrag)
      .commit();

      mProbeFrag = 
          (FeatureSelectFragment) mFragmentManager.findFragmentById(R.id.probe_func_fragment);
    }    

    // request an image
    if (mBitmap == null) {
      Intent i = new Intent(Intent.ACTION_PICK, Images.Media.INTERNAL_CONTENT_URI);
      startActivityForResult(i, REQUEST_CODE_SELECT_IMAGE);
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle state) {
    // save the fragments   
    if (mShowFrag.isAdded()) mFragmentManager.putFragment(state, BUNDLE_KEY_SHOW_FRAG, mShowFrag);
    if (mProbeFrag.isAdded()) mFragmentManager.putFragment(state, BUNDLE_KEY_PROBE_FRAG, mProbeFrag);
    // Save the bitmap
    if (mBitmap != null) {
      state.putParcelable(BUNDLE_KEY_BITMAP, mBitmap.getBitmap());
      state.putInt(BUNDLE_KEY_ORIENTATION, mBitmap.getOrientation());
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
          selectImage(data);
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
  private void selectImage(Intent data) {
    // Save and load the returned uri
    mUri = data.getData();
    mBitmap = Util.loadImage(mUri, mContentResolver, getWindowManager());
    updateBitmap();
  }

  //Updates the bitmap of the region showing fragment
  private void updateBitmap() {
    mShowFrag.setBitmap(mBitmap);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getSupportMenuInflater();
    inflater.inflate(R.menu.main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menu_about:
        showAbout();
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
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
  public void onAddRegion() {
    
    mMode = Mode.SELECT;
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
    onRegionCanceled();
  }

  public void onRegionCanceled() {
    mMode = Mode.SHOW;
    mFragmentManager.popBackStack();
  }

  @Override
  public void onPreferenceAttached(PreferenceScreen root, int xmlId) {
    if(root == null) return; //for whatever reason in very rare cases this is null   

    // Load features
    loadProbeFuncs();

    // Generate preference items from features    
    // generate a category for each given category    
    for (String catStr : mFeatures.keySet()) {
      List<ProbeFunc<Integer>> funcs = mFeatures.get(catStr);

      // only add the category if it is non empty
      if (funcs != null && !funcs.isEmpty()) {
        PreferenceCategory category = new PreferenceCategory(this);
        category.setTitle(catStr);
        category.setKey(catStr);
        root.addPreference(category);

        // generate a preference for each probe func
        for (ProbeFunc<Integer> func : funcs) {
          SwitchPreference pref = new SwitchPreference(this);

          // Set name and key
          String key = catStr + "_" + func.toString();
          pref.setTitle(func.toString());
          pref.setKey(key);

          pref.setOnPreferenceClickListener(this);
          pref.setOnPreferenceChangeListener(this);
          category.addPreference(pref);
        }
      }
    }
  }

  private void loadProbeFuncs() {
    // load all the standard probe funcs
    mFeatures.put("Colour", new ArrayList<ProbeFunc<Integer>>());
    List<ProbeFunc<Integer>> colourFuncs = mFeatures.get("Colour");
    colourFuncs.add(new AlphaFunc());
    colourFuncs.add(new RedFunc());
    colourFuncs.add(new GreenFunc());
    colourFuncs.add(new BlueFunc());

    // TODO: load probe funcs from external storage
  }

  @Override
  public boolean onPreferenceClick(Preference preference) {
    String key = preference.getKey();
    Log.i(TAG, key + " was pressed.");
    return true;
  }

  @Override
  public boolean onPreferenceChange(Preference preference, Object newValue) {
    return true;
  }

  @Override
  public void onToggleSelected(MenuItem item) {
    FragmentTransaction transaction = mFragmentManager.beginTransaction();
    transaction.setCustomAnimations(
        R.anim.slide_in, 
        R.anim.slide_out, 
        R.anim.slide_in, 
        R.anim.slide_out);
    // toggle hiding probe frag
    if (mProbeFrag.isHidden()) {
      item.setTitle(R.string.menu_hide_features);
      transaction.show(mProbeFrag);
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
        transaction.hide(mProbeFrag);
      }
    }
    transaction.commit();    
  }

}
