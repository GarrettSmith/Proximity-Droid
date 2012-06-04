/**
 * 
 */
package ca.uwinnipeg.proximitydroid;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import ca.uwinnipeg.compare.R;
import ca.uwinnipeg.proximity.ProbeFunc;
import ca.uwinnipeg.proximity.image.BlueFunc;
import ca.uwinnipeg.proximity.image.GreenFunc;
import ca.uwinnipeg.proximity.image.RedFunc;

import com.actionbarsherlock.app.SherlockActivity;

/**
 * @author Garrett Smith
 *
 */
public class ProbeFuncSelectActivity extends SherlockActivity {
  
  public static String TAG = "ProbeFuncSelectActivity";
  
  // The ListView used to display the selected probe functions
  protected ListView mListView;
  
  // The list of selected probe function
  protected List<ProbeFunc<Integer>> mProbeFuncs = new ArrayList<ProbeFunc<Integer>>();

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    setContentView(R.layout.probe_func_select);
    
    mListView = (ListView) findViewById(R.id.probe_func_select_list);
    
    for (int i = 0; i < 7; i++) {
      mProbeFuncs.add(new RedFunc());
      mProbeFuncs.add(new GreenFunc());
      mProbeFuncs.add(new BlueFunc());
    }
    
    mListView.setAdapter(new ArrayAdapter<ProbeFunc<Integer>>(this, R.layout.probe_func_list_item, R.id.probe_func_name, mProbeFuncs));
  }

}
