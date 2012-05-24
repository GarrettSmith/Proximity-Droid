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
import android.os.Parcelable;
import android.view.MotionEvent;
import android.view.View;

/**
 * @author Garrett Smith
 *
 */
// TODO: Mark dirty rects for redraw, rather than redraw entire image.
public class NeighbourhoodView {

  public static final String TAG = "NeighbourhoodView";

  // The default ratio of padding when resetting the neighbour hood size
  public static final float PADDING_RATIO = 1/8f;

  //Paint shared by all neighbourhoods, used to draw when focused
  private static Paint FOCUSED_PAINT;

  // The bounds of the neighbourhood IN IMAGE SPACE
  private Rect mBounds = new Rect();

  // The view containing this neighbourhood.
  View mView;

  // Whether this neighbourhood is selected or not.
  boolean mFocused;

  // The matrix used to move from image space to screen space
  private Matrix mMatrix; 
  
  // The image bounds in image space
  private Rect mImageRect;
  
  public enum Shape { Rectangle, Circle, Polygon }
  
  private Shape mShape = Shape.Rectangle;

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

    // TODO: TESTING, set focused properly
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

  public Rect getBounds() {
    return mBounds;
  }

  public void setBounds(Rect r) {
    mBounds.set(r);
    mView.invalidate(getScreenBounds());
  }

  /**
   * Sets the bounds to a default value.
   */
  public void resetBounds() {
    int w = mImageRect.width();
    int h = mImageRect.height();
    // Use the smaller side to determine the padding
    // This makes it feel more uniform
    int padding = (int) (Math.min(w, h) * PADDING_RATIO);
    mBounds.set(padding, padding, w-padding, h-padding);
    mView.invalidate(getScreenBounds());
  }
  
  public void setShape(Shape s) {
    mShape = s;
    mView.invalidate(getScreenBounds());
  }
  
  public Shape getShape() {
    return mShape;
  }

  // Movement and Events

  // TODO: Have touch padding shift with image size
  // The amount a touch can be off and still be considered touching an edge
  protected static final int TOUCH_PADDING = 75;
  protected static final int TOUCH_SHIFT = 25;

  // The minimum size of the neighbourhood
  protected static final int MIN_SIZE = 25;

  private Action mAction = Action.None;
  private Edge mEdge = Edge.None;

  // The Previously touched position IN IMAGE SPACE
  private float mLastX = 0;
  private float mLastY = 0;

  // The previous distance between two fingers, used for pinch zoom
  private float mLastDistanceX = 0;
  private float mLastDistanceY = 0;
  private Action mLastAction = Action.None;

  // The action currently taking place
  public enum Action { None, Move, Resize, Scale }

  // The edge or pair of edges that are currently selected
  public enum Edge { None, TL, T, TR, R, BR, B, BL, L, ALL }

  /** 
   * Handles a down event.
   */
  public void handleDown(MotionEvent event) {
    float[] p = convertToImageSpace(event.getX(), event.getY());
    float x = p[0];
    float y = p[1];

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
    // TODO: Use touch size (event.getSize())
    if      (Math.abs(x - left  + TOUCH_SHIFT)  <= TOUCH_PADDING) rtn = Edge.L;
    else if (Math.abs(x - right - TOUCH_SHIFT)  <= TOUCH_PADDING) rtn = Edge.R;

    // TODO: UGLY UGLY UGLY
    if (Math.abs(y - top + TOUCH_SHIFT)    <= TOUCH_PADDING) {
      rtn = rtn == Edge.L ? Edge.TL : (rtn == Edge.R ? Edge.TR : Edge.T); 
    }
    else if (Math.abs(y - bottom - TOUCH_SHIFT) <= TOUCH_PADDING) {
      rtn = rtn == Edge.L ? Edge.BL : (rtn == Edge.R ? Edge.BR : Edge.B);
    }

    return rtn;
  }

  /**
   * Handles an up event.
   */
  public void handleUp(MotionEvent event) {
    mAction = Action.None;
  }

