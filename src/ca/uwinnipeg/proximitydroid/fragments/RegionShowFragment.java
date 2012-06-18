/**
 * 
 */
package ca.uwinnipeg.proximitydroid.fragments;

import java.util.List;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import ca.uwinnipeg.proximitydroid.ProximityService;
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
    mView = (RegionShowView) inflater.inflate(R.layout.region_show, container, false);
    return mView;
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
    // register receiver
    IntentFilter filter = new IntentFilter(ProximityService.ACTION_REGION_ADDED);
    filter.addAction(ProximityService.ACTION_REGIONS_CLEARED);
    mBroadcastManager.registerReceiver(mRegionsChangedReceiver, filter);
  }
  
  @Override
  public void onDestroy() {
    mBroadcastManager.unregisterReceiver(mRegionsChangedReceiver);
    super.onDestroy();
  }

  @Override
  protected void setupView() {
    super.setupView();

    mView.clear();
    for (Region reg : mRegions) {
      mView.add(reg);
    }

    // TODO: setHighlight(mProvider.getHighlightIndices());
  }

  public void setHighlight(int[] indices) {
    if (indices == null) {
      mView.clearHighlight();
    }
    else {
      mView.setHighlight(indices);
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
      case R.id.menu_clear: 
        // TODO: broadcast regions cleared
        Intent intent = new Intent(ProximityService.ACTION_CLEAR_REGIONS);
        mBroadcastManager.sendBroadcast(intent);
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }
  
  // broadcasts
  
  protected RegionsChangedReciever mRegionsChangedReceiver = new RegionsChangedReciever();
  
  public class RegionsChangedReciever extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();
      if (action.equals(ProximityService.ACTION_REGION_ADDED)) {
        Region r = intent.getParcelableExtra(ProximityService.REGION);
        mRegions.add(r);
        mView.add(r);
      }
      else if (action.equals(ProximityService.ACTION_REGIONS_CLEARED)) {
        mRegions.clear();
        mView.clear();
      }
    }
    
  }
}
