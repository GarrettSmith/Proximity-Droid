package ca.uwinnipeg.proximitydroid.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import ca.uwinnipeg.proximitydroid.Region;
import ca.uwinnipeg.proximitydroid.SelectRegion;

/**
 * 
 * The view which the image and currently selected neighbourhood is drawn.
 *
 */
public class RegionSelectView extends ProximityImageView {  

  public static final String TAG = "SelectView";
  
  protected SelectRegion mRegion = new SelectRegion(this);

  // Neighbourhood following  
  private static final float FOLLOW_DURATION = 300f;
  private static final float SCALE_PADDING = 0.6f;
  private static final float SCALE_THRESHOLD = 0.1f;

  public RegionSelectView(Context context){
    super(context);
    init();
  }

  public RegionSelectView(Context context, AttributeSet attr) {
    super(context, attr);
    init();
  }
  
  private void init() {
    mRegion.setFocused(true);
  }

  public Region getNeighbourhood() {
    return mRegion;
  }
  
  @Override
  public void updateFinalMatrix() {
    super.updateFinalMatrix();
    // Let neighbourhood know the final matrix has changed
    mRegion.setScreenMatrix(getFinalMatrix());
  }

  @Override 
  public boolean onTouchEvent(MotionEvent event) {
    switch (event.getAction()) {
      case MotionEvent.ACTION_DOWN: 
        mRegion.handleDown(event);
        return true;
      case MotionEvent.ACTION_UP: 
        mRegion.handleUp(event);
        return true;
      case MotionEvent.ACTION_MOVE: 
        mRegion.handleMove(event);
        return true;
      default:                      
        return false;
    }
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    mRegion.draw(canvas);
  }

  // Neighbourhood following

  /**
   * Pan to center the given neighbourhood in the view.
   * @param nv
   */
  public void followMove(Region nv) {
    // Determine the zoom required
    RectF bounds = nv.getScreenSpaceBounds();

    float dx = 0;
    float dy = 0;

    float vw = getWidth();
    float vh = getHeight();

    // re-center if the neighbourhood leaves or gets too close to the edge of screen
    RectF screen = new RectF(getLeft(), getTop(), getRight(), getBottom());

    if (!screen.contains(bounds)) {
      dx = ((vw / 2f) - bounds.centerX());
      dy = ((vh / 2f) - bounds.centerY());
    }
    
    // Get the bounds of the image
    RectF imageBounds = getImageScreenBounds();
    float iw = imageBounds.width();
    float ih = imageBounds.height();
    
    // Show the entire axis if it can fit on the screen
    if (iw <= vw) {
      dx = (vw / 2f) - imageBounds.centerX();
    }

    if (ih <= vh) {
      dy = (vh / 2f) - imageBounds.centerY();
    }

    panBy(dx, dy, FOLLOW_DURATION);
  }

  /**
   * Zoom to fit and pan to center the given neighbourhood in the view.
   * @param nv
   */
  public void followResize(Region nv) {
    RectF bounds = nv.getScreenSpaceBounds();
    
    float w = bounds.width();
    float h = bounds.height();

    float vw = getWidth();
    float vh = getHeight();
    float zoom;
    // Skip everything and zoom completely out
    if (bounds.isEmpty()) {
      zoom = MIN_SCALE;
    }
    else {
      float z1 = vw / w * SCALE_PADDING;
      float z2 = vh / h * SCALE_PADDING;

      zoom = Math.min(z1, z2); 
      zoom *= getScale();
      // Limit the zoom
      zoom = Math.min(MAX_SCALE, zoom);
      zoom = Math.max(MIN_SCALE, zoom);
    }

    // Check if zoom has changed enough to need updating or we are at minimum zoom
    if ((Math.abs(zoom - getScale()) / zoom) > SCALE_THRESHOLD || zoom == MIN_SCALE) {
      float dx = (vw / 2f) - bounds.centerX();
      float dy = (vh / 2f) - bounds.centerY();

      // Check if we are zoomed out enough to center
      RectF imageBounds = getImageScreenBounds();
      float dScale = zoom / getScale();
      float iw = imageBounds.width() * dScale;
      float ih = imageBounds.height() * dScale;
      
      if (iw <= vw) {
        dx = (vw / 2f) - imageBounds.centerX();
      }

      if (ih <= vh) {
        dy = (vh / 2f) - imageBounds.centerY();
      }
      
      // Pan and zoom
      panByZoomTo(dx, dy, zoom, FOLLOW_DURATION);
    }
  }

}