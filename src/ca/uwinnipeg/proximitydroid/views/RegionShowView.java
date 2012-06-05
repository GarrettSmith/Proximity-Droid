/**
 * 
 */
package ca.uwinnipeg.proximitydroid.views;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import ca.uwinnipeg.proximitydroid.Region;

/**
 * @author Garrett Smith
 *
 */
public class RegionShowView extends ProximityImageView {
  
  public static final String TAG = "RegionShowView";
  
  // The regions of interest in this image
  protected List<Region> mRegions = new ArrayList<Region>();

  public RegionShowView(Context context) {
    super(context);
  }

  public RegionShowView(Context context, AttributeSet attr) {
    super(context, attr);
  }
  
  public void add(Region reg) {
    mRegions.add(reg);
    invalidate(reg.getPaddedScreenSpaceBounds());
  }
  
  public void remove(Region reg) {
    mRegions.remove(reg);
    invalidate(reg.getPaddedScreenSpaceBounds());
  }
  
  @Override
  public void draw(Canvas canvas) {
    super.draw(canvas);
    for (Region reg : mRegions) {
      reg.draw(canvas);
    }
  }

}
