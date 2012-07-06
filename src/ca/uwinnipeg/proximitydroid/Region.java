package ca.uwinnipeg.proximitydroid;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Parcel;
import android.os.Parcelable;
import ca.uwinnipeg.proximity.image.Image;

/**
 * A region of interest in the {@link Image}.
 * <p>
 * A region has a shape, rectangle, oval, or polygon, and a bounds.
 * @author Garrett Smith
 *
 */
public class Region implements Parcelable {

  public static final String TAG = "Region";  

  /**
   * The possible region shapes.
   * @author Garrett Smith
   *
   */
  public enum Shape { RECTANGLE, OVAL, POLYGON }

  /**
   * Represents the edges of a region being selected.
   * @author Garrett Smith
   *
   */
  public enum Edge { NONE, TL, T, TR, R, BR, B, BL, L }
  
  // The default ratio of padding when resetting the region size
  public static final float PADDING_RATIO = 1/8f;

  // Used to create the center paint
  private static final Paint CENTER_BASE_PAINT = new Paint();

  // Used to create the enter point path
  public static final Path CENTER_BASE_PATH = new Path();

  // The bounds of the neighbourhood IN IMAGE SPACE
  protected Rect mBounds = new Rect();

  // current shape of the region
  protected Shape mShape = Shape.RECTANGLE;

  // The list of points that make up the polygon
  protected Polygon mPoly = new Polygon();
  
  // The image this region belongs to
  protected Image mImage;  

  // Flags if one time setup has been done
  private static boolean SETUP = false;
  
  /**
   * Creates a new region within the given image.
   * @param image
   */
  public Region(Image image) {
    mImage = image;
    setup();
  }
  
  /**
   * Creates a new region that is a copy of the given region.
   * @param source
   */
  public Region(Region source) {
    if (source != null) {
      mShape = source.mShape;
      mBounds = source.mBounds;
      mPoly = source.mPoly;
      mImage = source.mImage;
    }
    setup();
  }  
  
  private void setup() {    
    // One-time setup
    if (!SETUP) {
      SETUP = true;

      CENTER_BASE_PAINT.setStyle(Paint.Style.FILL);
      CENTER_BASE_PAINT.setFlags(Paint.ANTI_ALIAS_FLAG);

      float size = 10;//rs.getDimension(R.dimen.region_center_radius);
      CENTER_BASE_PATH.addRect(-size, -size, size, size, Path.Direction.CW);
    }
  }
  
  /**
   * Sets the {@link Image} this region is a part of.
   * @param image
   */
  public void setImage(Image image) {
    mImage = image;
  }

  /**
   * Returns the bounds of the region.
   * @return
   */
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

  public RectF getBoundsF() {
    return new RectF(mBounds.left, mBounds.top, mBounds.right, mBounds.bottom);
  }

  /**
   * Sets bounds of region.
   * @param r
   * @return the dirty rectangle in image space, you can use this to invalidate nicely 
   */
  public Rect setBounds(Rect r) {
    Rect dirty = new Rect(getBounds());
    mPoly.setBounds(r);
    mBounds.set(r);
    dirty.union(getBounds());
    return dirty;
  }

  /**
   * Sets the bounds to a default value.
   * @return the dirty rectangle in image space, you can use this to invalidate nicely 
   */
  public Rect resetBounds() {
    int w = mImage.getWidth();
    int h = mImage.getHeight();
    // Use the smaller side to determine the padding
    // This makes it feel more uniform
    int padding = (int)(Math.min(w, h) * PADDING_RATIO);
    return setBounds(new Rect(padding, padding, w-padding, h-padding));
  }
  
  public void updateBounds() {
    if (mShape == Shape.POLYGON) {
      mBounds.set(getBounds());
    }
  }
  
  private Rect getImageBounds() {
    return new Rect(0, 0, mImage.getWidth(), mImage.getHeight());
  }

  /**
   * Returns the shape of the region.
   * @return
   */
  public Shape getShape() {
    return mShape;
  }

