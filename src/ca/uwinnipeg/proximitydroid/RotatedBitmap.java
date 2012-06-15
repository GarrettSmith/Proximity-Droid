package ca.uwinnipeg.proximitydroid;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Parcel;
import android.os.Parcelable;

public class RotatedBitmap implements Parcelable {

  public static final String TAG = "RotatedBitmap";

  private Bitmap mBitmap;
  private int mOrientation;

  public static final int NORMAL = 0;
  public static final int CW = 1;
  public static final int UPSIDEDOWN = 2;
  public static final int CCW = 3;

  public RotatedBitmap(Bitmap bm) {
    this(bm, NORMAL);
  }

  public RotatedBitmap(Bitmap bm, int or) {
    mBitmap = bm;
    mOrientation = or;
  }

  public void setOrientation(int or) {
    mOrientation = or;
  }

  public int getOrientation() {
    return mOrientation;
  }

  public void setBitmap(Bitmap bm) {
    mBitmap = bm;
  }

  public Bitmap getBitmap() {
    return mBitmap;
  }

  /**
   * @return the degrees the bitmap is rotated
   */
  public int getRotation() {
    return mOrientation * 90;
  }

  /**
   * 
   * @return true if the bitmap has changed int (is rotated CW or CCW)
   */
  public boolean isOrientationChanged() {
    return (mOrientation % 2 != 0);
  }

  public int getWidth() {
    if (isOrientationChanged()) return mBitmap.getHeight();
    else return mBitmap.getWidth();
  }


  public int getHeight() {
    if (isOrientationChanged()) return mBitmap.getWidth();
    else return mBitmap.getHeight();
  }
  
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
