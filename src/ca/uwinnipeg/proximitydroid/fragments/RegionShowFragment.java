/**
 * 
 */
package ca.uwinnipeg.proximitydroid.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import ca.uwinnipeg.proximitydroid.R;
import ca.uwinnipeg.proximitydroid.RotatedBitmap;
import ca.uwinnipeg.proximitydroid.views.RegionShowView;

import com.actionbarsherlock.app.SherlockFragment;

/**
 * @author Garrett Smith
 *
 */
public class RegionShowFragment extends SherlockFragment {
  
  // The bitmap being displayed
  protected RotatedBitmap mBitmap;
  
  private RegionShowView mShowView; 

  public static final String TAG = "RegionShowFragment";
  
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
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // retain state even if activity is destroyed
    setRetainInstance(true);
  }
  
  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    // restore the view's bitmap
    if (mBitmap != null) mShowView.setImageBitmap(mBitmap);
  }
  
  public void setBitmap(RotatedBitmap rbm) {
    mBitmap = rbm;
    mShowView.setImageBitmap(mBitmap);    
  }
}
