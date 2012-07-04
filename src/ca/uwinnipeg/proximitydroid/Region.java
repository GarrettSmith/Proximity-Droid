package ca.uwinnipeg.proximitydroid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.graphics.Rect;
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

  // The bounds of the neighbourhood IN IMAGE SPACE
  protected Rect mBounds = new Rect();

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

  // current shape of the region
  protected Shape mShape = Shape.RECTANGLE;

  // The list of points that make up the polygon
  protected Polygon mPoly = new Polygon();
  
  // The image this region belongs to
  protected Image mImage;
  
  /**
   * Creates a new region within the given image.
   * @param image
   */
  public Region(Image image) {
    mImage = image;
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

  /**
   * Sets bounds of region.
   * @param r
   */
  public void setBounds(Rect r) {
    mPoly.setBounds(r);
    mBounds.set(r);
  }

  /**
   * Sets the shape of the region.
   * @param s
   */
  public void setShape(Shape s) {
    mShape = s;
  }

  /**
   * Returns the shape of the region.
   * @return
   */
  public Shape getShape() {
    return mShape;
  }
  
  /**
   * Sets the polygon of this region to be a copy of the given polygon.
   * @param poly
   */
  public void setPolygon(Polygon poly) {
    mPoly.set(poly);
  }
  
  /**
   * Returns the region's polygon.
   * @return
   */
  public Polygon getPolygon() {
    return mPoly;
  }
  
  /**
   * Gets all the indices of pixels within the {@link Image} contained by this region.
   * @param img
   * @return
   */  
  public int[] getIndices() {
    
    int[] indices;
    
    switch (mShape) {
      case POLYGON:

        // find all the points within the poly
        int[] tmp = new int[mBounds.width() * mBounds.height()];
        int i = -1;
        for (int y = mBounds.top; y < mBounds.bottom; y++) {
          for (int x = mBounds.left; x < mBounds.right; x++) {
            if (mPoly.contains(x, y)) {
              tmp[++i] = mImage.getIndex(x, y);
            }
          }
        }
        // trim out the empty spots
        indices = Arrays.copyOf(tmp, i);
        break;
        
      case OVAL:
        // find all the points within the oval
        int[] tmp2 = new int[mBounds.width() * mBounds.height()];
        int j = -1;

        int cx = mBounds.centerX();
        int cy = mBounds.centerY();
        int rx2 = mBounds.right - cx;
        rx2 *= rx2; // square
        int ry2 = mBounds.bottom - cy;
        ry2 *= ry2; // square
        
        for (int y = mBounds.top; y < mBounds.bottom; y++) {
          for (int x = mBounds.left; x < mBounds.right; x++) {
            
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
        indices = Arrays.copyOf(tmp2, j);
        break;
        
      default:
        indices = mImage.getIndices(mBounds.left, mBounds.top, mBounds.right, mBounds.bottom);
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
    return mImage.getIndex(mBounds.centerX(), mBounds.centerY());
  }
  
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

  private Region(Parcel in) {
    readFromParcel(in);
  }

}
