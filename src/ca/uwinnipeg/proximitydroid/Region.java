package ca.uwinnipeg.proximitydroid;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import ca.uwinnipeg.proximitydroid.views.ProximityImageView;

/**
 * @author Garrett Smith
 *
 */
// TODO: Add drawing center point
// TODO: Look into making rotate not affect input
// TODO: Use spinner for title
// TODO: Add hiding bars
// TODO: Look into old shape being drawn after changing shape
// TODO: Split out more select only parts
public class Region {

  public static final String TAG = "Region";

  // The default ratio of padding when resetting the neighbour hood size
  public static final float PADDING_RATIO = 1/8f;  
  
  // The minimum size of the neighbourhood relative to screen size
  protected static final float MIN_SIZE = 0.2f;

  // Flags if one time setup has been done
  private static boolean SETUP = false;
  
  //Paint shared by all neighbourhoods
  protected static final Paint REGION_PAINT = new Paint();

  // The bounds of the neighbourhood IN IMAGE SPACE
  protected Rect mBounds = new Rect();

  // The view containing this neighbourhood.
  protected ProximityImageView mView;

  // Whether this neighbourhood is selected or not.
  boolean mFocused;

  // The matrix used to move from image space to screen space
  protected Matrix mScreenMatrix; 

  // The image bounds in image space
  protected Rect mImageBounds;

  public enum Shape { RECTANGLE, OVAL, POLYGON }

  // The edge or pair of edges that are currently selected
  public enum Edge { NONE, TL, T, TR, R, BR, B, BL, L, ALL }

  protected Shape mShape = Shape.RECTANGLE;

  // The list of points that make up the polygon
  protected Polygon mPoly = new Polygon();

