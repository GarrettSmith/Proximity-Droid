package ca.uwinnipeg.compare;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageView;

/**
 * 
 * The view which the image and currently selected neighbourhood is drawn.
 *
 */
public class SelectView extends ImageView {  

  public static final String TAG = "SelectionView";

  // This matrix transforms the image to fit within the screen. 
  // The image is scaled to fit the screen using letterboxing.
  private final Matrix mBaseMatrix = new Matrix();

  // This matrix reflects the transforms (scales and transforms) that the user has made while v
  // viewing the matrix.
  private final Matrix mUserMatrix = new Matrix();

  // This is the matrix created from the base and user matrix.
  private final Matrix mFinalMatrix = new Matrix();

  // This is the bitmap currently being displayed.
  private final RotatedBitmap mBitmap = new RotatedBitmap(null);

  // This is the neighbourhood being selected for the image.
  private ArrayList<NeighbourhoodView> mNeighbourhoods = new ArrayList<NeighbourhoodView>();

  public void add(NeighbourhoodView nv) {
    mNeighbourhoods.add(nv);
    invalidate(nv.getBounds()); // request redraw
  }

  // Ran to ensure the dimensions of the view are accessible to update the base matrix
  private Runnable mOnLayoutRunnable = null;

  public SelectView(Context context){
    super(context);
    init();
  }

  public SelectView(Context context, AttributeSet attr) {
    super(context, attr);
    init();
  }

  private void init() {
    setScaleType(ImageView.ScaleType.MATRIX); // set the image view to use a matrix
  }

  @Override 
  protected void onLayout(
      boolean changed, 
      int left, 
      int top, 
      int right, 
      int bottom) {

    super.onLayout(changed, left, top, right, bottom);

    Runnable run = mOnLayoutRunnable;
    if (run != null) {
      mOnLayoutRunnable = null; // reset
      run.run(); // update the matrix after the view dimensions have been set
    }

    if (mBitmap.getBitmap() != null) {
      updateFinalMatrix();
    }

  }

  @Override 
  public boolean onTouchEvent(MotionEvent event) {
    switch (event.getAction()) {
      case MotionEvent.ACTION_DOWN: 
        for (NeighbourhoodView n : mNeighbourhoods) {
          if (n.isFocused()) {
            n.handleDown(event);
          }
        }
        return true;
      case MotionEvent.ACTION_UP:   
        for (NeighbourhoodView n : mNeighbourhoods) {
          if (n.isFocused()) {
            n.handleUp(event);
          }          
        }
        return true;
      case MotionEvent.ACTION_MOVE: 
        for (NeighbourhoodView n : mNeighbourhoods) {
          if (n.isFocused()) {
            n.handleMove(event);
          }
        }
        return true;
      default:                      
        return false;
    }
  }

  @Override
  public void setImageBitmap(Bitmap bm) {
    setImageBitmap(bm, RotatedBitmap.NORMAL);
    center();
    zoomTo(MIN_SCALE, getWidth()/2f, getHeight()/2f);
  }

  protected void setImageBitmap(Bitmap bm, int or) {
    super.setImageBitmap(bm);
    mBitmap.setBitmap(bm);
    mBitmap.setOrientation(or);
    updateBaseMatrix(); // update the base matrix to reflect the new bitmap
  }

  // Matrix updates
  private void updateBaseMatrix() {
    // do nothing if there is no image to work with
    if (mBitmap.getBitmap() == null) return;

    float viewW = getWidth();
    float viewH = getHeight();

    // Deal with the dimensions of the view not being set yet
    if (viewW == 0) {
      mOnLayoutRunnable = new Runnable() {
        public void run() {
          updateBaseMatrix(); // run updateBaseMatrix() at a later time
        }
      };
    }

    float imgW = mBitmap.getWidth();
    float imgH = mBitmap.getHeight();

    mBaseMatrix.reset(); // start from identity

    // calculate both scales
    float scaleW = viewW / imgW;
    float scaleH = viewH / imgH;

    float scale = Math.min(scaleW, scaleH); // Find the smaller scale

    mBaseMatrix.postConcat(mBitmap.getMatrix()); // concat the image's matrix
    mBaseMatrix.postScale(scale, scale);
    mBaseMatrix.postTranslate( 
        ((viewW - imgW * scale) / 2), 
        ((viewH - imgH * scale) / 2));

    updateFinalMatrix(); // Reflect change in final matrix

  }  

