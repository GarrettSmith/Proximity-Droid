package ca.uwinnipeg.compare;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.SpinnerAdapter;

// TODO: Zoom and pan to follow neighbourhood
// TODO: Look into why image shifts after rotate, STATUS BAR does this need to be fixed?
// TODO: Support down to 2.1
// TODO: Shift of the main UI thread possibly
/**
 * The activity can select neighbourhoods from an image. 
 * @author Garrett Smith
 *
 */
public class NeighbourhoodSelectActivity 
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

  private SelectView mSelectView;
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


    // setup view
    mSelectView = (SelectView) findViewById(R.id.select_view);

    NeighbourhoodView.Shape shape = null;
    
    // Check if state needs to be restored
    if (state != null) {
      mBitmap = state.getParcelable(BUNDLE_KEY_BITMAP);
      mOrientation = state.getInt(BUNDLE_KEY_ORIENTATION);
      final Rect bounds = state.getParcelable(BUNDLE_KEY_BOUNDS);
      String shapeStr = state.getString(BUNDLE_KEY_SHAPE);
      if (shapeStr != null) {
       shape = NeighbourhoodView.Shape.valueOf(shapeStr);
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

    // UI
    if (android.os.Build.VERSION.SDK_INT >= 11) {
      setupActionBar(shape);
    }
    else {
      // TODO: Setup alternative to action bar
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

  @TargetApi(11)
  private void setupActionBar(NeighbourhoodView.Shape shape) {
    mActionBar = getActionBar();
    mActionBar.setDisplayHomeAsUpEnabled(true);

    // Action bar navigation
    mSpinnerAdapter = ArrayAdapter.createFromResource(
        this, 
        R.array.shape_list, 
        android.R.layout.simple_spinner_dropdown_item);

    mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);   
    mActionBar.setListNavigationCallbacks(mSpinnerAdapter, this);

    if (shape != null) {
      // Restore selected shape in spinner which then sets the neighbourhood shape
      mActionBar.setSelectedNavigationItem(shape.ordinal());
    }
  }

  private void init() {
    // set bitmap
    mSelectView.setImageBitmap(mBitmap, mOrientation);
    // Setup neighbourhood
    mNeighbourhoodView = new NeighbourhoodView(mSelectView);
    mSelectView.add(mNeighbourhoodView);

    mNeighbourhoodView.setFocused(true);

    int width = mBitmap.getWidth();
    int height = mBitmap.getHeight();

    Rect imageRect = new Rect(0, 0, width, height);    
    mNeighbourhoodView.setImageRect(imageRect);

    mNeighbourhoodView.resetBounds();

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

  //@Override
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
  // TODO: CLEANUP loading images
  @SuppressWarnings("deprecation")
  @TargetApi(13)
  protected void loadImage(Uri data) {
    String path = "";
    try {
      path = getRealPathFromURI(data);

      // Read the bitmap's size
      BitmapFactory.Options options = new BitmapFactory.Options();
      options.inJustDecodeBounds = true;

      BitmapFactory.decodeFile(path, options);

      // Get screen size
      Display display = getWindowManager().getDefaultDisplay();
      int width, height;
      // Get screen size depending on the api level
      if (android.os.Build.VERSION.SDK_INT >= 13) {
        Point size = new Point();
        display.getSize(size);
        width = size.x;
        height = size.y;
      }
      else {
        width = display.getWidth();
        height = display.getHeight();
      }

      // TODO: Deal with rotated images
      // Calculate sample size
      options.inSampleSize = 
          calculateInSampleSize(options, width/2, height/2);

      options.inJustDecodeBounds = false; 

      // load the bitmap
      mBitmap = BitmapFactory.decodeFile(path, options);

      // Load the orientation      
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

  /**
   * From http://developer.android.com/training/displaying-bitmaps/load-bitmap.html
   * @param options
   * @param reqWidth
   * @param reqHeight
   * @return
   */
  public static int calculateInSampleSize(
      BitmapFactory.Options options, int reqWidth, int reqHeight) {
    // Raw height and width of image
    final int height = options.outHeight;
    final int width = options.outWidth;
    int inSampleSize = 1;

    if (height > reqHeight || width > reqWidth) {
      if (width > height) {
        inSampleSize = Math.round((float)height / (float)reqHeight);
      } else {
        inSampleSize = Math.round((float)width / (float)reqWidth);
      }
    }
    return inSampleSize;
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
    mSelectView.updateFinalMatrix(); // recenter
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