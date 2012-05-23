package ca.uwinnipeg.compare;

import android.util.FloatMath;

public class MathUtil {

  public static float distance(float x1, float y1, float x2, float y2) {
    float x = x1 - x2;
    float y = y1 - y2;
    return FloatMath.sqrt(x * x + y * y);
  }
}
