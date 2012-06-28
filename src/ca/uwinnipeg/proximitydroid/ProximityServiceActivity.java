/**
 * 
 */
package ca.uwinnipeg.proximitydroid;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import ca.uwinnipeg.proximitydroid.services.LocalBinder;
import ca.uwinnipeg.proximitydroid.services.ProximityService;

import com.actionbarsherlock.app.SherlockFragmentActivity;

/**
 * An activity that starts and binds to the proximity service.
 * @author Garrett Smith
 *
 */
public abstract class ProximityServiceActivity extends SherlockFragmentActivity {
  
  public static final String TAG = "ProximityServiceActivity";
  
  // The service we are bound to
  private ProximityService mService;
  
  public ProximityService getService() {
    return mService;
  }

  // Whether we are currently bound
  private boolean mBound = false;
  
  public boolean isBound() {
    return mBound;
  }
  
  /**
   * Callbacks for binding the service.
   */
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
  
  protected void onProximityServiceConnected() {};
  protected void onProximityServiceDisconnected() {};
  
  // lifecycle
  
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    Intent intent = new Intent(this, ProximityService.class);
    // start the service
    startService(intent);
    // bind to service
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
