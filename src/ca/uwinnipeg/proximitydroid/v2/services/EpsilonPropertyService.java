/**
 * 
 */
package ca.uwinnipeg.proximitydroid.v2.services;

import ca.uwinnipeg.proximitydroid.v2.Util;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;

/**
 * A service that calculates the value of a service by using an epsilon.
 * @author Garrett Smith
 *
 */
public abstract class EpsilonPropertyService 
  extends PropertyService 
  implements  EpsilonProperty,
              OnSharedPreferenceChangeListener {
  
  public static final String TAG = "EpsilonPropertyService";
  
  // the current epsilon value
  protected float mEpsilon;
  
  protected final String mPreferencesKey;

  /**
   * Creates a new service with the given category and the shared preference key.
   * @param category
   * @param prefKey
   */
  public EpsilonPropertyService(String category, String prefKey) {
    super(category);
    mPreferencesKey = prefKey;
  }
  
  @Override
  public void onCreate() {
    super.onCreate();
    
    // get initial epsilon
    SharedPreferences prefs = Util.getSupportDefaultSharedPrefences(this);
    mEpsilon = prefs.getFloat(mPreferencesKey, 0);
    prefs.registerOnSharedPreferenceChangeListener(this);
  }
  
  @Override
  public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
    if (key.equals(mPreferencesKey)) {
      setEpsilon(prefs.getFloat(key, 0));
    }
  }
  
  @Override
  public float getEpsilon() {
    return mEpsilon;
  }
  
  @Override
  public void setEpsilon(float epsilon) {
    mEpsilon = epsilon;
    invalidate();
  }

}
