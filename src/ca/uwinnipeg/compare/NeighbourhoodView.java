/**
 * 
 */
// TODO: Implement polygons
// TODO: Handle varying image sizes
// TODO: Make drawing density independent
package ca.uwinnipeg.compare;

import java.util.ArrayList;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.util.Log;
import android.view.MotionEvent;

/**
 * @author Garrett Smith
 *
 */
public class NeighbourhoodView {

  public static final String TAG = "NeighbourhoodView";

  // The default ratio of padding when resetting the neighbour hood size
  public static final float PADDING_RATIO = 1/8f;

  //Paint shared by all neighbourhoods
  private static Paint FOCUSED_PAINT;
  private static Paint UNFOCUSED_PAINT;
  private static Paint GUIDE_PAINT;
  private static Paint HANDLE_PAINT;
  private static Paint UNSELECTED_PAINT;  

  // Handle drawing constants
  private static final float HANDLE_SIZE = 14;
  private static Path HANDLE_PATH;

  // The bounds of the neighbourhood IN IMAGE SPACE
  private Rect mBounds = new Rect();

  // The view containing this neighbourhood.
  SelectView mView;

  // Whether this neighbourhood is selected or not.
  boolean mFocused;

  // The matrix used to move from image space to screen space
  private Matrix mMatrix; 
  
  // The image bounds in image space
  private Rect mImageRect;
  
  public enum Shape { RECTANGLE, OVAL, POLYGON }
  
  private Shape mShape = Shape.RECTANGLE;
  
  // The list of points that make up the polygon
  private ArrayList<PointF> mPoints = new ArrayList<PointF>();

