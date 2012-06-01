package ca.uwinnipeg.compare;

import java.util.ArrayList;

import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;

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
  public Polygon(Polygon orig) {
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
  public void reset() {
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
    return addPoint(new Point(x, y));
  }
  
  /**
   * Adds a point at the given index.
   * @param index
   * @param p
   * @return
   */
  public Point addPoint(int index, Point p) {
    Point point = new Point(p);
    mPoints.add(index, point);
    return p;
  }
  
  /**
   * Adds a point at the given index.
   * @param index
   * @param x
   * @param y
   * @return
   */
  public Point addPoint(int index, int x, int y) {
    return addPoint(index, new Point(x, y));
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
    if (mPoints.size() > 0) {
      Point p1 = mPoints.get(0);

      // create an initial bounds from the first two points
      bounds.set(p1.x, p1.y, p1.x, p1.y);

      // Get the union of each point to get the final bounds
      for (int i = 1; i < mPoints.size(); i++) {
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
  public void setBounds(Rect newBounds) {

    int newWidth = newBounds.width();
    int newHeight = newBounds.height();

    // Scale to fit in the new bounds if there is more than one point 
    // and the width and height of the new bounds are non zero.
    if (mPoints.size() > 1 && newWidth != 0 && newHeight != 0) {

      Rect oldBounds = getBounds();
      int oldWidth = oldBounds.width();
      int oldHeight = oldBounds.height();

      float widthRatio = newWidth / (float)oldWidth;
      float heightRatio = newHeight / (float)oldHeight;

      for (Point p : mPoints) {
        p.x *= widthRatio;
        p.y *= heightRatio;
      }

    }
    // Offset to the new bounds
    offsetTo(newBounds.left, newBounds.top);
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
   * From http://code.google.com/p/straightedge/
   * 
   * @param x1
   * @param y1
   * @param x2
   * @param y2
   * @return
   */
  // TODO: Figure out licence
  public boolean intersectsLine(double x1, double y1, double x2, double y2){

    // Sometimes this method fails if the 'lines'
    // start and end on the same point, so here we check for that.
    if (x1 == x2 && y1 == y2){
      return false;
    }
    double ax = x2-x1;
    double ay = y2-y1;
    Point pointIBefore = mPoints.get(mPoints.size()-1);
    for (int i = 0; i < mPoints.size(); i++){
      Point pointI = mPoints.get(i);
      double x3 = pointIBefore.x;
      double y3 = pointIBefore.y;
      double x4 = pointI.x;
      double y4 = pointI.y;

      double bx = x3-x4;
      double by = y3-y4;
      double cx = x1-x3;
      double cy = y1-y3;

      double alphaNumerator = by*cx - bx*cy;
      double commonDenominator = ay*bx - ax*by;
      if (commonDenominator > 0){
        if (alphaNumerator < 0 || alphaNumerator > commonDenominator){
          pointIBefore = pointI;
          continue;
        }
      }else if (commonDenominator < 0){
        if (alphaNumerator > 0 || alphaNumerator < commonDenominator){
          pointIBefore = pointI;
          continue;
        }
      }
      double betaNumerator = ax*cy - ay*cx;
      if (commonDenominator > 0){
        if (betaNumerator < 0 || betaNumerator > commonDenominator){
          pointIBefore = pointI;
          continue;
        }
      }else if (commonDenominator < 0){
        if (betaNumerator > 0 || betaNumerator < commonDenominator){
          pointIBefore = pointI;
          continue;
        }
      }
      if (commonDenominator == 0){
        // This code wasn't in Franklin Antonio's method. It was added by Keith Woodward.
        // The lines are parallel.
        // Check if they're collinear.
        double collinearityTestForP3 = x1*(y2-y3) + x2*(y3-y1) + x3*(y1-y2);  // see http://mathworld.wolfram.com/Collinear.html
        // If p3 is collinear with p1 and p2 then p4 will also be collinear, since p1-p2 is parallel with p3-p4
        if (collinearityTestForP3 == 0){
          // The lines are collinear. Now check if they overlap.
          if (x1 >= x3 && x1 <= x4 || x1 <= x3 && x1 >= x4 ||
              x2 >= x3 && x2 <= x4 || x2 <= x3 && x2 >= x4 ||
              x3 >= x1 && x3 <= x2 || x3 <= x1 && x3 >= x2){
            if (y1 >= y3 && y1 <= y4 || y1 <= y3 && y1 >= y4 ||
                y2 >= y3 && y2 <= y4 || y2 <= y3 && y2 >= y4 ||
                y3 >= y1 && y3 <= y2 || y3 <= y1 && y3 >= y2){
              return true;
            }
          }
        }
        pointIBefore = pointI;
        continue;
      }
      return true;
    }
    return false;
  }

  /**
   * Returns the path representing this polygon transformed by the given matrix.
   * @param m
   * @return
   */
  public Path getPath() {

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
    }

    return path;
  }
}
