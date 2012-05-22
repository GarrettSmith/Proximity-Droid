package ca.uwinnipeg.compare;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageView;

/**
 * 
 * The view which the image and currently selected neighbourhood is drawn.
 *
 */
public class SelectionView extends ImageView {  
  @SuppressWarnings("unused")
  public static final String TAG = "SelectionView";
  
  //minimum space between the selection and the screen edge
  private static final int PADDING_RATIO = 2; 
  
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
  protected NeighbourhoodView mNeighbourhood;
  
  // Ran to ensure the dimensions of the view are accessible to update the base matrix
  private Runnable mOnLayoutRunnable = null;

  public SelectionView(Context context){
    super(context);
    init();
  }
  
  public SelectionView(Context context, AttributeSet attr) {
    super(context, attr);
    init();
  }
  
  private void init() {
    setScaleType(ImageView.ScaleType.MATRIX); // set the image view to use a matrix
    mNeighbourhood = new NeighbourhoodView(this);
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
    return true; // returning true means this input has been handled
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
    
    mNeighbourhood.resetBounds(mBitmap.getWidth(), mBitmap.getHeight()); // TODO: decide where to put reset
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
    mFinalMatrix.set(mBaseMatrix);
    mFinalMatrix.postConcat(mUserMatrix);
    setImageMatrix(mFinalMatrix); // Apply the final matrix to the image
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    canvas.concat(mFinalMatrix);
    mNeighbourhood.draw(canvas);
  }

}