  /**
   * Updates the final matrix to be the concatenation of the base and user matrix.
   */
  protected void updateFinalMatrix() {
    Matrix m = new Matrix(mBaseMatrix);
    m.postConcat(mUserMatrix);
    mFinalMatrix.set(m);
    setImageMatrix(mFinalMatrix); // Apply the final matrix to the image

    // Let neighbourhoods know the final matrix has changed
    for (NeighbourhoodView n : mNeighbourhoods) {
      n.setScreenMatrix(mFinalMatrix);
    }
  }

  public Matrix getFinalMatrix() {
    return new Matrix(mFinalMatrix);
  }
  
  public Matrix getUserMatrix() {
    return new Matrix(mUserMatrix);
  }
  
  public void setUserMatrix(Matrix m) {
    mUserMatrix.set(m);
    invalidate();
  } 
  
  public void setUserMatrix(float[] values) {
    mUserMatrix.setValues(values);
    invalidate();
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    //canvas.concat(mFinalMatrix);
    for (NeighbourhoodView n : mNeighbourhoods) {
      n.draw(canvas);
    }
  }

  // User matrix operations

  // Used to create operations that run on their own threads
  private Handler mHandler = new Handler();

  // Used to access the values of the user matrix
  private float[] mUserVals = new float[9];

  private float getValue(int key) {
    // read values
    mUserMatrix.getValues(mUserVals);
    return mUserVals[key];
  }

  public float getTranslateX() {
    return getValue(Matrix.MTRANS_X);
  }

  public float getTranslateY() {
    return getValue(Matrix.MTRANS_Y);
  }

  public float getScale() {
    return getValue(Matrix.MSCALE_X);
  }

  // Pan

  public void panBy(float dx, float dy) {
    mUserMatrix.postTranslate(dx, dy);
    updateFinalMatrix();
  }

  public void panTo(float x, float y) {
    float dx = x - getTranslateX();
    float dy = y - getTranslateY();
    panBy(dx, dy);
  }

  public void panBy(float dx, float dy, final float duration) {
    final float startX = getTranslateX();
    final float startY = getTranslateY();
    final float dxPerMs = dx / duration;
    final float dyPerMs = dy / duration;

    // create a runnable that will be repeated until duration, and thus target, is met
    new TransformRunnable(mHandler, duration) {
      @Override
      public void action(float elapsed) {
        float targetX = startX + dxPerMs * elapsed;
        float targetY = startY + dyPerMs * elapsed;
        panTo(targetX, targetY);
      }
    }.run();
  }

  public void panTo(float x, float y, float duration) {
    float dx = x - getTranslateX();
    float dy = y - getTranslateY();
    panBy(dx, dy, duration);
  }
  
  public void center() {
    RectF imageBounds = getImageScreenBounds();
    
    float vw = getWidth();
    float vh = getHeight();
    
    float dx = (vw / 2f) - imageBounds.centerX();
    float dy = (vh / 2f) - imageBounds.centerY();

    panBy(dx, dy);
  }
    
    // Zoom

  public void zoomBy(float dScale, float x, float y) {
    mUserMatrix.postScale(dScale, dScale, x, y);
    updateFinalMatrix();
  }

  /**
   * Zooms by the given delta scale to the view's center.
   * @param dScale
   */
  public void zoomBy(float dScale) {
    zoomBy(dScale, getWidth()/2f, getHeight()/2f);
  }

  public void zoomTo(float scale, float x, float y) {
    float dScale = scale / getScale();
    zoomBy(dScale, x, y);
  }

