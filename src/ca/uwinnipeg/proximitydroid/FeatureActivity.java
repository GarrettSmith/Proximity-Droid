/**
 * 
 */
package ca.uwinnipeg.proximitydroid;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import ca.uwinnipeg.proximitydroid.fragments.FeatureFragment;
import ca.uwinnipeg.proximitydroid.fragments.PreferenceListFragment.OnPreferenceAttachedListener;

import com.actionbarsherlock.view.MenuItem;

/**
 * A simple activity that displays the feature preference screen.
 * <p>
 * This activity is used on phones and other small screen devices on which a sliding drawer wouldn't 
 * work.
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
    
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    
    mFragmentManager = getSupportFragmentManager();
    
    // add the fragment to the activity
    mFragmentManager.beginTransaction()
      .add(android.R.id.content, new FeatureFragment())
      .commit();
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        // jump back to the main activity
        Intent intent = new Intent(this, ProximityDroidActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        return true;
      default:
        return super.onOptionsItemSelected(item);          
    }
  }
}
