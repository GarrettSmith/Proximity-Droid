/**
 * 
 */
package ca.uwinnipeg.compare;

import android.graphics.Point;

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
    return (float) Math.sqrt(x*x + y*y);
  }
}
