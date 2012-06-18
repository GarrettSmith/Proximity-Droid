/**
 * 
 */
package ca.uwinnipeg.proximitydroid.fragments;

import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import ca.uwinnipeg.proximitydroid.R;
import ca.uwinnipeg.proximitydroid.Region;
import ca.uwinnipeg.proximitydroid.RotatedBitmap;
import ca.uwinnipeg.proximitydroid.views.RegionShowView;
import ca.uwinnipeg.proximitydroid.views.RegionView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

/**
 * @author Garrett Smith
 *
 */
// TODO: Add selecting regions
// TODO: Add pan and zooming
public class RegionShowFragment extends ImageFragment<RegionShowView> {

  public static final String TAG = "RegionShowFragment";
  
  protected RegionShowView mShowView; 
  
  protected OnAddRegionSelectedListener mListener;  
  
  public interface OnAddRegionSelectedListener {
    public void onAddRegionSelected();
  }
  
  protected List<Region> mRegions;
  
  public RegionShowFragment(List<Region> regions, RotatedBitmap bitmap) {
    super(bitmap);
    mRegions = regions;
  }
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // declare there are items to be added to the action bar
    setHasOptionsMenu(true);
  }
  
  @Override
  public View onCreateView(
      LayoutInflater inflater, 
      ViewGroup container,
      Bundle savedInstanceState) {
    super.onCreateView(inflater, container, savedInstanceState);
    mShowView =  (RegionShowView) inflater.inflate(R.layout.region_show, container, false);
    mView = mShowView;
    return mShowView;
  }
  
  @Override
  public void onStart() {
    super.onStart();
    //if (mProvider != null) setupView();
    
  }
  
  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);    
    try {
      mListener = (OnAddRegionSelectedListener) activity;
    } catch (ClassCastException e) {
      throw new ClassCastException(activity.toString() + 
          " must implement OnAddRegionSelecetedListener");
    }  
  }

  @Override
  protected void setupView() {
    super.setupView();

    for (Region r : mRegions) {        
      // add the region to be drawn
      RegionView rv = new RegionView(mShowView);
      rv.setBounds(r.getBounds());
      rv.setPolygon(r.getPolygon());
      rv.setShape(r.getShape());
      mShowView.add(rv);
    }

    // TODO: setHighlight(mProvider.getHighlightIndices());
  }

  public void setHighlight(int[] indices) {
    if (indices == null) {
      mShowView.clearHighlight();
    }
    else {
      mShowView.setHighlight(indices);
    }
  }
  
  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.region_show, menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menu_add:
        mListener.onAddRegionSelected();
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }
}
