package ca.uwinnipeg.compare;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
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
  protected Matrix mBaseMatrix = new Matrix();

  // This matrix reflects the transforms (scales and transforms) that the user has made while v
  // viewing the matrix.
  protected Matrix mUserMatrix = new Matrix();

  // This is the matrix created from the base and user matrix.
  private final Matrix mFinalMatrix = new Matrix();

  // This is the bitmap currently being displayed.
  protected final RotatedBitmap mBitmap = new RotatedBitmap(null);

  // This is the neighbourhood being selected for the image.
  protected ArrayList<NeighbourhoodView> mNeighbourhoods = new ArrayList<NeighbourhoodView>();

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
    switch (event.getActionMasked()) {
      case MotionEvent.ACTION_DOWN: 
        for (NeighbourhoodView n : mNeighbourhoods) {
          n.handleDown(event);
        }
        return true;
      case MotionEvent.ACTION_UP:   
        for (NeighbourhoodView n : mNeighbourhoods) {
          n.handleUp(event);
        }
        return true;
      case MotionEvent.ACTION_MOVE: 
        for (NeighbourhoodView n : mNeighbourhoods) {
          n.handleMove(event);
        }
        return true;
      default:                      
        return false;
    }
  }

  @Override
  public void setImageBitmap(Bitmap bm) {
    setImageBitmap(bm, RotatedBitmap.NORMAL);
  }

  protected void setImageBitmap(Bitmap bm, int or) {
    super.setImageBitmap(bm);
    mBitmap.setBitmap(bm);
    mBitmap.setOrientation(or);
    updateBaseMatrix(); // update the base matrix to reflect the new bitmap
  }

  // Matrix updates
  private void updateBaseMatrix() {
    if (mBitmap.getBitmap() == null) return; // do nothing if there is not image to work with

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
    // THIS SHOULDN'T MATTER BECAUSE THEY HAVE A REFERENCE OF THE MATRIX
    //for (NeighbourhoodView n : mNeighbourhoods) {
    //  n.setMatrix(mFinalMatrix);
    //}
  }

  public Matrix getFinalMatrix() {
    return mFinalMatrix;
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
  
  private static final float TRANSFORM_DURATION = 1.25f;
  
  private float getValue(int key) {
    // read values
    mUserMatrix.getValues(mUserVals);
    return mUserVals[key];
  }
  
  private float getTranslateX() {
    return getValue(Matrix.MTRANS_X);
  }
  
  private float getTranslateY() {
    return getValue(Matrix.MTRANS_Y);
  }
  
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
    final long startTime = System.currentTimeMillis();
    
    mHandler.post(new Runnable() {
      
      @Override
      public void run() {
        long now = System.currentTimeMillis();
        float elapsed = Math.min(duration, now - startTime);
        float targetX = startX + dxPerMs * elapsed;
        float targetY = startY + dyPerMs * elapsed;
        panTo(targetX, targetY);
        
        if (elapsed < duration) {
          mHandler.post(this); // rerun
        }
      }
    });
  }
  
  public void panTo(float x, float y, float duration) {
    float dx = x - getTranslateX();
    float dy = y - getTranslateY();
    panBy(dx, dy, duration);
  }

}