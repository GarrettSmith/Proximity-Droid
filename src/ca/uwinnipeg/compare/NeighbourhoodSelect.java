package ca.uwinnipeg.compare;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.shapes.Shape;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

//TODO scale images
//TODO Resize selection
//TODO Change selection shape
//TODO Switch to xml based layout
/**
 * The activity can select neighbourhoods from an image. 
 * @author Garrett Smith
 *
 */
public class NeighbourhoodSelect extends Activity {
  
  @SuppressWarnings("unused")
  public static final String TAG = "NeighbourhoodSelect";
  
  
  private ContentResolver mContentResolver;
  private Bitmap mBitmap;
  
  private Shape selectionShape; // The shape of the current neighbourhood
  private Rect selectionBounds; // A rectangle that store the size and position of the selection
  
  private SelectionView mSelectionView;
  
  // constants
  private static final int PADDING_RATIO = 4;  
  
  private static final int SELECT_IMAGE = 1 << 0;
  
  private static final String BITMAP_NAME = "Bitmap";

  /**
   * Called when this Activity is first created.
   */
  @Override
  protected void onCreate(Bundle state){
    super.onCreate(state);    
    setContentView(R.layout.neighbourhood_select);
    
    mSelectionView = (SelectionView) findViewById(R.id.selection_view);
    
    mContentResolver = getContentResolver();
    
    
    // Check if state needs to be restored
    if (state != null) {
      mBitmap = state.getParcelable(BITMAP_NAME);
    }
    else {
      // Check if an intent with a given bitmap was sent
      Intent intent = getIntent();
      Bundle extras = intent.getExtras();

      if (extras != null) {
        mBitmap = (Bitmap) extras.getParcelable(BITMAP_NAME);
      }
    }
    
    // Request a bitmap if not given one
    if (mBitmap == null) {
      Intent i = new Intent(Intent.ACTION_PICK, Images.Media.INTERNAL_CONTENT_URI);
      startActivityForResult(i, SELECT_IMAGE);
    }
    else {
      setBitmap();
    }
    
  }
  
  // TODO save state of the current selection
  @Override
  protected void onSaveInstanceState(Bundle state) {    
    // Save the current image
    state.putParcelable(BITMAP_NAME, mBitmap);
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
      } 
      else {
        finish(); // If the intent failed or was cancelled exit
      }
  }
  
  protected void loadImage(Uri data) {
    // set the image to the received URI
    try {
      mBitmap = MediaStore.Images.Media.getBitmap(mContentResolver, data);
      setBitmap();
      resetBounds();
    }
    catch (Exception e) {
      Log.e(TAG, "Failed to load file. " + data.toString());
    }
  }

  /**
   * Create the menu.
   */
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
      MenuInflater inflater = getMenuInflater();
      inflater.inflate(R.menu.neighbourhood_select, menu);
      return true;
  }
  
  /**
   * Respond to buttons presses.
   */
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
  
  /**
   * Attempts to load the image located at image path and reflects the change 
   * in the view.
   */
  // TODO rotate the image correctly
  private void setBitmap() { 
    mSelectionView.setImageBitmap(mBitmap);
  }
  
  /**
   * Sets the bounds to a default value.
   */
  public void resetBounds() {
    if (mBitmap != null) { // Can't reset the bounds if there is no image
      /*
      // Load properties of the image
      BitmapFactory.Options options = new BitmapFactory.Options();
      options.inJustDecodeBounds = true;
      Uri uri = Uri.parse(mImagePath);
      BitmapFactory.decodeFile(getRealPathFromURI(uri), options);
      int w = options.outWidth;
      int h = options.outHeight;
      int padding;
      // Use the smaller side to determine the padding
      // This makes it feel more uniform
      if (w < h) {
        padding = w/PADDING_RATIO;
      }
      else {
        padding = h/PADDING_RATIO;
      }
      selectionBounds = new Rect(padding, padding, w-padding, h-padding);
      mSelectionView.setSelectionBounds(selectionBounds);
      */
    }
  }

}
