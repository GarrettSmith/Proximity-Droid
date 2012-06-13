/**
 * 
 */
package ca.uwinnipeg.proximitydroid;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import ca.uwinnipeg.proximity.image.Image;

/**
 * @author Garrett Smith
 *
 */
public class ProximityService extends IntentService {
  
  public static String TAG = "ProximityService";
  
  // The perceptual system
  protected Image mImage = new Image();
  
  // The bitmap image
  protected RotatedBitmap mBitmap;
  
  // The actions handled
  public static final String ACTION_ADD_REGION = "ProximityService.action.ADD_REGION";
  public static final String ACTION_REMOVE_REGION = "ProximityService.action.REMOVE_REGION";  

  public static final String ACTION_ADD_FEATURE = "ProximityService.action.ADD_FEATURE";
  public static final String ACTION_REMOVE_FEATURE = "ProximityService.action.REMOVE_FEATURE";
  
  public static final String ACTION_SET_BITMAP = "ProximityService.action.SET_BITMAP";
  public static final String ACTION_GET_BITMAP = "ProximityService.action.GET_BITMAP";

  public ProximityService() {
    super(TAG);
  }

  @Override
  protected void onHandleIntent(Intent intent) {
    // determine which action we want to take
    String action = intent.getAction();
    
    // The intent sent out as a response
    Intent response = new Intent();
    
    if (action.equals(ACTION_SET_BITMAP)) {
      setBitmap(intent, response);
    }
    else if (action.equals(ACTION_GET_BITMAP)) {
      getBitmap(intent, response);
    }
    else if (action.equals(ACTION_ADD_FEATURE)) {
      addFeature(intent, response);
    }
    else if (action.equals(ACTION_REMOVE_FEATURE)) {
      removeFeature(intent, response);
    }
    else if (action.equals(ACTION_ADD_REGION)) {
      addRegion(intent, response);
    }
    else if (action.equals(ACTION_REMOVE_REGION)) {
      removeRegion(intent, response);
    }
    else {
      throw new RuntimeException(action + " is not a supported action!");
    }
    
    // send the response
    sendBroadcast(response);
  }
  
  private void setBitmap(Intent intent, Intent response) {
    mBitmap = Util.loadImage(intent.getData(), getContentResolver());
    Util.setImage(mImage, mBitmap.getBitmap());
    // so we return the newly set bitmap
    getBitmap(intent, response);
  }
  
  private void getBitmap(Intent intent, Intent response) {
    out.putExtra
  }

  private void addFeature(Intent intent, Intent response) {
    // TODO Auto-generated method stub
    
  }

  private void removeFeature(Intent intent, Intent response) {
    // TODO Auto-generated method stub
    
  }

  private void addRegion(Intent intent, Intent response) {
    // TODO Auto-generated method stub
    
  }

  private void removeRegion(Intent intent, Intent response) {
    // TODO Auto-generated method stub
    
  }

}
