/**
 * 
 */
package ca.uwinnipeg.proximitydroid.v2.fragments;

import android.content.IntentFilter;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import ca.uwinnipeg.proximitydroid.v2.R;
import ca.uwinnipeg.proximitydroid.v2.services.PropertyService;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

/**
 * A fragment that displays the results of a calculation and sets the epsilon.
 * <p>
 * An {@link EpsilonDialogFragment} is used to select the epsilon.
 * @author Garrett Smith
 *
 */
public abstract class EpsilonPropertyFragment<S extends PropertyService> 
  extends PropertyFragment<S> {
  
  // the shared preference key used to set the epsilon
  protected final String mEpsilonKey;

  public EpsilonPropertyFragment(Class<S> clazz, String category, String epsilonKey) {
    super(clazz, category);
    mEpsilonKey = epsilonKey;
  }

  public EpsilonPropertyFragment(
      Class<S> clazz, 
      String category, 
      IntentFilter filter, 
      String epsilonKey) {
    super(clazz, category, filter);
    mEpsilonKey = epsilonKey;
  }
  
  // options menu
  
  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    super.onCreateOptionsMenu(menu, inflater);
    inflater.inflate(R.menu.epsilon, menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == R.id.menu_epsilon) {
      // show the epsilon dialog
      FragmentManager fm = getActivity().getSupportFragmentManager();
      FragmentTransaction transaction = fm.beginTransaction();
      Fragment prev = fm.findFragmentByTag("dialog");
      if (prev != null) {
        transaction.remove(prev);
      }
      transaction.addToBackStack(null);
      
      DialogFragment newFragment = 
          EpsilonDialogFragment.newInstance(mEpsilonKey, getService().getNorm());
      newFragment.show(transaction, "dialog");
      return true;
    }
    else {
      return super.onOptionsItemSelected(item);
    }
  }

}
