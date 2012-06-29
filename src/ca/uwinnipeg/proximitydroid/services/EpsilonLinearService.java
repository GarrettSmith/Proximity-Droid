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
  implements OnSharedPreferenceChangeListener, EpsilonProperty {
  
  public static final String TAG = "EpsilonLinearService";
  
  protected float mEpsilon;
  
  protected final String mEpsilonKey;

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
  
  /* (non-Javadoc)
   * @see ca.uwinnipeg.proximitydroid.services.EpsilonProperty#getEpsilon()
   */
  @Override
  public float getEpsilon() {
    return mEpsilon;
  }
  
  /* (non-Javadoc)
   * @see ca.uwinnipeg.proximitydroid.services.EpsilonProperty#setEpsilon(float)
   */
  @Override
  public void setEpsilon(float epsilon) {
    mEpsilon = epsilon;
    invalidate();
  }

}
