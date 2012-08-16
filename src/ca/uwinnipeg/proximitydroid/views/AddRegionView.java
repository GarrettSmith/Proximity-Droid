package ca.uwinnipeg.proximitydroid.views;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import ca.uwinnipeg.proximitydroid.Polygon;
import ca.uwinnipeg.proximitydroid.R;
import ca.uwinnipeg.proximitydroid.Region;
import ca.uwinnipeg.proximitydroid.Region.Edge;
import ca.uwinnipeg.proximitydroid.Region.Shape;
import ca.uwinnipeg.proximitydroid.RotatedBitmap;

/**
 * 
 * The view which the image and currently selected region is drawn.
 *
 */
public class AddRegionView extends ProximityImageView {  

  public static final String TAG = "AddRegionView";

  // The action currently taking place
  public enum Action { NONE, MOVE, RESIZE, MOVE_POINT }
  
  protected Region mRegion;
  
  // the currently selected point
  protected Point mSelectedPoint;

  // Neighbourhood following  
  private static final float FOLLOW_DURATION = 300f;
  private static final float SCALE_PADDING = 0.6f;
  private static final float SCALE_THRESHOLD = 0.1f;
  
  // Paint
  private static final Paint FOCUSED_PAINT = new Paint();
  private static final Paint GUIDE_PAINT = new Paint();
  private static final Paint HANDLE_PAINT = new Paint();
  private static final Paint UNSELECTED_PAINT = new Paint();
  private static final Paint SELECTED_POINT_PAINT = new Paint(); 
  private static final Paint REMOVE_POINT_PAINT = new Paint();

  // Handle drawing constants
  private static float HANDLE_SIZE;
  private static final Path HANDLE_PATH = new Path(); 
  
  // The amount a touch can be off and still be considered touching an edge
  protected static final float TOUCH_PADDING = 0.05f;
  protected static final float TOUCH_SHIFT = 0.025f;  
  
  // pointer debugging
  private static final boolean DEBUG_POINTER = false;
  private float debugX;
  private float debugY;
  
  // Bitmask for checking the touched edge of the region
  public static final byte LEFT =   8; // 0b1000
  public static final byte TOP  =   4; // 0b0100
  public static final byte RIGHT =  2; // 0b0010
  public static final byte BOTTOM = 1; // 0b0001
  
  public static final byte TOP_LEFT =     TOP + LEFT;     // 0b1100
  public static final byte TOP_RIGHT =    TOP + RIGHT;    // 0b0110
  public static final byte BOTTOM_LEFT =  BOTTOM + LEFT;  // 0b1001
  public static final byte BOTTOM_RIGHT = BOTTOM + RIGHT; // 0b0011
  
  // input handling
  private GestureDetector mSimpleDetector = 
      new GestureDetector(getContext(), new CustomSimpleOnGestureListener());;

  protected Action mAction = Action.NONE;
  protected Region.Edge mSelectedEdge = Region.Edge.NONE;
  
  // setup flag
  private static boolean SETUP = false;
  
  public AddRegionView(Context context){
    super(context);
    setup();
  }

  public AddRegionView(Context context, AttributeSet attributes){
    super(context, attributes);
    setup();
  }
  
