/**
 * 
 */
package ca.uwinnipeg.proximitydroid.fragments;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import ca.uwinnipeg.proximitydroid.ProximityService;
import ca.uwinnipeg.proximitydroid.RotatedBitmap;
import ca.uwinnipeg.proximitydroid.views.ProximityImageView;

import com.actionbarsherlock.app.SherlockFragment;

/**
 * @author Garrett Smith
 *
 */
public class ImageFragment<V extends ProximityImageView> extends SherlockFragment {
  
  public static final String TAG = "ImageFragment";

  protected RotatedBitmap mBitmap;

  protected V mView;

  protected LocalBroadcastManager mBroadcastManager;
  
  protected BitmapChangedReceiver mBitmapchangedReceiver = new BitmapChangedReceiver();
  
  protected ProximityServiceProvider mProvider;
  
  public interface ProximityServiceProvider {
    public ProximityService getService();
  }
  
  public ProximityService getService() {
    return mProvider.getService();
  }

//  public ImageFragment(RotatedBitmap bitmap) {
//    mBitmap = bitmap; 
//  }

  @Override
  public void onStart() {
    super.onStart();
    invalidate();
  }
  
  protected void setupView() {
    mView.setImageBitmap(mBitmap);
  }  

  public void setBitmap(RotatedBitmap bm) {
    mBitmap = bm;
    invalidate();
  } 
  
  public void invalidate() {
    if (mBitmap != null) setupView();
  }
  
  /**
   * Register to broadcasts.
   */
  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);    
    // setup access to the service
    try {
      mProvider = (ProximityServiceProvider) activity;
    } catch (ClassCastException e) {
      throw new ClassCastException(activity.toString() + 
          " must implement ProximityServiceProvider");
    }  
    // first time bitmap setup
    mBitmap = getService().getBitmap();
    // get the application's broadcast manager
    mBroadcastManager = LocalBroadcastManager.getInstance(activity.getApplicationContext());
    // register to receive bitmap broadcasts
    IntentFilter filter = new IntentFilter(ProximityService.ACTION_BITMAP_SET);
    mBroadcastManager.registerReceiver(mBitmapchangedReceiver, filter);
  }
  
  /**
   * Unregister from broadcasts.
   */
  @Override
  public void onDestroy() {
    super.onDestroy();
    // unregister from broadcasts
    mBroadcastManager.unregisterReceiver(mBitmapchangedReceiver);
  }
  
  // Broadcasts
  // These are used to update the views when a change or calculation has finished
  
  public class BitmapChangedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();
      if (action.equals(ProximityService.ACTION_BITMAP_SET)) {
        RotatedBitmap bm = intent.getParcelableExtra(ProximityService.BITMAP);
        if (bm != null) {
          setBitmap(bm);
        }
      }
    }
    
  }
  

}
