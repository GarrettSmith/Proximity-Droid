/**
 * 
 */
package ca.uwinnipeg.proximitydroid.fragments;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import ca.uwinnipeg.proximity.image.Image;
import ca.uwinnipeg.proximitydroid.R;
import ca.uwinnipeg.proximitydroid.Region;
import ca.uwinnipeg.proximitydroid.RotatedBitmap;
import ca.uwinnipeg.proximitydroid.views.RegionShowView;
import ca.uwinnipeg.proximitydroid.views.RegionView;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

/**
 * @author Garrett Smith
 *
 */
// TODO: Add selecting regions
// TODO: Add pan and zooming
public class RegionShowFragment extends SherlockFragment {

  public static final String TAG = "RegionShowFragment";
  
  protected RegionShowView mShowView; 
  
  protected RegionProvider mProvider;  
  public interface RegionProvider {
    public List<Region> getRegions();
    public RotatedBitmap getBitmap();
    public Image getImage();
  }
  
  protected OnAddRegionSelecetedListener mListener;  
  public interface OnAddRegionSelecetedListener {
    public void onAddRegionSelected();
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
    return mShowView;
  }
  
  @Override
  public void onStart() {
    super.onStart();
    if (mProvider != null) setupView();
  }
  
  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);    
    try {
      mProvider = (RegionProvider) activity;
    } catch (ClassCastException e) {
      throw new ClassCastException(activity.toString() + 
          " must implement RegionProvider");
    }
    super.onAttach(activity);    
    try {
      mListener = (OnAddRegionSelecetedListener) activity;
    } catch (ClassCastException e) {
      throw new ClassCastException(activity.toString() + 
          " must implement OnAddRegionSelecetedListener");
    }  
  }
  
  protected void setupView() {
    RotatedBitmap bm = mProvider.getBitmap();
    if (bm != null) {
      setBitmap(bm);

      for (Region r : mProvider.getRegions()) {        
        // add the region to be drawn
        RegionView rv = new RegionView(mShowView);
        rv.setBounds(r.getBounds());
        rv.setPolygon(r.getPolygon());
        rv.setShape(r.getShape());
        mShowView.add(rv);
      }

      Image image = mProvider.getImage();
      List<Integer> indices = getRelevantPixels(mProvider.getRegions(), image);
      int[] points = new int[indices.size() * 2];
      for (int i = 0; i < indices.size(); i++) {
        int index = indices.get(i);
        points[i*2] = image.getX(index);
        points[i*2 + 1] = image.getY(index);
      }
      mShowView.setRelevantPoints(points);
    }    

  }

  /**
   * Subclasses must implement this to higlight pixels.
   * @return
   */
  protected List<Integer> getRelevantPixels(List<Region> regions, Image image) {
    return new ArrayList<Integer>();
  }

  public void setBitmap(RotatedBitmap bm) {
    mShowView.setImageBitmap(bm);
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
