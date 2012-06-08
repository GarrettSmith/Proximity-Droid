/**
 * 
 */
package ca.uwinnipeg.proximitydroid;

import java.io.IOException;
import java.nio.IntBuffer;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import ca.uwinnipeg.proximity.image.Image;

/**
 * @author Garrett Smith
 *
 */
public class Util {

  public static final String TAG = "Util";

  /**
   * Loads the bitmap from the given uri and rotates it based on its image data.
   * @param data
   * @param context
   * @param wm
   * @return
   */
  //TODO: Shift loading image off the main UI thread
  public static RotatedBitmap loadImage(Uri data, ContentResolver context, WindowManager wm) {
    String path = Util.getRealPathFromURI(context, data);

    // Load the orientation
    int orientation = Util.readOrientation(path);

    // Get screen size
    Point displaySize = Util.getDisplaySize(wm);
    int width = displaySize.x;
    int height = displaySize.y;

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
  public static int readOrientation(String filePath) {
    ExifInterface exif;
    int orientation = ExifInterface.ORIENTATION_NORMAL;
    try {
      exif = new ExifInterface(filePath);
      orientation = 
          exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
    } catch (IOException e) {
      Log.e(TAG, "Failed to load file for exif data. " + filePath);
    }    
    return mapEXIF(orientation);
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
        inSampleSize = Math.round((float)height / (float)reqHeight * 4);
      } else {
        inSampleSize = Math.round((float)width / (float)reqWidth * 4);
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
  
  // TODO: Move off of UI thread
  public static Image bitmapToImage(Bitmap bm) {
    int width = bm.getWidth();
    int height = bm.getHeight();
    IntBuffer intBuff = IntBuffer.allocate(width * height);
    bm.copyPixelsToBuffer(intBuff);
    return new Image(intBuff.array(), width, height);
  }

}
