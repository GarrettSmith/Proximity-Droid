///**
// * 
// */
//package ca.uwinnipeg.proximitydroid.views;
//
//import android.content.res.Resources;
//import android.graphics.Canvas;
//import android.graphics.Matrix;
//import android.graphics.Paint;
//import android.graphics.Path;
//import android.graphics.Point;
//import android.graphics.Rect;
//import android.graphics.RectF;
//import android.view.MotionEvent;
//import ca.uwinnipeg.proximity.image.Image;
//import ca.uwinnipeg.proximitydroid.Polygon;
//import ca.uwinnipeg.proximitydroid.R;
//
///**
// * @author Garrett Smith
// *
// */
//// TODO: pan view  when region is NEAR edge of screen
//public class SelectRegionView extends RegionView {  
//  
//  public static final String TAG = "SelectRegionView";
//  

//
//
//
//  
//

//  
//  // The minimum size of the region relative to screen size
//  protected static int MIN_SIZE;
//
//  
//  // The Previously touched position IN IMAGE SPACE
//  protected float mLastX;
//  protected float mLastY;
//  
//  // The previous distance between two fingers, used for pinch zoom
//  protected float mLastDistanceX;
//  protected float mLastDistanceY;
//  
//  
//  protected AddRegionView mSelectView;
//
//
//
//  @Override
//  public void reset() {
//    super.reset();
//    mSelectView.followResize(this);
//  }
//


  


//
//  /**
//   * handles motion to move the neighbourhood.
//   */
//  public void handleMove(MotionEvent event) {
//
//    // Check if any action is being performed
//    if (mAction != Action.NONE) {
//
//      float[] p = mView.convertToImageSpace(event.getX(), event.getY());
//      float x = p[0];
//      float y = p[1];
//
//      // rectangle that needs redraw
//      Rect dirty = getPaddedScreenSpaceBounds();
//
//      float dx = x - mLastX;
//      float dy = y - mLastY;
//
//      // Determine which action to take
//      switch (mAction) {
//        case MOVE:
//          move((int)dx, (int)dy);
//          break;
//        case RESIZE:
//          resize((int)dx, (int)dy, mSelectedEdge);
//          break;
//        case MOVE_POINT:
//          mSelectedPoint.offset((int)dx, (int)dy);
//          updateBounds();
//          break;
//      }
//
//      // Record this as the previous position
//      mLastX = x;
//      mLastY = y;
//
//      // Reflect change on screen
//      dirty.union(getPaddedScreenSpaceBounds());
//      mView.invalidate(dirty);
//    }
//  }
//
//  @Override
//  protected void resize(int dx, int dy, Edge edg, Rect newBounds) {
//    switch (edg) {
//      case L: 
//        // constrain to image area
//        newBounds.left = Math.max(0, newBounds.left + dx); 
//        // prevent flipping and keep min size
//        newBounds.left = Math.min(newBounds.left, newBounds.right - MIN_SIZE); 
//        break;
//      case R: 
//        newBounds.right = Math.min(mImageBounds.right, newBounds.right + dx);
//        newBounds.right = Math.max(newBounds.right, newBounds.left + MIN_SIZE);
//        break;
//      case T: 
//        newBounds.top = Math.max(0, newBounds.top + dy);
//        newBounds.top = Math.min(newBounds.top, newBounds.bottom - MIN_SIZE);
//        break;
//      case B: 
//        newBounds.bottom = Math.min(mImageBounds.bottom, newBounds.bottom + dy);
//        newBounds.bottom = Math.max(newBounds.bottom, newBounds.top + MIN_SIZE);
//        break;
//      case TL:
//        resize(dx, dy, Edge.T, newBounds);
//        resize(dx, dy, Edge.L, newBounds);
//        break;
//      case TR:
//        resize(dx, dy, Edge.T, newBounds);
//        resize(dx, dy, Edge.R, newBounds);
//        break;
//      case BL:
//        resize(dx, dy, Edge.B, newBounds);
//        resize(dx, dy, Edge.L, newBounds);
//        break;
//      case BR:
//        resize(dx, dy, Edge.B, newBounds);
//        resize(dx, dy, Edge.R, newBounds);
//        break;
//    }
//  }
//
//
//
//}
