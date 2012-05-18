package ca.uwinnipeg.compare.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.Shape;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

/**
 * 
 * The view which the image and currently selected neighbourhood is drawn.
 *
 */
public class SelectionView extends View {
  private ShapeDrawable mSelection; // The shape to be drawn as the current neighbourhood
  private BitmapDrawable mImage; // The image to draw as the background
  private Matrix mTransform; // The matrix to transform the drawing by
  
  private static final String TAG = "Selection View";
  
  private static final int PADDING_RATIO = 4; // minimum space between the selection and the screen edge

  public SelectionView(Context context){
    super(context);
    setFocusable(true); 

    // Setup selection shape
    mSelection = new ShapeDrawable();
    
    // Setup transform matrix
    mTransform = new Matrix();
    
    // Setup the paint for the selection shape (colour, stroke, etc.)
    Paint selectionPaint = mSelection.getPaint();
    
    selectionPaint.setColor(0xff00ccff); // SEPERATE STYLE
    selectionPaint.setStrokeWidth(0); // SEPERATE STYLE
    selectionPaint.setStyle(Paint.Style.STROKE); // SEPERate STYLE
  }
  
  // TODO Lock selection to edge
  @Override 
  public boolean onTouchEvent(MotionEvent event) {
    
    // Get bounds data
    Rect selB = mSelection.getBounds();
    int selHalfWidth  = selB.width()/2;
    int selHalfHeight = selB.height()/2; 
    
    Rect imgB = mImage.getBounds();
    
    // Calculate touch position
    int x = (int)event.getX() - (this.getWidth()/2);
    int y = (int)event.getY() - (this.getHeight()/2);
    
    // Limit selection to be within the image
    int left   = x - selHalfWidth;
    int top    = y - selHalfHeight;
    int right  = x + selHalfWidth;
    int bottom = y + selHalfHeight;

    if (left < imgB.left)     x = imgB.left + selHalfWidth;
    if (top < imgB.top)       y = imgB.top + selHalfHeight;
    if (right > imgB.right)   x = imgB.right - selHalfWidth;
    if (bottom > imgB.bottom) y = imgB.bottom - selHalfHeight;

    selB.offsetTo(x-selHalfWidth, y-selHalfHeight); // move the selection

    this.invalidate(); // Request a redraw
    
    return true; // returning true means this input has been handled
  }

  /**
   * Set the shape being used to select a neighbourhood.
   */
  public void setSelectionShape(Shape s) {
    mSelection.setShape(s);
  }
  
  /**
   * Set the bounds of the selection.
   */
  public void setSelectionBounds(Rect r) {
    mSelection.setBounds(r);
  }

  /**
   * Sets the image that the neighbourhood is being selected for.
   */
  public void setImage(String path) {
    Bitmap image = BitmapFactory.decodeFile(path);
    mImage = new BitmapDrawable(getResources(), image);
    int halfWidth = image.getWidth()/2;
    int halfHeight = image.getHeight()/2;
    mImage.setBounds(-1*halfWidth, -1*halfHeight, halfWidth, halfHeight);
  }
  
  /**
   * Updates the transformation matrix to scale the view according to the selection.
   */
  // TODO set a maximum zoom
  public void updateMatrix() {
    
    // reset the matrix
    mTransform.reset();
    
    // Get size of selection
    Rect b = mSelection.getBounds();
    int w = b.width();
    int h = b.height();    
    
    // Get size of drawing area
    int vw = this.getWidth();
    int vh = this.getHeight();
    
    // Update the scale of the matrix
    
    float scale = 1; // value to scale the image by (default 1)
    
    // scale with width
    scale = calcScale(w, vw); 
    
    // if the height of the image WITH SCALE is still greater than the screen
    // calculate scale using height
    //if ( (h * scale) > vh) scale = calcScale(h, vh);   
    
    mTransform.postScale(scale, scale); // set scale from center
    
    // Translate so selection is centered
    float left = (float)vw/2;
    float top  = (float)vh/2;
    mTransform.postTranslate(left, top);
  }
  
  private float calcScale(int drwDimen, int contDimen) {
    return (float)contDimen / (drwDimen + contDimen / PADDING_RATIO);
  }

  @Override
  protected void onDraw(Canvas canvas) {
    
    updateMatrix(); //TMP
    
    if (mTransform != null) { canvas.concat(mTransform); }
    else { Log.w(TAG, "Transform matrix is null, cannot transform drawing."); }

    // Draw the image if it has been loaded
    if (mImage != null) { mImage.draw(canvas); }
    else { Log.w(TAG, "BitmapDrawable is null, cannot draw image."); }
    
    // Dim unselected area
    canvas.save();
    canvas.clipRect(mImage.getBounds());
    canvas.clipRect(mSelection.getBounds(), Region.Op.DIFFERENCE); // NEED TO GENERALIZE TO ANY SHAPE
    canvas.drawARGB(150,0,0,0); // SEPERATE STYLE
    canvas.restore();
    
    // Draw the shape of the current selection
    mSelection.draw(canvas);
  }

}