  /**
   * handles motion to move the neighbourhood.
   */
  public void handleMove(MotionEvent event) {
    float[] p = convertToImageSpace(event.getX(), event.getY());
    float x = p[0];
    float y = p[1];

    // Deal with multitouch
    if (mAction != Action.Scale) {
      // check for pinch
      if (event.getPointerCount() >= 2) {
        mLastAction = mAction;
        mAction = Action.Scale;
        mLastDistanceX = Math.abs(x - event.getX(1));
        mLastDistanceY = Math.abs(y - event.getY(1));
        // Mark last X to prevent jump
        mLastX = -1;
      }      
    }
    // Check if there are still other pointers
    else if (event.getPointerCount() == 1) { 
      // Reset last point
      mLastX = x;
      mLastY = y;
      // return to last action
      mAction = mLastAction;
    }

    // Check if any action is being performed
    if (mAction != Action.None) {

      float dx, dy;
      // Determine which action to take
      switch (mAction) {
        case Move:
          // Calculate change in position of first point
          dx = x - mLastX;
          dy = y - mLastY;
          move((int)dx, (int)dy);
          // Record position
          mLastX = x;
          mLastY = y;
          break;
        case Resize:
          // Calculate change in position of first point
          dx = x - mLastX;
          dy = y - mLastY;
          resize((int)dx, (int)dy, mEdge);
          // Record position
          mLastX = x;
          mLastY = y;
          break;
        case Scale:
          float x1 = event.getX(1);
          float y1 = event.getY(1);
          // Scale using distance
          float distX = Math.abs(x - x1);
          float distY = Math.abs(y - y1);
          float dDistX = distX - mLastDistanceX;
          float dDistY = distY - mLastDistanceY;
          scale((int)dDistX, (int)dDistY);
          // move using midpoint
          float midX = (x + x1) / 2f;
          float midY = (y + y1) / 2f;
          // check if first time scaling to prevent jump
          if (mLastX != -1) {
            dx = midX - mLastX;
            dy = midY - mLastY;
            move((int)dx, (int)dy);
          }
          // record values
          mLastDistanceX = distX;
          mLastDistanceY = distY;
          mLastX = midX;
          mLastY = midY;
          break;
      }

      // Reflect change on screen
      mView.invalidate();
    }

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
  // TODO: Make a minimum size
  private void resize(int dx, int dy, Edge edg) {
    switch (edg) {
      case L: 
        // constrain to image area
        mBounds.left = Math.max(0, mBounds.left + dx); 
        // prevent flipping and keep min size
        mBounds.left = Math.min(mBounds.left, mBounds.right - MIN_SIZE); 
        break;
      case R: 
        mBounds.right = Math.min(mImageRect.right, mBounds.right + dx);
        mBounds.right = Math.max(mBounds.right, mBounds.left + MIN_SIZE);
        break;
      case T: 
        mBounds.top = Math.max(0, mBounds.top + dy);
        mBounds.top = Math.min(mBounds.top, mBounds.bottom - MIN_SIZE);
        break;
      case B: 
        mBounds.bottom = Math.min(mImageRect.bottom, mBounds.bottom + dy);
        mBounds.bottom = Math.max(mBounds.bottom, mBounds.top + MIN_SIZE);
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
   * Scales the distance given the delta in distance between pointers.
   * @param dDist
   */
  private void scale(int dx, int dy) {
    resize(-dx, -dy, Edge.TL);
    resize(dx, dy, Edge.BR);
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
  private Rect getScreenBounds() {
    RectF r = new RectF(mBounds.left, mBounds.top, mBounds.right, mBounds.bottom);
    mMatrix.mapRect(r);
    return new Rect(Math.round(r.left), Math.round(r.top), Math.round(r.right), Math.round(r.bottom));
  }

  /**
   * Draws the neighbourhood to the given canvas.
   * @param canvas
   */
  protected void draw(Canvas canvas) {

    int count = canvas.save();
    canvas.concat(mMatrix);

    if (mFocused) {
      //TODO: Dim areas that are not rectangles
      canvas.save();
      Path path = new Path();
      switch (mShape) {
        case Rectangle:
          path.addRect(new RectF(mBounds), Path.Direction.CW);
          canvas.drawRect(mBounds, FOCUSED_PAINT);
          break;
        case Circle:
          path.addOval(new RectF(mBounds), Path.Direction.CW);
          canvas.drawOval(new RectF(mBounds), FOCUSED_PAINT);
          break;
        case Polygon:
          // TODO: Draw polygons
          break;
      }
      canvas.clipPath(path, Region.Op.DIFFERENCE);
      canvas.drawColor(0xaa000000);
      canvas.restore();
    }        

    canvas.restoreToCount(count);
  }

}
