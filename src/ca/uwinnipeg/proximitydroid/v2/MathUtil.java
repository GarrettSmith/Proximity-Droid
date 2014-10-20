/**
 * 
 */
package ca.uwinnipeg.proximitydroid.v2;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.graphics.Point;
import android.util.FloatMath;

/**
 * A class providing various math functions used by the system.
 * @author Garrett Smith
 *
 */
public final class MathUtil {

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
  
  /**
   * Returns the union of two Integer lists.
   * @param A
   * @param B
   * @return
   */
  public static <T> List<T> union(List<T> A, List<T> B) {
    Set<T> set = new HashSet<T>();

    set.addAll(A);
    set.addAll(B);

    return new ArrayList<T>(set);
  }
}
