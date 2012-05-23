/**
 * 
 */  
// TODO: Figure out how to represent shape  
package ca.uwinnipeg.compare;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
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
  
  //Paint shared by all neighbourhoods, used to draw when focused
  private static Paint FOCUSED_PAINT;

  // The bounds of the neighbourhood.
  protected Rect mBounds = new Rect(); // in image space
  
  // The view containing this neighbourhood.
  View mView;
  
  // Whether this neighbourhood is selected or not.
  boolean mFocused;
  
  // 
  private Matrix mMatrix; 
  private Rect mImageRect; // in image space
  
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
  
  /**
   * Perform initial setup so we can translate to image space later on when handling input.
   */
  public void setImageRect(Rect imageRect) {
    mImageRect = new Rect(imageRect);
  }
  
  public void setMatrix(Matrix m) {
    mMatrix = m;
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
  }
  
  // Movement and Events
  
  // The amount a touch can be off and still be considered touching an edge
  // TODO: Test different values of touch padding
  // TODO: Look into shifting it outside to make moving easier
  protected static final int TOUCH_PADDING = 75;
  
  private Action mAction = Action.None;
  private Edge mEdge = Edge.None;
  
  // The Previously touched position IN IMAGE SPACE
  private float mLastX = 0;
  private float mLastY = 0;
  
  // The action currently taking place
  public enum Action { None, Move, Resize }
  
  // The edge or pair of edges that are currently selected
  public enum Edge { None, TL, T, TR, R, BR, B, BL, L, ALL }
  
  /** 
   * Handles a down event.
   */
  public void handleDown(float x, float y) {    
    float[] p = convertToImageSpace(x, y);
    x = p[0];
    y = p[1];
    
    // TODO: Check if touch was on an edge
    if ( (mEdge = checkEdges(x, y)) != Edge.None) {
      mAction = Action.Resize;
    }
    // Check if touch was within neighbourhood
    else if (mBounds.contains((int)x, (int)y)) {
      mAction = Action.Move;      
    }

    // Record position
    mLastX = x;
    mLastY = y;    
  }
  
  /**
   * Given a point in image space determines which edge or pair of edges are within a TOUCH_PADDING
   * of the point.
   * @param x
   * @param y
   * @return the edges being touched
   */
  private Edge checkEdges(float x, float y) {
    int left = mBounds.left;
    int right = mBounds.right;
    int top = mBounds.top;
    int bottom = mBounds.bottom;
    
    Edge rtn = Edge.None;
    
    // TODO: Make this less of a brute force
    // TODO: Detect edge pairs
    // TODO: Use touch size
    // TODO: Handle pinch
    if      (Math.abs(x - left)   <= TOUCH_PADDING) rtn = Edge.L;
    else if (Math.abs(x - right)  <= TOUCH_PADDING) rtn = Edge.R;
    
    // TODO: UGLY UGLY UGLY
    if (Math.abs(y - top)    <= TOUCH_PADDING) {
      rtn = rtn == Edge.L ? Edge.TL : (rtn == Edge.R ? Edge.TR : Edge.T); 
    }
    else if (Math.abs(y - bottom) <= TOUCH_PADDING) {
      rtn = rtn == Edge.L ? Edge.BL : (rtn == Edge.R ? Edge.BR : Edge.B);
    }
    
    return rtn;
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
    float[] p = convertToImageSpace(x, y);
    x = p[0];
    y = p[1];
    
    // Check if any action is being performed
    if (mAction != Action.None) {
      
      // Calculate change in position
      float dx = x - mLastX;
      float dy = y - mLastY;

      // Determine which action to take
      switch (mAction) {
      case Move:
        move((int)dx, (int)dy);
        break;
      case Resize:
        resize((int)dx, (int)dy, mEdge);
        break;
      }

      // Reflect change on screen
      mView.invalidate();
    }
    
    // Record position
    mLastX = x;
    mLastY = y;
  }
  
  /**
   * Moves the neighbourhood by the given delta.
   * @param dx
   * @param dy
   */
  private void move(int dx, int dy) {
    // Move the neighbourhood by the change
    mBounds.offset(dx, dy);

    // constrain top and left
    mBounds.offsetTo(
        Math.max(0, mBounds.left),
        Math.max(0, mBounds.top));

    // constrain bottom and right
    mBounds.offsetTo(
        Math.min(mImageRect.width() - mBounds.width(), mBounds.left),
        Math.min(mImageRect.height() - mBounds.height(), mBounds.top));
  }
  
  /**
   * Resizes the given edge by the given delta.
   * @param dx
   * @param dy
   * @param edg
   */
  // TODO: Constrain resize
  private void resize(int dx, int dy, Edge edg) {
    switch (edg) {
    case L: 
      mBounds.left = Math.max(0, mBounds.left + dx);
      break;
    case R: 
      mBounds.right = Math.min(mImageRect.right, mBounds.right + dx);
      break;
    case T: 
      mBounds.top = Math.max(0, mBounds.top + dy);
      break;
    case B: 
      mBounds.bottom = Math.min(mImageRect.bottom, mBounds.bottom + dy);
      break;
    case TL:
      resize(dx, dy, Edge.T);
      resize(dx, dy, Edge.L);
      break;
    case TR:
      resize(dx, dy, Edge.T);
      resize(dx, dy, Edge.R);
      break;
    case BL:
      resize(dx, dy, Edge.B);
      resize(dx, dy, Edge.L);
      break;
    case BR:
      resize(dx, dy, Edge.B);
      resize(dx, dy, Edge.R);
      break;
    }
  }
  
  /**
   * 
   * converts the given points to image space
   * @param x
   * @param y
   */
  private float[] convertToImageSpace(float x, float y) {
    float[] point = new float[]{x, y};
    Matrix inverse = new Matrix();
    mMatrix.invert(inverse);
    inverse.mapPoints(point);
    return point;
  }

  /**
   *  Maps the neighbourhood bounds from image space to screen space.
   * @return
   */
  private Rect getScreenSpaceBounds() {
    RectF r = new RectF(mBounds.left, mBounds.top, mBounds.right, mBounds.bottom);
    mMatrix.mapRect(r);
    return new Rect(Math.round(r.left), Math.round(r.top), Math.round(r.right), Math.round(r.bottom));
  }

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
