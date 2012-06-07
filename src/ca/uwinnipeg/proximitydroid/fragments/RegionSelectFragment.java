package ca.uwinnipeg.proximitydroid.fragments;

import android.app.Activity;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.SpinnerAdapter;
import ca.uwinnipeg.proximitydroid.R;
import ca.uwinnipeg.proximitydroid.Region;
import ca.uwinnipeg.proximitydroid.RegionView;
import ca.uwinnipeg.proximitydroid.RotatedBitmap;
import ca.uwinnipeg.proximitydroid.views.RegionSelectView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

// TODO: Fix spinner on 2.1
// TODO: Deal with Failed Binder Transaction
// TODO: Change to fragment?
/**
 * The activity can select neighbourhoods from an image. 
 * @author Garrett Smith
 *
 */
public class RegionSelectFragment 
extends SherlockFragment 
implements ActionBar.OnNavigationListener {

  public static final String TAG = "RegionSelectFragment";

  // Indices of the items in the drop down menu.
  // Must match the string array shape_list in arrays.xml and the ordinal values of
  // NeighbourhoodView.Shape.
  // Used by onNavigationItemSelected.
  public static final int BUTTON_RECTANGLE_INDEX = 0;
  public static final int BUTTON_OVAL_INDEX = 1;
  public static final int BUTTON_POLYGON_INDEX   = 2;

  private RotatedBitmap mBitmap;

  private RegionSelectView mSelectView;
  private RegionView mRegionView;

  // UI
  private SpinnerAdapter mSpinnerAdapter;

  // Result keys
  public static final String RESULT_KEY_BOUNDS = "Bounds";
  public static final String RESULT_KEY_SHAPE = "Shape";
  public static final String RESULT_KEY_POLY = "Poly";

  // Containers must implement this interface so we can add regions to them
  OnRegionSelectedListener mListener;
  public interface OnRegionSelectedListener {
    public void onRegionSelected(Region region);
    public void onRegionCanceled();
  }

  public RegionSelectFragment(RotatedBitmap rbm) {
    mBitmap = rbm;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // retain state even if activity is destroyed
    setRetainInstance(true);
    // declare there are items to be added to the action bar
    setHasOptionsMenu(true);
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, 
      ViewGroup container,
      Bundle savedInstanceState) {
    super.onCreateView(inflater, container, savedInstanceState);
    mSelectView =  (RegionSelectView) inflater.inflate(R.layout.region_select, container, false);    
    mRegionView = mSelectView.getNeighbourhood();
    // set bitmap
    mSelectView.setImageBitmap(mBitmap);

    int width = mBitmap.getWidth();
    int height = mBitmap.getHeight();

    Rect imageRect = new Rect(0, 0, width, height);    
    mRegionView.setImageRect(imageRect);

    mRegionView.resetBounds();
    return mSelectView;
  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    try {
      mListener = (OnRegionSelectedListener) activity;
    } catch (ClassCastException e) {
      throw new ClassCastException(activity.toString() + 
          " must implement OnRegionSelectedListener");
    }
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.region_select, menu);
    
    // Setup the actionbar
    // TODO: Remove this cast
    ActionBar bar = ((SherlockFragmentActivity)getActivity()).getSupportActionBar();

    // Action bar navigation
    mSpinnerAdapter = ArrayAdapter.createFromResource(
        getActivity(), 
        R.array.shape_list, 
        android.R.layout.simple_spinner_dropdown_item);

    bar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);   
    bar.setListNavigationCallbacks(mSpinnerAdapter, this);
  }

  @Override
  public void onDestroyOptionsMenu() {
    super.onDestroyOptionsMenu();
    // Tear down the actionbar
    // TODO: Remove this cast
    ActionBar bar = ((SherlockFragmentActivity)getActivity()).getSupportActionBar();
    bar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menu_reset:
        mRegionView.reset();
        return true;

      case R.id.menu_accept:
        mListener.onRegionSelected(mRegionView);
        return true;

      case R.id.menu_cancel:
        mListener.onRegionCanceled();
        return true;

      default:
        return false;
    }
  }


  @Override
  public boolean onNavigationItemSelected(int position, long itemId) {
    switch(position) {
      case BUTTON_RECTANGLE_INDEX:
        mRegionView.setShape(RegionView.Shape.RECTANGLE);
        break;
      case BUTTON_OVAL_INDEX:
        mRegionView.setShape(RegionView.Shape.OVAL);
        break;
      case BUTTON_POLYGON_INDEX:
        mRegionView.setShape(RegionView.Shape.POLYGON);
        break;     
    }
    return true;
  }

}
