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
import android.graphics.Rect;
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
import ca.uwinnipeg.proximitydroid.fragments.ProbeFuncSelectFragment;
import ca.uwinnipeg.proximitydroid.fragments.RegionShowFragment;

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
implements OnPreferenceAttachedListener, OnPreferenceClickListener, OnPreferenceChangeListener {

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
  private ProbeFuncSelectFragment mProbeFrag;
      
  private ContentResolver mContentResolver;
  private FragmentManager mFragmentManager;
  
  protected boolean mSmallScreen;

  // Intents
  private static final int REQUEST_CODE_SELECT_IMAGE = 0;
  private static final int REQUEST_CODE_ADD_REGION = 1;

  // bundle keys
  protected static final String BUNDLE_KEY_BITMAP = "Bitmap";
  protected static final String BUNDLE_KEY_ORIENTATION = "Orientation";
  protected static final String BUNDLE_KEY_URI = "Uri";
  protected static final String BUNDLE_KEY_SHOW_FRAG = "Show Fragment";
  protected static final String BUNDLE_KEY_PROBE_FRAG = "Probe Fragment";

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    setContentView(R.layout.main);

    mContentResolver = getContentResolver();
    mFragmentManager = getSupportFragmentManager();
    
    mSmallScreen = findViewById(R.id.fragment_container) != null;

    // restore previous state
    if (state != null) {
      // restore fragments
      mShowFrag = 
          (RegionShowFragment) mFragmentManager.getFragment(state, BUNDLE_KEY_SHOW_FRAG);
      mProbeFrag = 
          (ProbeFuncSelectFragment) mFragmentManager.getFragment(state, BUNDLE_KEY_PROBE_FRAG);

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
      mProbeFrag = new ProbeFuncSelectFragment();

      // add the fragments to the view
      mFragmentManager.beginTransaction()
        .add(R.id.fragment_container, mShowFrag)
        .add(R.id.fragment_container, mProbeFrag)
        .commit();
      
      // hide probe frag
      mFragmentManager.beginTransaction()
        .hide(mProbeFrag)
        .commit();
    }
    // Get fragments by their id
    else {
      mShowFrag = 
          (RegionShowFragment) mFragmentManager.findFragmentById(R.id.show_fragment);
      mProbeFrag = 
          (ProbeFuncSelectFragment) mFragmentManager.findFragmentById(R.id.probe_func_fragment);
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
      case REQUEST_CODE_ADD_REGION:
        if (resultCode == Activity.RESULT_OK) addRegion(data);
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

  /**
   * Adds the returned region.
   * @param data
   */
  private void addRegion(Intent data) {
      Rect bounds = (Rect) data.getParcelableExtra(RegionSelectActivity.RESULT_KEY_BOUNDS);
      String shapeStr = data.getStringExtra(RegionSelectActivity.RESULT_KEY_SHAPE);
      Region.Shape shape = Region.Shape.valueOf(Region.Shape.class, shapeStr);
      Polygon poly = new Polygon(data.getIntArrayExtra(RegionSelectActivity.RESULT_KEY_POLY));
      mShowFrag.addRegion(bounds, shape, poly);
  }

  //Updates the bitmap of the region showing fragment
  private void updateBitmap() {
      mShowFrag.setBitmap(mBitmap);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getSupportMenuInflater();
    inflater.inflate(R.menu.region_show, menu);
    // setup toggle button text
    if (!mSmallScreen) {
      menu.findItem(R.id.menu_features).setTitle(R.string.menu_hide_features);
    }
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menu_about:
        showAbout();
        return true;
      case R.id.menu_add:
        addRegion();
        return true;
      case R.id.menu_features:
        toggleFeatures();
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
  private void addRegion() {
    Intent i = new Intent(this, RegionSelectActivity.class);
    // give it the image and orientation to work with
    i.setData(mUri);
    // start the activity
    startActivityForResult(i, REQUEST_CODE_ADD_REGION);
  }
  
  /**
   * Toggles the display of the features fragment.
   */
  private void toggleFeatures() {
    FragmentTransaction transaction = mFragmentManager.beginTransaction();
    transaction.setCustomAnimations(
        R.anim.slide_in, 
        R.anim.slide_out, 
        R.anim.slide_in, 
        R.anim.slide_out);
    // toggle hiding probe frag
    if (mProbeFrag.isHidden()) {
      transaction
      .show(mProbeFrag);
    }
    else {
      transaction.hide(mProbeFrag);
    }
    transaction.commit();
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

}
