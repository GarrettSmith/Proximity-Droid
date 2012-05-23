package ca.uwinnipeg.compare;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

//TODO: Scale down bitmaps
// TODO: Zoom and pan to follow neighbourhood
/**
 * The activity can select neighbourhoods from an image. 
 * @author Garrett Smith
 *
 */
public class NeighbourhoodSelect extends Activity {
  
  public static final String TAG = "NeighbourhoodSelect";
  
  
  private ContentResolver mContentResolver;
  private Bitmap mBitmap;
  private int mOrientation;
  
  private SelectionView mSelectionView;
  private NeighbourhoodView mNeighbourhoodView;
  
  // constants
  
  private static final int SELECT_IMAGE = 1 << 0;
  
  private static final String BITMAP_KEY = "Bitmap";
  private static final String ORIENTATION_KEY = "Orientation";
  private static final String NEIGHBOURHOOD_BOUNDS_KEY = "Neighbourhood Bounds";

  /**
   * Called when this Activity is first created.
   */
  @Override
  protected void onCreate(Bundle state){
    super.onCreate(state);    
    setContentView(R.layout.neighbourhood_select);
    
    mSelectionView = (SelectionView) findViewById(R.id.selection_view);
    
    mContentResolver = getContentResolver();
    
    Rect bounds = null;

    // Check if state needs to be restored
    if (state != null) {
      mBitmap = state.getParcelable(BITMAP_KEY);
      mOrientation = state.getInt(ORIENTATION_KEY);
      bounds = state.getParcelable(NEIGHBOURHOOD_BOUNDS_KEY);
    }
    else {
      // Check if an intent with a given bitmap was sent
      Intent intent = getIntent();
      Bundle extras = intent.getExtras();

      if (extras != null) {
        mBitmap = (Bitmap) extras.getParcelable(BITMAP_KEY);
      }
    }
    
    // Request a bitmap if not given one
    if (mBitmap == null) {
      Intent i = new Intent(Intent.ACTION_PICK, Images.Media.INTERNAL_CONTENT_URI);
      startActivityForResult(i, SELECT_IMAGE);
    }
    else {
      init(bounds);
    }    
    
  }
  
  private void init(Rect bounds) {
    // set bitmap
    mSelectionView.setImageBitmap(mBitmap, mOrientation);
    // Setup neighbourhood
    mNeighbourhoodView = new NeighbourhoodView(mSelectionView);
    
    int width = mBitmap.getWidth();
    int height = mBitmap.getHeight();
    
    Rect imageRect = new Rect(0, 0, width, height);    
    mNeighbourhoodView.setImageRect(imageRect);
    
    mNeighbourhoodView.setMatrix(mSelectionView.getFinalMatrix());
    
    if (bounds != null) {
      mNeighbourhoodView.setBounds(bounds);
    }
    else {
      mNeighbourhoodView.resetBounds(); // setup default bounds
    }
      

    mSelectionView.add(mNeighbourhoodView);
  }
  
  // TODO save state of the current selection
  @Override
  protected void onSaveInstanceState(Bundle state) {    
    // Save the current image
    state.putParcelable(BITMAP_KEY, mBitmap);
    state.putInt(ORIENTATION_KEY, mOrientation);
    if (mNeighbourhoodView != null) {
      state.putParcelable(NEIGHBOURHOOD_BOUNDS_KEY, mNeighbourhoodView.getBounds());
    }
    super.onSaveInstanceState(state);
  }
  
  /**
   * Respond to activity results to receive images from other applications.
   */
  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == SELECT_IMAGE)
      if (resultCode == Activity.RESULT_OK) {
        loadImage(data.getData()); // Load the returned uri
        init(null);
      } 
      else {
        finish(); // If the intent failed or was cancelled exit
      }
  }
  
  /**
   * Loads the bitmap from data and sets the orientation and bitmap to the view.
   * @param data
   */
  protected void loadImage(Uri data) {
    // set the image to the received URI
    try {
      // load the bitmap
      mBitmap = MediaStore.Images.Media.getBitmap(mContentResolver, data);
      
      // Load the orientation
      ExifInterface exif = new ExifInterface(getRealPathFromURI(data));
      int orientation = 
          exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
      mOrientation = mapEXIF(orientation);
    }
    catch (Exception e) { // TODO: handle specific exceptions
      Log.e(TAG, "Failed to load file. " + data.toString());
    }
  }
  
  private static int mapEXIF(int exifVal) {
    switch (exifVal) {
    case ExifInterface.ORIENTATION_ROTATE_90:   return RotatedBitmap.CW;
    case ExifInterface.ORIENTATION_ROTATE_180:  return RotatedBitmap.UPSIDEDOWN;
    case ExifInterface.ORIENTATION_ROTATE_270:  return RotatedBitmap.CCW;
    default:                                    return RotatedBitmap.NORMAL;
    }
  }
  
  /**
   * modified from http://android-er.blogspot.ca/2011/04/convert-uri-to-real-path-format.html
   * @param contentUri
   * @return the file path represented by contentUri
   */
  private String getRealPathFromURI(Uri contentUri) {
    String[] proj = { MediaStore.Images.Media.DATA };
    Cursor cursor = mContentResolver.query(contentUri, proj, null, null, null);
    int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
    cursor.moveToFirst();
    cursor.close();
    return cursor.getString(column_index);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
      MenuInflater inflater = getMenuInflater();
      inflater.inflate(R.menu.neighbourhood_select, menu);
      mSelectionView.updateFinalMatrix(); // recenter
      return true;
  }
  
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
    case R.id.menu_about:
      showAbout();
      return true;
     default:
       return super.onOptionsItemSelected(item);
    }
  }
  
  /**
   * Displays the about dialog.
   */
  public void showAbout() {
    Intent i = new Intent(this, AboutActivity.class);
    startActivity(i);
  }

}
