package ca.uwinnipeg.proximitydroid.v2;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * A wrapper of bitmap that also contains rotation information and creates transformation matrices 
 * to display the bitmap as intended.
 * @author Garrett Smith
 *
 */
public class RotatedBitmap implements Parcelable {

  public static final String TAG = "RotatedBitmap";

  // the wrapped bitmap
  private Bitmap mBitmap;
  
  // the rotation of the bitmap
  private int mOrientation;

  public static final int NORMAL = 0;
  public static final int CW = 1;
  public static final int UPSIDEDOWN = 2;
  public static final int CCW = 3;

  /**
   * Creates an unrotated rotated bitmap from the given bitmap.
   * @param bm
   */
  public RotatedBitmap(Bitmap bm) {
    this(bm, NORMAL);
  }

  /**
   * Creates a rotated bitmap from the given bitmap with the given orienation.
   * @param bm
   * @param or the orientation of the bitmap
   */
  public RotatedBitmap(Bitmap bm, int or) {
    mBitmap = bm;
    mOrientation = or;
  }

  /**
   * Sets the orientation of the bitmap.
   * @param or
   */
  public void setOrientation(int or) {
    mOrientation = or;
  }

  /**
   * Gets an int representing the orientation of the bitmap.
   * @return
   */
  public int getOrientation() {
    return mOrientation;
  }

  /**
   * Sets the bitmap to wrap.
   * @param bm
   */
  public void setBitmap(Bitmap bm) {
    mBitmap = bm;
  }

  /**
   * Returns the wrapped bitmap.
   * @return
   */
  public Bitmap getBitmap() {
    return mBitmap;
  }

  /**
   * Returns the degrees the bitmap is rotated.
   * @return
   */
  public int getRotation() {
    return mOrientation * 90;
  }

  /**
   * 
   * @return true if the bitmap has changed orientation for the original bitmap 
   * (is rotated CW or CCW)
   */
  public boolean isOrientationChanged() {
    return (mOrientation % 2 != 0);
  }

  /**
   * Return the width of the bitmap after it has been rotated.
   * @return
   */
  public int getWidth() {
    if (isOrientationChanged()) return mBitmap.getHeight();
    else return mBitmap.getWidth();
  }

  /**
   * Returns the height of the bitmap after it has been rotated.
   * @return
   */
  public int getHeight() {
    if (isOrientationChanged()) return mBitmap.getWidth();
    else return mBitmap.getHeight();
  }
  
  /**
   * Returns the integer representation of the pixel at the given coordinated of the image. 
   * @param x
   * @param y
   * @return
   */
  public int getPixel(int x, int y) {
    int width = mBitmap.getWidth();
    int height = mBitmap.getHeight();
    
    // the axis accordingly
    switch (mOrientation) {
      case CW:
        x = width - x;
        break;
      case UPSIDEDOWN:
        x = width -x;
        y = height - y;
        break;
      case CCW:
        y = height - y;
        break;
    }
    
    return mBitmap.getPixel(x, y);
  }

  /**
   * Returns the transformation used to display this bitmap as intended.
   * @return
   */
  public Matrix getMatrix() {
    Matrix m = new Matrix(); // Identity

    if (mOrientation != NORMAL) {
      int centerX = mBitmap.getWidth() / 2;
      int centerY = mBitmap.getHeight() / 2;

      m.preTranslate(-centerX, -centerY); // Move to center of unrotated
      m.postRotate(getRotation()); // rotate
      m.postTranslate(getWidth()/2, getHeight()/2); // to top left of rotated
    }

    return m;
  }
  
  // Parcelable

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeParcelable(mBitmap, flags);
    dest.writeInt(mOrientation);
  }

  private void readFromParcel(Parcel in) {
    mBitmap = (Bitmap) in.readParcelable(Bitmap.class.getClassLoader());
    mOrientation = in.readInt();
  }

  /**
   * The rotated bitmap parcelable creator.
   */
  public static final Parcelable.Creator<RotatedBitmap> CREATOR =
      new Parcelable.Creator<RotatedBitmap>() {

    @Override
    public RotatedBitmap createFromParcel(Parcel source) {
      return new RotatedBitmap(source);
    }

    @Override
    public RotatedBitmap[] newArray(int size) {
      return new RotatedBitmap[size];
    }
  };    

  private RotatedBitmap(Parcel in) {
    readFromParcel(in);
  }
}
