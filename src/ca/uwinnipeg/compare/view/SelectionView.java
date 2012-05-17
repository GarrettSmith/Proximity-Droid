package ca.uwinnipeg.compare.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.Shape;
import android.view.View;

/**
 * 
 * The view which the image and currently selected neighbourhood is drawn.
 *
 */
public class SelectionView extends View {
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
    canvas.drawBitmap(image, 0, 0, imagePaint);
    
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