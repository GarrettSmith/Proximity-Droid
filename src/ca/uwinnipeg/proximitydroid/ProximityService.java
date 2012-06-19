/**
 * 
 */
package ca.uwinnipeg.proximitydroid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
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
// TODO: change neighbourhood calculation to be linear?
public class ProximityService 
  extends Service
  implements OnPreferenceChangeListener {
  
  public static final String TAG = "ProximityService"; 
  
  // broadcast actions
  
  // status changes
  public static final String ACTION_BITMAP_SET = "action.BITMAP_SET";
  
  public static final String ACTION_REGION_ADDED = "action.REGION_ADDED";
  public static final String ACTION_REGIONS_CLEARED = "action.REGIONS_CLEARED";
  
  public static final String ACTION_NEIGHBOURHOOD_PROGRESS = "action.NEIGHBOURHOOD_PROGRESS";
  public static final String ACTION_INTERSECTION_PROGRESS = "action.INTERSECTION_PROGRESS";

  public static final String ACTION_NEIGHBOURHOOD_SET = "action.NEIGHBOURHOOD_SET";
  public static final String ACTION_INTERSECTION_SET = "action.INTERSECTION_SET";
  
  // Parcel keys
  public static final String BITMAP = "Bitmap";
  public static final String REGION = "Region";
  public static final String PROGRESS = "Progress";
  public static final String POINTS = "points";
  
  // The broadcast manager used to send and receive messages
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
    
    // Load probe funcs
    Map<String, List<ProbeFunc<Integer>>> features = loadProbeFuncs();
    for (String catStr : features.keySet()) {
      for (ProbeFunc<Integer> func : features.get(catStr)) {
        String key = catStr + "_" + func.toString();
        mProbeFuncs.put(key, func);
      }
    }

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
  protected List<Integer> mIntersection = new ArrayList<Integer>();

  // Map each region to the task generating it's neighbourhood
  protected Map<Region, NeighbourhoodTask> mNeighbourhoodTasks = 
      new HashMap<Region, NeighbourhoodTask>();
  
  // The list of intersection operations that will bring us to our desired result 
  protected IntersectTask mIntersectTask = null;
  
  // The list of regions to be used by intersect tasks
  protected List<Region> mIntersectQueue = new ArrayList<Region>(); 
  
  // the current progress of calculations  
  protected Map<String, Integer> mProgress = new HashMap<String, Integer>();
  
  protected void updateNeighbourhood(Region region) {
    // clear old points
    mNeighbourhoods.put(region, new ArrayList<Integer>());
    
    // Cancel running task
    NeighbourhoodTask task = mNeighbourhoodTasks.get(region);
    if (task != null && task.isRunning()) {
      task.cancel(true);
    }

    // run the new task
    task = new NeighbourhoodTask();
    mNeighbourhoodTasks.put(region, task);
    task.execute(region);
  }
  
  protected void addIntersectionTask(Region region) {
    // add the region to the queue
    mIntersectQueue.add(region);
    
    // run if this is the only region in the queue
    if (mIntersectQueue.size() == 1) {
      runNextIntersectionTask();
    }
      
  }
  
  protected void runNextIntersectionTask() {
    // only run if there are regions in the queue
    if (!mIntersectQueue.isEmpty()) {
      
      // run the task on the next region
      mIntersectTask = new IntersectTask();
      Region region = mIntersectQueue.remove(0);

      // start the task
      mIntersectTask.execute(region);
    }
  }
  
  protected void updateAll() {
    // stop all intersection tasks
    // cancel running task
    if (mIntersectTask != null) mIntersectTask.cancel(true);
    // clear upcoming tasks
    mIntersectQueue.clear();
    // clear calculated intersection
    mIntersection.clear();    
    //clear all neighbourhoods
    mNeighbourhoods.clear();
    
    // recalculate all neighbourhoods and intersections
    for (Region r : mRegions) {
      updateNeighbourhood(r);
      addIntersectionTask(r);
    }    
    // start running intersection tasks
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
      publishProgress(10000);
    }
    
    public abstract String ProgressKey();

    protected float mLastProgress = 0;
    protected final float PROGRESS_THERSHOLD = 0.001f;

    @Override
    public void updateProgress(float progress) {
      if (progress - mLastProgress > PROGRESS_THERSHOLD) {
        mLastProgress = progress;
        publishProgress(Integer.valueOf((int) (progress * 10000)));
      }
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
      int value = values[0].intValue();
      // store progress
      mProgress.put(ProgressKey(), value);
      
      // broadcast progress
      Intent intent = new Intent(ProgressKey());
      intent.putExtra(PROGRESS, value);
      mBroadcastManager.sendBroadcast(intent);
    }

  }

  private class NeighbourhoodTask extends ProcessingTask {

    @Override
    protected List<Integer> doInBackground(Region... params) {
      mRegion = params[0];

      // check if we should stop because the task was cancelled
      if (isCancelled()) return null;

      int center = mRegion.getCenterIndex(mImage);
      int[] regionPixels = mRegion.getIndices(mImage);

      // this is wrapped in a try catch so if we get an async runtime exception the task will stop
      try {
        // TODO: set epsilon
        return mImage.getHybridNeighbourhoodIndices(center, regionPixels, 0.1, this);
      } 
      catch(RuntimeException ex) {
        return null;
      }
    }

    @Override
    protected void onPostExecute(List<Integer> result) {      
      super.onPostExecute(result);
      // save the result
      setNeighbourhood(mRegion, result);
    }

    @Override
    public String ProgressKey() {
      return ACTION_NEIGHBOURHOOD_PROGRESS;
    }

  }

  private class IntersectTask extends ProcessingTask {
    
      @Override
      protected List<Integer> doInBackground(Region... params) {
        mRegion = params[0];
    
        // check if we should stop because the task was cancelled
        if (isCancelled()) return null;
        
        List<Integer> indices = new ArrayList<Integer>();
    
        // this is wrapped in a try catch so if we get an async runtime exception the task will stop
        try {
          // check if this is the only region
          if (mIntersection.isEmpty()) {
            indices = mRegion.getIndicesList(mImage);
          }
          // else take the intersection of the region and the current intersection
          else {
            indices = mImage.getDescriptionBasedIntersectIndices(mIntersection, mRegion.getIndicesList(mImage), this);
          }
        } 
        catch(RuntimeException ex) {
          return null;
        }
    
        return indices;
        
      }
      
      @Override
      protected void onPostExecute(List<Integer> result) {
        super.onPostExecute(result);
        // store result as the new intersection
        setIntersection(result);
        // run the next intersection task if there is one
        runNextIntersectionTask();
      }

      @Override
      public String ProgressKey() {
        return ACTION_INTERSECTION_PROGRESS;
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
    // clear all regions
    mRegions.clear();
    
    // remove all tasks
    updateAll();
    
    // broadcast clear
    Intent intent = new Intent(ACTION_REGIONS_CLEARED);
    mBroadcastManager.sendBroadcast(intent);
  }
  
  protected void setNeighbourhood(Region region, List<Integer> indices) {
    // save the change
    if (indices != null) {
      mNeighbourhoods.put(region, indices);
    }
    else {
      mNeighbourhoods.get(region).clear();
    }
    
    // convert the indices to points
    indices = mNeighbourhoods.get(region);
    int[] points = indicesToPoints(indices);
    
    // broadcast the change
    Intent intent = new Intent(ACTION_NEIGHBOURHOOD_SET);
    intent.putExtra(REGION, region);
    intent.putExtra(POINTS, points);
    mBroadcastManager.sendBroadcast(intent);
  }

  public Map<Region, int[]> getNeighbourhoods() {
    Map<Region, int[]> nhs = new HashMap<Region, int[]>();
    for (Region reg : mNeighbourhoods.keySet()) {
      List<Integer> indices = mNeighbourhoods.get(reg);
      nhs.put(reg, indicesToPoints(indices));
    }
    return nhs;
  }
  
  public int getNeighbourhoodProgress() {
    // count the number of tasks running
    int runningTasks = 0;
    for (NeighbourhoodTask task : mNeighbourhoodTasks.values()) {
      if (task.isRunning()) {
        runningTasks++;
      }
    }
    return getProgress(ACTION_NEIGHBOURHOOD_PROGRESS, runningTasks);
  }
  
  protected void setIntersection(List<Integer> indices) {
    // save the new intersection
    mIntersection.clear();
    if (indices != null) mIntersection.addAll(indices);
    
    // convert to points
    int[] points = indicesToPoints(indices);
    
    // broadcast the change
    Intent intent = new Intent(ACTION_INTERSECTION_SET);
    intent.putExtra(POINTS, points);
    mBroadcastManager.sendBroadcast(intent);
  }
  
  public int[] getIntersection() {
    return indicesToPoints(mIntersection);
  }
  
  public int getIntersectionProgress() {
    return getProgress(ACTION_NEIGHBOURHOOD_PROGRESS, mIntersectQueue.size());
  }
  
  protected int getProgress(String key, int runningTasks) {
    Integer prog =  mProgress.get(key);
    // don't give us a null pointer if we haven't set progress yet
    // don't try to divide by 0
    if (prog != null && runningTasks != 0) {
      return prog / runningTasks;
    }
    else {
      return 10000;
    }
  }
  
  protected int[] indicesToPoints(List<Integer> indices) {
    int[] points = new int[indices.size() * 2];
    for (int i = 0; i < indices.size(); i++) {
      int index = indices.get(i);
      points[i*2] = mImage.getX(index);
      points[i*2 + 1] = mImage.getY(index);
    }
    return points;
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

  public void populatePreferences(PreferenceScreen root) {
    // Load features
    Map<String, List<ProbeFunc<Integer>>> features = loadProbeFuncs();

    // Generate preference items from features    
    // generate a category for each given category    
    for (String catStr : features.keySet()) {
      List<ProbeFunc<Integer>> funcs = features.get(catStr);

      // only add the category if it is non empty
      if (funcs != null && !funcs.isEmpty()) {
        PreferenceCategory category = new PreferenceCategory(this);
        category.setTitle(catStr);
        category.setKey(catStr);
        root.addPreference(category);

        // generate a preference for each probe func
        for (ProbeFunc<Integer> func : funcs) {
          
          Preference pref;
          
          // Use switches when supported
          if (android.os.Build.VERSION.SDK_INT >= 14) {
            pref = new SwitchPreference(this);
          }
          else {
            pref = new CheckBoxPreference(this);
          }

          // Set name and key
          String key = catStr + "_" + func.toString();
          pref.setTitle(func.toString());
          pref.setKey(key);
          category.addPreference(pref);
          
          // register the service as a preference listener
          pref.setOnPreferenceChangeListener(this);
        }
      }
    }
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
  
}