  private void setup() {
    // on time static setup
    if (!SETUP) {
      SETUP = true;
      
      // Borrow the view's resources
      Resources rs = getResources();

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
  }

  /**
   * Sets the region to display.
   * @param reg
   */
  public void setRegion(Region reg) {
    mRegion = reg;
    invalidate();
  }

  /**
   * Returns the region added to the view.
   * @return
   */
  public Region getRegion() {
    return mRegion;
  }
  
  /**
   * Returns the bounds of the region in screen space.
   * @return
   */
  public RectF getScreenSpaceBoundsF() {
    RectF r = mRegion.getBoundsF();
    mFinalMatrix.mapRect(r);
    return r;
  }
  
  /**
   * Returns the bounds of the region in screen space.
   * @return
   */
  public Rect getScreenSpaceBounds() {
    RectF r = getScreenSpaceBoundsF();
    return new Rect(
        Math.round(r.left), 
        Math.round(r.top), 
        Math.round(r.right), 
        Math.round(r.bottom));
  }
  
  /**
   * Returns the bounds of the region in screen space with an additional padding to cover the 
   * handles being drawn.
   * @return
   */
  public Rect getPaddedScreenSpaceBounds() {
    int padding = (int) HANDLE_SIZE + 1;
    Rect r = getScreenSpaceBounds();
    r.inset(-padding, -padding);
    return new Rect(
        Math.round(r.left), 
        Math.round(r.top), 
        Math.round(r.right), 
        Math.round(r.bottom));
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    // only draw if we have a region to draw
    if (mRegion != null) {
      
      Path shapePath = getShapePath();
      shapePath.transform(mFinalMatrix);

      // dim outside
      drawUnselected(canvas, shapePath);
      
      // draw the shape
      canvas.drawPath(shapePath, FOCUSED_PAINT);

      // draw the center pixel
      try {
        if (!(mRegion.getShape() == Shape.POLYGON && mRegion.getPolygon().size() < 3)) {
          drawCenter(canvas);
        }
      }
      catch(NullPointerException ex) {
        Log.e(TAG, "Pixel wasn't loaded yet to be used for center");
      }

      // draw shape
      if (mAction == Action.MOVE_POINT) {
        drawSelectedPoint(canvas);
      }
      else {      
        drawHandles(canvas);          
      }

      // Draw bounds guide for non rectangles
      if (mRegion.getShape() != Shape.RECTANGLE) {
        canvas.drawRect(getScreenSpaceBounds(), GUIDE_PAINT);
      }
      
      // draw debug points
      if (DEBUG_POINTER) {
        canvas.save();
        canvas.concat(mFinalMatrix);
        canvas.drawCircle(debugX, debugY, 80, FOCUSED_PAINT);
        canvas.restore();
      }
      
    }
  }

  private Path getShapePath() {
    // remove the selected point from the drawn poly if it was moved outside of the image bounds
    if (mRegion.getShape() == Shape.POLYGON && 
        mAction == Action.MOVE_POINT && 
        !getImageBounds().contains(mSelectedPoint.x, mSelectedPoint.y)) {
      Path shapePath = new Path();
      Polygon p = new Polygon(mRegion.getPolygon());
      p.removePoint(p.indexOf(mSelectedPoint));
      shapePath.addPath(p.getPath());
      //shapePath.transform(mScreenMatrix);
      return shapePath;
    }
    else {
      return mRegion.getShapePath();
    }
  }

  private void drawUnselected(Canvas canvas, Path shapePath) {
    canvas.save();
    canvas.clipPath(shapePath, android.graphics.Region.Op.DIFFERENCE);
    canvas.drawPaint(UNSELECTED_PAINT);
    canvas.restore();
  }
  
  private void drawCenter(Canvas canvas) {
    canvas.drawPath(mRegion.getCenterPath(mFinalMatrix), mRegion.getCenterPaint());
  }

  /**
   * Draws handle on the neighbourhood
   * @param canvas
   */
  private void drawHandles(Canvas canvas) {
    // Don't draw handles while moving
    if (mAction != Action.MOVE) {
      Path handlePath = new Path();

      if (mRegion.getShape() == Shape.POLYGON) {
        // don't draw handles when resizing polygons
        if (mAction != Action.RESIZE) {
          float[] ps = mRegion.getPolygon().toFloatArray();
          mFinalMatrix.mapPoints(ps);
          for (int i = 0; i < ps.length; i += 2) {
            handlePath.addPath(HANDLE_PATH, ps[i], ps[i+1]);
          }      
        }
      }
      else {
        RectF screenBounds = getScreenSpaceBoundsF();
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
  
  private RectF getImageBounds() {
    return new RectF(0, 0, mBitmap.getWidth(), mBitmap.getHeight());
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
  private void drawSelectedPoint(Canvas canvas) {   

    int x = mSelectedPoint.x;
    int y = mSelectedPoint.y;

    Path selectedPath = new Path();

    // recolour lines when point is out of bounds
    if (!getImageBounds().contains(x, y)) {  
      Polygon poly = mRegion.getPolygon();

      int index = poly.indexOf(mSelectedPoint);
      int size = poly.size();

      Point p1 = poly.getPoint((index - 1 + size) % size);
      Point p2 = mSelectedPoint;
      Point p3 = poly.getPoint((index + 1) % size);

      selectedPath.moveTo(p1.x, p1.y);
      selectedPath.lineTo(p2.x, p2.y);
      selectedPath.lineTo(p3.x, p3.y);

      // transform by the screen
      selectedPath.transform(mFinalMatrix);
      // draw handles
      canvas.drawPath(selectedPath, REMOVE_POINT_PAINT);

      // reset to draw point
      selectedPath.reset(); 
    }  

    // draw the handle

    // Find the dimension
    float[] p = {x, y};
    mFinalMatrix.mapPoints(p);

    // add the handle
    selectedPath.addPath(HANDLE_PATH, p[0], p[1]);
    // draw handles
    canvas.drawPath(selectedPath, SELECTED_POINT_PAINT); 
  }

  /**
   * Pan to center the given neighbourhood in the view.
   * @param reg
   */
  public void followMove(Region reg) {
    // Determine the zoom required
    RectF bounds = reg.getBoundsF();
    mFinalMatrix.mapRect(bounds);

    float dx = 0;
    float dy = 0;

    float vw = getWidth();
    float vh = getHeight();

    // re-center if the region leaves or gets too close to the edge of screen
    RectF screen = new RectF(getLeft(), getTop(), getRight(), getBottom());

    if (!screen.contains(bounds)) {
      dx = ((vw / 2f) - bounds.centerX());
      dy = ((vh / 2f) - bounds.centerY());
    }
    
    // Get the bounds of the image
    RectF imageBounds = getImageScreenBounds();
    float iw = imageBounds.width();
    float ih = imageBounds.height();
    
    // Show the entire axis if it can fit on the screen
    if (iw <= vw) {
      dx = (vw / 2f) - imageBounds.centerX();
    }

    if (ih <= vh) {
      dy = (vh / 2f) - imageBounds.centerY();
    }

    panBy(dx, dy, FOLLOW_DURATION);
  }

  /**
   * Zoom to fit and pan to center the given region in the view.
   * @param reg
   */
  public void followResize(Region reg) {
    RectF bounds = reg.getBoundsF();
    mFinalMatrix.mapRect(bounds);
    
    float w = bounds.width();
    float h = bounds.height();

    float vw = getWidth();
    float vh = getHeight();
    float zoom;
    // Skip everything and zoom completely out
    if (bounds.isEmpty()) {
      zoom = MIN_SCALE;
    }
    else {
      float z1 = vw / w * SCALE_PADDING;
      float z2 = vh / h * SCALE_PADDING;

      zoom = Math.min(z1, z2); 
      zoom *= getScale();
      // Limit the zoom
      zoom = Math.min(MAX_SCALE, zoom);
      zoom = Math.max(MIN_SCALE, zoom);
    }

    // Check if zoom has changed enough to need updating or we are at minimum zoom
    if ((Math.abs(zoom - getScale()) / zoom) > SCALE_THRESHOLD || zoom == MIN_SCALE) {
      float dx = (vw / 2f) - bounds.centerX();
      float dy = (vh / 2f) - bounds.centerY();

      // Check if we are zoomed out enough to center
      RectF imageBounds = getImageScreenBounds();
      float dScale = zoom / getScale();
      float iw = imageBounds.width() * dScale;
      float ih = imageBounds.height() * dScale;
      
      if (iw <= vw) {
        dx = (vw / 2f) - imageBounds.centerX();
      }

      if (ih <= vh) {
        dy = (vh / 2f) - imageBounds.centerY();
      }
      
      // Pan and zoom
      panByZoomTo(dx, dy, zoom, FOLLOW_DURATION);
    }
  }

  @Override 
  public boolean onTouchEvent(MotionEvent event) {
    if (event.getAction() == MotionEvent.ACTION_UP) {
      onUp(event);
    }
    return mSimpleDetector.onTouchEvent(event);
  }
  
  /**
   * Handles an up event.
   * @param event
   */
  public void onUp(MotionEvent event) {
    // Move view to follow neighbourhood
    switch(mAction) {
      case MOVE: 
        followMove(mRegion);
        break;
      case RESIZE:
        followResize(mRegion);
        break;
      case MOVE_POINT:
        //delete the selected point if it is outside of the image bounds
        if (!getImageBounds().contains(mSelectedPoint.x, mSelectedPoint.y)) {
          mRegion.getPolygon().removePoint(mSelectedPoint);
          mRegion.updateBounds();
          invalidate();
        }
        followResize(mRegion);
        break;
    }

    // Reset current action
    mAction = Action.NONE;
    invalidate(getPaddedScreenSpaceBounds());
  }

  protected class CustomSimpleOnGestureListener extends SimpleOnGestureListener {
    
    @Override
    public boolean onDown(MotionEvent e) {
      float[] p = convertToImageSpace(e.getX(), e.getY());
      float x = p[0];
      float y = p[1];      

      // display debug pointer
      if (DEBUG_POINTER) {
        debugX = x;
        debugY = y;
        invalidate();
      }

      // Polygons
      if (mRegion.getShape() == Shape.POLYGON) {      
        if ((mSelectedPoint = checkPoints(x, y)) != null) {
          mAction = Action.MOVE_POINT;
        }
        else if(mRegion.getPolygon().contains((int)x, (int)y)) {        
          mAction = Action.MOVE;
        }
        else {
          // Create a new point
          mRegion.addPoint((int)x, (int)y);
        }

      }
      // Deal with non-polygons
      else {
        if ( (mSelectedEdge = checkEdges(x, y)) != Edge.NONE) {
          mAction = Action.RESIZE;
        }
        // Check if touch was within neighbourhood
        else if (mRegion.getBounds().contains((int)x, (int)y)) {
          mAction = Action.MOVE;      
        }
      }
      return true;
    }
    
    /**
     * Given a point in image space determines which edge or pair of edges are within a TOUCH_PADDING
     * of the point.
     * @param x
     * @param y
     * @return the edges being touched
     */
    private Edge checkEdges(float x, float y) {
      Rect bounds = mRegion.getBounds();
      int left = bounds.left;
      int right = bounds.right;
      int top = bounds.top;
      int bottom = bounds.bottom;
      
      byte mask = 0;

      float shift = Math.min(getWidth(), getHeight()) * TOUCH_SHIFT;
      float padding = Math.min(getWidth(), getHeight()) * TOUCH_PADDING;
      
      // the distance of the point from the edges
      float dLeft, dRight, dTop, dBottom;    
      dLeft =   Math.abs(x - left);
      dRight =  Math.abs(x - right);
      dTop =    Math.abs(y - top);
      dBottom = Math.abs(y - bottom);

      // left and right
      if (dLeft < dRight && (dLeft + shift) <= padding) {
        mask = LEFT;
      }
      else if ((dRight - shift) <= padding) {
        mask = RIGHT;
      }

      // top and bottom
      if (dTop < dBottom && (dTop + shift) <= padding) {
        mask += TOP;
      }
      else if ((dBottom - shift) <= padding) {
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
          return Edge.TR;
        case BOTTOM_LEFT:
          return Edge.BL;
        case BOTTOM_RIGHT:
          return Edge.BR;
        default:
          return Edge.NONE;
      }
    }

    /**
     * Given the touched point determines which point of the polygon should be selected.
     * @param x
     * @param y
     * @return the touched point or null if no point is being touched
     */
    private Point checkPoints(float x, float y) {
      float padding = Math.max(getWidth(), getHeight()) * TOUCH_PADDING;
      for (Point p : mRegion.getPolygon().getPoints()) {
        if ( Math.abs(p.x - x) <= padding && Math.abs(p.y - y) <= padding) {
          return p;
        }
      }
      return null;
    }
    
    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float dx, float dy) {
      // handle orientation change
      switch (mBitmap.getOrientation()) {
        case RotatedBitmap.CW:
          float tmp = dx;
          dx = dy;
          dy = -tmp;
          break;
        case RotatedBitmap.UPSIDEDOWN:
          dy = -dy;
          dx = -dx;
          break;
        case RotatedBitmap.CCW:
          float tmp2 = dx;
          dx = -dy;
          dy = tmp2;
          break;           
      }
      // scale delta
      dx /= getScale();
      dy /= getScale();
      // display debug pointer
      if (DEBUG_POINTER) {
        float[] p = convertToImageSpace(e2.getX(), e2.getY());
        float x = p[0];
        float y = p[1];   
        debugX = x;
        debugY = y;
        invalidate();
      }
      // Determine which action to take
      switch (mAction) {
        case MOVE:
          mRegion.move((int)-dx, (int)-dy);
          break;
        case RESIZE:
          mRegion.resize((int)-dx, (int)-dy, mSelectedEdge);
          break;
        case MOVE_POINT:
          mSelectedPoint.offset((int)-dx, (int)-dy);
          mRegion.updateBounds();
          break;
      }
      invalidate();
      return true;
    }
  }

}