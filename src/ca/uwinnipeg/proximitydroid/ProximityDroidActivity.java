/**
 * 
 */
package ca.uwinnipeg.proximitydroid;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.MediaStore.Images;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.widget.SpinnerAdapter;
import ca.uwinnipeg.proximitydroid.fragments.AboutDialogFragment;
import ca.uwinnipeg.proximitydroid.fragments.ComplimentFragment;
import ca.uwinnipeg.proximitydroid.fragments.ImageFragment;
import ca.uwinnipeg.proximitydroid.fragments.IntersectionFragment;
import ca.uwinnipeg.proximitydroid.fragments.NeighbourhoodFragment;
import ca.uwinnipeg.proximitydroid.fragments.RegionFragment;
import ca.uwinnipeg.proximitydroid.fragments.RegionFragment.OnAddRegionSelectedListener;
import ca.uwinnipeg.proximitydroid.fragments.RegionSelectFragment;
import ca.uwinnipeg.proximitydroid.fragments.RegionSelectFragment.ListNavigationProvider;
import ca.uwinnipeg.proximitydroid.fragments.RegionSelectFragment.OnClosedListener;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.OnNavigationListener;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;

/**
 * @author Garrett Smith
 *
 */
//TODO: Add editing created regions
//TODO: Show an image loading view
//TODO: Clear progress on switching tabs
public class ProximityDroidActivity 
  extends FeaturePopulatingFragment 
  implements OnAddRegionSelectedListener,
             ListNavigationProvider,
             OnClosedListener {

  public static final String TAG = "ProximityDroidActivity";
  
  // UI
  private FragmentManager mFragmentManager;
  protected ActionBar mActionBar;
  protected SpinnerAdapter mSpinnerAdapter;  

  // true if we are on a small screen devices
  protected boolean mSmallScreen;
  
  // Constants
  
  // Fragment tags
  public static final String REGION_TAG = "region";
  public static final String NEIGHBOURHOOD_TAG = "neighbourhood";
  public static final String INTERSECTION_TAG = "intersection";
  public static final String COMPLIMENT_TAG = "compliment";
  public static final String SELECT_TAG = "select";
  
  @SuppressWarnings("unchecked")
  public static final Class<ImageFragment<?>>[] FRAGMENT_CLASSES = 
    (Class<ImageFragment<?>>[]) new Class<?>[] {
      RegionFragment.class,
      NeighbourhoodFragment.class,
      IntersectionFragment.class,
      ComplimentFragment.class
    };
  
  public static final String[] FRAGMENT_TAGS = new String[] {
    REGION_TAG,
    NEIGHBOURHOOD_TAG,
    INTERSECTION_TAG,
    COMPLIMENT_TAG
  };
  
  public static final int[] FRAGMENT_TEXT = new int[] {
    R.string.regions,
    R.string.neighbourhoods,
    R.string.intersection,
    R.string.compliment
  };
  
  // bundle keys
  protected static final String BUNDLE_SELECTED_TAB = "Selected Tab";

  
  // Service connection
  
  protected void onProximityServiceConnected() {
    super.onProximityServiceConnected();
    // request an image if the service does not already have one
    if (!getService().hasBitmap()) {
      Intent i = new Intent(Intent.ACTION_PICK, Images.Media.INTERNAL_CONTENT_URI);
      startActivityForResult(i, REQUEST_CODE_SELECT_IMAGE);
    }
    
    // get the currently added fragments
    List<ImageFragment<?>> fragments = new ArrayList<ImageFragment<?>>();
    for (String tag : FRAGMENT_TAGS) {
      Fragment frag = mFragmentManager.findFragmentByTag(tag);
      if (frag != null) fragments.add((ImageFragment<?>) frag);
    }
    
    // connect the service to the fragments
    for (ImageFragment<?> frag : fragments) {
      frag.setService(getService());
    }
  }
  
  // Intents
  private static final int REQUEST_CODE_SELECT_IMAGE = 0;

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch(requestCode) {
      case REQUEST_CODE_SELECT_IMAGE:
        if (resultCode == Activity.RESULT_OK) {
          getService().setBitmap(data.getData());
        }
    }
  }
  
  // lifecycle

  @Override
  protected void onCreate(Bundle state) {    
    super.onCreate(state);    

    // to display progress
    requestWindowFeature(Window.FEATURE_PROGRESS);    

    setContentView(R.layout.main);

    mFragmentManager = getSupportFragmentManager();

    mSmallScreen = findViewById(R.id.main_layout) == null;

    mActionBar = getSupportActionBar();

    // setup tabs
    mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
    
    for (int i = 0; i < FRAGMENT_CLASSES.length; i++) {
      Class<ImageFragment<?>> clazz = FRAGMENT_CLASSES[i];
      String tag = FRAGMENT_TAGS[i];
      int text = FRAGMENT_TEXT[i];
      
      Tab tab = mActionBar.newTab()
          .setText(text)
          .setTabListener(
              new TabListener(this, tag, clazz));
      mActionBar.addTab(tab);
    }
    
    // restore the selected tab
    int selected = 0;
    if (state != null) {
      selected = state.getInt(BUNDLE_SELECTED_TAB);
    }
    mActionBar.setSelectedNavigationItem(selected);
  }
  
  @Override
  protected void onSaveInstanceState(Bundle state) {
    // save the selected tab
    state.putInt(BUNDLE_SELECTED_TAB, mActionBar.getSelectedNavigationIndex());
    super.onSaveInstanceState(state);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getSupportMenuInflater();
    inflater.inflate(R.menu.main, menu);
    inflater.inflate(R.menu.features_select, menu);
    // setup toggle text on large screens
    if (!mSmallScreen) {
      updateToggleText(menu.findItem(R.id.menu_features), true);
    }
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

    if (mSmallScreen) {
      // launch feature activity
      Intent intent = new Intent(this, FeatureActivity.class);
      startActivity(intent);
    }
    else {
      // toggle showing feature fragment
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
        transaction.show(fragment);
      }
      else { 
        transaction.hide(fragment);      
      }
      transaction.commit();
      updateToggleText(item, show);
    }    

  }

  private void updateToggleText(MenuItem item, Boolean shown) {
    int text = shown ? R.string.menu_hide_features : R.string.menu_show_features;
    item.setTitle(text);    
  }

  /**
   * Displays the about dialog.
   */
  public void showAbout() {
    // Create and show the dialog.
    FragmentTransaction transaction = mFragmentManager.beginTransaction();
    Fragment prev = mFragmentManager.findFragmentByTag("dialog");
    if (prev != null) {
      transaction.remove(prev);
    }
    transaction.addToBackStack(null);
    DialogFragment newFragment = new AboutDialogFragment();
    newFragment.show(transaction, "dialog");
  }

  /**
   * Change to the select region fragment
   */
  public void onAddRegionSelected() {

    // we will need the options menu redrawn
    invalidateOptionsMenu(); 
    
    // create the fragment
    RegionSelectFragment frag = new RegionSelectFragment();
    frag.setService(getService());

    // swap the select fragment in
    mFragmentManager.beginTransaction()
    .replace(R.id.image_fragment_container, frag, SELECT_TAG)
    .addToBackStack(null)
    .commit();
  }

  @Override
  public void setListNavigationCallbacks(
      SpinnerAdapter adapter,
      OnNavigationListener listener) {
    mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
    mActionBar.setListNavigationCallbacks(adapter, listener);
  }

  @Override
  public void resetListNavigationCallbacks() {
    mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
  }

  @Override
  public void onClosed() {
    mFragmentManager.popBackStack();
  }
  
  /**
   * @author Garrett Smith
   *
   * @param <T>
   */
  public class TabListener implements ActionBar.TabListener {
    private ImageFragment<?> mFragment;
    private final Activity mActivity;
    private final String mTag;
    private final Class<ImageFragment<?>> mClass;

    /** Constructor used each time a new tab is created.
      * @param activity  The host Activity, used to instantiate the fragment
      * @param tag  The identifier tag for the fragment
      * @param clz  The fragment's Class, used to instantiate the fragment
      */
    public TabListener(Activity activity, String tag, Class<ImageFragment<?>> clz) {
        mActivity = activity;
        mTag = tag;
        mClass = clz;
    }

    /* The following are each of the ActionBar.TabListener callbacks */

    @Override
    public void onTabSelected(Tab tab, FragmentTransaction ft) {
      // previous Fragment management
      Fragment prevFragment = mFragmentManager.findFragmentByTag(mTag); 
      if (prevFragment != null) { 
        mFragment = (ImageFragment<?>) prevFragment; 
        // connect the service if we can
        if (isBound()) mFragment.setService(getService());
      }
      
      // Check if the fragment is already initialized
      if (mFragment == null) {
        // If not, instantiate and add it to the activity
        mFragment = (ImageFragment<?>) Fragment.instantiate(mActivity, mClass.getName());
        // Set the tab's tag to be the fragment
        tab.setTag(mFragment);
        // connect the service if we can
        if (isBound()) mFragment.setService(getService());
        ft.add(R.id.image_fragment_container, mFragment, mTag);
      } else {
        // If it exists, simply attach it in order to show it
        ft.attach(mFragment);
      }
    }

    @Override
    public void onTabUnselected(Tab tab, FragmentTransaction ft) {
      if (mFragment != null) {
        // Remove the fragment, because another one is being added
        ft.detach(mFragment);
      }
    }

    @Override
    public void onTabReselected(Tab tab, FragmentTransaction ft) {
      // User selected the already selected tab. Usually do nothing.
    }
  }

}
