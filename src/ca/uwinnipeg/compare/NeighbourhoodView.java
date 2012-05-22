/**
 * 
 */
package ca.uwinnipeg.compare;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.shapes.Shape;
import android.util.Log;
import android.view.View;

/**
 * @author Garrett Smith
 *
 */
public class NeighbourhoodView {
  
  @SuppressWarnings("unused")
  public final String TAG = "NeighbourhoodView";
  
  // The shape of the neighbourhood.
  protected Shape mShape;
  
  // The view containing this neighbourhood.
  View mView;
  
  // Whether this neighbourhood is selected or not.
  boolean mFocused;
  
  /**
   * Sets the focused status of this neighbourhood.
   * A focused neighbourhood draws differently.
   * @param focus
   */
  public void setFocused(Boolean focus) {
    mFocused = focus;
  }
  
  public boolean isFocused() {
    return mFocused;
  }
  
  public NeighbourhoodView(View v){
    mView = v;
  }

  /**
   * Set the shape being used to select a neighbourhood.
   */
  public void setSelectionShape(Shape s) {
    //mSelection.setShape(s);
  }
  
  /**
   * Set the bounds of the selection.
   */
  public void setSelectionBounds(Rect r) {
    //mSelection.setBounds(r);
  }
  
  /**
   * Draws the neighbourhood to the given canvas.
   * @param canvas
   */
  protected void draw(Canvas canvas) { 
    
    Log.i(TAG, "Drawing neighbourhood!");
    
    if (mFocused) {
      // TODO: Dim unselected area
    }
    
    // TODO: Draw the shape of the current selection
  }
}
