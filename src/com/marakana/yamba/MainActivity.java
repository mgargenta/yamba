package com.marakana.yamba;

import winterwell.jtwitter.TwitterException;
import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.ActionBar.TabListener;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

public class MainActivity extends Activity implements TabListener {
  private static final String TAG = "Yamba";
  private int mThemeId = -1;
  static final String FRAGMENT_COMPOSE = "FRAGMENT_COMPOSE";
  static final String FRAGMENT_PREFS = "FRAGMENT_PREFS";
  YambaApp yamba;
  
  FragmentManager fragmentManager;
  FragmentTransaction fragmentTransaction;

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    yamba = (YambaApp)getApplication();
    
    // Update theme, if previously set
    if (savedInstanceState != null) {
      mThemeId = savedInstanceState.getInt("theme");
      setTheme(mThemeId);
    }

    setContentView(R.layout.main);

    // Set the action bar
    ActionBar bar = getActionBar();
    bar.addTab(bar.newTab().setText("Timeline").setTabListener(this));
//    bar.addTab(bar.newTab().setText("@Mentions").setTabListener(this));

    bar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM
        | ActionBar.DISPLAY_USE_LOGO);
    bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
    bar.setDisplayShowHomeEnabled(true);

    fragmentManager = getFragmentManager();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.main_menu, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
    case R.id.itemAuthorize:
      startActivity(new Intent(this, OAuthActivity.class));
      return true;
    case R.id.toggleTheme:
      if (mThemeId == android.R.style.Theme_Holo_Light) {
        mThemeId = android.R.style.Theme_Holo;
      } else {
        mThemeId = android.R.style.Theme_Holo_Light;
      }
      Log.d(TAG, "mThemeId=" + mThemeId);
      this.recreate();
      return true;
    case R.id.itemRefresh:
      startService(new Intent(this, UpdaterService.class));
      return true;
    case R.id.itemCompose:
      fragmentTransaction = fragmentManager.beginTransaction();
      
      // Remove old compose fragment
      Fragment prev = getFragmentManager().findFragmentByTag(FRAGMENT_COMPOSE);
      if (prev != null) {
        fragmentTransaction.remove(prev);
      }

      fragmentTransaction.addToBackStack(null);

      // Create and show the dialog.
      ComposeFragment composeFragment = ComposeFragment.newInstance();
      composeFragment.show(fragmentTransaction, FRAGMENT_COMPOSE);
      return true;
    case R.id.itemPrefs:
      fragmentTransaction = fragmentManager.beginTransaction();
      
      // Remove old compose fragment
      prev = getFragmentManager().findFragmentByTag(FRAGMENT_PREFS);
      if (prev != null) {
        fragmentTransaction.remove(prev);
      }

      fragmentTransaction.addToBackStack(null);

      // Create and show the dialog.
      PrefsFragment prefsFragment = new PrefsFragment();
//      prefsFragment.show(fragmentTransaction, FRAGMENT_PREFS);
      return true;
    default:
      return super.onOptionsItemSelected(item);
    }
  }

  /** Saves state of the activity before it gets recreated */
  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putInt("theme", mThemeId);
  }

  /* TabListener callback when current tab was re-selected */
  public void onTabReselected(Tab tab, FragmentTransaction ft) {
    TimelineFragment timelineFragment = (TimelineFragment) getFragmentManager()
        .findFragmentById(R.id.list);

  }

  /* TabListener callback when tab was selected */
  public void onTabSelected(Tab tab, FragmentTransaction ft) {

  }

  /* TabListener callback was unselected */
  public void onTabUnselected(Tab tab, FragmentTransaction ft) {

  }

  //--- Other methods
  
  public void postToTwitter(String status) {
    // Remove old compose fragment
    fragmentTransaction = fragmentManager.beginTransaction();

    Fragment prev = getFragmentManager().findFragmentByTag(FRAGMENT_COMPOSE);
    if (prev != null) {
      fragmentTransaction.remove(prev);
    }
    fragmentTransaction.commit();
    
    (new PostToTwitterTask()).execute(status);
  }
  
  class PostToTwitterTask extends AsyncTask<String,Void,String> {

    @Override
    protected String doInBackground(String... status) {
      String ret=null;
      
      try {
        yamba.twitter.setStatus(status[0]);
        ret = "Successfully posted";
      } catch (TwitterException e) {
        Log.e(TAG, "Failed to post to twitter", e);
        ret = "Failed to post to Twitter";
      }

      return ret;
    }

    @Override
    protected void onPostExecute(String result) {
      Toast.makeText(yamba, result, Toast.LENGTH_LONG).show();
    }
    
  }
}