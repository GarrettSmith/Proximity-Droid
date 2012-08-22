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
public class IntersectionFragment 
  extends UseNeighbourhoodPropertyFragment<IntersectionService, NeighbourhoodIntersectionService> {

  // The text view used to display the degree of nearness
  protected TextView mDegreeText;
  
  // the format used by the degree text
  public static final String DEGREE_FORMAT = " %1.2f";
  
  // The bar used to display a visual of the degree of nearness
  protected ProgressBar mDegreeBar;
  
  // the text view used to display the size of the union
  protected TextView mUnionText;
  
  // the text view used to display the size of the intersection
  protected TextView mIntersectionSizeText;
  
  // the number of steps in the degree bar
  protected final static int DEGREE_STEPS = 100;
  
  public int[] mPoints;
  
  // the current degree
  protected float mDegree = 1;
  
  // the current union size
  protected int mUnionSize = 0;
  
  protected MenuItem mUnionSizeItem;
  
  // the current instersection size
  protected int mIntersectionSize = 0;
  
  protected MenuItem mIntersectionsizeItem;
  
  public IntersectionFragment() {
    super(
        IntersectionService.class, 
        IntersectionService.CATEGORY,
        NeighbourhoodIntersectionService.class, 
        NeighbourhoodIntersectionService.CATEGORY,
        new IntentFilter(IntersectionService.ACTION_DEGREE_CHANGED),
        IntersectionService.EPSILON_KEY);
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
  
  // options menu
  
  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    super.onCreateOptionsMenu(menu, inflater);
    inflater.inflate(R.menu.intersection, menu);
    
    MenuItem item = menu.findItem(R.id.menu_degree);
    View view = item.getActionView();
    mDegreeText = (TextView) view.findViewById(R.id.degree_value);
    mDegreeBar = (ProgressBar) view.findViewById(R.id.degree_bar);
    
    mUnionSizeItem = menu.findItem(R.id.menu_union);
    if (mUnionSizeItem != null) {
      view = mUnionSizeItem.getActionView();
    }
    mUnionText = (TextView) view.findViewById(R.id.union_value);

    mIntersectionsizeItem = menu.findItem(R.id.menu_size);
    if (mIntersectionsizeItem != null) {
      view = mIntersectionsizeItem.getActionView();
    }
    mIntersectionSizeText = (TextView) view.findViewById(R.id.intersection_size_value);
    
    // set the values degree
    setDegree(mDegree);
    setUnion(mUnionSize);
    setIntersectionSize(mIntersectionSize);
  }
  
  @Override
  protected void onRecieve(Context context, Intent intent) {
    String action = intent.getAction();
    if (action.equals(IntersectionService.ACTION_DEGREE_CHANGED)) {
      onDegreeChanged(intent.getFloatExtra(IntersectionService.DEGREE, 1));
      onUnionChanged(intent.getIntExtra(IntersectionService.UNION_SIZE, 0));
      onIntersectionSizeChanged(intent.getIntExtra(IntersectionService.INTERSECTION_SIZE, 0));
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
    onDegreeChanged(1);
    onUnionChanged(0);
    onIntersectionSizeChanged(0);
  }
  
  /**
   * Udpates the degree of nearness, setting it to the given value.
   * @param degree
   */
  protected void onDegreeChanged(float degree) {
    mDegree = degree;
    setDegree(mDegree);
  }
  
  protected void onUnionChanged(int union) {
    mUnionSize = union;
    setUnion(mUnionSize);
  }
  
  protected void onIntersectionSizeChanged(int intersectionSize) {
    mIntersectionSize = intersectionSize;
    setIntersectionSize(mIntersectionSize);
  }
  
  @Override
  public void useNeighbourhoods(boolean enabled) {
    super.useNeighbourhoods(enabled);
    // keep degree up to date
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
  
  protected void setUnion(int union) {
    String unionStr = Integer.toString(union);
    mUnionText.setText(unionStr);
    mUnionSizeItem.setTitle(getResources().getString(R.string.intersection_union) + ' ' + unionStr);
    
  }
  
  protected void setIntersectionSize(int intersectionSize) {
    String str = Integer.toString(intersectionSize);
    mIntersectionSizeText.setText(str);
    mIntersectionsizeItem.setTitle(getResources().getString(R.string.intersection_size) + ' ' + str);
  }
}
