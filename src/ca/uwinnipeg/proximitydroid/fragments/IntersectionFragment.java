/**
 * 
 */
package ca.uwinnipeg.proximitydroid.fragments;

import java.util.Formatter;

import android.app.Activity;
import android.content.BroadcastReceiver;
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
import ca.uwinnipeg.proximitydroid.services.PropertyService;
import ca.uwinnipeg.proximitydroid.services.ProximityService;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;


/**
 * @author Garrett Smith
 *
 */
// TODO: remove intersection and neighbourhood fragment duplication
public class IntersectionFragment extends RegionFragment {
  
  protected TextView mDegreeText;
  protected ProgressBar mDegreeBar;
  
  protected final static int DEGREE_STEPS = 100;
  
  public static final String DEGREE_FORMAT = " %1.2f";
  
  public int[] mPoints;
  
  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    IntentFilter filter = new IntentFilter();
    filter.addAction(PropertyService.ACTION_PROGRESS_CHANGED);
    filter.addAction(PropertyService.ACTION_VALUE_CHANGED);    
    filter.addAction(ProximityService.ACTION_REGIONS_CLEARED);  
    filter.addAction(IntersectionService.ACTION_DEGREE_CHANGED);
    filter.addCategory(IntersectionService.CATEGORY);
    mBroadcastManager.registerReceiver(mIntersectionReceiver, filter);
    
  }
  
  @Override
  public void onDestroy() {
    mBroadcastManager.unregisterReceiver(mIntersectionReceiver);
    super.onDestroy();
  }
  
  @Override
  public void onServiceAttach(ProximityService service) {
    super.onServiceAttach(service);
    IntersectionService s = 
        (IntersectionService) service.getPropertyService(IntersectionService.class);
    mPoints = s.getIntersection();
  }
  
  @Override
  protected void setupView() {
    super.setupView();
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
//    setDegree(getService().getDegree());
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
  
  // broadcasts
  
  protected IntersectionReceiver  mIntersectionReceiver = new IntersectionReceiver ();
  
  public class IntersectionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();
      if (action.equals(PropertyService.ACTION_VALUE_CHANGED)) {
        mPoints = intent.getIntArrayExtra(PropertyService.POINTS);
        invalidate();
      }
      else if (action.equals(PropertyService.ACTION_PROGRESS_CHANGED)) {
        int progress = intent.getIntExtra(PropertyService.PROGRESS, 0);
        setProgress(progress);
      }
      else if (action.equals(ProximityService.ACTION_REGIONS_CLEARED)) {
        mPoints = null;
        invalidate();
      }
      else if (action.equals(IntersectionService.ACTION_DEGREE_CHANGED)) {
        setDegree(intent.getFloatExtra(IntersectionService.DEGREE, 1));
      }
    }
    
  }
}
