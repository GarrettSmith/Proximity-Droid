package ca.uwinnipeg.compare;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.graphics.drawable.shapes.Shape;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

public class NeighbourhoodSelectionActivity extends Activity {
  private Shape selectionShape; // The shape of the current neighbourhood
  private Rect selectionBounds; // A rectangle that store the size and position of the selection
  private Bitmap image; // The image the neighbourhood belongs to
  
  private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
  private Uri fileUri;

  /**
   * Called when this Activity is first created.
   */
  @Override
  protected void onCreate(Bundle savedInstanceState){
    super.onCreate(savedInstanceState);
    
    // Load image
    Resources res = getResources();
    image = BitmapFactory.decodeResource(res, R.drawable.sample_0);
    
    // Request image from camera
    Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    fileUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE);
    i.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
    
    startActivityForResult(i, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
    
    // Create shape
    selectionShape = new RectShape();
    
    // Create Bounds to fill entire image
    int padding = 8;
    int padWidth = image.getWidth()/padding;
    int padHeight = image.getHeight()/padding;
    selectionBounds = new Rect(padWidth, padHeight, image.getWidth()-padWidth, image.getHeight()-padHeight);
    
    // Create view
    SelectionView selView = new SelectionView(this);
    
    // Setup view
    selView.setImage(image);
    selView.setSelectionShape(selectionShape);
    selView.setSelectionBounds(selectionBounds);
    
    // add view to activity
    setContentView(selView);
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
   * 
   * The view which the image and currently selected neighbourhood is drawn.
   *
   */
  private static class SelectionView extends View {
    private ShapeDrawable selection; // The shape to be drawn as the current neighbourhood
    private Bitmap image; // The image to draw as the background
    
    private Paint imagePaint;

    public SelectionView(Context context){
      super(context);
      setFocusable(true);	

      // Setup selection shape
      selection = new ShapeDrawable();
      
      // Setup the paint for the selection shape (colour, stroke, etc.)
      Paint selectionPaint = selection.getPaint();
      
      selectionPaint.setColor(0xff00ccff); // SEPERATE STYLE
      selectionPaint.setStrokeWidth(2); // SEPERATE STYLE
      selectionPaint.setStyle(Paint.Style.STROKE); // SEPERate STYLE

      imagePaint = new Paint();
    }

    /**
     * Set the shape being used to select a neighbourhood.
     */
    public void setSelectionShape(Shape s) {
      selection.setShape(s);
    }
    
    /**
     * Set the bounds of the selection.
     */
    public void setSelectionBounds(Rect r) {
      selection.setBounds(r);
    }

    /**
     * Sets the image that the neighbourhood is being selected for.
     */
    public void setImage(Bitmap bm) {
      image = bm;
    }

    @Override
    protected void onDraw(Canvas canvas) {

      // Draw the image to the background
      canvas.drawBitmap(image, 75, 75, imagePaint);
      canvas.translate(75, 75); // TMP
      
      // Dim unselected area
      canvas.save();
      canvas.clipRect(0, 0, canvas.getWidth(), canvas.getHeight());
      canvas.clipRect(selection.getBounds(), Region.Op.DIFFERENCE); // NEED TO GENERALIZE TO ANY SHAPE
      canvas.drawARGB(150,0,0,0);
      canvas.restore();
      
      // Draw the shape of the current selection
      selection.draw(canvas);
    }

  }

}
