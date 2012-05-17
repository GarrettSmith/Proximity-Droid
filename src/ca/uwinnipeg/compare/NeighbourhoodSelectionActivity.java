package ca.uwinnipeg.compare;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.graphics.drawable.shapes.Shape;
import android.os.Bundle;
import android.view.View;

public class NeighbourhoodSelectionActivity extends Activity {
  private Shape selectionShape; // The shape of the current neighbourhood
  private Bitmap image;

  /**
   * Called when this Activity is first created.
   */
  @Override
  protected void onCreate(Bundle savedInstanceState){
    super.onCreate(savedInstanceState);
    
    // Load image
    Resources res = getResources();
    BitmapFactory bmf = new BitmapFactory();
    image = bmf.decodeResource(res, R.drawable.sample_0);
    
    // Create shape
    selectionShape = new OvalShape();
    selectionShape.resize(200, 200);
    
    // Create view
    SelectionView selView = new SelectionView(this);
    selView.setImage(image);
    selView.setSelectionShape(selectionShape);
    
    // add view to activity
    setContentView(selView);
  }

  /**
   * 
   * @author Garrett Smith
   *
   */
  private static class SelectionView extends View {
    private ShapeDrawable selectionShape; // The shape to be drawn as the current neighbourhood
    private Bitmap image; // The image to draw as the background
    
    private float rot;
    
    private Paint imagePaint;
    
    private ShapeDrawable mDrawable;

    public SelectionView(Context context){
      super(context);
      setFocusable(true);	

      // Setup selection shape
      selectionShape = new ShapeDrawable();
      selectionShape.setBounds(100, 100, 300, 300);
      
      // Setup the paint for the selection shape (colour, stroke, etc.)
      Paint selectionPaint = selectionShape.getPaint();
      selectionPaint.setColor(0xff0066ff);
      selectionPaint.setStrokeWidth(0);
      selectionPaint.setStyle(Paint.Style.STROKE);

      imagePaint = new Paint();
    }

    /**
     * Set the shape being used to select a neighbourhood.
     */
    public void setSelectionShape(Shape s) {
      selectionShape.setShape(s);
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
      
      // Draw the shape of the current selection
      selectionShape.draw(canvas);
    }

  }

}
