package ca.uwinnipeg.proximitydroid;

import java.util.List;

import android.graphics.Rect;
import ca.uwinnipeg.proximity.image.Image;
import ca.uwinnipeg.proximity.image.Pixel;

/**
 * @author Garrett Smith
 *
 */
public class Region {

  public static final String TAG = "Region";

  // The bounds of the neighbourhood IN IMAGE SPACE
  protected Rect mBounds = new Rect();

  public enum Shape { RECTANGLE, OVAL, POLYGON }

  // The edge or pair of edges that are currently selected
  public enum Edge { NONE, TL, T, TR, R, BR, B, BL, L, ALL }

  protected Shape mShape = Shape.RECTANGLE;

  // The list of points that make up the polygon
  protected Polygon mPoly = new Polygon();

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
    mPoly.setBounds(r);
    mBounds.set(r);
  }

  public void setShape(Shape s) {
    mShape = s;
  }

  public Shape getShape() {
    return mShape;
  }
  
  public void setPolygon(Polygon poly) {
    mPoly.set(poly);
  }
  
  public Polygon getPolygon() {
    return mPoly;
  }
  
  /**
   * Gets all the pixels contained by this region.
   * @param img
   * @return
   */
  public List<Pixel> getPixels(Image img) {
    // TODO: trim non rectangle shapes
    switch(mShape) {
      case OVAL:
        break;
      case POLYGON:
        break;
    }
    return img.getPixels(mBounds.left, mBounds.top, mBounds.right, mBounds.bottom);
  }
  
  public int[] getIndices(Image img) {
    return img.getIndices(mBounds.left, mBounds.top, mBounds.right, mBounds.bottom);
  }
  
  /**
   * Returns the center pixel of this region.
   * @param img
   * @return
   */
  public Pixel getCenterPixel(Image img) {
    return img.getPixel(mBounds.centerX(), mBounds.centerY());
  }

}
