/**
 * 
 */
package ca.uwinnipeg.proximitydroid.fragments;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
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
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;
import ca.uwinnipeg.proximitydroid.R;
import ca.uwinnipeg.proximitydroid.RotatedBitmap;
import ca.uwinnipeg.proximitydroid.services.ProximityService;
import ca.uwinnipeg.proximitydroid.views.ProximityImageView;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

/**
 * A fragment that displays a {@link RotatedBitmap}.
 * @author Garrett Smith
 *
 */
public class ImageFragment<V extends ProximityImageView> extends SherlockFragment {
  
  public static final String TAG = "ImageFragment";

  // the bitmap displayed
  protected RotatedBitmap mBitmap;
  
  // the filename of the displayed bitmap
  protected String mFileName;

  // the view used to display the bitmap
  protected V mView;

  // used to register to receive broadcasts
  protected LocalBroadcastManager mBroadcastManager;
  
  // receives broadcasts when the bitmap changes
  protected BitmapChangedReceiver mBitmapchangedReceiver = new BitmapChangedReceiver();
  
  // the subdirectory in the pictures directory used to save screen shots
  public static final String PICTURES_DIRECTORY_NAME = "Proximity-System";  
  
  // Service
  
  private ProximityService mService;

  /**
   * Binds the given service to the fragment.
   * @param service
   */
  public void bindService(ProximityService service) {
    mService = service;
    onServiceAttached(mService);
  }  
  
  /**
   * Returns the currently bound service or null if no service is bound.
   * @return
   */
  public ProximityService getService() {
    return mService;
  }
  
  /**
   * Callback for when the {@link ProximityService} is bound to the fragment.
   * @param service the bound service
   */
  protected void onServiceAttached(ProximityService service) {
    // first time bitmap setup
    if (service.hasBitmap()) {
      mBitmap = service.getBitmap();
      mFileName = service.getFileName();
    }
  }
  
  // Image saving
  
  /**
   * Saves the image given all the settings.
   * @param format
   * @param quality
   * @param stream
   * @return
   */
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
    boolean rtn = false;
    try {
      output = new FileOutputStream(file);
      rtn = saveImage(CompressFormat.PNG, 0, output);
      output.close();
      return rtn;
    } 
    catch (FileNotFoundException e) {
      Log.e(TAG, e.toString());
      return false;
    } 
    catch (IOException e) {
      Log.e(TAG, e.toString());
      // return whether the image save successfully
      return rtn;
    }
  }
  
  /**
   * Saves the image in the images media folder with the given name.
   * @param fileName
   * @return
   */
  public boolean saveImage(String fileName) {
    
    // check if we can write to external storage
    String state = Environment.getExternalStorageState();
    if (!Environment.MEDIA_MOUNTED.equals(state)) return false;
    
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
  
  // Lifecycle
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // set that this fragment has an options menu
    setHasOptionsMenu(true);
    setRetainInstance(true);
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
      
      // save image off of the ui thread
      new AsyncTask<Void, Void, Boolean>() {

        @Override
        protected Boolean doInBackground(Void... params) {
          return saveImage(getFileName());
        }
        
        protected void onPostExecute(Boolean result) {
          // TODO: give better error message
          String msg = result ? getString(R.string.save_success) : getString(R.string.save_fail);
          Toast.makeText(getActivity(), msg, Toast.LENGTH_LONG).show();
        };
        
      }.execute();
      
      return true;
    }
    else {
      return super.onOptionsItemSelected(item);
    }
  }
  
  /**
   * Returns the file name used to save screen shots.
   * <p>
   * The path is something like {PICTURES_DIRECTORY}/ProximityDroid/{BITMAP_NAME}-1.jpg and
   * increments as you save more shots.
   * @return
   */
  protected String getFileName() {
    File path = new File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), 
        PICTURES_DIRECTORY_NAME);
    
    // get the file's name
    StringBuilder str = new StringBuilder(mFileName);
    str.append('-');
    
    // append number
    for (int i = 0; true; i++) {
      if (!new File(path, str.toString() + i + ".png").exists()) {
        str.append(i);
        break;
      }
    }
    
    // append file type
    str.append(".png");
    return str.toString();
  }
  
  /**
   * Requests the draw method to be called. This does nothing if the bitmap is not set.
   * <p>
   * The service is not guaranteed to be attached here.
   */
  public void invalidate() {
    if (mBitmap != null) draw();
  }
  
  /**
   * Called by invalidate. This should be called by subclasses to redraw the bitmap and overridden
   * by them to draw changes.
   */
  protected void draw() {
    mView.setImageBitmap(mBitmap);
  }  

  /**
   * Sets the bitmap and its filename and invalidates the fragment.
   * @param bm
   * @param fileName
   */
  public void setBitmap(RotatedBitmap bm, String fileName) {
    mBitmap = bm;
    mFileName = fileName;
    invalidate();
  } 
  
  /**
   * Register to broadcasts.
   */
  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
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
    // disconnect from the service 
    mService = null;
  }
  
  // Broadcasts
  // These are used to update the views when a change or calculation has finished
  /**
   * Watches for changes in the bitmap and updates the fragment when the occur.
   * @author Garrett Smith
   *
   */
  public class BitmapChangedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();
      if (action.equals(ProximityService.ACTION_BITMAP_SET)) {
        RotatedBitmap bm = intent.getParcelableExtra(ProximityService.BITMAP);
        if (bm != null) {
          String fileName = intent.getStringExtra(ProximityService.FILE_NAME);
          setBitmap(bm, fileName);
        }
      }
    }
    
  }

}
