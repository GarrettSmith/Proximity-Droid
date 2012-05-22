/**
 * 
 */
package ca.uwinnipeg.compare;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.graphics.drawable.shapes.Shape;
import android.view.View;

/**
 * @author Garrett Smith
 *
 */
public class NeighbourhoodView {
  
  @SuppressWarnings("unused")
  public final String TAG = "NeighbourhoodView";
  
  // The default ratio of padding when resetting the neighbour hood size
  public static final float PADDING_RATIO = 1/5f;
  
  // The shape of the neighbourhood.
  protected Shape mShape = new RectShape();
  private final ShapeDrawable mShapeDrawable = new ShapeDrawable();
  private static Paint FOCUSED_PAINT;
  
  // The bounds of the neighbourhood.
  protected Rect mBounds = new Rect();
  
  // The view containing this neighbourhood.
  View mView;
  
  // Whether this neighbourhood is selected or not.
  boolean mFocused;
  
  /**
   * Sets the focused status of this neighbourhood.
   * A focused neighbourhood draws differently.
   * @param focus
   */
  public void setFocused(Boolean focus) {
    mFocused = focus;
  }
  
  public boolean isFocused() {
    return mFocused;
  }
  
  public NeighbourhoodView(View v){
    mView = v;
    mShapeDrawable.setShape(mShape);
    mShapeDrawable.setBounds(mBounds);
    
    // One-time setup paint
    if (FOCUSED_PAINT == null) {
      FOCUSED_PAINT = new Paint();
      FOCUSED_PAINT.setStyle(Paint.Style.STROKE);
      //FOCUSED_PAINT.setStrokeWidth(0);
      FOCUSED_PAINT.setColor(0xff00ccff);
    }
    
    mShapeDrawable.getPaint().set(FOCUSED_PAINT);
    
    // TESTING
    mFocused = true;
  }

  public void setShape(Shape s) {
    mShape = s;
  }
  
  public void setBounds(Rect r) {
    mBounds.set(r);
  }
  
  /**
   * Sets the bounds to a default value.
   */
  public void resetBounds(int w, int h) {
    // Use the smaller side to determine the padding
    // This makes it feel more uniform
    int padding = (int) (Math.min(w, h) * PADDING_RATIO);
    mBounds.set(padding, padding, w-padding, h-padding);
    mShapeDrawable.setBounds(mBounds);
  }

  /**
   * Draws the neighbourhood to the given canvas.
   * @param canvas
   */
  protected void draw(Canvas canvas) {

    if (mFocused) {
      //TODO: Dim areas that are not rectangles
      canvas.save();
      Path path = new Path();
      path.addRect(new RectF(mBounds), Path.Direction.CW);
      canvas.clipPath(path, Region.Op.DIFFERENCE);
      canvas.drawColor(0xaa000000);
      canvas.restore();
    }

    // TODO: Draw the shape of the current selection
    mShapeDrawable.draw(canvas);
  }
}
