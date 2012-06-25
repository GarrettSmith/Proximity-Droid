/**
 * 
 */
package ca.uwinnipeg.proximitydroid;

import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.StrictMode;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.MediaStore.Images;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.widget.SpinnerAdapter;
import ca.uwinnipeg.proximity.ProbeFunc;
import ca.uwinnipeg.proximitydroid.fragments.AboutDialogFragment;
import ca.uwinnipeg.proximitydroid.fragments.ImageFragment;
import ca.uwinnipeg.proximitydroid.fragments.IntersectionFragment;
import ca.uwinnipeg.proximitydroid.fragments.NeighbourhoodFragment;
import ca.uwinnipeg.proximitydroid.fragments.PreferenceListFragment.OnPreferenceAttachedListener;
import ca.uwinnipeg.proximitydroid.fragments.RegionSelectFragment;
import ca.uwinnipeg.proximitydroid.fragments.RegionSelectFragment.ListNavigationProvider;
import ca.uwinnipeg.proximitydroid.fragments.RegionSelectFragment.OnClosedListener;
import ca.uwinnipeg.proximitydroid.fragments.RegionShowFragment;
import ca.uwinnipeg.proximitydroid.fragments.RegionShowFragment.OnAddRegionSelectedListener;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.OnNavigationListener;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;

/**
 * @author Garrett Smith
 *
 */
//TODO: Allow screen rotation
//TODO: Add editing created regions
//TODO: Show an image loading view
public class ProximityDroidActivity 
  extends SherlockFragmentActivity 
  implements OnPreferenceAttachedListener,
             OnAddRegionSelectedListener,
             ListNavigationProvider,
             OnClosedListener {

  public static final String TAG = "ProximityDroidActivity";
  
  public static final boolean DEVELOPER_MODE = true;
  
  // UI
  private FragmentManager mFragmentManager;
  protected ActionBar mActionBar;
  protected SpinnerAdapter mSpinnerAdapter;  
  protected PreferenceScreen mPreferenceScreen;

  // true if we are on a small screen devices
  protected boolean mSmallScreen;
  
  // Constants

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
      
      // Check if we need to populate the preference screen
      if (mPreferenceScreen != null) {
        populatePreferences(mService.getProbeFuncs());
      }
      
      // connect the service to the current fragment
      ((ImageFragment<?>) mActionBar.getSelectedTab().getTag()).setService(mService);
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
    if (DEVELOPER_MODE) {
      StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
      .detectAll()
      .penaltyLog()
      .build());
      StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
      .detectAll()
      .penaltyLog()
      .penaltyDeath()
      .build());
    }
    super.onCreate(state);    

    // to display progress
    requestWindowFeature(Window.FEATURE_PROGRESS);    

    setContentView(R.layout.main);

    mFragmentManager = getSupportFragmentManager();

    mSmallScreen = findViewById(R.id.main_layout) == null;

    mActionBar = getSupportActionBar();
    
    // setup tabs
    mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
    
    Tab tab = mActionBar.newTab()
        .setText(R.string.regions)
        .setTabListener(
            new TabListener<RegionShowFragment>(this, "TAG", RegionShowFragment.class));
    mActionBar.addTab(tab, true);
    
    tab = mActionBar.newTab()
        .setText(R.string.neighbourhoods)
        .setTabListener(
            new TabListener<NeighbourhoodFragment>(this, "TAG", NeighbourhoodFragment.class));
    mActionBar.addTab(tab);
    
    tab = mActionBar.newTab()
        .setText(R.string.intersection)
        .setTabListener(
            new TabListener<IntersectionFragment>(this, "TAG", IntersectionFragment.class));
    mActionBar.addTab(tab);
    
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
    super.onSaveInstanceState(state);
  }
  
  @Override
  protected void onRestoreInstanceState(Bundle savedInstanceState) {
    super.onRestoreInstanceState(savedInstanceState);
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
    frag.setService(mService);

    // swap the select fragment in
    mFragmentManager.beginTransaction()
    .replace(R.id.image_fragment_container, frag)
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
  

  public void populatePreferences(Map<String, List<ProbeFunc<Integer>>> features) {

    // Generate preference items from features    
    // generate a category for each given category    
    for (String catStr : features.keySet()) {
      List<ProbeFunc<Integer>> funcs = features.get(catStr);

      // only add the category if it is non empty
      if (funcs != null && !funcs.isEmpty()) {
        PreferenceCategory category = new PreferenceCategory(this);
        category.setTitle(catStr);
        category.setKey(catStr);
        mPreferenceScreen.addPreference(category);

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
          category.addPreference(pref);
        }
      }
    }
  }
  
  /**
   * From http://developer.android.com/guide/topics/ui/actionbar.html#Tabs
   * @author Garrett Smith
   *
   * @param <T>
   */
  public class TabListener<T extends ImageFragment<?>> implements ActionBar.TabListener {
    private ImageFragment<?> mFragment;
    private final Activity mActivity;
    private final String mTag;
    private final Class<T> mClass;

    /** Constructor used each time a new tab is created.
      * @param activity  The host Activity, used to instantiate the fragment
      * @param tag  The identifier tag for the fragment
      * @param clz  The fragment's Class, used to instantiate the fragment
      */
    public TabListener(Activity activity, String tag, Class<T> clz) {
        mActivity = activity;
        mTag = tag;
        mClass = clz;
    }

    /* The following are each of the ActionBar.TabListener callbacks */

    @Override
    public void onTabSelected(Tab tab, FragmentTransaction ft) {
        // Check if the fragment is already initialized
        if (mFragment == null) {
            // If not, instantiate and add it to the activity
            mFragment = (ImageFragment<?>) Fragment.instantiate(mActivity, mClass.getName());
            // provide the service to the fragment if it is already attached
            if (mService != null) mFragment.setService(mService);
            // set the tag to be the tab's fragment
            tab.setTag(mFragment);
            // connect the service if it is 
            if (mService != null) mFragment.setService(mService);
            // add the fragment
            ft.add(R.id.image_fragment_container, mFragment, mTag);
        } else {
            // If it exists, simply attach it in order to show it
            ft.attach(mFragment);
        }
    }

    @Override
    public void onTabUnselected(Tab tab, FragmentTransaction ft) {
        if (mFragment != null) {
            // Detach the fragment, because another one is being attached
            ft.detach(mFragment);
        }
    }

    @Override
    public void onTabReselected(Tab tab, FragmentTransaction ft) {
        // User selected the already selected tab. Usually do nothing.
    }
}

}
