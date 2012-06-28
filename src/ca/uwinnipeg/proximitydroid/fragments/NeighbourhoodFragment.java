/**
 * 
 */
package ca.uwinnipeg.proximitydroid.fragments;

import java.util.HashMap;
import java.util.Map;

import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import ca.uwinnipeg.proximitydroid.R;
import ca.uwinnipeg.proximitydroid.Region;
import ca.uwinnipeg.proximitydroid.services.NeighbourhoodService;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

/**
 * @author Garrett Smith
 *
 */
public class NeighbourhoodFragment extends PropertyFragment<NeighbourhoodService> {

  protected Map<Region, int[]> mNeighbourhoods = new HashMap<Region, int[]>();
  
  public NeighbourhoodFragment() {
    super(NeighbourhoodService.class, NeighbourhoodService.CATEGORY);
  }
  
  @Override
  protected void onPropertyServiceAvailable(NeighbourhoodService service) {
    mNeighbourhoods = service.getNeighbourhoods();
  }
  
  @Override
  protected void draw() {
    super.draw();
    
//    mNeighbourhoods = getService().getNeighbourhoods();
//    
    mView.clearHighlight();
    for (Region reg : mNeighbourhoods.keySet()) {
      int[] points = mNeighbourhoods.get(reg);
      if (points != null) {
        mView.addHighlight(points);
      }
    }
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
      FragmentManager fm = getActivity().getSupportFragmentManager();
      FragmentTransaction transaction = fm.beginTransaction();
      Fragment prev = fm.findFragmentByTag("dialog");
      if (prev != null) {
        transaction.remove(prev);
      }
      transaction.addToBackStack(null);
      
      DialogFragment newFragment = 
          EpsilonDialogFragment.newInstance(NeighbourhoodService.EPSILON_KEY);
      newFragment.show(transaction, "dialog");
      return true;
    }
    else {
      return super.onOptionsItemSelected(item);
    }
  }
  
  @Override
  protected void onValueChanged(Region region, int[] points) {
    mNeighbourhoods.put(region, points);
  }
  
  @Override
  protected void onRegionsCleared() {
    mNeighbourhoods.clear();
  }
}
