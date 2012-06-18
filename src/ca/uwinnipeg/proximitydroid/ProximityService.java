/**
 * 
 */
package ca.uwinnipeg.proximitydroid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.support.v4.content.LocalBroadcastManager;
import ca.uwinnipeg.proximity.PerceptualSystem.PerceptualSystemSubscriber;
import ca.uwinnipeg.proximity.ProbeFunc;
import ca.uwinnipeg.proximity.image.AlphaFunc;
import ca.uwinnipeg.proximity.image.BlueFunc;
import ca.uwinnipeg.proximity.image.GreenFunc;
import ca.uwinnipeg.proximity.image.Image;
import ca.uwinnipeg.proximity.image.RedFunc;

/**
 * This service does the heavy lifting of generating neighbourhoods, intersections, maintaining the
 * perceptual system, etc.
 * @author Garrett Smith
 *
 */
public class ProximityService 
  extends Service
  implements OnPreferenceChangeListener {
  
  public static final String TAG = "ProximityService"; 
  
  // broadcast actions
  
  // status changes
  public static final String ACTION_BITMAP_SET = "action.BITMAP_SET";
  public static final String ACTION_REGION_ADDED = "action.REGION_ADDED";
  public static final String ACTION_REGIONS_CLEARED = "action.REGIONS_CLEARED";
  
    
  // actions
  public static final String ACTION_ADD_REGION = "action.ADD_REGION";
  public static final String ACTION_CLEAR_REGIONS = "action.CLEAR_REGIONS";
  
  // Parcel keys
  public static final String BITMAP = "Bitmap";
  public static final String REGION = "Region";
  
  // The broadcast manager used to send and recieve messages
  protected LocalBroadcastManager mBroadcastManager;
  
  // Binding
  
  // Binder given to the client 
  private final IBinder mBinder = new LocalBinder<ProximityService>(this);

  @Override
  public IBinder onBind(Intent intent) {
    return mBinder;
  }
  
  // Lifecycle
  
  @Override
  public void onCreate() {
    super.onCreate();
    
    // Get the application broadcast manager
    mBroadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());
    
    // register to receive region update messages
    IntentFilter filter = new IntentFilter(ACTION_ADD_REGION);
    filter.addAction(ACTION_CLEAR_REGIONS);
    mBroadcastManager.registerReceiver(mRegionUpdateReceiver, filter);
    
    // TODO: get image
    

    // add enabled probe funcs
    SharedPreferences settings = Util.getSupportDefaultSharedPrefences(this);
    for (String key : mProbeFuncs.keySet()) {
      // add enabled funcs
      if (settings.getBoolean(key, false)) {
        mImage.addProbeFunc(mProbeFuncs.get(key));
      }
    }
  }  

  
  // Tasking 

  // The map of regions to the indices of the points in their neighbourhoods
  protected Map<Region, List<Integer>> mNeighbourhoods = new HashMap<Region, List<Integer>>();
  
  // The indices of the points in the intersection
  protected List<Integer> mIntersection = null;

  // Map each region to the task generating it's neighbourhood
  protected Map<Region, NeighbourhoodTask> mNeighbourhoodTasks = 
      new HashMap<Region, NeighbourhoodTask>();
  
  // The list of intersection operations that will bring us to our desired result 
  protected IntersectTask mIntersectTask = null;
  
  // The list of regions to be used by intersect tasks
  protected List<Region> mIntersectRegions = new ArrayList<Region>(); 
  
  protected void updateNeighbourhood(Region region) {

    // Calculate Neighbourhood

    // clear old points
    mNeighbourhoods.put(region, new ArrayList<Integer>());
    
    // Cancel running task
    NeighbourhoodTask task = mNeighbourhoodTasks.get(region);
    if (task != null && task.isRunning()) {
      task.cancel(true);
    }

    // run the task
    task = new NeighbourhoodTask();
    mNeighbourhoodTasks.put(region, task);
    task.execute(region);
  }
  
  protected void addIntersectionTask(Region region) {
    // add the region to the queue
    mIntersectRegions.add(region);
    
    // run if this is the only region in the queue
    if (mIntersectRegions.size() == 1) {
      runNextIntersectionTask();
    }
      
  }
  
  protected void runNextIntersectionTask() {
    // only run if there are regions in the queue
    if (!mIntersectRegions.isEmpty()) {
      
      // run the task on the next region
      mIntersectTask = new IntersectTask();
      Region region = mIntersectRegions.remove(0);

      // start the task
      mIntersectTask.execute(region);
    }
  }
  
  protected void stopIntersectionTasks() {
    if (mIntersectTask != null) mIntersectTask.cancel(true);
    mIntersectRegions.clear();
  }
  
  protected void updateAll() {
    // stop all intersection tasks
    stopIntersectionTasks();
    
    for (Region r : mRegions) {
      updateNeighbourhood(r);
      addIntersectionTask(r);
    }
    
    runNextIntersectionTask();
  }
  

  private abstract class ProcessingTask 
      extends AsyncTask<Region, Integer, List<Integer>>
      implements PerceptualSystemSubscriber {
      
      protected Region mRegion;
      protected boolean mRunning = true;
      
      public boolean isRunning() {
        return mRunning;
      }
    
      @Override
      protected void onPostExecute(List<Integer> result) {
        mRunning = false;
        // update loading and point
        // TODO: set task progress complete
  //      if (mViewMode == KEY && mShowFrag != null) {
  //        mShowFrag.setHighlight(getHighlightIndices());
  //        setProgressBarVisibility(isLoading());
  //      }
      }
      
      protected float mLastProgress = 0;
      protected final float PROGRESS_THERSHOLD = 0.001f;
      
      @Override
      public void updateProgress(float progress) {
        // TODO: record progress
  //      if (mViewMode == KEY && (progress - mLastProgress > PROGRESS_THERSHOLD)) {
  //        mLastProgress = progress;
  //        publishProgress(Integer.valueOf((int) (progress * 10000)));
  //      }
      }
      
      @Override
      protected void onProgressUpdate(Integer... values) {
  //      setProgress(values[0].intValue());
      }
    
    }

  private class NeighbourhoodTask extends ProcessingTask {
      
      protected Region mRegion;
    
      @Override
      protected List<Integer> doInBackground(Region... params) {
        
        mRegion = params[0];
    
        // check if we should stop because the task was cancelled
        if (isCancelled()) return null;
    
        int center = mRegion.getCenterIndex(mImage);
        int[] regionPixels = mRegion.getIndices(mImage);
    
        // this is wrapped in a try catch so if we get an async runtime exception the task will stop
        try {
          return mImage.getHybridNeighbourhoodIndices(center, regionPixels, 0.1, this);
        } 
        catch(RuntimeException ex) {
          return null;
        }
      }
    
      @Override
      protected void onPostExecute(List<Integer> result) {
        // save the result
  //      if (result != null) {
  //      mNeighbourhoods.put(mRegion, result);
  //      }
  //      else {
  //        mNeighbourhoods.get(mRegion).clear();
  //      }
        super.onPostExecute(result);
      }
      
    }

  private class IntersectTask extends ProcessingTask {
    
      @Override
      protected List<Integer> doInBackground(Region... params) {    
  //      
  //      mRegion = params[0];
  //  
  //      // check if we should stop because the task was cancelled
  //      if (isCancelled()) return null;
  //      
  //      List<Integer> indices = new ArrayList<Integer>();
  //  
  //      // this is wrapped in a try catch so if we get an async runtime exception the task will stop
  //      try {
  //        // check if this is the only region
  //        if (mIntersection == null) {
  //          indices = mRegion.getIndicesList(mImage);
  //        }
  //        // else take the intersection of the region and the current intersection
  //        else {
  //          indices = mImage.getDescriptionBasedIntersectIndices(mIntersection, mRegion.getIndicesList(mImage), this);
  //        }
  //      } 
  //      catch(RuntimeException ex) {
  //        return null;
  //      }
  //  
  //      return indices;
        return null;
        
      }
      
      @Override
      protected void onPostExecute(List<Integer> result) {
        // store result as the new intersection
  //      if (result != null) {
  //        mIntersection = result;
  //      }
  //      else {
  //        mIntersection.clear();
  //      }
  //      // run the next intersection task if there is one
  //      runNextIntersectionTask();
        super.onPostExecute(result);
      }
      
    }

  // Proximity
    
  // The perceptual system
  protected Image mImage = new Image();
  
  // The regions of interest
  protected List<Region> mRegions = new ArrayList<Region>();  
  
  // The probe functions
  protected Map<String, ProbeFunc<Integer>> mProbeFuncs = new HashMap<String, ProbeFunc<Integer>>();

  // The bitmap we are working on
  protected RotatedBitmap mBitmap;  
  
  // whether a bitmap has been set yet
  protected boolean mHasBitmap = false;
  
  // the uri of the image
  protected Uri mUri;

  public List<Region> getRegions() {
    return new ArrayList<Region>(mRegions);
  }

  public void addRegion(Region region) { 
    mRegions.add(region);
    // run the update on the added region
    updateNeighbourhood(region);
    addIntersectionTask(region);
    // TODO: update after adding region
    
    // broadcast that a region has been added
    Intent intent = new Intent(ACTION_REGION_ADDED);
    intent.putExtra(REGION, region);
    mBroadcastManager.sendBroadcast(intent);
  }
  
  public void removeRegion(Region region) {
    mRegions.remove(region);
    // TODO: update neighbourhood and intersect after removing region
  }
  
  public void clearRegions() {
    mRegions.clear();
    // TODO: update neighbourhood and intersect after clearing region
    Intent intent = new Intent(ACTION_REGIONS_CLEARED);
    mBroadcastManager.sendBroadcast(intent);
  }
  
  public boolean hasBitmap() {
    return mHasBitmap;
  }
  
  public RotatedBitmap getBitmap() {
    return mBitmap;
  }   
  
  public void setBitmap(Uri data) {
    // record that we now have a bitmap
    mHasBitmap = true;
    
    // Load the bitmap and update the perceptual system
    mUri = data;
    
    // a task that loads the bitmap from disk
    new AsyncTask<Uri, Void, RotatedBitmap>() {

      @Override
      protected RotatedBitmap doInBackground(Uri... params) {
        return Util.loadImage(params[0], getContentResolver());
      }
      
      protected void onPostExecute(RotatedBitmap result) {
        setBitmap(result);        
      }
      
    }.execute(mUri);
  }
  
  public void setBitmap(RotatedBitmap bitmap) {
    mBitmap = bitmap;
    
    // Broadcast the change
    Intent intent = new Intent(ACTION_BITMAP_SET);
    intent.putExtra(BITMAP, mBitmap);
    mBroadcastManager.sendBroadcast(intent);
    
    // a task that loads the pixels into the perceptual system
    new AsyncTask<RotatedBitmap, Void, Void>() {

      @Override
      protected Void doInBackground(RotatedBitmap... params) {
        RotatedBitmap rbm = params[0];
        if (rbm != null) {
          Util.setImage(mImage, rbm.getBitmap());
        }
        return null;
      }
      
    }.execute(mBitmap);
  }
  

