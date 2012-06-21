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
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import ca.uwinnipeg.proximitydroid.ProximityService;
import ca.uwinnipeg.proximitydroid.R;
import ca.uwinnipeg.proximitydroid.RotatedBitmap;
import ca.uwinnipeg.proximitydroid.views.ProximityImageView;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

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
  // TODO: notify on save
  // TODO: take off ui thread
  public boolean saveImage(Bitmap.CompressFormat format, int quality, OutputStream stream) {
    Bitmap bm = mView.getCroppedBitmap();
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
      Log.e(TAG, e.toString());
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
    
    // TODO: check if the file already exists
    
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
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // set that this fragment has an options menu
    setHasOptionsMenu(true);
  }

  @Override
  public void onStart() {
    super.onStart();
    invalidate();
  }
  
  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    super.onCreateOptionsMenu(menu, inflater);
    inflater.inflate(R.menu.image, menu);
  }
  
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == R.id.menu_save_image) {
      // TODO: set image name
      saveImage("TEST.jpg");
      return true;
    }
    else {
      return super.onOptionsItemSelected(item);
    }
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
