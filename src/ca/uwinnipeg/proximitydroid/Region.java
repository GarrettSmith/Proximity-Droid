package ca.uwinnipeg.proximitydroid;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import ca.uwinnipeg.proximity.image.Image;

/**
 * @author Garrett Smith
 *
 */
public class Region implements Parcelable {

  public static final String TAG = "Region";

  // The bounds of the neighbourhood IN IMAGE SPACE
  protected Rect mBounds = new Rect();

  public enum Shape { RECTANGLE, OVAL, POLYGON }

  // The edge or pair of edges that are currently selected
  public enum Edge { NONE, TL, T, TR, R, BR, B, BL, L, ALL }

  protected Shape mShape = Shape.RECTANGLE;

  // The list of points that make up the polygon
  protected Polygon mPoly = new Polygon();
  
  public Region() {}
  
  public Region(Region source) {
    if (source != null) {
      mShape = source.mShape;
      mBounds = source.mBounds;
      mPoly = source.mPoly;
    }
  }

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
   * Gets all the indices contained by this region.
   * @param img
   * @return
   */  
  public int[] getIndices(Image img) {
    return img.getIndices(mBounds.left, mBounds.top, mBounds.right, mBounds.bottom);
  }
  
  public List<Integer> getIndicesList(Image img) {
    int[] indices = getIndices(img);
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
  public int getCenterIndex(Image img) {
    return img.getIndex(mBounds.centerX(), mBounds.centerY());
  }

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
