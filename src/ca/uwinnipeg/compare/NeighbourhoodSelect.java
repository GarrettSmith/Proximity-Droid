package ca.uwinnipeg.compare;

import android.app.ActionBar;
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
import android.widget.ArrayAdapter;
import android.widget.SpinnerAdapter;

// TODO: Scale down bitmaps
// TODO: Zoom and pan to follow neighbourhood
// TODO: Look into why image shifts after rotate, STATUS BAR does this need to be fixed?
// TODO: Support down to 2.1
/**
 * The activity can select neighbourhoods from an image. 
 * @author Garrett Smith
 *
 */
public class NeighbourhoodSelect 
extends Activity 
implements ActionBar.OnNavigationListener {

  public static final String TAG = "NeighbourhoodSelect";

  // Indices of the items in the drop down menu.
  // Must match the string array shape_list in arrays.xml and the ordinal values of
  // NeighbourhoodView.Shape.
  // Used by onNavigationItemSelected.
  public static final int BUTTON_RECTANGLE_INDEX = 0;
  public static final int BUTTON_OVAL_INDEX = 1;
  public static final int BUTTON_POLYGON_INDEX   = 2;

  private ContentResolver mContentResolver;
  
  private Bitmap mBitmap;
  private int mOrientation;

  private SelectView mselectView;
  private NeighbourhoodView mNeighbourhoodView;
  
  // Used to restore state properly without having the restored bounds overwritten.
  // TODO: Is this really what I should be using a runnable for?
  private Runnable mOnCreateRunnable = null;

  // UI
  private ActionBar mActionBar;
  private SpinnerAdapter mSpinnerAdapter;

  // Intents
  private static final int NAVIGATION_ITEM_SELECT_IMAGE = 0;

  // bundle keys
  private static final String BUNDLE_KEY_BITMAP = "Bitmap";
  private static final String BUNDLE_KEY_ORIENTATION = "Orientation";
  private static final String BUNDLE_KEY_BOUNDS = "Bounds";
  private static final String BUNDLE_KEY_SHAPE = "Shape";

  /**
   * Called when this Activity is first created.
   */
  @Override
  protected void onCreate(Bundle state){
    super.onCreate(state);    
    setContentView(R.layout.neighbourhood_select);

    mContentResolver = getContentResolver();

    // UI
    mActionBar = getActionBar();
    mActionBar.setDisplayHomeAsUpEnabled(true);

    // Action bar navigation
    mSpinnerAdapter = ArrayAdapter.createFromResource(
        this, 
        R.array.shape_list, 
        android.R.layout.simple_spinner_dropdown_item);

    mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);   
    mActionBar.setListNavigationCallbacks(mSpinnerAdapter, this);

    // setup view
    mselectView = (SelectView) findViewById(R.id.select_view);

    // Check if state needs to be restored
    if (state != null) {
      mBitmap = state.getParcelable(BUNDLE_KEY_BITMAP);
      mOrientation = state.getInt(BUNDLE_KEY_ORIENTATION);
      final Rect bounds = state.getParcelable(BUNDLE_KEY_BOUNDS);
      NeighbourhoodView.Shape shape = 
          NeighbourhoodView.Shape.valueOf(state.getString(BUNDLE_KEY_SHAPE, ""));
      if (shape != null) {
        // Restore selected shape in spinner which then sets the neighbourhood shape
        mActionBar.setSelectedNavigationItem(shape.ordinal());
      }
      if (bounds != null) {
        // Create a runnable to restore the bounds after the shape has been restored
        mOnCreateRunnable = new Runnable() {
          public void run() {
            if (bounds != null) {
              mNeighbourhoodView.setBounds(bounds);
            }
          }
        };
      }
    }
    else {
      // Check if an intent with a given bitmap was sent
      Intent intent = getIntent();
      Bundle extras = intent.getExtras();

      if (extras != null) {
        mBitmap = (Bitmap) extras.getParcelable(BUNDLE_KEY_BITMAP);
      }
    }

    // Request a bitmap if not given one
    if (mBitmap == null) {
      Intent i = new Intent(Intent.ACTION_PICK, Images.Media.INTERNAL_CONTENT_URI);
      startActivityForResult(i, NAVIGATION_ITEM_SELECT_IMAGE);
    }
    else {
      init();
    }

  }

  private void init() {
    // set bitmap
    mselectView.setImageBitmap(mBitmap, mOrientation);
    // Setup neighbourhood
    mNeighbourhoodView = new NeighbourhoodView(mselectView);
    mselectView.add(mNeighbourhoodView);
    
    mNeighbourhoodView.setFocused(true);

    int width = mBitmap.getWidth();
    int height = mBitmap.getHeight();

    Rect imageRect = new Rect(0, 0, width, height);    
    mNeighbourhoodView.setImageRect(imageRect);

    // TODO: Move to constructor
    mNeighbourhoodView.setMatrix(mselectView.getFinalMatrix());    

  }

  @Override
  protected void onSaveInstanceState(Bundle state) {    
    // Save the current image
    state.putParcelable(BUNDLE_KEY_BITMAP, mBitmap);
    state.putInt(BUNDLE_KEY_ORIENTATION, mOrientation);
    if (mNeighbourhoodView != null) {
      state.putParcelable(BUNDLE_KEY_BOUNDS, mNeighbourhoodView.getBounds());
      state.putString(BUNDLE_KEY_SHAPE, mNeighbourhoodView.getShape().name());
    }
    super.onSaveInstanceState(state);
  }

  @Override
  public boolean onNavigationItemSelected(int position, long itemId) {
    switch(position) {
      case BUTTON_RECTANGLE_INDEX:
        mNeighbourhoodView.setShape(NeighbourhoodView.Shape.RECTANGLE);
        break;
      case BUTTON_OVAL_INDEX:
        mNeighbourhoodView.setShape(NeighbourhoodView.Shape.OVAL);
        break;
      case BUTTON_POLYGON_INDEX:
        mNeighbourhoodView.setShape(NeighbourhoodView.Shape.POLYGON);
        break;     
    }
    // Check to see if bounds need to be restored
    if (mOnCreateRunnable != null) {
      mOnCreateRunnable.run();
      mOnCreateRunnable = null;
    }
    return true;
  }

  /**
   * Respond to activity results to receive images from other applications.
   */
  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == NAVIGATION_ITEM_SELECT_IMAGE)
      if (resultCode == Activity.RESULT_OK) {
        loadImage(data.getData()); // Load the returned uri
        init();
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
    String path = "";
    try {
      // load the bitmap
      mBitmap = MediaStore.Images.Media.getBitmap(mContentResolver, data);

      // Load the orientation
      path = getRealPathFromURI(data);
      ExifInterface exif = new ExifInterface(path);
      int orientation = 
          exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
      mOrientation = mapEXIF(orientation);
    }
    // TODO: Deal with specific exceptions
    catch (Exception e) {
      Log.e(TAG, "Failed to load file for exif data. " + path);
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
   * Modified from http://android-er.blogspot.ca/2011/04/convert-uri-to-real-path-format.html
   * @param contentUri
   * @return the file path represented by contentUri
   */
  private String getRealPathFromURI(Uri contentUri) {
    String[] proj = { MediaStore.Images.Media.DATA };
    Cursor cursor = mContentResolver.query(contentUri, proj, null, null, null);
    int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
    cursor.moveToFirst();
    String rtn = cursor.getString(column_index);
    cursor.close();
    return rtn;
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.neighbourhood_select, menu);
    mselectView.updateFinalMatrix(); // recenter
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
