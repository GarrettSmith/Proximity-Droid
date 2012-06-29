/**
 * 
 */
package ca.uwinnipeg.proximitydroid.views;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.MotionEvent;
import ca.uwinnipeg.proximity.image.Image;
import ca.uwinnipeg.proximitydroid.Polygon;
import ca.uwinnipeg.proximitydroid.R;

/**
 * @author Garrett Smith
 *
 */
public class SelectRegionView extends RegionView {  
  
  public static final String TAG = "SelectRegionView";
  
  // Bitmask for checking the touched edge of the region
  public static final byte LEFT =   8;
  public static final byte TOP  =   4;
  public static final byte RIGHT =  2;
  public static final byte BOTTOM = 1;
  
  public static final byte TOP_LEFT =     12;
  public static final byte TOP_RIGHT =    10;
  public static final byte BOTTOM_LEFT =  9;
  public static final byte BOTTOM_RIGHT = 3;

  // Paint shared by all select regions
  private static final Paint FOCUSED_PAINT = new Paint();
  private static final Paint GUIDE_PAINT = new Paint();
  private static final Paint HANDLE_PAINT = new Paint();
  private static final Paint UNSELECTED_PAINT = new Paint();
  private static final Paint SELECTED_POINT_PAINT = new Paint(); 
  private static final Paint REMOVE_POINT_PAINT = new Paint();
  
  private static boolean SETUP = false;

  // Handle drawing constants
  private static float HANDLE_SIZE;
  private static final Path HANDLE_PATH = new Path();  
  
  // The action currently taking place
  public enum Action { NONE, MOVE, RESIZE, MOVE_POINT }

  // The amount a touch can be off and still be considered touching an edge
  protected static final float TOUCH_PADDING = 0.05f;
  protected static final float TOUCH_SHIFT = 0.025f;

  protected Action mAction = Action.NONE;
  protected Edge mSelectedEdge = Edge.NONE;
  
  // The Previously touched position IN IMAGE SPACE
  protected float mLastX;
  protected float mLastY;
  
  // The previous distance between two fingers, used for pinch zoom
  protected float mLastDistanceX;
  protected float mLastDistanceY;
  
  protected Action mLastAction = Action.NONE;
  protected Point mSelectedPoint;
  
  protected RegionSelectView mSelectView;

  public SelectRegionView(RegionSelectView v, Image image) {
    super(v, image);
    
    if (!SETUP) {
      SETUP = true;
      
      // Borrow the view's resources
      Resources rs = v.getResources();

      FOCUSED_PAINT.setStyle(Paint.Style.STROKE);
      FOCUSED_PAINT.setStrokeWidth(rs.getDimension(R.dimen.region_focused_stroke));
      FOCUSED_PAINT.setColor(rs.getColor(R.color.region_focused_color));
      FOCUSED_PAINT.setFlags(Paint.ANTI_ALIAS_FLAG);

      GUIDE_PAINT.setStyle(Paint.Style.STROKE);
      GUIDE_PAINT.setStrokeWidth(rs.getDimension(R.dimen.region_guide_stroke));
      GUIDE_PAINT.setColor(rs.getColor(R.color.region_guide_color));
      GUIDE_PAINT.setFlags(Paint.ANTI_ALIAS_FLAG);

      HANDLE_PAINT.setStyle(Paint.Style.FILL);
      HANDLE_PAINT.setColor(rs.getColor(R.color.region_focused_color));
      HANDLE_PAINT.setFlags(Paint.ANTI_ALIAS_FLAG);

      UNSELECTED_PAINT.setColor(rs.getColor(R.color.region_unselected_color));
      UNSELECTED_PAINT.setFlags(Paint.ANTI_ALIAS_FLAG);

      SELECTED_POINT_PAINT.setStyle(Paint.Style.FILL);
      SELECTED_POINT_PAINT.setColor(rs.getColor(R.color.region_focused_color));
      SELECTED_POINT_PAINT.setFlags(Paint.ANTI_ALIAS_FLAG);

      REMOVE_POINT_PAINT.setStyle(Paint.Style.STROKE);
      REMOVE_POINT_PAINT.setStrokeWidth(rs.getDimension(R.dimen.region_focused_stroke));
      REMOVE_POINT_PAINT.setColor(rs.getColor(R.color.region_remove_point_color));
      REMOVE_POINT_PAINT.setFlags(Paint.ANTI_ALIAS_FLAG);

      HANDLE_SIZE = rs.getDimension(R.dimen.region_handle_size);

      float halfSize = HANDLE_SIZE / 2;
      HANDLE_PATH.addRect(-halfSize, -halfSize, halfSize, halfSize, Path.Direction.CW);
      Matrix m = new Matrix();
      m.postRotate(45);
      HANDLE_PATH.transform(m);
    }    
    
    mSelectView = v;
  }

