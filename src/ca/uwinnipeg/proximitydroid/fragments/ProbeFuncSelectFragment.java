/**
 * 
 */
package ca.uwinnipeg.proximitydroid.fragments;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import ca.uwinnipeg.proximity.ProbeFunc;
import ca.uwinnipeg.proximity.image.BlueFunc;
import ca.uwinnipeg.proximity.image.GreenFunc;
import ca.uwinnipeg.proximity.image.RedFunc;
import ca.uwinnipeg.proximitydroid.R;

import com.actionbarsherlock.app.SherlockListFragment;

/**
 * @author Garrett Smith
 *
 */
// TODO: Look into preference fragments
public class ProbeFuncSelectFragment extends SherlockListFragment {

  public static String TAG = "ProbeFuncSelectActivity";

  // The ListView used to display the selected probe functions
  protected ListView mListView;

  // The list of selected probe function
  protected List<ProbeFunc<Integer>> mProbeFuncs = new ArrayList<ProbeFunc<Integer>>();

  @Override
  public View onCreateView(
      LayoutInflater inflater, 
      ViewGroup container, 
      Bundle savedInstanceState) {
    super.onCreateView(inflater, container, savedInstanceState);

    // TODO: Dynamically load default or previous probe functions
    mProbeFuncs.add(new RedFunc());
    mProbeFuncs.add(new GreenFunc());
    mProbeFuncs.add(new BlueFunc());

    return inflater.inflate(R.layout.probe_func_select, container, false);
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    mListView = getListView();

    mListView.setAdapter(
        new ArrayAdapter<ProbeFunc<Integer>>(
            getActivity(), 
            R.layout.probe_func_list_item, 
            R.id.probe_func_name, 
            mProbeFuncs));
  }

}
