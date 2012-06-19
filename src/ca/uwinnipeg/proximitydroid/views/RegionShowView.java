/**
 * 
 */
package ca.uwinnipeg.proximitydroid.views;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import ca.uwinnipeg.proximitydroid.R;
import ca.uwinnipeg.proximitydroid.Region;

/**
 * @author Garrett Smith
 *
 */
// TODO: split into base class and one that draws highlights
public class RegionShowView extends ProximityImageView {
  
  public static final String TAG = "RegionShowView";
  
  // Paint to dim unselected area
  private static boolean SETUP = false;
  protected final static Paint UNSELECTED_PAINT = new Paint();
  protected final static Paint POINT_PAINT = new Paint();
  
  // The regions of interest in this image
  protected Map<Region, RegionView> mRegions = new HashMap<Region, RegionView>();
  
  // The highlight to draw over the image, this shows the neighbourhoods and intersetion
  protected int[] mHighlight = new int[0];

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
      
      float invert[] =
        {
         -1.0f,  0.0f,  0.0f,  1.0f,  0.0f,
          0.0f, -1.0f,  0.0f,  1.0f,  0.0f,
          0.0f,  0.0f, -1.0f,  1.0f,  0.0f,
          1.0f,  1.0f,  1.0f,  1.0f,  0.0f 
        };

      POINT_PAINT.setStyle(Paint.Style.FILL);
      POINT_PAINT.setFlags(Paint.ANTI_ALIAS_FLAG);
      ColorMatrix cm = new ColorMatrix(invert);
      POINT_PAINT.setColorFilter(new ColorMatrixColorFilter(cm));
    }
  }
  
  @Override
  protected void setImageBitmap(Bitmap bm, int or) {
    super.setImageBitmap(bm, or);
    mHighlight = new int[bm.getWidth() * bm.getHeight()];
  }
  
  public void add(Region reg) {
    mRegions.put(reg, new RegionView(this, reg));
    invalidate(mRegions.get(reg).getPaddedScreenSpaceBounds());
  }
  
  public void remove(Region reg) {
    mRegions.remove(reg);
    invalidate(mRegions.get(reg).getPaddedScreenSpaceBounds());
  }
  
  public void clear() {
    mRegions.clear();
    invalidate();
  }
  
  @Override
  protected void updateFinalMatrix() {
    super.updateFinalMatrix();
    for (RegionView reg : mRegions.values()) {
      reg.setScreenMatrix(getFinalMatrix());
    }
  }
  
  public void clearHighlight() {
    // fill the highlight with transparent pixels
    Arrays.fill(mHighlight, 0x00000000);
    invalidate();
  }

  public void setHighlight(int[] points) {
    clearHighlight();
    addHighlight(points);
  }
  
  public void addHighlight(int[] points) {
    int width = mBitmap.getWidth();
    for (int i = 0; i < points.length; i += 2) {
      int x = points[i];
      int y = points[i+1];
      mHighlight[y * width + x] = mBitmap.getPixel(x, y);
    }
    invalidate();
  }

  @Override
  public void draw(Canvas canvas) {
    super.draw(canvas);
    // dim the unselected area
    Path unselected = new Path();
    for (RegionView reg : mRegions.values()) {
      unselected.addPath(reg.getShapePath());
    }
    canvas.save();
    canvas.clipPath(unselected, android.graphics.Region.Op.DIFFERENCE);
    canvas.drawPaint(UNSELECTED_PAINT);
    canvas.restore();
    
    // draw the highlight    
    if (mBitmap != null && mHighlight.length > 0) {
      canvas.save();
      canvas.concat(mFinalMatrix);
      int width = mBitmap.getWidth();
      int height = mBitmap.getHeight();
      canvas.drawBitmap(mHighlight, 0, width, 0, 0, width, height, true, POINT_PAINT);
      canvas.restore();
    }
    
    // draw all the regions
    for (RegionView reg : mRegions.values()) {
      reg.draw(canvas);
    }   
  }

}
