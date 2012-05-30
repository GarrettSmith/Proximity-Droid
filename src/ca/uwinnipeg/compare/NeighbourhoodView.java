/**
 * 
 */
// TODO: Implement polygons
// TODO: Look into making rotate not affect input
// TODO: Use spinner for title
// TODO: Add hiding bars
// TODO: Look into old shape being drawn after changing shape
package ca.uwinnipeg.compare;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.view.MotionEvent;

/**
 * @author Garrett Smith
 *
 */
public class NeighbourhoodView {

  public static final String TAG = "NeighbourhoodView";

  // The default ratio of padding when resetting the neighbour hood size
  public static final float PADDING_RATIO = 1/8f;

  // Flags if one time setup has been done
  private static boolean SETUP = false;
  
  //Paint shared by all neighbourhoods
  private static final Paint FOCUSED_PAINT = new Paint();
  private static final Paint UNFOCUSED_PAINT = new Paint();
  private static final Paint GUIDE_PAINT = new Paint();
  private static final Paint HANDLE_PAINT = new Paint();
  private static final Paint UNSELECTED_PAINT = new Paint();
  private static final Paint SELECTED_POINT_PAINT = new Paint(); 
  private static final Paint REMOVE_POINT_PAINT = new Paint();

  // Handle drawing constants
  private static float HANDLE_SIZE;
  private static final Path HANDLE_PATH = new Path();

  // The bounds of the neighbourhood IN IMAGE SPACE
  private Rect mBounds = new Rect();

  // The view containing this neighbourhood.
  SelectView mView;

  // Whether this neighbourhood is selected or not.
  boolean mFocused;

  // The matrix used to move from image space to screen space
  private Matrix mMatrix; 

  // The image bounds in image space
  private Rect mImageBounds;

  public enum Shape { RECTANGLE, OVAL, POLYGON }

  private Shape mShape = Shape.RECTANGLE;

  // The list of points that make up the polygon
  private Polygon mPoly = new Polygon();

