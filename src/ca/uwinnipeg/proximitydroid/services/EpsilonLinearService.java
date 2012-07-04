/**
 * 
 */
package ca.uwinnipeg.proximitydroid.services;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import ca.uwinnipeg.proximitydroid.Util;

/**
 * A property service that uses a linear list of tasks and an epsilon to calculate its property.
 * @author Garrett Smith
 *
 */
public abstract class EpsilonLinearService 
  extends LinearService 
  implements OnSharedPreferenceChangeListener, EpsilonProperty {
  
  public static final String TAG = "EpsilonLinearService";
  
  // the current epsilon value
  protected float mEpsilon;
  
  // the shared preference key of the epsilon
  protected final String mEpsilonKey;

  /**
   * Creates a new service with the given category and shared preference key.
   * @param category
   * @param prefKey
   */
  public EpsilonLinearService(String category, String prefKey) {
    super(category);
    mEpsilonKey = prefKey;
  }
  
  @Override
  public void onCreate() {
    super.onCreate();
    
    // get initial epsilon
    SharedPreferences prefs = Util.getSupportDefaultSharedPrefences(this);
    mEpsilon = prefs.getFloat(mEpsilonKey, 0);
    prefs.registerOnSharedPreferenceChangeListener(this);
  }
  
  @Override
  public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
    if (key.equals(mEpsilonKey)) {
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