  @Override
  public void reset() {
    super.reset();
    mSelectView.followResize(this);
  }

  @Override
  public Rect getPaddedScreenSpaceBounds() {
    int padding = (int) HANDLE_SIZE;
    padding += 1;
    RectF r = new RectF(mBounds.left, mBounds.top, mBounds.right, mBounds.bottom);
    mScreenMatrix.mapRect(r);
    r.inset(-padding, -padding);
    return new Rect(
        Math.round(r.left), 
        Math.round(r.top), 
        Math.round(r.right), 
        Math.round(r.bottom));
  }

  /** 
   * Handles a down event.
   */
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
      if ( (mSelectedEdge = checkEdges(x, y)) != Edge.NONE) {
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
    
    byte mask = 0;

    float shift = Math.min(mView.getWidth(), mView.getHeight()) * TOUCH_SHIFT;
    float padding = Math.min(mView.getWidth(), mView.getHeight()) * TOUCH_PADDING;

    // left and right
    if (Math.abs(x - left  + shift) <= padding) {
      mask = LEFT;
    }
    else if (Math.abs(x - right - shift) <= padding) {
      mask = RIGHT;
    }

    // top and bottom
    if (Math.abs(y - top + shift) <= padding) {
      mask += TOP;
    }
    else if (Math.abs(y - bottom - shift) <= padding) {
      mask += BOTTOM;
    }
    
    // translate bitmask to Edge
    switch (mask) {
      case LEFT:
        return Edge.L;
      case RIGHT:
        return Edge.R;
      case TOP:
        return Edge.T;
      case BOTTOM:
        return Edge.B;
      case TOP_LEFT:
        return Edge.TL;
      case TOP_RIGHT:
        return Edge.TL;
      case BOTTOM_LEFT:
        return Edge.BL;
      default: // BOTTOM_RIGHT
        return Edge.BR;
    }
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
        mSelectView.followMove(this);
        break;
      case RESIZE:
        mSelectView.followResize(this);
        break;
      case MOVE_POINT:
        //delete the selected point if it is outside of the image bounds
        if (!mImageBounds.contains(mSelectedPoint.x, mSelectedPoint.y)) {
          mPoly.removePoint(mSelectedPoint);
          updateBounds();
          mView.invalidate();
        }
        mSelectView.followResize(this);
        break;
    }