  public NeighbourhoodView(SelectView v){
    mView = v;
    
    // Grab the matrix
    mMatrix = mView.getFinalMatrix();

    // One-time setup paint
    if (FOCUSED_PAINT == null) {
      Log.i(TAG, "One time setup."); // TODO: make sure this only happens once
      FOCUSED_PAINT = new Paint();
      FOCUSED_PAINT.setStyle(Paint.Style.STROKE);
      FOCUSED_PAINT.setStrokeWidth(3);
      FOCUSED_PAINT.setColor(Color.CYAN);
      FOCUSED_PAINT.setFlags(Paint.ANTI_ALIAS_FLAG);
      
      UNFOCUSED_PAINT = new Paint();
      UNFOCUSED_PAINT.setStyle(Paint.Style.FILL);
      UNFOCUSED_PAINT.setColor(Color.CYAN);
      UNFOCUSED_PAINT.setAlpha(100);
      UNFOCUSED_PAINT.setFlags(Paint.ANTI_ALIAS_FLAG);
      
      GUIDE_PAINT = new Paint();
      GUIDE_PAINT.setStyle(Paint.Style.STROKE);
      GUIDE_PAINT.setStrokeWidth(0);
      GUIDE_PAINT.setColor(Color.WHITE);
      GUIDE_PAINT.setAlpha(50);
      GUIDE_PAINT.setFlags(Paint.ANTI_ALIAS_FLAG);
      
      HANDLE_PAINT = new Paint(UNFOCUSED_PAINT);
      HANDLE_PAINT.setAlpha(255);
      
      UNSELECTED_PAINT = new Paint();
      UNSELECTED_PAINT.setColor(Color.BLACK);
      UNSELECTED_PAINT.setAlpha(100);
      UNSELECTED_PAINT.setFlags(Paint.ANTI_ALIAS_FLAG);
      
      HANDLE_PATH = new Path();
      float halfSize = HANDLE_SIZE / 2;
      HANDLE_PATH.addRect(-halfSize, -halfSize, halfSize, halfSize, Path.Direction.CW);
      Matrix m = new Matrix();
      m.postRotate(45);
      HANDLE_PATH.transform(m);
    }
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

  public Rect getBounds() {
    return mBounds;
  }

  /**
   * Sets bounds of neighbourhood and invalidates the containing view.
   * @param r
   */
  public void setBounds(Rect r) {
    Rect dirty = new Rect(getPaddedScreenSpaceBounds());
    mBounds.set(r);
    dirty.union(getPaddedScreenSpaceBounds());
    mView.invalidate(dirty);
  }

  /**
   * Sets the bounds to a default value.
   */
  public void resetBounds() {
    // can't do anything if you don't have an image to work with yet
    if (mImageRect == null) return;
    
    int w = mImageRect.width();
    int h = mImageRect.height();
    // Use the smaller side to determine the padding
    // This makes it feel more uniform
    int padding = (int)(Math.min(w, h) * PADDING_RATIO);
    setBounds(new Rect(padding, padding, w-padding, h-padding));
  }
  
  public void setShape(Shape s) {
    mShape = s;
    if (mShape == Shape.POLYGON) {
      mPoints.clear();
      // TESTING
      mPoints.add(new PointF(50,50));
      mPoints.add(new PointF(100, 50));
      mPoints.add(new PointF(200,200));
      mPoints.add(new PointF(50, 200));
      mPoints.add(new PointF(0, 100));
      mView.invalidate();
    }
    mView.invalidate(getPaddedScreenSpaceBounds());
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

  private Action mAction = Action.NONE;
  private Edge mEdge = Edge.NONE;

  // The Previously touched position IN IMAGE SPACE
  private float mLastX;
  private float mLastY;

  // The previous distance between two fingers, used for pinch zoom
  private float mLastDistanceX;
  private float mLastDistanceY;
  private Action mLastAction = Action.NONE;

  // The action currently taking place
  public enum Action { NONE, MOVE, RESIZE, SCALE }

  // The edge or pair of edges that are currently selected
  public enum Edge { NONE, TL, T, TR, R, BR, B, BL, L, ALL }

  /** 
   * Handles a down event.
   */
  public void handleDown(MotionEvent event) {
    float[] p = convertToImageSpace(event.getX(), event.getY());
    float x = p[0];
    float y = p[1];

    if ( (mEdge = checkEdges(x, y)) != Edge.NONE) {
      mAction = Action.RESIZE;
    }
    // Check if touch was within neighbourhood
    else if (mBounds.contains((int)x, (int)y)) {
      mAction = Action.MOVE;      
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

    Edge rtn = Edge.NONE;

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
    mAction = Action.NONE;
  }

  /**
   * handles motion to move the neighbourhood.
   */
  public void handleMove(MotionEvent event) {
    float[] p = convertToImageSpace(event.getX(), event.getY());
    float x = p[0];
    float y = p[1];

    // Deal with multitouch
    if (mAction != Action.SCALE) {
      // check for pinch
      if (event.getPointerCount() >= 2) {
        mLastAction = mAction;
        mAction = Action.SCALE;
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
    if (mAction != Action.NONE) {
      
      // rectangle that needs redraw
      Rect dirty = getPaddedScreenSpaceBounds();

      float dx = 0;
      float dy = 0;
      // Determine which action to take
      switch (mAction) {
        case MOVE:
          // Calculate change in position of first point
          dx = x - mLastX;
          dy = y - mLastY;
          move((int)dx, (int)dy);
          // Record position
          mLastX = x;
          mLastY = y;
          break;
        case RESIZE:
          // Calculate change in position of first point
          dx = x - mLastX;
          dy = y - mLastY;
          resize((int)dx, (int)dy, mEdge);
          // Record position
          mLastX = x;
          mLastY = y;
          break;
        case SCALE:
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
      dirty.union(getPaddedScreenSpaceBounds());
      mView.invalidate(dirty);      
      
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
  private RectF getScreenSpaceBounds() {
    RectF r = new RectF(mBounds.left, mBounds.top, mBounds.right, mBounds.bottom);
    mMatrix.mapRect(r);
    r.left    = Math.round(r.left);
    r.top     = Math.round(r.top);
    r.right   = Math.round(r.right); 
    r.bottom  = Math.round(r.bottom);
    return r;
  }

  private Rect getPaddedScreenSpaceBounds() {
    int padding = 
        (int)(mFocused ? HANDLE_SIZE: UNFOCUSED_PAINT.getStrokeWidth());
    padding += 1;
    RectF r = new RectF(mBounds.left, mBounds.top, mBounds.right, mBounds.bottom);
    mMatrix.mapRect(r);
    r.left    -= padding;
    r.top     -= padding; 
    r.right   += padding; 
    r.bottom  += padding;
    return new Rect(
      Math.round(r.left), 
      Math.round(r.top), 
      Math.round(r.right), 
      Math.round(r.bottom));
  }
  
  /**
   * Converts mPoints to an array of primitive floats
   * @return
   */
  private float[] getPoints() {
    final int size = mPoints.size();
    float[] fs = new float[size * 2];
    for (int i = 0; i < size; i++) {
      PointF p = mPoints.get(i);
      fs[i * 2]     = p.x;
      fs[i * 2 + 1] = p.y;
    }
    return fs;
  }
  
  private float[] getScreenSpacePoints() {
    float[] ps = getPoints();
    mMatrix.mapPoints(ps);
    return ps;
  }

  /**
   * Draws the neighbourhood to the given canvas.
   * @param canvas
   */
  protected void draw(Canvas canvas) {
    
    RectF bounds = getScreenSpaceBounds();
    
    Path shapePath = new Path();

    switch (mShape) {
      case RECTANGLE:
        shapePath.addRect(bounds, Path.Direction.CW);
        break;
      case OVAL:
        canvas.drawRect(bounds, GUIDE_PAINT);
        shapePath.addOval(bounds, Path.Direction.CW);
        break;
      case POLYGON:
        float[] ps = getScreenSpacePoints();
        int size = ps.length;
        // Move to last point
        shapePath.moveTo(ps[size-2], ps[size-1]); 
        // Connect all points
        for (int i = 0; i < size; i += 2) {
          shapePath.lineTo( ps[i], ps[i+1]);
        }
        break;
    }
    
    if (mFocused) {        
      
      // Darken outside
      canvas.save();
      canvas.clipPath(shapePath, Region.Op.DIFFERENCE);
      canvas.drawPaint(UNSELECTED_PAINT);
      canvas.drawPath(shapePath, FOCUSED_PAINT);
      canvas.restore();
      
      // Draw handles
      Path handlePath = new Path();
      if (mShape == Shape.POLYGON) {
        float[] ps = getScreenSpacePoints();
        for (int i = 0; i < ps.length; i += 2) {
          handlePath.addPath(HANDLE_PATH, ps[i], ps[i+1]);
        }
      }
      else {        
        float l = bounds.left;
        float r = bounds.right;
        float t = bounds.top;
        float b = bounds.bottom;

        float midX = l + bounds.width() / 2;
        float midY = t + bounds.height() / 2;
        
        // Draw a handle in the middle of each side
        handlePath.addPath(HANDLE_PATH, midX, t);
        handlePath.addPath(HANDLE_PATH, midX, b);
        handlePath.addPath(HANDLE_PATH, l, midY);
        handlePath.addPath(HANDLE_PATH, r, midY);
      }
      canvas.drawPath(handlePath, HANDLE_PAINT);
    }
    else {
      canvas.drawPath(shapePath, UNFOCUSED_PAINT);
    }
    
    if (mShape != Shape.RECTANGLE) {
      canvas.drawRect(bounds, GUIDE_PAINT);
    }
  }

}
