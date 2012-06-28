/**
 * 
 */
package ca.uwinnipeg.proximitydroid.fragments;

import java.util.Formatter;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import ca.uwinnipeg.proximitydroid.R;
import ca.uwinnipeg.proximitydroid.services.IntersectionService;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;


/**
 * @author Garrett Smith
 *
 */
// TODO: remove intersection and neighbourhood fragment duplication
public class IntersectionFragment extends PropertyFragment<IntersectionService> {

  protected TextView mDegreeText;
  protected ProgressBar mDegreeBar;
  
  protected final static int DEGREE_STEPS = 100;
  
  public static final String DEGREE_FORMAT = " %1.2f";
  
  public int[] mPoints;
  public float mDegree = 1;
  
  public IntersectionFragment() {
    super(
        IntersectionService.class, 
        IntersectionService.CATEGORY,
        new IntentFilter(IntersectionService.ACTION_DEGREE_CHANGED));
  }
  
  @Override
  protected void onPropertyServiceAvailable(IntersectionService service) {
    super.onPropertyServiceAvailable(service);
    mPoints = service.getIntersection();
    mDegree = service.getDegree();
  }
  
  @Override
  protected void draw() {
    super.draw();
    if (mPoints != null) {
      mView.setHighlight(mPoints);
    }
    else {
      mView.clearHighlight();
    }
  } 
  
  protected void setDegree(float degree) {
    mDegreeBar.setProgress((int) (DEGREE_STEPS - (degree * DEGREE_STEPS)));
    mDegreeText.setText(new Formatter().format(DEGREE_FORMAT, degree).toString());
  }
  
  // options menu
  
  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    super.onCreateOptionsMenu(menu, inflater);
    inflater.inflate(R.menu.epsilon, menu);
    inflater.inflate(R.menu.intersection, menu);
    
    MenuItem item = menu.findItem(R.id.menu_degree);
    View view = item.getActionView();
    mDegreeText = (TextView) view.findViewById(R.id.degree_value);
    mDegreeBar = (ProgressBar) view.findViewById(R.id.degree_bar);
    
    // set the current degree
    setDegree(mDegree);
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
          EpsilonDialogFragment.newInstance(IntersectionService.EPSILON_KEY);
      newFragment.show(transaction, "dialog");
      return true;
    }
    else {
      return super.onOptionsItemSelected(item);
    }
  }
  
  @Override
  protected void onRecieve(Context context, Intent intent) {
    String action = intent.getAction();
    if (action.equals(IntersectionService.ACTION_DEGREE_CHANGED)) {
      onDegreeChanged(intent.getFloatExtra(IntersectionService.DEGREE, 1));
    }
    else {
      super.onRecieve(context, intent);
    }
  }
  
  @Override
  protected void onValueChanged(int[] points) {
    mPoints = points;
  }
  
  @Override
  protected void onRegionsCleared() {
    mPoints = null;
  }
  
  protected void onDegreeChanged(float degree) {
    mDegree = degree;
    setDegree(mDegree);
  }
}
