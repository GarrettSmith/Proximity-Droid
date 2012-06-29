/**
 * 
 */
package ca.uwinnipeg.proximitydroid.services;

import ca.uwinnipeg.proximitydroid.Util;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;

/**
 * @author Garrett Smith
 *
 */
public abstract class EpsilonPropertyService 
  extends PropertyService 
  implements  EpsilonProperty,
              OnSharedPreferenceChangeListener {
  
  public static final String TAG = "EpsilonPropertyService";
  
  protected float mEpsilon;
  
  protected final String mPreferencesKey;

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
