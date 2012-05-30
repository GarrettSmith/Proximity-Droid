package ca.uwinnipeg.compare;

import java.util.ArrayList;

import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Matrix;

// TODO: add drawing handles.
// TODO: add drawing points outside of bounds differently.
/**
 * A polygon used by neighbourhoods.
 * @author garrett
 *
 */
public class Polygon {

  private final ArrayList<Point> mPoints = new ArrayList<Point>();

  /**
   * Creates a new empty polygon.
   */
  public Polygon() {}
  /**
   * 
   * Creates a new polygon that is a copy of the given polygon.
   * @param orig the original polygon to copy
   * @param bounds the bounds of the image this polygon is contained in
   */
  public Polygon(Polygon orig, Rect bounds) {
    set(orig);
  }
  
  /**
   * Returns the number of points in the polygon.
   * @return
   */
  public int size() {
    return mPoints.size();
  }
  
  /**
   * Returns true if the polygon has no points.
   * @return
   */
  public boolean isEmpty() {
    return mPoints.size() == 0;
  }

  /**
   * Returns the point at the given index.
   * @param index
   * @return
   */
  public Point getPoint(int index) {
    return mPoints.get(index);
  }

  /**
   * Returns an array of all the points.
   * @return
   */
  public Point[] getPoints() {
    Point[] points = new Point[mPoints.size()];
    mPoints.toArray(points);
    return points;
  }

  /**
   * Converts the points of the polygon to an array of floats.
   * @return
   */
  public float[] toFloatArray() {
    final int size = mPoints.size();
    float[] fs = new float[size * 2];
    for (int i = 0; i < size; i++) {
      Point p = mPoints.get(i);
      fs[i * 2]     = p.x;
      fs[i * 2 + 1] = p.y;
    }
    return fs;
  }
  
  public int indexOf(Point p) {
    return mPoints.indexOf(p);
  }

  /**
   * Makes this polygon a copy of the given polygon.
   * @param orig
   */
  public void set(Polygon orig) {
    mPoints.clear();
    for (Point p : orig.mPoints) {
      addPoint(p);
    }
  }
  
  /**
   * Empties all points from the polygon.
   */
  public void clear() {
    mPoints.clear();
  }

  /**
   * Adds a point to the polygon and returns the added point.
   * @param p
   * @return
   */
  public Point addPoint(Point p) {
    Point point = new Point(p);
    mPoints.add(point);
    return point;
  }

  /**
   * Adds a point to the polygon and returns the added point.
   * @param x
   * @param y
   * @return
   */
  public Point addPoint(int x, int y) {
    Point p = new Point(x, y);
    mPoints.add(p);
    return p;
  }

  /**
   * Removes a point at the given index and returns the removed point.
   * @param index
   * @return
   */
  public Point removePoint(int index) {
    return mPoints.remove(index);
  }

  /**
   * Removes the given point.
   * @param p
   * @return true if the point was part of the polygon and removed
   */
  public boolean removePoint(Point p) {
    return mPoints.remove(p);
  }

  /**
   * Returns the bounds of this polygon. The bounds are empty if the polygon has less than 2 points.
   * @return
   */
  public Rect getBounds() {
    Rect bounds = new Rect();
    if (mPoints.size() > 1) {
      Point p1 = mPoints.get(0);
      Point p2 = mPoints.get(1);

      // create an initial bounds from the first two points
      bounds.set(p1.x, p1.y, p2.x, p2.y);
      bounds.sort(); // make sure the left is actually on the left etc.

      // Get the union of each point to get the final bounds
      for (int i = 2; i < mPoints.size(); i++) {
        Point p = mPoints.get(i);
        bounds.union(p.x, p.y);
      }
    }
    return bounds;
  }
  
  /**
   * 
   * @param bounds
   */
  public void setBounds(Rect bounds) {
    // TODO: Add setting polygon bounds
  }
  
  /**
   * Moves the polygon by the given deltas.
   * @param dx
   * @param dy
   */
  public void offset(int dx, int dy) {
    for (Point p : mPoints) {
      p.offset(dx, dy);
    }
  }
  
  public void offsetTo(int x, int y) {
    Rect bounds = getBounds();    
    int dx = x - bounds.left;
    int dy = y - bounds.top;
    offset(dx, dy);
  }
  
  /**
   * Checks if the given point is within the polygon.
   * Adapted from http://www.ecse.rpi.edu/Homepages/wrf/Research/Short_Notes/pnpoly.html
   * @param x
   * @param y
   * @return
   */
  public boolean contains(int x, int y) {
    boolean inside = false;
    for (int i = 0, j = (mPoints.size() - 1); i < mPoints.size(); j = i++) {
      Point pi = mPoints.get(i);
      Point pj = mPoints.get(j);
      if ( ((pi.y > y) != (pj.y > y)) && (x < (pj.x - pi.x) * (y - pi.y) / (pj.y - pi.y) + pi.x) )
        inside = !inside;
    }
    return inside;
  }

  /**
   * Returns the path representing this polygon transformed by the given matrix.
   * @param m
   * @return
   */
  public Path getPath(Matrix m) {

    Path path = new Path();

    // we need at least 2 points to draw a poly
    if (mPoints.size() > 1) {
      int size = mPoints.size();
      
      Point first = mPoints.get(0);
      
      // Move to the first point
      path.moveTo(first.x, first.y); 
      
      // Connect all other points
      for (Point p : mPoints.subList(1, size)) {
        path.lineTo( p.x, p.y);
      }
      
      // close the path
      path.close();
      
      // apply the matrix
      path.transform(m);
    }

    return path;
  }
}
