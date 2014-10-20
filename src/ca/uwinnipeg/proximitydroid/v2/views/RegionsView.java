/**
 * 
 */
package ca.uwinnipeg.proximitydroid.v2.views;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import ca.uwinnipeg.proximitydroid.v2.R;
import ca.uwinnipeg.proximitydroid.v2.Region;

/**
 * A view that shows a list of {@link RegionView} over a bitmap. It can also show a highlight to
 * represent the results of calculations.
 * @author Garrett Smith
 *
 */
public class RegionsView extends ProximityImageView {
  
  public static final String TAG = "RegionView";
  
  // Paint to dim unselected area
  private static boolean SETUP = false;

  private static final Paint FOCUSED_PAINT = new Paint();
  protected final static Paint UNSELECTED_PAINT = new Paint();
  protected final static Paint POINT_PAINT = new Paint();
  
  // The regions of interest in this image
  protected List<Region> mRegions = new ArrayList<Region>();
  
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

  public RegionsView(Context context) {
    super(context);
    init();
  }

  public RegionsView(Context context, AttributeSet attr) {
    super(context, attr);
    init();
  }
  
  private void init() {    
    // one time static setup
    if (!SETUP) {
      SETUP = true;
      
      Resources rs = getResources();

      FOCUSED_PAINT.setStyle(Paint.Style.STROKE);
      FOCUSED_PAINT.setStrokeWidth(rs.getDimension(R.dimen.region_focused_stroke));
      FOCUSED_PAINT.setColor(rs.getColor(R.color.region_focused_color));
      FOCUSED_PAINT.setFlags(Paint.ANTI_ALIAS_FLAG);
      
      UNSELECTED_PAINT.setStyle(Paint.Style.FILL);
      UNSELECTED_PAINT.setFlags(Paint.ANTI_ALIAS_FLAG);
      UNSELECTED_PAINT.setColor(rs.getColor(R.color.region_unselected_color));
      
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
    if (bm != null) {
      super.setImageBitmap(bm, or);
      mHighlight = new int[bm.getWidth() * bm.getHeight()];      
    }
  }
  
  /**
   * Adds a region to the view.
   * @param reg
   */
  public void add(Region reg) {
    mRegions.add(reg);
    // TODO: invalidate(mRegions.get(reg).getPaddedScreenSpaceBounds());
  }
  
  /**
   * Removes a region from the view.
   * @param reg
   */
  public void remove(Region reg) {
    mRegions.remove(reg);
    // TODO: invalidate(mRegions.get(reg).getPaddedScreenSpaceBounds());
  }
  
  /**
   * Clears all regions from the view.
   */
  public void clear() {
    mRegions.clear();
    invalidate();
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
    if (points.length > 0 && points[0] != -1) {
      for (int i = 0; i < points.length; i += 2) {
        int x = points[i];
        int y = points[i+1];
        mHighlight[y * width + x] = mBitmap.getPixel(x, y);
      }
    }
    else {
      int color = 0;
      Random rnd = new Random(); 
      for (int i = 0; i < points.length; i += 2) {
        int x = points[i];
        int y = points[i+1];
        if (x == -1) {
          color = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
        }
        else {
          mHighlight[y * width + x] = color;       
        }
      }
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
    mDim = dim;
    if (changed) {
      invalidate();
    }
  }

  @Override
  public void draw(Canvas canvas) {
    super.draw(canvas);
    canvas.save();
    canvas.concat(mFinalMatrix);
    // dim the unselected area
    if (mDim) {
      Path unselected = new Path();
      for (Region reg : mRegions) {
        unselected.addPath(reg.getShapePath());
      }
      canvas.save();
      canvas.clipPath(unselected, android.graphics.Region.Op.DIFFERENCE);
      canvas.drawPaint(UNSELECTED_PAINT);
      canvas.restore();
    }
    
    // draw the highlight    
    if (mBitmap != null && mHighlight.length > 0) {
      int width = mBitmap.getWidth();
      int height = mBitmap.getHeight();
      canvas.drawBitmap(mHighlight, 0, width, 0, 0, width, height, true, POINT_PAINT);
    }
    
    // draw all the regions
    for (Region reg : mRegions) {
      canvas.drawPath(reg.getShapePath(), FOCUSED_PAINT);
    }
    canvas.restore();
    
    // draw centers
    if (mDrawCenter) {
      for (Region reg : mRegions) {
        canvas.drawPath(reg.getCenterPath(mFinalMatrix), reg.getCenterPaint());
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
      
      if (Math.abs(getScale() - MAX_SCALE) <= (MAX_SCALE / 2)) {
        center();
        zoomTo(MIN_SCALE, 200);
      }
      else {     
        zoomTo(MAX_SCALE/2, e.getX(), e.getY(), 200);
      }
      return true;
    }
    
    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float dx, float dy) {
      panBy(-dx, -dy);
      return true;
    }
  }
  
  protected class CustomScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
    
    @Override
    public boolean onScale(ScaleGestureDetector detector) {
      zoomBy(detector.getScaleFactor(), detector.getFocusX(), detector.getFocusY());
      return true;
    }
    
    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
      // TODO: center axis that are fully visible
    }
  }

}