  public Region(ProximityImageView v){
    mView = v;

    // Grab the matrix
    mScreenMatrix = mView.getFinalMatrix();
    
    // One-time setup
    if (!SETUP) {
      SETUP = true;

      // Borrow the view's resources
      Resources rs = v.getResources();

      REGION_PAINT.setStyle(Paint.Style.STROKE);
      REGION_PAINT.setColor(rs.getColor(R.color.region_unfocused_color));
      REGION_PAINT.setFlags(Paint.ANTI_ALIAS_FLAG);
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
    mScreenMatrix.set(m);
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
    mPoly.setBounds(r);
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
  
  protected void updateBounds() {
    if (mShape == Shape.POLYGON) {
      mBounds.set(getBounds());
    }
    else {
      resetBounds();
    }
  }

  public void setShape(Shape s) {
    mShape = s;
    mView.invalidate(getPaddedScreenSpaceBounds());
    // update the bounds if the poly has one point or less
    if (mPoly.size() < 2) updateBounds();
  }

  public Shape getShape() {
    return mShape;
  }
  
  public void setPolygon(Polygon poly) {
    Rect dirty = new Rect(getPaddedScreenSpaceBounds());
    mPoly.set(poly);
    updateBounds();
    dirty.union(getPaddedScreenSpaceBounds());
    mView.invalidate(dirty);
  }
  
  public Polygon getPolygon() {
    return mPoly;
  }

  public void reset() {
    if (mShape == Shape.POLYGON) {
      mPoly.reset();
      updateBounds();
    }
    else {
      resetBounds();
    }
    mView.invalidate();
  }

  // Movement and Events

  public Point addPoint(int x, int y) {
    Point newPoint = new Point(x, y);    
    // only add a point if it is within image bounds
    if (mImageBounds.contains(x, y)) {
      int size = mPoly.size();
      int index = 0;
      // if we have two or fewer points this doesn't matter
      if (size > 2) {
        // find the edge that is closest to the point
        float closest = Float.MAX_VALUE;
        Point current, next;
        for (int i = 0; i < size; i++) {
          current = mPoly.getPoint(i);
          next = mPoly.getPoint((i + 1) % size);
          float d = MathUtil.pointLineDistance(current, next, newPoint);
          if (d < closest) {
            closest = d;
            index = i + 1;
          }
        }
      }        

      // Add the point between the nearest point and it's nearest, to the new point, neighbour
      newPoint = mPoly.addPoint(index, newPoint);
      updateBounds();
      // TODO: Invalidate dirty rect when adding points to poly
      mView.invalidate();
    }
    return newPoint;
  }

  /**
   * Moves the neighbourhood by the given delta.
   * @param dx
   * @param dy
   */
  public void move(int dx, int dy) {
    Rect bounds = getBounds();

    // move
    bounds.offset(dx, dy);

    // constrain top and left
    bounds.offsetTo(
        Math.max(0, bounds.left),
        Math.max(0, bounds.top));

    // constrain bottom and right
    bounds.offsetTo(
        Math.min(mImageBounds.width() - bounds.width(), bounds.left),
        Math.min(mImageBounds.height() - bounds.height(), bounds.top));

    setBounds(bounds);
  }

  /**
   * Resizes the given edge by the given delta.
   * @param dx
   * @param dy
   * @param edg
   */
  private void resize(int dx, int dy, Edge edg, Rect newBounds) {
    int minSize = 
        (int) (Math.min(mView.getWidth(), mView.getHeight()) * MIN_SIZE / mView.getScale());
    switch (edg) {
      case L: 
        // constrain to image area
        newBounds.left = Math.max(0, newBounds.left + dx); 
        // prevent flipping and keep min size
        newBounds.left = Math.min(newBounds.left, newBounds.right - minSize); 
        break;
      case R: 
        newBounds.right = Math.min(mImageBounds.right, newBounds.right + dx);
        newBounds.right = Math.max(newBounds.right, newBounds.left + minSize);
        break;
      case T: 
        newBounds.top = Math.max(0, newBounds.top + dy);
        newBounds.top = Math.min(newBounds.top, newBounds.bottom - minSize);
        break;
      case B: 
        newBounds.bottom = Math.min(mImageBounds.bottom, newBounds.bottom + dy);
        newBounds.bottom = Math.max(newBounds.bottom, newBounds.top + minSize);
        break;
      case TL:
        resize(dx, dy, Edge.T, newBounds);
        resize(dx, dy, Edge.L, newBounds);
        break;
      case TR:
        resize(dx, dy, Edge.T, newBounds);
        resize(dx, dy, Edge.R, newBounds);
        break;
      case BL:
        resize(dx, dy, Edge.B, newBounds);
        resize(dx, dy, Edge.L, newBounds);
        break;
      case BR:
        resize(dx, dy, Edge.B, newBounds);
        resize(dx, dy, Edge.R, newBounds);
        break;
    }
  }
  
  /**
   * Resizes the given edge by the given delta.
   * @param dx
   * @param dy
   * @param edg
   */
  public void resize(int dx, int dy, Edge edg) {
    Rect newBounds = getBounds();
    resize(dx, dy, edg, newBounds);
    setBounds(newBounds);
  }

  /**
   * Scales the distance given the delta in distance between pointers.
   * @param dDist
   */
  protected void scale(int dx, int dy) {
    resize(-dx, -dy, Edge.TL);
    resize(dx, dy, Edge.BR);
  }

  /**
   * 
   * converts the given points to image space
   * @param x
   * @param y
   */
  protected float[] convertToImageSpace(float x, float y) {
    float[] point = new float[]{x, y};
    Matrix inverse = new Matrix();
    mScreenMatrix.invert(inverse);
    inverse.mapPoints(point);
    return point;
  }
  
  /**
   *  Maps the neighbourhood bounds from image space to screen space.
   * @return
   */
  public Rect getScreenSpaceBoundsRect() {
   return new Rect(mBounds.left, mBounds.top, mBounds.right, mBounds.bottom);
  }

  /**
   *  Maps the neighbourhood bounds from image space to screen space.
   * @return
   */
  public RectF getScreenSpaceBounds() {
    RectF r = new RectF(mBounds.left, mBounds.top, mBounds.right, mBounds.bottom);
    mScreenMatrix.mapRect(r);
    r.left    = Math.round(r.left);
    r.top     = Math.round(r.top);
    r.right   = Math.round(r.right); 
    r.bottom  = Math.round(r.bottom);
    return r;
  }
  
  public Rect getPaddedScreenSpaceBounds() {
    int padding = 1;
    RectF r = new RectF(mBounds.left, mBounds.top, mBounds.right, mBounds.bottom);
    mScreenMatrix.mapRect(r);
    r.inset(-padding, -padding);
    return new Rect(
        Math.round(r.left), 
        Math.round(r.top), 
        Math.round(r.right), 
        Math.round(r.bottom));
  }
  
  public void draw(Canvas canvas) {
    canvas.drawPath(getShapePath(), REGION_PAINT);
  }
  
  
  public Path getShapePath() {
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
        shapePath.addPath(mPoly.getPath());
        shapePath.transform(mScreenMatrix);
        break;
    }
    
    return shapePath;
  }

}