  /**
   * Sets the shape of the region.
   * @param s
   */
  public void setShape(Shape s) {
    mShape = s;
    // update the bounds if the poly has one point or less
    if (mShape == Shape.POLYGON && mPoly.size() < 2) updateBounds();
  }

  /**
   * Returns the region's polygon.
   * @return
   */
  public Polygon getPolygon() {
    return mPoly;
  }

  /**
   * Sets the polygon of this region to be a copy of the given polygon.
   * @param poly
   * @return the dirty rectangle in image space, you can use this to invalidate nicely 
   */
  public Rect setPolygon(Polygon poly) {
    Rect dirty = new Rect(getBounds());
    mPoly.set(poly);
    updateBounds();
    dirty.union(getBounds());
    return dirty;
  }
  
  /**
   * Reset the polygon's points.
   * @return
   */
  public Rect resetPolygon() {
    Rect dirty = new Rect(getBounds());
    mPoly.reset();
    updateBounds();
    return dirty;
  }
  
  /**
   * Gets all the indices of pixels within the {@link Image} contained by this region.
   * @param img
   * @return
   */  
  public int[] getIndices() {
    Rect bounds = getBounds();
    int[] indices;
    
    switch (mShape) {
      case POLYGON:

        // find all the points within the poly
        int[] tmp = new int[bounds.width() * bounds.height()];
        int i = -1;
        for (int y = bounds.top; y < bounds.bottom; y++) {
          for (int x = bounds.left; x < bounds.right; x++) {
            if (mPoly.contains(x, y)) {
              tmp[++i] = mImage.getIndex(x, y);
            }
          }
        }
        // trim out the empty spots
        indices = Util.copyOf(tmp, i);
        break;
        
      case OVAL:
        // find all the points within the oval
        int[] tmp2 = new int[bounds.width() * bounds.height()];
        int j = -1;

        int cx = bounds.centerX();
        int cy = bounds.centerY();
        int rx2 = bounds.right - cx;
        rx2 *= rx2; // square
        int ry2 = bounds.bottom - cy;
        ry2 *= ry2; // square
        
        for (int y = bounds.top; y < bounds.bottom; y++) {
          for (int x = bounds.left; x < bounds.right; x++) {
            
            float dx = (float)(x - cx);
            dx *= dx;
            dx /= rx2;
            float dy = (float)(y - cy);
            dy *= dy;
            dy /= ry2;
            
            // if the point is within the oval
            if ( dx + dy <= 1) {
              tmp2[++j] = mImage.getIndex(x, y);
            }
          }
        }
        // trim out the empty spots
        indices = Util.copyOf(tmp2, j);
        break;
        
      default: // RECTANGLE
        indices = mImage.getIndices(bounds.left, bounds.top, bounds.right, bounds.bottom);
        break;
    }
    return indices;
  }
  
  /**
   * Gets the indices of all pixels within this region in list form.
   * @return
   */
  public List<Integer> getIndicesList() {
    int[] indices = getIndices();
    List<Integer> list = new ArrayList<Integer>();
    for (int i = 0; i < indices.length; i++) {
      list.add(indices[i]);
    }
    return list;
  }
  
  /**
   * Returns the center pixel of this region.
   * @param img
   * @return
   */  
  public int getCenterIndex() {
    Rect bounds = getBounds();
    return mImage.getIndex(bounds.centerX(), bounds.centerY());
  }
  
  /**
   * Moves the region by the given delta.
   * @param dx
   * @param dy
   */
  public Rect move(int dx, int dy) {
    Rect bounds = getBounds();
  
    // move
    bounds.offset(dx, dy);
  
    // constrain top and left
    bounds.offsetTo(
        Math.max(0, bounds.left),
        Math.max(0, bounds.top));
  
    // constrain bottom and right
    bounds.offsetTo(
        Math.min(mImage.getWidth() - bounds.width(), bounds.left),
        Math.min(mImage.getHeight() - bounds.height(), bounds.top));
  
    return setBounds(bounds);
  }

