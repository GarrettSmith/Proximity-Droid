/**
 * 
 */
package ca.uwinnipeg.proximitydroid;

import android.graphics.Point;
import android.util.FloatMath;

/**
 * @author Garrett Smith
 *
 */
public class MathUtil {

  /**
   * Finds the distance between two points.
   * @param p1
   * @param p2
   * @return
   */
  public static float distance(Point p1, Point p2) {
    return distance(p1.x, p1.y, p2.x, p2.y);
  }

  /**
   * Finds the distance between two points.
   * @param p1
   * @param p2
   * @return
   */
  public static float distance(int x1, int y1, int x2, int y2) {
    int x = x1 - x2;
    int y = y1 - y2;
    return FloatMath.sqrt(x*x + y*y);
  }
  
  /**
   * Finds the intersect of two lines.
   * From http://thirdpartyninjas.com/blog/2008/10/07/line-segment-intersection/
   * @param a
   * @param b
   * @param c
   * @param d
   * @return
   */
  public static Point intersect(Point a, Point b, Point c, Point d) {
    double denom = (d.y - c.y) * (b.x - a.x) - (d.x - c.x) * (b.y - a.y);
    
    //if (denom == 0) return null; // lines are parallel
    
    double ua = ((d.x - c.x) * (a.y - c.y) - (d.y - c.y) * (a.x - c.x)) / denom;
    double ub = ((b.x - a.x) * (a.y - c.y) - (b.y - a.y) * (a.x - c.x)) / denom;
    
    if ( 0 > ua || ua > 1 || 0 > ub || ub > 1) return null; // lines intersect but segments do not
    
    int x = (int) Math.round(a.x + ua * (b.x - a.x));
    int y = (int) Math.round(a.y + ua * (b.y - a.y));
    
    return new Point(x, y); // returns the intersect
  }
  
  /**
   * Finds the distance between point c and a line segment from a to b.
   * @param a
   * @param b
   * @param c
   * @return
   */
  public static float pointLineDistance(Point a, Point b, Point c) {
    int px = b.x - a.x;
    int py = b.y - a.y;
    
    float denom = px*px + py*py;
    
    float u = ((c.x - a.x) * px + (c.y - a.y) * py) / denom;
    
    // limit between 0 and 1
    u = Math.min(u, 1);
    u = Math.max(u, 0);
    
    float x = Math.round(a.x + u * px);
    float y = Math.round(a.y + u * py);
    
    float dx = x - c.x;
    float dy = y - c.y;
    
    float dist = FloatMath.sqrt(dx*dx + dy*dy);

    return dist;
  }
  
}
