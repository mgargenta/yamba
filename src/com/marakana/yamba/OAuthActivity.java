package com.marakana.yamba;

import oauth.signpost.OAuth;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.basic.DefaultOAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;
import winterwell.jtwitter.OAuthSignpostClient;
import winterwell.jtwitter.Twitter;
import winterwell.jtwitter.TwitterException;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class OAuthActivity extends Activity {
  static final String TAG = "Yamba-OAuthActivity";
  static final String OAUTH_KEY = "1csHpu9jAh9XB41E210A";
  static final String OAUTH_SECRET = "7QuUrV43ULb4Pevtaly9RJqNKU6khLQpdtWGmT8c";
  static final String OAUTH_CALLBACK_SCHEME = "x-marakana-yamba-oauth-twitter";
  static final String OAUTH_CALLBACK_URL = OAUTH_CALLBACK_SCHEME
      + "://callback";

  YambaApp yamba;
  private String username;
  private OAuthSignpostClient oauthClient;
  private OAuthConsumer mConsumer;
  private OAuthProvider mProvider;

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    yamba = (YambaApp) getApplication();
    
    setContentView(R.layout.oauth);
    
    TextView usernameText = (TextView)findViewById(R.id.username);
    usernameText.setText( yamba.prefs.getString("username", ""));
    
    // mConsumer = new DefaultOAuthConsumer(OAUTH_KEY, OAUTH_SECRET);
    mConsumer = new CommonsHttpOAuthConsumer(OAUTH_KEY, OAUTH_SECRET);
    mProvider = new DefaultOAuthProvider(
        "https://api.twitter.com/oauth/request_token",
        "https://api.twitter.com/oauth/access_token",
        "https://api.twitter.com/oauth/authorize");

    // Read the prefs to see if we have token
    yamba.prefs = PreferenceManager.getDefaultSharedPreferences(this);
    String username = yamba.prefs.getString("username", "");
    String token = yamba.prefs.getString("token", null);
    String tokenSecret = yamba.prefs.getString("tokenSecret", null);
    if (token != null && tokenSecret != null) {
      // We have token, use it
      mConsumer.setTokenWithSecret(token, tokenSecret);
      // Make a Twitter object
      oauthClient = new OAuthSignpostClient(OAUTH_KEY, OAUTH_SECRET, token,
          tokenSecret);
      yamba.twitter = new Twitter(username, oauthClient);
    }
  }

  /* Callback once we are done with the authorization of this app with Twitter. */
  @Override
  public void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    Log.d(TAG, "intent: " + intent);

    // Check if this is a callback from OAuth
    Uri uri = intent.getData();
    if (uri != null && uri.getScheme().equals(OAUTH_CALLBACK_SCHEME)) {
      Log.d(TAG, "callback: " + uri.getPath());

      String verifier = uri.getQueryParameter(OAuth.OAUTH_VERIFIER);
      Log.d(TAG, "verifier: " + verifier);

      new RetrieveAccessTokenTask().execute(verifier);
    }
  }

  public void onClickAuthorize(View view) {
    username = ((EditText) findViewById(R.id.username)).getText().toString(); 
    new OAuthAuthorizeTask().execute();
  }

  /* Responsible for starting the Twitter authorization */
  class OAuthAuthorizeTask extends AsyncTask<Void, Void, String> {

    @Override
    protected String doInBackground(Void... params) {
      String authUrl;
      String message = null;
      try {
        authUrl = mProvider.retrieveRequestToken(mConsumer, OAUTH_CALLBACK_URL);
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl));
        startActivity(intent);
      } catch (OAuthMessageSignerException e) {
        message = "OAuthMessageSignerException";
        e.printStackTrace();
      } catch (OAuthNotAuthorizedException e) {
        message = "OAuthNotAuthorizedException";
        e.printStackTrace();
      } catch (OAuthExpectationFailedException e) {
        message = "OAuthExpectationFailedException";
        e.printStackTrace();
      } catch (OAuthCommunicationException e) {
        message = "OAuthCommunicationException";
        e.printStackTrace();
      }
      return message;
    }

    @Override
    protected void onPostExecute(String result) {
      super.onPostExecute(result);
      if (result != null) {
        Toast.makeText(OAuthActivity.this, result, Toast.LENGTH_LONG).show();
      }
    }
  }

  /* Responsible for retrieving access tokens from twitter */
  class RetrieveAccessTokenTask extends AsyncTask<String, Void, String> {

    @Override
    protected String doInBackground(String... params) {
      String message = null;
      String verifier = params[0];
      try {
        // Get the token
        Log.d(TAG, "mConsumer: " + mConsumer);
        Log.d(TAG, "mProvider: " + mProvider);
        mProvider.retrieveAccessToken(mConsumer, verifier);
        String token = mConsumer.getToken();
        String tokenSecret = mConsumer.getTokenSecret();
        mConsumer.setTokenWithSecret(token, tokenSecret);

        Log.d(TAG, String.format("verifier: %s, token: %s, tokenSecret: %s",
            verifier, token, tokenSecret));

        // Store token in prefs
        yamba.prefs.edit().putString("username", username).putString("token", token).putString("tokenSecret",
            tokenSecret).commit();

        // Make a Twitter object
        oauthClient = new OAuthSignpostClient(OAUTH_KEY, OAUTH_SECRET, token,
            tokenSecret);
        yamba.twitter = new Twitter("MarkoGargenta", oauthClient);

        Log.d(TAG, "token: " + token);
        
        message = "Successfully authorized with Twitter";
      } catch (OAuthMessageSignerException e) {
        message = "OAuthMessageSignerException";
        e.printStackTrace();
      } catch (OAuthNotAuthorizedException e) {
        message = "OAuthNotAuthorizedException";
        e.printStackTrace();
      } catch (OAuthExpectationFailedException e) {
        message = "OAuthExpectationFailedException";
        e.printStackTrace();
      } catch (OAuthCommunicationException e) {
        message = "OAuthCommunicationException";
        e.printStackTrace();
      }
      return message;
    }

    @Override
    protected void onPostExecute(String result) {
      super.onPostExecute(result);
      if (result != null) {
        Toast.makeText(getApplicationContext(), result, Toast.LENGTH_LONG)
            .show();
//        OAuthActivity.this.finish();
      }
    }
  }

  /* Responsible for getting Twitter status */
  class GetStatusTask extends AsyncTask<Void, Void, String> {
    @Override
    protected String doInBackground(Void... params) {
      return yamba.twitter.getStatus().text;
    }

    @Override
    protected void onPostExecute(String result) {
      super.onPostExecute(result);
      Toast.makeText(getApplicationContext(), result, Toast.LENGTH_LONG).show();
    }
  }

  /* Responsible for posting new status to Twitter */
  class PostStatusTask extends AsyncTask<String, Void, String> {
    @Override
    protected String doInBackground(String... params) {
      try {
        yamba.twitter.setStatus(params[0]);
        return "Successfully posted: " + params[0];
      } catch (TwitterException e) {
        return "Error connecting to server.";
      }
    }

    @Override
    protected void onPostExecute(String result) {
      super.onPostExecute(result);
      Toast.makeText(getApplicationContext(), result, Toast.LENGTH_LONG).show();
    }
  }
}
