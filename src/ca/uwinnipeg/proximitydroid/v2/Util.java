/**
 * 
 */
package ca.uwinnipeg.proximitydroid.v2;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.Display;
import android.view.WindowManager;
import ca.uwinnipeg.proximity.image.Image;

/**
 * A collection of helper methods used by the system.
 * @author Garrett Smith
 *
 */
public final class Util {

  public static final String TAG = "Util";

  /**
   * Loads the bitmap from the given uri and rotates it based on its image data.
   * @param data
   * @param context
   * @param wm
   * @return
   */
  public static RotatedBitmap loadImage(Uri data, ContentResolver context) {
    String path = Util.getRealPathFromURI(context, data);

    // Load the orientation
    int orientation = Util.readOrientation(path);
    
    int width = 400;
    int height = 400;

    // Read the bitmap's size
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inJustDecodeBounds = true;
    BitmapFactory.decodeFile(path, options);

    // Calculate sample size
    options.inSampleSize = calculateInSampleSize(options, width, height, orientation);

    // load the bitmap
    options.inJustDecodeBounds = false; 
    Bitmap bm = BitmapFactory.decodeFile(path, options);
    return new RotatedBitmap(bm, orientation);
  }

  /**
   * Reads the exif orientation information of a file and returns a RotatedBitmap rotation value.
   * @param filePath
   * @return
   */
  //TODO: get rotated bitmaps handeled properly
  public static int readOrientation(String filePath) {
//    ExifInterface exif;
//    int orientation = ExifInterface.ORIENTATION_NORMAL;
//    try {
//      exif = new ExifInterface(filePath);
//      orientation = 
//          exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
//    } catch (IOException e) {
//      Log.e(TAG, "Failed to load file for exif data. " + filePath);
//    }    
//    return mapEXIF(orientation);
    return RotatedBitmap.NORMAL;
  }

  /**
   * Maps an exif orientation to {@link RotatedBitmap} orientation.
   * @param exifVal
   * @return
   */
  public static int mapEXIF(int exifVal) {
    switch (exifVal) {
      case ExifInterface.ORIENTATION_ROTATE_90:   return RotatedBitmap.CW;
      case ExifInterface.ORIENTATION_ROTATE_180:  return RotatedBitmap.UPSIDEDOWN;
      case ExifInterface.ORIENTATION_ROTATE_270:  return RotatedBitmap.CCW;
      default:                                    return RotatedBitmap.NORMAL;
    }
  }

  /**
   * Calculates the inSampleSize for loading a bitmap at the desired size.
   * From http://developer.android.com/training/displaying-bitmaps/load-bitmap.html
   * @param options
   * @param reqWidth
   * @param reqHeight
   * @param rotation same as RotatedBitmap
   * @return
   */
  // TODO: figure out optimal size
  public static int calculateInSampleSize(
      BitmapFactory.Options options, 
      int reqWidth, 
      int reqHeight,
      int rotation) {

    // Raw height and width of image rotated properly
    int width, height;
    if (rotation == RotatedBitmap.CW || rotation == RotatedBitmap.CCW) {
      // switch width and height for images that change orientation
      width = options.outHeight;
      height = options.outWidth;
    }
    else {
      height = options.outHeight;
      width = options.outWidth;
    }

    int inSampleSize = 1;

    if (height > reqHeight || width > reqWidth) {
      if (width > height) {
        inSampleSize = Math.round((float)height / (float)reqHeight);
      } else {
        inSampleSize = Math.round((float)width / (float)reqWidth);
      }
    }
    return inSampleSize;
  }

  /**
   * Gets the file path from a uri.
   * Modified from http://android-er.blogspot.ca/2011/04/convert-uri-to-real-path-format.html
   * @param contentUri
   * @return the file path represented by contentUri
   */
  public static String getRealPathFromURI(ContentResolver context, Uri contentUri) {
    String[] proj = { MediaStore.Images.Media.DATA };
    Cursor cursor = context.query(contentUri, proj, null, null, null);
    int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
    cursor.moveToFirst();
    String rtn = cursor.getString(column_index);
    cursor.close();
    return rtn;
  }

  /**
   * Returns the dimensions of the screen in a {@link Point}.
   * @return
   */
  @SuppressWarnings("deprecation")
  @TargetApi(13)
  public static Point getDisplaySize(WindowManager wm) {
    Display display = wm.getDefaultDisplay();
    Point p = new Point();
    if (android.os.Build.VERSION.SDK_INT >= 13) {
      display.getSize(p);
    }
    else {
      p.x = display.getWidth();
      p.y = display.getHeight();
    }
    return p;
  }
  
  /**
   * Sets the pixels of the given image from the given bitmap.
   * @param img
   * @param bm
   */
  public static void setImage(Image img, Bitmap bm) {
    int width = bm.getWidth();
    int height = bm.getHeight();
    
    int[] pixels = new int[width * height];
    bm.getPixels(pixels, 0, width, 0, 0, width, height);
    img.set(pixels, width, height);
  }  
  
  /**
   * Support method to get the default preferences.
   * <p>
   * This is needed because for whatever silly reason it wasn't defined in the support library.
   * @param context
   * @return
   */
  public static SharedPreferences getSupportDefaultSharedPrefences(Context context) {
    return context.getSharedPreferences(context.getPackageName() + "_preferences", 0);
  }
  
  /**
   * Makes a copy of the original array taking the first given number of elements from it.
   * <p>
   * This is just an implementation of Arrays.copyOf() because it wasn't introduced in our min api.
   * @param orig
   * @param newLength
   * @return
   */
  public static int[] copyOf(int[] orig, int newLength) {
    int[] newArray = new int[newLength];
    for (int i = 0; i < newArray.length; i++) {
      newArray[i] = orig[i];
    }
    return newArray;
  }

}
