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
import ca.uwinnipeg.proximitydroid.RegionView;
import ca.uwinnipeg.proximitydroid.RotatedBitmap;
import ca.uwinnipeg.proximitydroid.views.RegionShowView;

import com.actionbarsherlock.app.SherlockFragment;

/**
 * @author Garrett Smith
 *
 */
public class RegionShowFragment extends SherlockFragment {

  public static final String TAG = "RegionShowFragment";
  
  private RegionShowView mShowView; 
  
  private RegionProvider mProvider;
  
  public interface RegionProvider {
    public List<Region> getRegions();
    public RotatedBitmap getBitmap();
  }
  
  @Override
  public View onCreateView(
      LayoutInflater inflater, 
      ViewGroup container,
      Bundle savedInstanceState) {
    super.onCreateView(inflater, container, savedInstanceState);
    mShowView =  (RegionShowView) inflater.inflate(R.layout.region_show, container, false);
    if (mProvider != null) setupView();
    return mShowView;
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
    setupView();    
  }
  
  private void setupView() {
    RotatedBitmap bm = mProvider.getBitmap();
    if (bm != null) {
      setBitmap(bm);
    }
    
    for (Region r : mProvider.getRegions()) {
      RegionView rv = new RegionView(mShowView);
      rv.setBounds(r.getBounds());
      rv.setPolygon(r.getPolygon());
      rv.setShape(r.getShape());
      mShowView.add(rv);
    }
  }
  
  public void setBitmap(RotatedBitmap bm) {
    mShowView.setImageBitmap(bm);
  }
}
