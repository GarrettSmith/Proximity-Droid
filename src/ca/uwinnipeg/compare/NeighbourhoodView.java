/**
 * 
 */
package ca.uwinnipeg.compare;

import android.graphics.Canvas;
import android.graphics.Matrix;
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
 
  public static final String TAG = "NeighbourhoodView";
  
  // The default ratio of padding when resetting the neighbour hood size
  public static final float PADDING_RATIO = 1/8f;
  
  // The shape of the neighbourhood.
  protected Shape mShape = new RectShape();
  
  private static Paint FOCUSED_PAINT;
  
  // The bounds of the neighbourhood.
  protected Rect mBounds = new Rect(); // in image space
  
  // The view containing this neighbourhood.
  View mView;
  
  // Whether this neighbourhood is selected or not.
  boolean mFocused;
  
  private Matrix mMatrix;
  private Rect mImageRect; // in image space
  
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
    
    // One-time setup paint
    if (FOCUSED_PAINT == null) {
      FOCUSED_PAINT = new Paint();
      FOCUSED_PAINT.setStyle(Paint.Style.STROKE);
      FOCUSED_PAINT.setStrokeWidth(0);
      FOCUSED_PAINT.setColor(0xff00ccff);
      FOCUSED_PAINT.setFlags(Paint.ANTI_ALIAS_FLAG);
    }
    
    // TESTING
    mFocused = true;
  }
  
  /**
   * Perform initial setup so we can translate to image space later on when handling input.
   */
  public void setImageRect(Rect imageRect) {
    mImageRect = new Rect(imageRect);
  }
  
  public void setMatrix(Matrix m) {
    mMatrix = m;
  }

  public void setShape(Shape s) {
    mShape = s;
  }
  
  public void setBounds(Rect r) {
    mBounds.set(r);
  }
  
  // Maps the neighbourhood bounds from image space to screen space.
  private Rect computeLayout() {
    RectF r = new RectF(mBounds.left, mBounds.top, mBounds.right, mBounds.bottom);
    mMatrix.mapRect(r);
    return new Rect(Math.round(r.left), Math.round(r.top), Math.round(r.right), Math.round(r.bottom));
  }
  
  /**
   * Sets the bounds to a default value.
   */
  public void resetBounds(int w, int h) {
    // Use the smaller side to determine the padding
    // This makes it feel more uniform
    int padding = (int) (Math.min(w, h) * PADDING_RATIO);
    mBounds.set(padding, padding, w-padding, h-padding);
  }
  
  // Movement and Events
  
  // The amount a touch can be off and still be considered touching an edge
  // TODO: Test different values of touch padding
  protected static final int TOUCH_PADDING = 25;
  
  private Action mAction = Action.None;
  private Edge mEdge = null;
  
  // The Previously touched position IN SCREEN SPACE
  private float mLastX = 0;
  private float mLastY = 0;
  
  // The action currently taking place
  public enum Action { None, Move, Resize }
  
  // The edge or pair of edges that are currently selected
  public enum Edge { TL, T, TR, R, BR, B, BL, L }
  
  /** 
   * Handles a down event.
   */
  public void handleDown(float x, float y) {    
    //float[] p = convertToImageSpace(x, y);
    //x = p[0];
    //y = p[1];
    Rect r = computeLayout();
    
    // TODO: Check if touch was on an edge
    if ( (mEdge = checkEdges(x, y)) != null) {
      mAction = Action.Resize;
    }
    // Check if touch was within neighbourhood
    else if (r.contains((int)x, (int)y)) {
      mAction = Action.Move;      
    }

    // Record position
    mLastX = x;
    mLastY = y;    
  }
  
  private Edge checkEdges(float x, float y) {
    return null;
  }
  
  /**
   * Handles an up event.
   */
  public void handleUp(float x, float y) {
    mAction = Action.None;
  }
  
  /**
   * handles motion to move the neighbourhood.
   */
  public void handleMove(float x, float y) {
    //float[] p = convertToImageSpace(x, y);
    //x = p[0];
    //y = p[1];
    
    switch (mAction) {
    case Move:      
      
      // Calculate change in position
      float dx = x - mLastX;
      float dy = y - mLastY;

      // Move the neighbourhood by the change
      mBounds.offset((int)dx, (int)dy);

      // constrain top and left
      mBounds.offsetTo(
          Math.max(0, mBounds.left),
          Math.max(0, mBounds.top));

      // constrain bottom and right
      mBounds.offsetTo(
          Math.min(mImageRect.width() - mBounds.width(), mBounds.left),
          Math.min(mImageRect.height() - mBounds.height(), mBounds.top));

      // Reflect change on screen
      mView.invalidate();
      break;
      
    case Resize:
    }
    
    // Record position
    mLastX = x;
    mLastY = y;
  }
  
//  /**
//   * TODO: THIS IS BROKEN
//   * converts the given points to image space
//   * @param x
//   * @param y
//   */
//  private float[] convertToImageSpace(float x, float y) {
//    float[] point = new float[]{x, y};
//    mMatrix.mapPoints(point);
//    return point;
//  }

  /**
   * Draws the neighbourhood to the given canvas.
   * @param canvas
   */
  protected void draw(Canvas canvas) {
    
    canvas.save();
    canvas.concat(mMatrix);

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
    canvas.drawRect(mBounds, FOCUSED_PAINT);
    
    canvas.restore();
  }
}
