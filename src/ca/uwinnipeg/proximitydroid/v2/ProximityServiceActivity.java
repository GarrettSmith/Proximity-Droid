/**
 * 
 */
package ca.uwinnipeg.proximitydroid.v2;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import ca.uwinnipeg.proximitydroid.v2.services.LocalBinder;
import ca.uwinnipeg.proximitydroid.v2.services.ProximityService;

import com.actionbarsherlock.app.SherlockFragmentActivity;

/**
 * An activity that starts and binds to the {@link ProximityService}.
 * @author Garrett Smith
 *
 */
public abstract class ProximityServiceActivity extends SherlockFragmentActivity {
  
  public static final String TAG = "ProximityServiceActivity";
  
  // The service we are bound to
  private ProximityService mService;

  // Whether we are currently bound
  private boolean mBound = false;
  
  /**
   * Returns the {@link ProximityService} we are connected to or null
   * @return
   */
  public ProximityService getService() {
    return mService;
  }
  
  /**
   * Returns true if we are bound to the service and false otherwise.
   * @return
   */
  public boolean isBound() {
    return mBound;
  }
  
  // the connection to the service
  private ServiceConnection mConnection = new ProximityServiceConnection();
  
  protected class ProximityServiceConnection implements ServiceConnection {
    
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
      @SuppressWarnings("unchecked")
      LocalBinder<ProximityService> binder = (LocalBinder<ProximityService>) service;
      mService = binder.getService();
      mBound = true;
      
      Log.i(TAG, "Binding service");

      onProximityServiceConnected();
    }
    
    @Override
    public void onServiceDisconnected(ComponentName name) {
      onProximityServiceDisconnected();
      mBound = false;
      mService = null;
    }
  }
  
  // service callbacks
  
  /**
   * This method is called when the {@link ProximityService} is connected and available to the 
   * activity through getService();
   */
  protected void onProximityServiceConnected() {};
  
  /**
   * This method is called when the {@link ProximityService} is no longer available to the activity.
   */
  protected void onProximityServiceDisconnected() {};
  
  // lifecycle
  
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    Intent intent = new Intent(this, ProximityService.class);
    // start the service
    startService(intent);
    // bind to the service
    bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
  }

  @Override
  protected void onDestroy() {
    // stop the service if we are finishing
    if (isFinishing()) {
      Log.i(TAG, "Stopping service");
      Intent intent = new Intent(this, ProximityService.class);
      stopService(intent);
    }
    // release the service
    if (mBound) {
      Log.i(TAG, "Unbinding service");
      unbindService(mConnection);
      mBound = false;
    }
    super.onDestroy();
  }
}
