/**
 * 
 */
package ca.uwinnipeg.proximitydroid.services;

import java.lang.ref.WeakReference;

import android.os.Binder;

/**
 * A generic implementation of Binder to be used for local services
 * From http://www.ozdroid.com/#!BLOG/2010/12/19/How_to_make_a_local_Service_and_bind_to_it_in_Android
 * @author Geoff Bruckner  12th December 2009
 *
 * @param <S> The type of the service being bound
 */

public class LocalBinder<S> extends Binder {
  
  @SuppressWarnings("unused")
  private static final String TAG = "LocalBinder";
  
  private WeakReference<S> mService;

  public LocalBinder(S service){
    mService = new WeakReference<S>(service);
  }

  public S getService() {
    return mService.get();
  }
}