package ca.uwinnipeg.compare;

import android.graphics.Bitmap;
import android.graphics.Matrix;

public class RotatedBitmap {
  
  public static final String TAG = "RotatedBitmap";
  
  private Bitmap mBitmap;
  private Orientation mOrientation;
  
  enum Orientation { Normal, CW, UpsideDown, CCW }
  
  public RotatedBitmap(Bitmap bm) {
    this(bm, Orientation.Normal);
  }
  
  public RotatedBitmap(Bitmap bm, Orientation or) {
    mBitmap = bm;
    mOrientation = or;
  }
  
  public void setOrientation(Orientation or) {
    mOrientation = or;
  }
  
  public Orientation getOrientation() {
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
    switch(mOrientation) {
    case Normal:      return 0;
    case CW:          return 90;
    case UpsideDown:  return 180;
    default:          return 270; //CCW
    }
  }
  
  /**
   * 
   * @return true if the bitmap has changed orientation (is rotated CW or CCW)
   */
  public boolean isOrientationChanged() {
    return (mOrientation == Orientation.CW || mOrientation == Orientation.CCW);
  }
  
  public int getWidth() {
    if (isOrientationChanged()) return mBitmap.getHeight();
    else return mBitmap.getWidth();
  }
  

  public int getHeight() {
    if (isOrientationChanged()) return mBitmap.getWidth();
    else return mBitmap.getHeight();
  }
  
  public Matrix getMatrix() {
    Matrix m = new Matrix(); // Identity
    
    if (mOrientation != Orientation.Normal) {
      int centerX = mBitmap.getWidth() / 2;
      int centerY = mBitmap.getHeight() / 2;
      
      m.preTranslate(-centerX, -centerY); // Move to center of unrotated
      m.postRotate(getRotation()); // rotate
      m.postTranslate(getWidth()/2, getHeight()/2); // to top left of rotated
    }
    
    return m;
  }
}
