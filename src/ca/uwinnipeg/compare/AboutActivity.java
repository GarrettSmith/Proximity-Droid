package ca.uwinnipeg.compare;

import android.app.Activity;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class AboutActivity extends Activity {
  
  /**
   * Called when this Activity is first created.
   */
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.about);
    TextView verText = (TextView) findViewById(R.id.about_app_version);
    verText.setText(getVersionName());
  }
  
  /**
   * Returns the version name of the app.
   */
  public String getVersionName() {
    String ver = "";
    try {
      // Ask for version
      ver = this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionName; 
    }
    catch (NameNotFoundException e) {
      Log.v("Error", e.getMessage()); // Catch errors
    }
    return ver;
  }

}
