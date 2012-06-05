package ca.uwinnipeg.proximitydroid;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.SpinnerAdapter;

import ca.uwinnipeg.proximitydroid.views.RegionSelectView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

// TODO: Fix spinner on 2.1
// TODO: Deal with Failed Binder Transaction
/**
 * The activity can select neighbourhoods from an image. 
 * @author Garrett Smith
 *
 */
public class RegionSelectActivity 
extends SherlockActivity 
implements ActionBar.OnNavigationListener {

  public static final String TAG = "RegionSelectActivity";

  // Indices of the items in the drop down menu.
  // Must match the string array shape_list in arrays.xml and the ordinal values of
  // NeighbourhoodView.Shape.
  // Used by onNavigationItemSelected.
  public static final int BUTTON_RECTANGLE_INDEX = 0;
  public static final int BUTTON_OVAL_INDEX = 1;
  public static final int BUTTON_POLYGON_INDEX   = 2;

  private RotatedBitmap mBitmap;

  private RegionSelectView mSelectView;
  private RegionView mRegionView;

  // Used to restore state properly without having the restored bounds overwritten.
  private Runnable mOnCreateRunnable = null;

  // UI
  private ActionBar mActionBar;
  private SpinnerAdapter mSpinnerAdapter;

  // bundle keys
  private static final String BUNDLE_KEY_BITMAP = "Bitmap";
  private static final String BUNDLE_KEY_ORIENTATION = "Orientation";
  private static final String BUNDLE_KEY_BOUNDS = "Bounds";
  private static final String BUNDLE_KEY_SHAPE = "Shape";

  /**
   * Called when this Activity is first created.
   */
  @Override
  protected void onCreate(Bundle state){
    super.onCreate(state);    
    setContentView(R.layout.region_select);

    // Make the window full screen
    getWindow().setFlags(
        WindowManager.LayoutParams.FLAG_FULLSCREEN, 
        WindowManager.LayoutParams.FLAG_FULLSCREEN);

    // setup view
    mSelectView = (RegionSelectView) findViewById(R.id.select_view);

    RegionView.Shape shape = null;
    
    // Check if state needs to be restored
    if (state != null) {
      restoreBitmap(state);
      
      // Restore NeighbourhoodView
      final Rect bounds = state.getParcelable(BUNDLE_KEY_BOUNDS);
      String shapeStr = state.getString(BUNDLE_KEY_SHAPE);
      if (shapeStr != null) {
        shape = RegionView.Shape.valueOf(RegionView.Shape.class, shapeStr);
      }

      // Create a runnable to restore the bounds after the shape has been restored
      if (bounds != null) {
        mOnCreateRunnable = new Runnable() {
          public void run() {
            mRegionView.setBounds(bounds);
            // TODO: See if this follow needs to be put off
            mSelectView.followResize(mRegionView);
          }
        };
      }
      
    }
    // Get information from the sent intent
    else {
      Intent intent = getIntent();
      Bundle extras = intent.getExtras();
      if (extras != null) {
        restoreBitmap(extras);
      }
    }    

    // UI
    setupActionBar(shape);

    // exit if we haven't gotten a bitmap by this point
    if (mBitmap != null) {
      init();
    } 
    else {
      finish();
    }

  }
  
  private void restoreBitmap(Bundle extras) {
    Bitmap bm = (Bitmap) extras.getParcelable(BUNDLE_KEY_BITMAP);
    int orientation = extras.getInt(BUNDLE_KEY_ORIENTATION);
    mBitmap = new RotatedBitmap(bm, orientation);
  }

  private void setupActionBar(RegionView.Shape shape) {
    mActionBar = getSupportActionBar();
    mActionBar.setDisplayHomeAsUpEnabled(true);

    // Action bar navigation
    mSpinnerAdapter = ArrayAdapter.createFromResource(
        this, 
        R.array.shape_list, 
        android.R.layout.simple_spinner_dropdown_item);

    mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);   
    mActionBar.setListNavigationCallbacks(mSpinnerAdapter, this);

    if (shape != null) {
      // Restore selected shape in spinner which then sets the neighbourhood shape
      mActionBar.setSelectedNavigationItem(shape.ordinal());
    }
  }

  private void init() {
    // set bitmap
    mSelectView.setImageBitmap(mBitmap);
    
    mRegionView = mSelectView.getNeighbourhood();

    int width = mBitmap.getWidth();
    int height = mBitmap.getHeight();

    Rect imageRect = new Rect(0, 0, width, height);    
    mRegionView.setImageRect(imageRect);

    mRegionView.resetBounds();
  }

  @Override
  protected void onSaveInstanceState(Bundle state) {    
    // Save the current image
    if (mBitmap != null) {
      state.putParcelable(BUNDLE_KEY_BITMAP, mBitmap.getBitmap());
      state.putInt(BUNDLE_KEY_ORIENTATION, mBitmap.getOrientation());
    }
    
    if (mRegionView != null) {
      state.putParcelable(BUNDLE_KEY_BOUNDS, mRegionView.getBounds());
      state.putString(BUNDLE_KEY_SHAPE, mRegionView.getShape().name());
    }
    super.onSaveInstanceState(state);
  }

  //@Override
  public boolean onNavigationItemSelected(int position, long itemId) {
    switch(position) {
      case BUTTON_RECTANGLE_INDEX:
        mRegionView.setShape(RegionView.Shape.RECTANGLE);
        break;
      case BUTTON_OVAL_INDEX:
        mRegionView.setShape(RegionView.Shape.OVAL);
        break;
      case BUTTON_POLYGON_INDEX:
        mRegionView.setShape(RegionView.Shape.POLYGON);
        break;     
    }
    // Check to see if bounds need to be restored
    if (mOnCreateRunnable != null) {
      mOnCreateRunnable.run();
      mOnCreateRunnable = null;
    }
    return true;
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getSupportMenuInflater();
    inflater.inflate(R.menu.region_select, menu);
    //mSelectView.updateFinalMatrix(); // recenter
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menu_reset:
        reset();
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }
  
  /**
   * Reset the neighbourhood.
   */
  public void reset() {
    mRegionView.reset();
  }

}
