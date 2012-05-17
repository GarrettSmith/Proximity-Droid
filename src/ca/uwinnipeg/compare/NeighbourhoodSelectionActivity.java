package ca.uwinnipeg.compare;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
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

public class NeighbourhoodSelectionActivity extends Activity {
  private Shape selectionShape; // The shape of the current neighbourhood
  private Rect selectionBounds; // A rectangle that store the size and position of the selection
  private Bitmap image; // The image the neighbourhood belongs to
  private SelectionView selView;
  
  // Image constants
  private static final int PADDING_RATIO = 8;
  
  private static final int SELECT_IMAGE = 101;
  private static final String TAG = "Neighbourhood Activity";

  /**
   * Called when this Activity is first created.
   */
  @Override
  protected void onCreate(Bundle savedInstanceState){
    super.onCreate(savedInstanceState);
    
    // Request image
    Intent i = new Intent(Intent.ACTION_PICK, Images.Media.INTERNAL_CONTENT_URI);
    startActivityForResult(i, SELECT_IMAGE);
    
    // Create shape
    selectionShape = new RectShape();
    
    // Create Bounds to fill entire image
    
    // Create view
    selView = new SelectionView(this);
    
    // Setup view
    selView.setSelectionShape(selectionShape);
    
    // add view to activity
    setContentView(selView);
  }
  
  /**
   * Respond to activity results to recieve images from other applications.
   */
  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == SELECT_IMAGE)
      if (resultCode == Activity.RESULT_OK) {
        setImage(data.getData());        
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
   * Sets the image being selected and reflects the change in the view.
   * @param uri
   */
  public void setImage(Uri uri) {
    // set the image to the received URI
    try {
      image = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
      selView.setImage(image);
      resetBounds();
    }
    catch (Exception e) {
      Log.e(TAG, "Failed to load file " + uri.getPath() + "\n" + e.getMessage());
    }
  }
  
  /**
   * Sets the bounds to a default value.
   */
  public void resetBounds() {
    if (image != null) { // Can't reset the bounds if there is no image
      int padWidth = image.getWidth()/PADDING_RATIO;
      int padHeight = image.getHeight()/PADDING_RATIO;
      selectionBounds = new Rect(padWidth, padHeight, image.getWidth()-padWidth, image.getHeight()-padHeight);
      selView.setSelectionBounds(selectionBounds);
    }
  }

}
