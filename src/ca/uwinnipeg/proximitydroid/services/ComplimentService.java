/**
 * 
 */
package ca.uwinnipeg.proximitydroid.services;

import java.util.ArrayList;
import java.util.List;

import ca.uwinnipeg.proximitydroid.Region;

/**
 * @author Garrett Smith
 *
 */
public class ComplimentService extends PropertyService {
  
  public static final String TAG = "PropertyService";
  
  public static final String CATEGORY = "Compliment";

  // the indices of the pixels in the compliment
  protected List<Integer> mCompliment = new ArrayList<Integer>();
  
  public ComplimentService() {
    super(CATEGORY);
  }
  
  public int[] getCompliment() {
    return indicesToPoints(mCompliment);
  }

  protected void setCompliment(List<Integer> indices) {
    // save the new compliment
    mCompliment.clear();
    mCompliment.addAll(indices);
    
    //broadcast
    broadcastValueChanged(mCompliment);
  }

  protected void invalidate() {
    // TODO: recalculate compliment
  }

  // Tasking 
  
  private class ComplimentTask extends PropertyTask {
  
    @Override
    protected List<Integer> doInBackground(Region... params) {
      // TODO Auto-generated method stub
      return null;
    }
    
    @Override
    protected void onPostExecute(List<Integer> result) {
      // TODO Auto-generated method stub
      super.onPostExecute(result);
    }
    
  }

}
