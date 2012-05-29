package ca.uwinnipeg.compare;

import java.util.ArrayList;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;

// TODO: DOCUMENT POLYGON!
/**
 * @author garrett
 *
 */
public class Polygon {

  private final ArrayList<Point> mPoints = new ArrayList<Point>();

  /**
   * 
   */
  public Polygon() {}

  /**
   * @param orig
   */
  public Polygon(Polygon orig) {
    set(orig);
  }

  /**
   * @return
   */
  public int size() {
    return mPoints.size();
  }

  /**
   * @param index
   * @return
   */
  public Point getPoint(int index) {
    return mPoints.get(index);
  }

  /**
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
  /**
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

  /**
   * @param orig
   */
  public void set(Polygon orig) {
    mPoints.clear();
    for (Point p : orig.mPoints) {
      addPoint(p);
    }
  }

  public Point addPoint(Point p) {
    Point point = new Point(p);
    mPoints.add(point);
    return point;
  }

  /**
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
   * @param index
   * @return
   */
  public Point removePoint(int index) {
    return mPoints.remove(index);
  }

  /**
   * @param p
   * @return
   */
  public boolean removePoint(Point p) {
    return mPoints.remove(p);
  }

  /**
   * @param canvas
   * @param paint
   */
  public void draw(Canvas canvas, Paint paint) {
    Path path = new Path();
    float[] ps = toFloatArray();
    int size = ps.length;
    // We can only draw a shape if we have more than 1 point
    if (size > 1) {
      // Move to last point
      path.moveTo(ps[size-2], ps[size-1]); 
      // Connect all points
      for (int i = 0; i < size; i += 2) {
        path.lineTo( ps[i], ps[i+1]);
      }
      canvas.drawPath(path, paint);
    }
  }
}