//  public int[] getHighlightIndices() {
//    
//    // get the indices we are currently interseted in
//    List<Integer> indices;
//    if (mViewMode == NEIGHBOURHOOD_KEY) {
//      indices = new ArrayList<Integer>();
//      for (Region r : mRegions) {
//        indices.addAll(mNeighbourhoods.get(r));
//      }
//    }
//    else if (mViewMode == INTERSECT_KEY && mIntersection != null) {
//      indices = mIntersection;
//    }
//    else {
//      return null;
//    }
//    
//    // convert indices to positions
//    int[] points = new int[indices.size() * 2];
//    for (int i = 0; i < indices.size(); i++) {
//      int index = indices.get(i);
//      points[i*2] = mImage.getX(index);
//      points[i*2+1] = mImage.getY(index);
//    }
//    return points;
//  }  

  // Feature Preferences

  public Map<String, List<ProbeFunc<Integer>>> getFeatures() {
    return loadProbeFuncs();
  }

  private Map<String, List<ProbeFunc<Integer>>> loadProbeFuncs() {
    Map<String, List<ProbeFunc<Integer>>> features = new HashMap<String, List<ProbeFunc<Integer>>>();
    // load all the standard probe funcs
    features.put("Colour", new ArrayList<ProbeFunc<Integer>>());
    List<ProbeFunc<Integer>> colourFuncs = features.get("Colour");
    colourFuncs.add(new AlphaFunc());
    colourFuncs.add(new RedFunc());
    colourFuncs.add(new GreenFunc());
    colourFuncs.add(new BlueFunc());

    // TODO: load probe funcs from external storage
    return features;
  }

  @Override
  public boolean onPreferenceChange(Preference preference, Object newValue) {
    if (newValue instanceof Boolean) {
      // add or remove the probe func from the perceptual system
      ProbeFunc<Integer> func = mProbeFuncs.get(preference.getKey());
      if ((Boolean)newValue) {
        mImage.addProbeFunc(func);
      }
      else {
        mImage.removeProbeFunc(func);
      }
      // recalculate all neighbourhoods and intersection
      updateAll();
    }
    return true;
  }
  
  // Broadcasts
  
  protected RegionUpdateReceiver mRegionUpdateReceiver = new RegionUpdateReceiver();
  
  public class RegionUpdateReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();
      if (action.equals(ACTION_ADD_REGION)) {
        Region r = intent.getParcelableExtra(REGION);
        addRegion(r);
      }
      else if (action.equals(ACTION_CLEAR_REGIONS)) {
        clearRegions();
      }
    }
    
  }
}
