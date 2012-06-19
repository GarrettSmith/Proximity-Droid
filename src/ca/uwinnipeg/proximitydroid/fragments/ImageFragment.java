/**
 * 
 */
package ca.uwinnipeg.proximitydroid.fragments;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
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
  
  public static final String PICTURES_DIRECTORY_NAME = "ProximityDroid";
  
  public interface ProximityServiceProvider {
    public ProximityService getService();
  }
  
  public ProximityService getService() {
    return mProvider.getService();
  }
  
  /**
   * Saves the image given all the settings.
   * @param format
   * @param quality
   * @param stream
   * @return
   */
  public boolean saveImage(Bitmap.CompressFormat format, int quality, OutputStream stream) {
    mView.setDrawingCacheEnabled(true);
    Bitmap bm = mView.getDrawingCache();
    return bm.compress(format, quality, stream);
  }
  
  /**
   * Saves the image in the given file.
   * @param file
   * @return
   */
  public boolean saveImage(File file) {
    FileOutputStream output;
    try {
      output = new FileOutputStream(file);
    } catch (FileNotFoundException e) {
      Log.e(TAG, file + " not found.");
      return false;
    }
    return saveImage(CompressFormat.JPEG, 95, output);
  }
  
  /**
   * Saves the image in the images media folder with the given name.
   * @param fileName
   * @return
   */
  // TODO: check if media is mounted
  public boolean saveImage(String fileName) {
    File path = new File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), 
        PICTURES_DIRECTORY_NAME);
    File file = new File(path, fileName);
    
    // Make sure the directory exists
    path.mkdirs();
    
    // save the image
    if (saveImage(file)) {
      // tell the media scanner about the file so the user has immediate access
      MediaScannerConnection.scanFile(
          getActivity(), 
          new String[]{ file.toString() }, 
          null,
          new MediaScannerConnection.OnScanCompletedListener() {
            public void onScanCompleted(String path, Uri uri) {
              Log.i("ExternalStorage", "Scanned " + path + ":");
              Log.i("ExternalStorage", "-> uri=" + uri);
            }
          });
      return true;
    }
    else {
      return false;
    }
  }

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
