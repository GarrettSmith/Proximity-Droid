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
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import ca.uwinnipeg.proximitydroid.R;
import ca.uwinnipeg.proximitydroid.Region;

/**
 * A view that shows a list of {@link RegionView} over a bitmap. It can also show a highlight to
 * represent the results of calculations.
 * @author Garrett Smith
 *
 */
public class RegionShowView extends ProximityImageView {
  
  public static final String TAG = "RegionView";
  
  // Paint to dim unselected area
  private static boolean SETUP = false;
  protected final static Paint UNSELECTED_PAINT = new Paint();
  protected final static Paint POINT_PAINT = new Paint();
  
  // The regions of interest in this image
  protected Map<Region, RegionView> mRegions = new HashMap<Region, RegionView>();
  
  // The highlight to draw over the image, this shows the neighbourhoods and intersetion
  protected int[] mHighlight = new int[0];
  
  // if the center point of regions will be drawn
  protected boolean mDrawCenter = false;
  
  // if we should dim the areas that are outside of the regions
  protected boolean mDim = true;
  
  private GestureDetector mSimpleDetector = 
      new GestureDetector(getContext(), new CustomSimpleOnGestureListener());;
      
  private ScaleGestureDetector mScaleDetector = 
      new ScaleGestureDetector(getContext(), new CustomScaleListener());

  public RegionShowView(Context context) {
    super(context);
    init();
  }

  public RegionShowView(Context context, AttributeSet attr) {
    super(context, attr);
    init();
  }
  
  private void init() {    
    // one time static setup
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
  
  /**
   * Adds a region to the view.
   * @param reg
   */
  public void add(Region reg) {
    mRegions.put(reg, new RegionView(this, reg));
    invalidate(mRegions.get(reg).getPaddedScreenSpaceBounds());
  }
  
  /**
   * Removes a region from the view.
   * @param reg
   */
  public void remove(Region reg) {
    mRegions.remove(reg);
    invalidate(mRegions.get(reg).getPaddedScreenSpaceBounds());
  }
  
  /**
   * Clears all regions from the view.
   */
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
  
  /**
   * Clears the highlighted pixels of this view.
   */
  public void clearHighlight() {
    // fill the highlight with transparent pixels
    Arrays.fill(mHighlight, 0x00000000);
    invalidate();
  }

  /**
   * Sets the highlighted pixels to the given array of points.
   * @param points
   */
  public void setHighlight(int[] points) {
    clearHighlight();
    addHighlight(points);
  }
  
  /**
   * Adds points to the highlight.
   * @param points
   */
  public void addHighlight(int[] points) {
    int width = mBitmap.getWidth();
    for (int i = 0; i < points.length; i += 2) {
      int x = points[i];
      int y = points[i+1];
      mHighlight[y * width + x] = mBitmap.getPixel(x, y);
    }
    invalidate();
  }
  
  /**
   * Sets whether the center of regions should be drawn.
   * @param drawCenter
   */
  public void setDrawCenterPoint(boolean drawCenter) {
    boolean changed = mDrawCenter != drawCenter;
    mDrawCenter = true;
    if (changed) {
      invalidate();
    }
  }
  
  /**
   * Sets whether areas outside of regions should be dimmed.
   * @param dim
   */
  public void setDim(boolean dim) {
    boolean changed = mDim != dim;
    mDim = true;
    if (changed) {
      invalidate();
    }
  }

  @Override
  public void draw(Canvas canvas) {
    super.draw(canvas);
    // dim the unselected area
    if (mDim) {
      Path unselected = new Path();
      for (RegionView reg : mRegions.values()) {
        unselected.addPath(reg.getShapePath());
      }
      canvas.save();
      canvas.clipPath(unselected, android.graphics.Region.Op.DIFFERENCE);
      canvas.drawPaint(UNSELECTED_PAINT);
      canvas.restore();
    }
    
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
      if (mDrawCenter) {
        reg.drawWithCenter(canvas);
      }
      else {
        reg.draw(canvas);
      }
    }   
  }
  
  // input  
  
  @Override
  public boolean onTouchEvent(MotionEvent event) {
    mSimpleDetector.onTouchEvent(event);
    mScaleDetector.onTouchEvent(event);
    return true;
  }
  
  protected class CustomSimpleOnGestureListener extends SimpleOnGestureListener {
    
    @Override
    public boolean onDown(MotionEvent e) {
      return true;
    }
    
    @Override
    public boolean onDoubleTap(MotionEvent e) {
      
      float[] p = convertToImageSpace(e.getX(), e.getY());
      float x = p[0];
      float y = p[1];
      
      if (Math.abs(getScale() - MAX_SCALE) <= (MAX_SCALE / 2)) {
        zoomTo(MIN_SCALE, 200);
      }
      else {
        zoomTo(MAX_SCALE/2, 200);
      }
      return true;
    }
  }
  
  protected class CustomScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
    @Override
    public boolean onScale(ScaleGestureDetector detector) {
      zoomBy(detector.getScaleFactor());
      return true;
    }
  }

}
