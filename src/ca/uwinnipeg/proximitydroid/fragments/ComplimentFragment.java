/**
 * 
 */
package ca.uwinnipeg.proximitydroid.fragments;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

/**
 * @author Garrett Smith
 *
 */
public class ComplimentFragment extends RegionShowFragment {

  
  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    IntentFilter filter = new IntentFilter();
    mBroadcastManager.registerReceiver(mIntersectionReceiver, filter);    
  }
  
  @Override
  public void onDestroy() {
    mBroadcastManager.unregisterReceiver(mIntersectionReceiver);
    super.onDestroy();
  }
  
  @Override
  protected void setupView() {
    super.setupView();
  }
  
  // broadcasts
  
  protected ComplimentReceiver  mIntersectionReceiver = new ComplimentReceiver ();
  
  public class ComplimentReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();
      // TODO: receive compliment broadcasts
    }
    
  }
}
