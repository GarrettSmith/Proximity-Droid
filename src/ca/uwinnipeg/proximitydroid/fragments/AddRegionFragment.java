package ca.uwinnipeg.proximitydroid.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.SpinnerAdapter;
import android.widget.Toast;
import ca.uwinnipeg.proximity.image.Image;
import ca.uwinnipeg.proximitydroid.R;
import ca.uwinnipeg.proximitydroid.Region;
import ca.uwinnipeg.proximitydroid.Region.Shape;
import ca.uwinnipeg.proximitydroid.services.ProximityService;
import ca.uwinnipeg.proximitydroid.views.AddRegionView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.OnNavigationListener;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

/**
 * Fragment used to add {@link Region} to the {@link Image}.
 * @author Garrett Smith
 *
 */
public class AddRegionFragment 
  extends ImageFragment<AddRegionView>
  implements ActionBar.OnNavigationListener {

  public static final String TAG = "RegionSelectFragment";

  // Indices of the items in the drop down menu.
  // Must match the string array shape_list in arrays.xml and the ordinal values of
  // NeighbourhoodView.Shape.
  // Used by onNavigationItemSelected.
  public static final int BUTTON_RECTANGLE_INDEX = 0;
  public static final int BUTTON_OVAL_INDEX = 1;
  public static final int BUTTON_POLYGON_INDEX = 2;

  
  // the region being added
  private Region mRegion;

  // UI
  private SpinnerAdapter mSpinnerAdapter;

  // Result keys
  public static final String RESULT_KEY_BOUNDS = "Bounds";
  public static final String RESULT_KEY_SHAPE = "Shape";
  public static final String RESULT_KEY_POLY = "Poly";

  // Containers must implement this interface so we can add regions to them
  OnClosedListener mListener;
  public interface OnClosedListener {
    public void onClosed();
  }
  
  ListNavigationProvider mProvider;
  public interface ListNavigationProvider {
    public void setListNavigationCallbacks(
        SpinnerAdapter adapter, 
        OnNavigationListener listener);
    public void resetListNavigationCallbacks();
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // retain state even if activity is destroyed
    setRetainInstance(true);
  }
  
  @Override
  protected void onServiceAttached(ProximityService service) {
    super.onServiceAttached(service);
    mRegion = new Region(service.getImage());
    mRegion.resetBounds();
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, 
      ViewGroup container,
      Bundle savedInstanceState) {
    super.onCreateView(inflater, container, savedInstanceState);
    mView = (AddRegionView) inflater.inflate(R.layout.add_region, container, false); 
    mView.setRegion(mRegion);
    return mView;
  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    try {
      mListener = (OnClosedListener) activity;
    } catch (ClassCastException e) {
      throw new ClassCastException(activity.toString() + 
          " must implement OnClosedListener");
    }
    try {
      mProvider = (ListNavigationProvider) activity;
    } catch (ClassCastException e) {
      throw new ClassCastException(activity.toString() + 
          " must implement ListNavigationProvider");
    }
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    super.onCreateOptionsMenu(menu, inflater);
    inflater.inflate(R.menu.region_select, menu);
    
    // Action bar navigation
    mSpinnerAdapter = ArrayAdapter.createFromResource(
        getActivity(), 
        R.array.shape_list, 
        R.layout.sherlock_spinner_dropdown_item);
    mProvider.setListNavigationCallbacks(mSpinnerAdapter, this);
  }
  
  @Override
  public void onDestroyOptionsMenu() {
    super.onDestroyOptionsMenu();
    mProvider.resetListNavigationCallbacks();
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menu_reset:
        // TODO: mRegion.reset();
        return true;

      case R.id.menu_accept:
        // check for trying to save a polygon with less than 3 points
        if (!(mRegion.getShape() == Shape.POLYGON && mRegion.getPolygon().size() < 3)) {
          getService().addRegion(mRegion);
          mListener.onClosed();
        }
        else {
          // print the error message that the polygon is invalid
          Toast.makeText(getActivity(), R.string.invalid_polygon, Toast.LENGTH_LONG).show();          
        }
        return true;

      case R.id.menu_cancel:
        mListener.onClosed();
        return true;

      default:
        return false;
    }
  }

  @Override
  public boolean onNavigationItemSelected(int position, long itemId) {
    if (mRegion != null) {
      switch(position) {
        case BUTTON_RECTANGLE_INDEX:
          mRegion.setShape(Region.Shape.RECTANGLE);
          break;
        case BUTTON_OVAL_INDEX:
          mRegion.setShape(Region.Shape.OVAL);
          break;
        case BUTTON_POLYGON_INDEX:
          mRegion.setShape(Region.Shape.POLYGON);
          break;     
      }
    }
    return true;
  }

}