    // Reset current action
    mAction = Action.NONE;
    mView.invalidate(getPaddedScreenSpaceBounds());
  }

  /**
   * handles motion to move the neighbourhood.
   */
  // TODO: Handle input history
  public void handleMove(MotionEvent event) {

    // Check if any action is being performed
    if (mAction != Action.NONE) {

      float[] p = convertToImageSpace(event.getX(), event.getY());
      float x = p[0];
      float y = p[1];

      // rectangle that needs redraw
      Rect dirty = getPaddedScreenSpaceBounds();

      float dx = x - mLastX;
      float dy = y - mLastY;

      // Determine which action to take
      switch (mAction) {
        case MOVE:
          move((int)dx, (int)dy);
          break;
        case RESIZE:
          resize((int)dx, (int)dy, mSelectedEdge);
          break;
        case MOVE_POINT:
          mSelectedPoint.offset((int)dx, (int)dy);
          updateBounds();
          break;
      }

      // Record this as the previous position
      mLastX = x;
      mLastY = y;

      // Reflect change on screen
      dirty.union(getPaddedScreenSpaceBounds());
      mView.invalidate(dirty);
    }
  }

  /**
   * Draws the neighbourhood to the given canvas.
   * @param canvas
   */
  public void draw(Canvas canvas) {
  
    Path shapePath = getShapePath();
    
    // dim outside
    drawUnselected(canvas, shapePath);

    // draw handles
    if (mAction == Action.MOVE_POINT) {
      drawSelected(canvas);
    }
    else {      
      drawHandles(canvas);          
    }
  
    // Draw bounds guide for non rectangles
    if (mShape != Shape.RECTANGLE) {
      canvas.drawRect(getScreenSpaceBounds(), GUIDE_PAINT);
    }
  }

  @Override
  public Path getShapePath() {
    // remove the selected point from the drawn poly if it was moved outside of the image bounds
    if (mShape == Shape.POLYGON && 
        mAction == Action.MOVE_POINT && 
        !mImageBounds.contains(mSelectedPoint.x, mSelectedPoint.y)) {
      Path shapePath = new Path();
      Polygon p = new Polygon(mPoly);
      p.removePoint(mPoly.indexOf(mSelectedPoint));
      shapePath.addPath(p.getPath());
      shapePath.transform(mScreenMatrix);
      return shapePath;
    }
    else {
      return super.getShapePath();
    }
  }

  private void drawUnselected(Canvas canvas, Path shapePath) {
    canvas.save();
    canvas.clipPath(shapePath, android.graphics.Region.Op.DIFFERENCE);
    canvas.drawPaint(UNSELECTED_PAINT);
    canvas.restore();
    canvas.drawPath(shapePath, FOCUSED_PAINT);
  }

  /**
   * Draws handle on the neighbourhood
   * @param canvas
   */
  private void drawHandles(Canvas canvas) {
    // Don't draw handles while moving
    if (mAction != Action.MOVE) {
      Path handlePath = new Path();
  
      if (mShape == Shape.POLYGON) {
        // don't draw handles when resizing polygons
        if (mAction != Action.RESIZE) {
          float[] ps = mPoly.toFloatArray();
          mScreenMatrix.mapPoints(ps);
          for (int i = 0; i < ps.length; i += 2) {
            handlePath.addPath(HANDLE_PATH, ps[i], ps[i+1]);
          }      
        }
      }
      else {
        RectF screenBounds = getScreenSpaceBounds();
        if (mAction == Action.RESIZE) {
          // Draw only the resized handle
          handlePath.addPath(getHandlePath(mSelectedEdge, screenBounds));
        }
        else {
          // Draw a handle in the middle of each side
          handlePath.addPath(getHandlePath(screenBounds));
        }
      }
  
      // draw handles
      canvas.drawPath(handlePath, HANDLE_PAINT);
    }
  }

  private Path getHandlePath(Edge edg, RectF bounds) {
    Path p = new Path();
  
    float l = bounds.left;
    float r = bounds.right;
    float t = bounds.top;
    float b = bounds.bottom;
  
    float midX = l + bounds.width() / 2;
    float midY = t + bounds.height() / 2;
    
    switch (edg) {
      case L:
        p.addPath(HANDLE_PATH, l, midY);
        break;
      case R: 
        p.addPath(HANDLE_PATH, r, midY);
        break;
      case T: 
        p.addPath(HANDLE_PATH, midX, t);
        break;
      case B: 
        p.addPath(HANDLE_PATH, midX, b);
        break;
      case TL:
        p.addPath(getHandlePath(Edge.T, bounds));
        p.addPath(getHandlePath(Edge.L, bounds));
        break;
      case TR:
        p.addPath(getHandlePath(Edge.T, bounds));
        p.addPath(getHandlePath(Edge.R, bounds));
        break;
      case BL:
        p.addPath(getHandlePath(Edge.B, bounds));
        p.addPath(getHandlePath(Edge.L, bounds));
        break;
      case BR:
        p.addPath(getHandlePath(Edge.B, bounds));
        p.addPath(getHandlePath(Edge.R, bounds));
        break;
    }
    return p;
  }

  private Path getHandlePath(RectF bounds) {
    Path p = getHandlePath(Edge.TL, bounds);
    p.addPath(getHandlePath(Edge.BR, bounds));
    return p;
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
      
      // transform by the screen
      selectedPath.transform(mScreenMatrix);
      // draw handles
      canvas.drawPath(selectedPath, REMOVE_POINT_PAINT);
      
      // reset to draw point
      selectedPath.reset(); 
    }  
    
    // draw the handle
    
    // Find the dimension
    float[] p = {x, y};
    mScreenMatrix.mapPoints(p);
    
    // add the handle
    selectedPath.addPath(HANDLE_PATH, p[0], p[1]);
    // draw handles
    canvas.drawPath(selectedPath, SELECTED_POINT_PAINT); 
  }


}
