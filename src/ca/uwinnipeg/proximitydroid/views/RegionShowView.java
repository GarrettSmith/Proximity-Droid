/**
 * 
 */
package ca.uwinnipeg.proximitydroid.views;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import ca.uwinnipeg.proximitydroid.RegionView;

/**
 * @author Garrett Smith
 *
 */
public class RegionShowView extends ProximityImageView {
  
  public static final String TAG = "RegionShowView";
  
  // The regions of interest in this image
  protected List<RegionView> mRegions = new ArrayList<RegionView>();

  public RegionShowView(Context context) {
    super(context);
  }

  public RegionShowView(Context context, AttributeSet attr) {
    super(context, attr);
  }
  
  public void add(RegionView reg) {
    mRegions.add(reg);
    invalidate(reg.getPaddedScreenSpaceBounds());
  }
  
  public void remove(RegionView reg) {
    mRegions.remove(reg);
    invalidate(reg.getPaddedScreenSpaceBounds());
  }
  
  @Override
  public void draw(Canvas canvas) {
    super.draw(canvas);
    for (RegionView reg : mRegions) {
      reg.draw(canvas);
    }
  }

}