  public NeighbourhoodView(SelectView v){
    mView = v;

    // Grab the matrix
    mMatrix = mView.getFinalMatrix();

    // Borrow the view's resources
    Resources rs = v.getResources();

    // One-time setup
    if (!SETUP) {
      SETUP = true;
      
      FOCUSED_PAINT.setStyle(Paint.Style.STROKE);
      FOCUSED_PAINT.setStrokeWidth(rs.getDimension(R.dimen.neighbourhood_focused_stroke));
      FOCUSED_PAINT.setColor(rs.getColor(R.color.neighbourhood_focused_color));
      FOCUSED_PAINT.setFlags(Paint.ANTI_ALIAS_FLAG);

      UNFOCUSED_PAINT.setStyle(Paint.Style.FILL);
      UNFOCUSED_PAINT.setColor(rs.getColor(R.color.neighbourhood_unfocused_color));
      UNFOCUSED_PAINT.setFlags(Paint.ANTI_ALIAS_FLAG);

      GUIDE_PAINT.setStyle(Paint.Style.STROKE);
      GUIDE_PAINT.setStrokeWidth(rs.getDimension(R.dimen.neighbourhood_guide_stroke));
      GUIDE_PAINT.setColor(rs.getColor(R.color.neighbourhood_guide_color));
      GUIDE_PAINT.setFlags(Paint.ANTI_ALIAS_FLAG);

      HANDLE_PAINT.setStyle(Paint.Style.FILL);
      HANDLE_PAINT.setColor(rs.getColor(R.color.neighbourhood_focused_color));
      HANDLE_PAINT.setFlags(Paint.ANTI_ALIAS_FLAG);

      UNSELECTED_PAINT.setColor(rs.getColor(R.color.neighbourhood_unselected_color));
      UNSELECTED_PAINT.setFlags(Paint.ANTI_ALIAS_FLAG);
      
      SELECTED_POINT_PAINT.setStyle(Paint.Style.FILL);
      SELECTED_POINT_PAINT.setColor(rs.getColor(R.color.neighbourhood_selected_point_color));
      SELECTED_POINT_PAINT.setFlags(Paint.ANTI_ALIAS_FLAG);
      
      REMOVE_POINT_PAINT.setStyle(Paint.Style.STROKE);
      REMOVE_POINT_PAINT.setStrokeWidth(rs.getDimension(R.dimen.neighbourhood_focused_stroke));
      REMOVE_POINT_PAINT.setColor(rs.getColor(R.color.neighbourhood_remove_point_color));
      REMOVE_POINT_PAINT.setFlags(Paint.ANTI_ALIAS_FLAG);

      HANDLE_SIZE = rs.getDimension(R.dimen.neighbourhood_handle_size);

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
    mImageBounds = new Rect(imageRect);
  }

  public void setScreenMatrix(Matrix m) {
    mMatrix.set(m);
  }

  public Rect getBounds() {
    Rect bounds;
    // Calculate the bounds of the polygon
    if (mShape == Shape.POLYGON) {
      bounds = mPoly.getBounds();
    }
    else {
      bounds = new Rect(mBounds);
    }
    return bounds;
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
    if (mImageBounds == null) return;

    int w = mImageBounds.width();
    int h = mImageBounds.height();
    // Use the smaller side to determine the padding
    // This makes it feel more uniform
    int padding = (int)(Math.min(w, h) * PADDING_RATIO);
    setBounds(new Rect(padding, padding, w-padding, h-padding));
  }
  
  public void updateBounds() {
    mBounds.set(getBounds());
  }

  public void setShape(Shape s) {
    mShape = s;
    if (mShape == Shape.POLYGON) {
      updateBounds();
      mView.invalidate();
    }
    else {
      if (mBounds.isEmpty()) {
        resetBounds();
      }
      mView.invalidate(getPaddedScreenSpaceBounds());
    }
  }

  public Shape getShape() {
    return mShape;
  }

  // Movement and Events

  public void reset() {
    if (mShape == Shape.POLYGON) {
      mPoly.clear();
      updateBounds();
    }
    else {
      resetBounds();
    }
  
    // Check if we need to follow the focused neighbourhood
    if (mFocused) {
      mView.followResize(this);
    }
    mView.invalidate();
  }

  // The amount a touch can be off and still be considered touching an edge
  protected static final float TOUCH_PADDING = 0.05f;
  protected static final float TOUCH_SHIFT = 0.025f;

  // The minimum size of the neighbourhood relative to screen size
  protected static final float MIN_SIZE = 0.2f;

  private Action mAction = Action.NONE;
  private Edge mEdge = Edge.NONE;

  // The Previously touched position IN IMAGE SPACE
  private float mLastX;
  private float mLastY;

  // The previous distance between two fingers, used for pinch zoom
  private float mLastDistanceX;
  private float mLastDistanceY;
  private Action mLastAction = Action.NONE;
  
  private Point mSelectedPoint;

  // The action currently taking place
  public enum Action { NONE, MOVE, RESIZE, SCALE, MOVE_POINT }

  // The edge or pair of edges that are currently selected
  public enum Edge { NONE, TL, T, TR, R, BR, B, BL, L, ALL }

  /** 
   * Handles a down event.
   */
  // TODO: Deal with complex polygons
  public void handleDown(MotionEvent event) {
    float[] p = convertToImageSpace(event.getX(), event.getY());
    float x = p[0];
    float y = p[1];

    // Polygons
    if (mShape == Shape.POLYGON) {      
      if ((mSelectedPoint = checkPoints(x, y)) != null) {
        mAction = Action.MOVE_POINT;
      }
      else if(mPoly.contains((int)x, (int)y)) {        
        mAction = Action.MOVE;
      }
      else {
        // Create a new point
        addPoint((int)x, (int)y);
      }
    }
    // Deal with non-polygons
    else {
      if ( (mEdge = checkEdges(x, y)) != Edge.NONE) {
        mAction = Action.RESIZE;
      }
      // Check if touch was within neighbourhood
      else if (mBounds.contains((int)x, (int)y)) {
        mAction = Action.MOVE;      
      }
    }

    // Record position
    mLastX = x;
    mLastY = y;
  }
  
  public Point addPoint(int x, int y) {
    Point p = null;
    if (mImageBounds.contains(x, y)) {
      p = mPoly.addPoint(x, y);
      updateBounds();
      // TODO: Invalidate dirty rect when adding points to poly
      mView.invalidate();
    }
    return p;
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

    float shift = Math.min(mView.getWidth(), mView.getHeight()) * TOUCH_SHIFT;
    float padding = Math.min(mView.getWidth(), mView.getHeight()) * TOUCH_PADDING;

    if      (Math.abs(x - left  + shift) <= padding) rtn = Edge.L;
    else if (Math.abs(x - right - shift) <= padding) rtn = Edge.R;

    // TODO: UGLY UGLY UGLY
    if (Math.abs(y - top + shift) <= padding) {
      rtn = (rtn == Edge.L) ? Edge.TL : (rtn == Edge.R ? Edge.TR : Edge.T); 
    }
    else if (Math.abs(y - bottom - shift) <= padding) {
      rtn = (rtn == Edge.L) ? Edge.BL : (rtn == Edge.R ? Edge.BR : Edge.B);
    }

    return rtn;
  }

  /**
   * Given the touched point determines which point of the polygon should be selected.
   * @param x
   * @param y
   * @return the touched point or null if no point is being touched
   */
  private Point checkPoints(float x, float y) {
    float padding = Math.max(mView.getWidth(), mView.getHeight()) * TOUCH_PADDING;
    for (Point p : mPoly.getPoints()) {
      if ( Math.abs(p.x - x) <= padding && Math.abs(p.y - y) <= padding) {
        return p;
      }
    }
    return null;
  }

  /**
   * Handles an up event.
   */
  public void handleUp(MotionEvent event) {
    // Move view to follow neighbourhood
    switch(mAction) {
      case MOVE: 
        mView.followMove(this);
        break;
      case RESIZE:
        mView.followResize(this);
        break;
      case MOVE_POINT:
        //delete the selected point if it is outside of the image bounds
        if (!mImageBounds.contains(mSelectedPoint.x, mSelectedPoint.y)) {
          mPoly.removePoint(mSelectedPoint);
          updateBounds();
          mView.invalidate();
        }
        break;
    }

    // Reset current action
    mAction = Action.NONE;
    mView.invalidate(getPaddedScreenSpaceBounds());
  }

  /**
   * handles motion to move the neighbourhood.
   */
  // TODO: Break down handleMove
  // TODO: Make work with polygons
  // TODO: Why are we recording the last point like 7 times
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
          // TODO: Constrain scale to screen and maintain aspect ratio
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
        case MOVE_POINT:
          dx = x - mLastX;
          dy = y - mLastY;
          mSelectedPoint.offset((int)dx, (int)dy);
          // TODO: Alert that the point will be deleted
          updateBounds();
          mLastX = x;
          mLastY = y;
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
        Math.min(mImageBounds.width() - mBounds.width(), mBounds.left),
        Math.min(mImageBounds.height() - mBounds.height(), mBounds.top));
  }

  /**
   * Resizes the given edge by the given delta.
   * @param dx
   * @param dy
   * @param edg
   */
  private void resize(int dx, int dy, Edge edg) {
    int minSize = 
        (int) (Math.min(mView.getWidth(), mView.getHeight()) * MIN_SIZE / mView.getScale());
    switch (edg) {
      case L: 
        // constrain to image area
        mBounds.left = Math.max(0, mBounds.left + dx); 
        // prevent flipping and keep min size
        mBounds.left = Math.min(mBounds.left, mBounds.right - minSize); 
        break;
      case R: 
        mBounds.right = Math.min(mImageBounds.right, mBounds.right + dx);
        mBounds.right = Math.max(mBounds.right, mBounds.left + minSize);
        break;
      case T: 
        mBounds.top = Math.max(0, mBounds.top + dy);
        mBounds.top = Math.min(mBounds.top, mBounds.bottom - minSize);
        break;
      case B: 
        mBounds.bottom = Math.min(mImageBounds.bottom, mBounds.bottom + dy);
        mBounds.bottom = Math.max(mBounds.bottom, mBounds.top + minSize);
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
  public RectF getScreenSpaceBounds() {
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
    r.inset(-padding, -padding);
    return new Rect(
        Math.round(r.left), 
        Math.round(r.top), 
        Math.round(r.right), 
        Math.round(r.bottom));
  }

  /**
   * Draws the neighbourhood to the given canvas.
   * @param canvas
   */
  // TODO: Only draw relevant handles while resizing
  protected void draw(Canvas canvas) {

    RectF bounds = getScreenSpaceBounds();

    Path shapePath = new Path();

    switch (mShape) {
      case RECTANGLE:
        shapePath.addRect(bounds, Path.Direction.CW);
        break;
      case OVAL:
        shapePath.addOval(bounds, Path.Direction.CW);
        break;
      case POLYGON:
        shapePath.addPath(mPoly.getPath(mMatrix));
        break;
    }

    if (mFocused) {        

      // Darken outside
      drawUnselected(canvas, shapePath);

      // Don't draw handles while moving
      if (mAction != Action.MOVE) {
        if (mAction == Action.MOVE_POINT) {
          drawSelected(canvas);
        }
        else {      
          drawHandles(canvas);          
        }
      }
    }
    else {
      canvas.drawPath(shapePath, UNFOCUSED_PAINT);
    }

    // Draw bounds guide for non rectangles
    if (mShape != Shape.RECTANGLE) {
      canvas.drawRect(bounds, GUIDE_PAINT);
    }
  }
  
  private void drawUnselected(Canvas canvas, Path shapePath) {
    canvas.save();
    canvas.clipPath(shapePath, Region.Op.DIFFERENCE);
    canvas.drawPaint(UNSELECTED_PAINT);
    canvas.restore();
    canvas.drawPath(shapePath, FOCUSED_PAINT);
  }
  
  /**
   * Draws handle on the neighbourhood
   * @param canvas
   */
  private void drawHandles(Canvas canvas) {
    Path handlePath = new Path();
    
    if (mShape == Shape.POLYGON) {
      for (Point p : mPoly.getPoints()) {
        // check if the current point is selected
        handlePath.addPath(HANDLE_PATH, p.x, p.y);
      }      
    }
    else {

      float l = mBounds.left;
      float r = mBounds.right;
      float t = mBounds.top;
      float b = mBounds.bottom;

      float midX = l + mBounds.width() / 2;
      float midY = t + mBounds.height() / 2;

      // Draw a handle in the middle of each side
      handlePath.addPath(HANDLE_PATH, midX, t);
      handlePath.addPath(HANDLE_PATH, midX, b);
      handlePath.addPath(HANDLE_PATH, l, midY);
      handlePath.addPath(HANDLE_PATH, r, midY);
    }
    
    // transform by the screen
    handlePath.transform(mMatrix);
    
    // draw handles
    canvas.drawPath(handlePath, HANDLE_PAINT);
  }
  
  /**
   * Draws the selected handle and lines attached to it differently
   * @param canvas
   */
  private void drawSelected(Canvas canvas) {   
    
    int x = mSelectedPoint.x;
    int y = mSelectedPoint.y;
    
    Path selectedPath = new Path();

    // recolour lines when point is out of bounds
    if (!mImageBounds.contains(x, y)) {  
      
      int index = mPoly.indexOf(mSelectedPoint);
      int size = mPoly.size();
      
      Point p1 = mPoly.getPoint((index - 1 + size) % size);
      Point p2 = mSelectedPoint;
      Point p3 = mPoly.getPoint((index + 1) % size);
      
      selectedPath.moveTo(p1.x, p1.y);
      selectedPath.lineTo(p2.x, p2.y);
      selectedPath.lineTo(p3.x, p3.y);
      selectedPath.close();
      
      // transform by the screen
      selectedPath.transform(mMatrix);
      // draw handles
      canvas.drawPath(selectedPath, REMOVE_POINT_PAINT);
      
      // reset to draw point
      selectedPath.reset(); 
    }  
    
    // draw the handle
    
    // add the handle
    selectedPath.addPath(HANDLE_PATH, x, y);    
    // transform by the screen
    selectedPath.transform(mMatrix);
    // draw handles
    canvas.drawPath(selectedPath, SELECTED_POINT_PAINT); 
  }

}
