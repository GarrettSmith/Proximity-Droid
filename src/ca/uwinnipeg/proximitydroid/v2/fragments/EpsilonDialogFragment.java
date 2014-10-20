/**
 * 
 */
package ca.uwinnipeg.proximitydroid.v2.fragments;

import java.util.Formatter;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import ca.uwinnipeg.proximitydroid.v2.R;
import ca.uwinnipeg.proximitydroid.v2.Util;

/**
 * Dialog used to select the epsilon for a calculation.
 * <p>
 * This works be changing the value for a shared preference which the service performing the 
 * calculation watches.
 * @author Garrett Smith
 *
 */
public class EpsilonDialogFragment 
  extends DialogFragment 
  implements OnSeekBarChangeListener {
  
  public static final String TAG = "EpsilonDialogFragment";
  
  // selected epsilon
  protected float mCurrentValue;
  
  // the key of the preference to save to
  protected String mPrefKey;
  
  // the maximum value we can set, the norm
  protected float mNorm;
  
  protected SharedPreferences mPreferencs;
  
  // the number of steps you can adjust by
  protected final int STEPS = 30;
  
  // the number formatter
  protected static final String FORMAT = "%1.2f";
  
  // Argument keys
  protected static final String PREF_KEY = "pref key";
  protected static final String NORM_KEY = "norm key";
  
  protected TextView mValueText;
  
  /**
   * Default constructor.
   */
  public EpsilonDialogFragment() {}
  
  /**
   * Returns a new epsilon dialog which sets the preference with the given key.
   * @param prefKey
   * @return
   */
  public static EpsilonDialogFragment newInstance(String prefKey, float maxValue) {
    EpsilonDialogFragment frag = new EpsilonDialogFragment();
    Bundle args = new Bundle();
    args.putString(PREF_KEY, prefKey);
    args.putFloat(NORM_KEY, maxValue);
    frag.setArguments(args);
    return frag;
  }
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    // passed arguments
    Bundle args = getArguments();
    mPrefKey = args.getString(PREF_KEY);    
    mNorm = args.getFloat(NORM_KEY);
    
    // get the preferences
    mPreferencs = Util.getSupportDefaultSharedPrefences(getActivity());
    
    // get the initial preference value
    mCurrentValue = mPreferencs.getFloat(mPrefKey, 0);
  }
  
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    
    // inflate custom view
    Activity activity = getActivity();
    LayoutInflater inflater = activity.getLayoutInflater();
    View view = inflater.inflate(R.layout.epsilon, null);
    
    // setup seek bar
    SeekBar sb = (SeekBar) view.findViewById(R.id.epsilon_seekbar); 
    sb.setMax(STEPS);
    sb.setProgress((int) (mCurrentValue * STEPS));
    sb.setOnSeekBarChangeListener(this);
    
    // grab the value text view
    mValueText = (TextView) view.findViewById(R.id.epsilon_textview);
    updateValueText();
    
    // create dialog
    return new AlertDialog.Builder(activity)
                .setTitle(R.string.app_epsilon)
                .setView(view)
                .setPositiveButton(android.R.string.ok, 
                    new OnClickListener() {
                
                      @Override
                      public void onClick(DialogInterface dialog, int which) {
                        // save epsilon to preferences
                        SharedPreferences.Editor editor = mPreferencs.edit();
                        editor.putFloat(mPrefKey, mCurrentValue);
                        editor.commit();
                      }
                    }
                )
                .setNegativeButton(android.R.string.cancel, 
                    new OnClickListener() {
            
                      @Override
                      public void onClick(DialogInterface dialog, int which) {
                        dismiss();
                      }
                    }
                )
                .create();
  }
  
  /**
   * Updates the value of the text field.
   */
  protected void updateValueText() {
    String str = new Formatter().format(FORMAT, mCurrentValue).toString();
    mValueText.setText(str);
  }

  @Override
  public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
    mCurrentValue = progress / (float)STEPS * mNorm;
    // set the text to show the current epsilon
    updateValueText();
  }

  @Override
  public void onStartTrackingTouch(SeekBar seekBar) {
    // Do nothing!
    
  }

  @Override
  public void onStopTrackingTouch(SeekBar seekBar) {
    // Still do nothing!!
    
  }

}
