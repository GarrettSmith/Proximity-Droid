/**
 * 
 */
package ca.uwinnipeg.proximitydroid;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import ca.uwinnipeg.proximitydroid.fragments.FeatureFragment;
import ca.uwinnipeg.proximitydroid.fragments.PreferenceListFragment.OnPreferenceAttachedListener;

/**
 * @author Garrett Smith
 *
 */
public class FeatureActivity 
  extends FeaturePopulatingFragment
  implements OnPreferenceAttachedListener {
  
  protected FragmentManager mFragmentManager;
  
  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    
    mFragmentManager = getSupportFragmentManager();
    
    mFragmentManager.beginTransaction()
      .add(android.R.id.content, new FeatureFragment())
      .commit();
  }
}
