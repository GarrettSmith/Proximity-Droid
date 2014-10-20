/**
 * 
 */
package ca.uwinnipeg.proximitydroid.v2;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.NavUtils;
import ca.uwinnipeg.proximitydroid.v2.fragments.FeatureFragment;
import ca.uwinnipeg.proximitydroid.v2.fragments.PreferenceListFragment.OnPreferenceAttachedListener;

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
//        Intent intent = new Intent(this, ProximityDroidActivity.class);
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//        startActivity(intent);
        NavUtils.navigateUpFromSameTask(this);
        return true;
      default:
        return super.onOptionsItemSelected(item);          
    }
  }
}
