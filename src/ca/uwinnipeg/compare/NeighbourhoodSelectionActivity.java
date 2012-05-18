package ca.uwinnipeg.compare;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.drawable.shapes.RectShape;
import android.graphics.drawable.shapes.Shape;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import ca.uwinnipeg.compare.view.SelectionView;

//TODO scale images
//TODO Resize selection
//TODO Change selection shape
//TODO Switch to xml based layout
public class NeighbourhoodSelectionActivity extends Activity {
  private Shape selectionShape; // The shape of the current neighbourhood
  private Rect selectionBounds; // A rectangle that store the size and position of the selection
  private String mImagePath; // The path to the currently selected image
  private SelectionView selView;
  
  // constants
  private static final int PADDING_RATIO = 4;  
  private static final int SELECT_IMAGE = 101;
  private static final String TAG = "Neighbourhood Activity";
  private static final String IMAGE_PATH_NAME = "Image Path";

  /**
   * Called when this Activity is first created.
   */
  @Override
  protected void onCreate(Bundle state){
    super.onCreate(state);
    
    // Request image
    if (state != null) {
      mImagePath = state.getString(IMAGE_PATH_NAME);
    }
    else {
      // Create view for first time
      selView = new SelectionView(this);
    }
    
    // Select an image if no image has been selected yet
    if (mImagePath == null) {
      Intent i = new Intent(Intent.ACTION_PICK, Images.Media.INTERNAL_CONTENT_URI);
      startActivityForResult(i, SELECT_IMAGE);
    }
    else {
      loadImage(); // load previously selected image
    }
    
    // Create shape
    selectionShape = new RectShape();
    
    // Setup view
    selView.setSelectionShape(selectionShape);
    
    // add view to activity
    setContentView(selView);
  }
  
  // TODO save state of the current selection
  @Override
  protected void onSaveInstanceState(Bundle state) {    
    // Save the current image
    state.putString(IMAGE_PATH_NAME, mImagePath);
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
        mImagePath = data.getDataString();
        loadImage();        
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
  public void loadImage() {
    // set the image to the received URI
    try {
      Uri uri = Uri.parse(mImagePath);
      selView.setImage(getRealPathFromURI(uri));
      resetBounds();
    }
    catch (Exception e) {
      Log.e(TAG, "Failed to load file. " + e.getMessage());
    }
  }
  
  /**
   * Gets an absolute file path from a uri.
   */
  private String getRealPathFromURI(Uri contentUri) {
    String[] proj = { MediaStore.Images.Media.DATA };
    Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);
    int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
    cursor.moveToFirst();
    return cursor.getString(column_index);
  }
  
  /**
   * Sets the bounds to a default value.
   */
  public void resetBounds() {
    if (mImagePath != null) { // Can't reset the bounds if there is no image
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
      int halfWidth = (w-padding)/2;
      int halfHeight = (h-padding)/2;
      selectionBounds = new Rect(-1*halfWidth, -1*halfHeight, halfWidth, halfHeight);
      selView.setSelectionBounds(selectionBounds);
    }
  }

}
