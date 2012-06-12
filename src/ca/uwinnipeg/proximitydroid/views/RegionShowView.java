/**
 * 
 */
package ca.uwinnipeg.proximitydroid.views;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import ca.uwinnipeg.proximitydroid.R;

/**
 * @author Garrett Smith
 *
 */
public class RegionShowView extends ProximityImageView {
  
  public static final String TAG = "RegionShowView";
  
  // Paint to dim unselected area
  private static boolean SETUP = false;
  protected final static Paint UNSELECTED_PAINT = new Paint();
  
  // The regions of interest in this image
  protected List<RegionView> mRegions = new ArrayList<RegionView>();
  
  // The list of relevant Points to draw
  protected float[] mPoints;

  public RegionShowView(Context context) {
    super(context);
    init();
  }

  public RegionShowView(Context context, AttributeSet attr) {
    super(context, attr);
    init();
  }
  
  private void init() {
    if (!SETUP) {
      SETUP = true;
      UNSELECTED_PAINT.setStyle(Paint.Style.FILL);
      UNSELECTED_PAINT.setFlags(Paint.ANTI_ALIAS_FLAG);
      UNSELECTED_PAINT.setColor(getResources().getColor(R.color.region_unselected_color));
    }
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
  protected void updateFinalMatrix() {
    super.updateFinalMatrix();
    for (RegionView reg : mRegions) {
      reg.setScreenMatrix(getFinalMatrix());
    }
  }
  
  public void clearPoints() {
    mPoints = null;
  }

  public void setRelevantPoints(int[] points) {
    if (points == null) {
      clearPoints();
    }
    else {
      mPoints = new float[points.length];
      for (int i = 0; i < mPoints.length; i++) {
        mPoints[i] = points[i];
      }
    }
  }

  @Override
  public void draw(Canvas canvas) {
    super.draw(canvas);
    // dim the unselected area
    Path unselected = new Path();
    for (RegionView reg : mRegions) {
      unselected.addPath(reg.getShapePath());
    }
    canvas.save();
    canvas.clipPath(unselected, android.graphics.Region.Op.DIFFERENCE);
    canvas.drawPaint(UNSELECTED_PAINT);
    canvas.restore();
    
    // draw all the regions
    for (RegionView reg : mRegions) {
      reg.draw(canvas);
    }    
    
    // draw all relevant Points
    if (mPoints != null) {
      float[] mappedPoints = new float[mPoints.length];
      mFinalMatrix.mapPoints(mappedPoints, mPoints);
      for (int i = 0; i < mappedPoints.length; i += 2) {        
        canvas.drawPoint(mappedPoints[i], mappedPoints[i+1], RegionView.REGION_PAINT);
      }
    }
  }

}
