/**
 * 
 */
package ca.uwinnipeg.proximitydroid.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import ca.uwinnipeg.proximity.ProbeFunc;
import ca.uwinnipeg.proximity.image.AlphaFunc;
import ca.uwinnipeg.proximity.image.BlueFunc;
import ca.uwinnipeg.proximity.image.GreenFunc;
import ca.uwinnipeg.proximity.image.Image;
import ca.uwinnipeg.proximity.image.RedFunc;
import ca.uwinnipeg.proximitydroid.Region;
import ca.uwinnipeg.proximitydroid.RotatedBitmap;
import ca.uwinnipeg.proximitydroid.Util;

/**
 * This service maintains a list of {@link PropertyService}s and the attached {@link Image}.
 * @author Garrett Smith
 *
 */
// TODO: make the currently viewed service run first followed by the remaining
// TODO: make sure we remove highlights of removed regions
public class ProximityService 
extends Service
implements OnSharedPreferenceChangeListener {

  public static final String TAG = "ProximityService"; 

  @SuppressWarnings("unchecked")
  public static final Class<PropertyService>[] SERVICE_CLASSES = 
    (Class<PropertyService>[]) new Class<?>[] { 
      NeighbourhoodService.class, 
      IntersectionService.class, 
      DifferenceService.class,
      ComplimentService.class
    };

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

  // broadcast actions

  // status changes
  public static final String ACTION_BITMAP_SET = "action.BITMAP_SET";

  public static final String ACTION_REGION_ADDED = "action.REGION_ADDED";
  public static final String ACTION_REGIONS_CLEARED = "action.REGIONS_CLEARED";
  
  // Parcel keys
  public static final String REGION = "Region";
  public static final String BITMAP = "Bitmap";
  public static final String FILE_NAME = "file name";
  
  // broadcast manager used to send and receive messages
  protected LocalBroadcastManager mBroadcastManager;
  
  //  list of connections to services
  protected List<PropertyServiceConnection> mConnections = 
      new ArrayList<PropertyServiceConnection>();
  
  // map of component names to services
  protected Map<ComponentName, PropertyService> mServices =
      new HashMap<ComponentName, PropertyService>();
  
  // Binding
  
  // Binder given to the client 
  private final IBinder mBinder = new LocalBinder<ProximityService>(this);

  @Override
  public IBinder onBind(Intent intent) {
    return mBinder;
  }
  
  protected class PropertyServiceConnection implements ServiceConnection {
    
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
      @SuppressWarnings("unchecked")
      LocalBinder<PropertyService> binder = (LocalBinder<PropertyService>) service;
      PropertyService propService = binder.getService();
      mServices.put(name, propService); 
      Log.i(TAG, "Binding " + name);
    }
    
    @Override
    public void onServiceDisconnected(ComponentName name) {
      mServices.remove(name);
    }
  }
  
  // Lifecycle
  
  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    // state this service must be explicitly stopped
    return START_STICKY;
  }
  
  @Override
  public void onCreate() {
    super.onCreate();
    
    // Start other services
    for (Class<?> clazz : SERVICE_CLASSES) {
      Intent intent = new Intent(this, clazz);
      // create and store connection
      PropertyServiceConnection con = new PropertyServiceConnection();
      mConnections.add(con);
      // start and bind service
      startService(intent);
      bindService(intent, con, BIND_AUTO_CREATE);
    }
    
    // Get the application broadcast manager
    mBroadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());
    
    // Load probe funcs
    Map<String, List<ProbeFunc<Integer>>> features = getProbeFuncs();
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
    
    // register to watch settings
    settings.registerOnSharedPreferenceChangeListener(this);
  }
  
  public PropertyService getPropertyService(Class<? extends PropertyService> clazz) {
    ComponentName key = new ComponentName(this, clazz);
    return mServices.get(key);
  }

  // Proximity
  
  public Image getImage() {
    return mImage;
  }

  public Uri getUri() {
    return mUri;
  }

  /**
   * Returns the filename of the bitmap currently being used.
   * @return
   */
  public String getFileName() {
    String path = mUri.getPath();
    int start = path.lastIndexOf('/');
    int end = path.lastIndexOf('.');
    end = end < start ? path.length() : end;
    return path.substring(start, end);
  }

  public List<Region> getRegions() {
    return new ArrayList<Region>(mRegions);
  }

  /**
   * Adds a region to the system and all property services.
   * @param region
   */
  public void addRegion(Region region) { 
    mRegions.add(region);
    // tell services
    for (PropertyService serv : mServices.values()) {
      serv.addRegion(region);
    }
    // broadcast that a region has been added
    Intent intent = new Intent(ACTION_REGION_ADDED);
    intent.putExtra(REGION, region);
    mBroadcastManager.sendBroadcast(intent);
  }
  
  /**
   * Removes a region from the system and property services.
   * @param region
   */
  public void removeRegion(Region region) {
    mRegions.remove(region);
    // tell services
    for (PropertyService serv : mServices.values()) {
      serv.removeRegion(region);
    }
  }
  
  /**
   * clears all regions.
   */
  public void clearRegions() {
    // clear all regions
    mRegions.clear();
    // tell services
    for (PropertyService serv : mServices.values()) {
      serv.clearRegions();
    }
    // broadcast clear
    Intent intent = new Intent(ACTION_REGIONS_CLEARED);
    mBroadcastManager.sendBroadcast(intent);
  }
  
  /**
   * Returns true if the bitmap has been set.
   * @return
   */
  public boolean hasBitmap() {
    return mHasBitmap;
  }
  
  /**
   * Returns the currently set bitmap.
   * @return
   */
  public RotatedBitmap getBitmap() {
    return mBitmap;
  }   
  
  /**
   * Sets the bitmap from the  given data Uri.
   * @param data
   */
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
  
  /**
   * Sets the bitmap from the given bitmap.
   * @param bitmap
   */
  public void setBitmap(RotatedBitmap bitmap) {
    mBitmap = bitmap;
    
    // Broadcast the change
    Intent intent = new Intent(ACTION_BITMAP_SET);
    intent.putExtra(BITMAP, mBitmap);
    intent.putExtra(FILE_NAME, getFileName());
    mBroadcastManager.sendBroadcast(intent);
    
    // a task that loads the pixels into the perceptual system
    new AsyncTask<RotatedBitmap, Void, Void>() {

      @Override
      protected Void doInBackground(RotatedBitmap... params) {
        RotatedBitmap rbm = params[0];
        if (rbm != null) {
          Util.setImage(mImage, rbm.getBitmap());
          // setup all the services
          for (PropertyService serv : mServices.values()) {
            serv.setup(mImage);
          }
        }
        return null;
      }
      
    }.execute(mBitmap);
  }
  
  /**
   * Returns a map of strings representing categories to lists of propbe functions.
   * @return
   */
  public Map<String, List<ProbeFunc<Integer>>> getProbeFuncs() {
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

  // Preferences

  @Override
  public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
    // handle probe func changes
    if (mProbeFuncs.keySet().contains(key)) {
      ProbeFunc<Integer> func = mProbeFuncs.get(key);
      if (prefs.getBoolean(key, false)) {
        mImage.addProbeFunc(func);
      }
      else {
        mImage.removeProbeFunc(func);
      }
      // tell services
      for (PropertyService serv : mServices.values()) {
        serv.onProbeFuncsChanged();
      }
    }    
  }
  
}