  public void zoomTo(float scale) {
    zoomTo(scale, getWidth()/2f, getHeight()/2f);
  }

  public void zoomBy(float dScale, final float x, final float y, final float duration) {
    final float startScale = getScale();
    final float dScalePerMs = dScale / duration;

    // create a runnable that will be repeated until duration, and thus target, is met
    new TransformRunnable(mHandler, duration) {      
      @Override
      public void action(float elapsed) {
        float targetScale = startScale + dScalePerMs * elapsed;
        zoomTo(targetScale, x, y);
      }
    }.run();
  }

  public void zoomBy(float dScale, float duration) {
    zoomBy(dScale, getWidth()/2f, getHeight()/2f, duration);
  }

  public void zoomTo(float scale, float x, float y, float duration) {
    float dScale = scale - getScale();
    zoomBy(dScale, x, y, duration);
  }  

  public void zoomTo(float scale, float duration) {
    float dScale = scale - getScale();
    zoomBy(dScale, getWidth()/2f, getHeight()/2f, duration);
  }
  
  public void panByZoomTo(float dx, float dy, float scale, final float duration) {
    final float startX = getTranslateX();
    final float startY = getTranslateY();
    final float dxPerMs = dx / duration;
    final float dyPerMs = dy / duration;
    
    final float startScale = getScale();
    final float dScalePerMs = (scale - startScale) / duration;
    
    // create a runnable that will be repeated until duration, and thus target, is met
    new TransformRunnable(mHandler, duration) {
      private boolean mPanned = false;
      @Override
      public void action(float elapsed) {
        // Pan first
        if (elapsed * 2 < duration) {
          float targetX = startX + dxPerMs * elapsed * 2;
          float targetY = startY + dyPerMs * elapsed * 2;
          panTo(targetX, targetY);
        }
        // Make sure we made it all the way there
        else if (!mPanned) {
          float targetX = startX + dxPerMs * duration;
          float targetY = startY + dyPerMs * duration;
          panTo(targetX, targetY);
          mPanned = true;
        }
        // Zoom
        else {
          float targetScale = startScale + dScalePerMs * elapsed;
          zoomTo(targetScale);
        }
      }
    }.run();
  }

  // Neighbourhood following

  private static final float FOLLOW_DURATION = 300f;

  /**
   * Zoom to fit and pan to center the given neighbourhood in the view.
   * @param nv
   */
  public void followMove(NeighbourhoodView nv) {
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

  private static final float SCALE_PADDING = 0.6f;
  private static final float SCALE_THRESHOLD = 0.1f;
  private static final float MAX_SCALE = 6f;
  private static final float MIN_SCALE = 0.9f;

  public void followResize(NeighbourhoodView nv) {
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
  
  private RectF getImageScreenBounds() {
    RectF imageBounds = new RectF(
        0, 
        0, 
        mBitmap.getBitmap().getWidth(), 
        mBitmap.getBitmap().getHeight());
    mFinalMatrix.mapRect(imageBounds);

    
    return imageBounds;
  }
  
  /**
   * A runnable that fires a series of events using the elapsed time.
   * @author garrett
   *
   */
  public abstract class TransformRunnable implements Runnable {
    private Handler mHandler;    
    private float mDuration;
    private long mStartTime = -1;
    
    public TransformRunnable(Handler hand, float dur) {
      mHandler = hand;
      mDuration = dur;
    }

    @Override
    public void run() {
      // Setup start time on first run
      if (mStartTime == -1) {
        mStartTime = System.currentTimeMillis();
        mHandler.post(this);
        return;
      }
      
      long now = System.currentTimeMillis();
      float elapsed = Math.min(mDuration, now - mStartTime);
      // Take its action
      action(elapsed);
      // Check if it should rerun or run the next in the chain
      if (elapsed < mDuration) {
        mHandler.post(this);
      }
    }
    
    /**
     * The action taken by this runnable.
     * @param elapsed the elapsed time since start time
     */
    public abstract void action(float elapsed);
    
  }

}