/**
 * 
 */
package ca.uwinnipeg.proximitydroid;

import java.util.List;
import java.util.Map;

import android.annotation.TargetApi;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import ca.uwinnipeg.proximity.ProbeFunc;
import ca.uwinnipeg.proximitydroid.fragments.PreferenceListFragment.OnPreferenceAttachedListener;

/**
 * An activity that populates the items in the attached preferences from a connected Proximity 
 * Service.
 * @author Garrett Smith
 *
 */
public abstract class FeaturePopulatingFragment 
  extends ProximityServiceActivity
  implements OnPreferenceAttachedListener {

  // attached preference screen to be populated
  protected PreferenceScreen mPreferenceScreen;
  
  @Override
  protected void onProximityServiceConnected() {    
    // Check if we need to populate the preference screen
    if (mPreferenceScreen != null) {
      populatePreferences(getService().getProbeFuncs());
    }
  }
  
  @Override
  public void onPreferenceAttached(PreferenceScreen root, int xmlId) {
    if(root == null) return; //for whatever reason in very rare cases this is null   
    
    // store the preference screen so we can add to it when the service is bound
    mPreferenceScreen = root;
  }
  
  /**
   * Populates the feature preferences from a map of Strings, representing categories, to lists of
   * probe functions. The toString method of the probe function is used to name the corresponding
   * preference.
   * <p>
   * This method is called when the Proximity Service is connected to this activity.
   * @param features
   */
  @TargetApi(14)
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

}
