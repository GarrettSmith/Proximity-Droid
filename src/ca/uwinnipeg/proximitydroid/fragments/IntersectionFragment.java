/**
 * 
 */
package ca.uwinnipeg.proximitydroid.fragments;

import java.util.Formatter;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import ca.uwinnipeg.proximitydroid.R;
import ca.uwinnipeg.proximitydroid.services.IntersectionService;
import ca.uwinnipeg.proximitydroid.services.NeighbourhoodIntersectionService;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

/**
 * Displays the results of {@link IntersectionService}.
 * @author Garrett Smith
 *
 */
public class IntersectionFragment extends EpsilonPropertyFragment<NeighbourhoodIntersectionService> {

  // The text view used to display the degree of nearness
  protected TextView mDegreeText;
  
  // the format used by the degree text
  public static final String DEGREE_FORMAT = " %1.2f";
  
  // The bar used to display a visual of the degree of nearness
  protected ProgressBar mDegreeBar;
  
  // the number of steps in the degree bar
  protected final static int DEGREE_STEPS = 100;
  
  public int[] mPoints;
  
  // the current degree
  public float mDegree = 1;
  
  public IntersectionFragment() {
    super(
        NeighbourhoodIntersectionService.class, 
        NeighbourhoodIntersectionService.CATEGORY,
        new IntentFilter(IntersectionService.ACTION_DEGREE_CHANGED),
        IntersectionService.EPSILON_KEY);
  }
  
  @Override
  protected void onPropertyServiceAvailable(NeighbourhoodIntersectionService service) {
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
  
  // options menu
  
  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    super.onCreateOptionsMenu(menu, inflater);
    inflater.inflate(R.menu.intersection, menu);
    
    MenuItem item = menu.findItem(R.id.menu_degree);
    View view = item.getActionView();
    mDegreeText = (TextView) view.findViewById(R.id.degree_value);
    mDegreeBar = (ProgressBar) view.findViewById(R.id.degree_bar);
    
    // set the current degree
    setDegree(mDegree);
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
  
  /**
   * Udpates the degree of nearness, setting it to the given value.
   * @param degree
   */
  protected void onDegreeChanged(float degree) {
    mDegree = degree;
    setDegree(mDegree);
  }
  
  /**
   * Updates the bar and text to show the given degree of nearness.
   * @param degree
   */
  protected void setDegree(float degree) {
    mDegreeBar.setProgress((int) (DEGREE_STEPS - (degree * DEGREE_STEPS)));
    mDegreeText.setText(new Formatter().format(DEGREE_FORMAT, degree).toString());
  }
}
