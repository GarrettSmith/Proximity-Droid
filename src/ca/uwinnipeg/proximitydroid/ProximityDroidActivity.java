/**
 * 
 */
package ca.uwinnipeg.proximitydroid;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.os.Bundle;
import android.provider.MediaStore.Images;
import ca.uwinnipeg.compare.R;

import com.actionbarsherlock.app.SherlockFragmentActivity;

/**
 * @author Garrett Smith
 *
 */
public class ProximityDroidActivity extends SherlockFragmentActivity {

  // The image we are working on
  protected RotatedBitmap mBitmap;

  private ContentResolver mContentResolver = getContentResolver();

  // Intents
  private static final int REQUEST_CODE_SELECT_IMAGE = 0;

  // bundle keys
  protected static final String BUNDLE_KEY_BITMAP = "Bitmap";
  protected static final String BUNDLE_KEY_ORIENTATION = "Orientation";
  
  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    setContentView(R.layout.main);

    // request an image
    if (mBitmap == null) {
      Intent i = new Intent(Intent.ACTION_PICK, Images.Media.INTERNAL_CONTENT_URI);
      startActivityForResult(i, REQUEST_CODE_SELECT_IMAGE);
    }

  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == REQUEST_CODE_SELECT_IMAGE)
      if (resultCode == Activity.RESULT_OK) {
        // Load the returned uri
        mBitmap = Util.loadImage(data.getData(), mContentResolver, getWindowManager());
      } 
      else {
        finish(); // If the intent failed or was cancelled exit
      }
  }

}
