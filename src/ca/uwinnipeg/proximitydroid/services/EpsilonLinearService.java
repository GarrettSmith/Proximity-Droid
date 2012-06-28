/**
 * 
 */
package ca.uwinnipeg.proximitydroid.services;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import ca.uwinnipeg.proximitydroid.Util;

/**
 * @author Garrett Smith
 *
 */
public abstract class EpsilonLinearService 
  extends LinearService 
  implements OnSharedPreferenceChangeListener {
  
  public static final String TAG = "EpsilonLinearService";
  
  protected float mEpsilon;
  
  protected final String mPreferencesKey;

  public EpsilonLinearService(String category, String prefKey) {
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
  
  public float getEpsilon() {
    return mEpsilon;
  }
  
  public void setEpsilon(float epsilon) {
    mEpsilon = epsilon;
    invalidate();
  }

}