  /**
   * Resizes the given edge by the given delta.
   * @param dx
   * @param dy
   * @param edg
   */
  public Rect resize(int dx, int dy, Edge edg) {
    Rect newBounds = getBounds();
    resize(dx, dy, edg, newBounds);
    return setBounds(newBounds);
  }

  private void resize(int dx, int dy, Edge edg, Rect newBounds) {
    switch (edg) {
      case L: 
        // constrain to image area
        newBounds.left = Math.max(0, newBounds.left + dx); 
        // prevent flipping and keep min size
        newBounds.left = Math.min(newBounds.left, newBounds.right); 
        break;
      case R: 
        newBounds.right = Math.min(mImage.getWidth(), newBounds.right + dx);
        newBounds.right = Math.max(newBounds.right, newBounds.left);
        break;
      case T: 
        newBounds.top = Math.max(0, newBounds.top + dy);
        newBounds.top = Math.min(newBounds.top, newBounds.bottom);
        break;
      case B: 
        newBounds.bottom = Math.min(mImage.getHeight(), newBounds.bottom + dy);
        newBounds.bottom = Math.max(newBounds.bottom, newBounds.top);
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

  public Point addPoint(int x, int y) {
    Point newPoint = new Point(x, y);   
    Rect imageBounds = getImageBounds();
    // only add a point if it is within image bounds
    if (imageBounds.contains(x, y)) {
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
    }
    return newPoint;
  }
  
  public Path getPath() {
    Path path = new Path();
    path.addPath(getShapePath());

    // only add center point when the shape is a poly with atleast 3 points
    if (!(mShape == Shape.POLYGON && mPoly.size() < 3)) {
      path.addPath(getCenterPath());
    }
    
    return path;
  }

  /**
   * Returns the path representing this region in image space.
   * @return
   */
  public Path getShapePath() {
    Path shapePath = new Path();
  
    switch (mShape) {
      case RECTANGLE:
        shapePath.addRect(getBoundsF(), Path.Direction.CW);
        break;
      case OVAL:
        shapePath.addOval(getBoundsF(), Path.Direction.CW);
        break;
      case POLYGON:
        shapePath.addPath(mPoly.getPath());
        break;
    }
    
    return shapePath;
  }

  /**
   * Returns the path representing the center pixel of this region in image space.
   * @return
   */
  public Path getCenterPath() {
    return getCenterPath(new Matrix());
  }

  /**
   * Returns the path representing the center pixel of this region in image space.
   * @param matrix
   * @return
   */
  public Path getCenterPath(Matrix matrix) {
    Path centerPath = new Path();
    
    Rect bounds = getBounds();
    float[] p = new float[]{ bounds.centerX(), bounds.centerY() };
    matrix.mapPoints(p);
    
    centerPath.addPath(CENTER_BASE_PATH, p[0], p[1]);
    
    return centerPath;
  }
  
  /**
   * Returns the paint that is the same colour as the center pixel of this region.
   * @return
   */
  public Paint getCenterPaint() {
    Rect bounds = getBounds();
    Paint paint = new Paint(CENTER_BASE_PAINT);
    int color = mImage.getPixel(bounds.centerX(), bounds.centerY());
    paint.setColor(color);
    return paint;
  }

  /**
   * The region parcelable creator.
   */
  public static final Parcelable.Creator<Region> CREATOR =
      new Parcelable.Creator<Region>() {

    @Override
    public Region createFromParcel(Parcel source) {
      return new Region(source);
    }

    @Override
    public Region[] newArray(int size) {
      return new Region[size];
    }
  };    

  // Parcelable
  
  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeParcelable(mBounds, flags);
    dest.writeParcelable(mPoly, flags);
    dest.writeString(mShape.name());
    
  }

  private void readFromParcel(Parcel source) {
    mBounds = source.readParcelable(Rect.class.getClassLoader());
    mPoly = source.readParcelable(Polygon.class.getClassLoader());
    String name = source.readString();
    mShape = Shape.valueOf(name);
  }
  
  private Region(Parcel in) {
    readFromParcel(in);
  }

}
