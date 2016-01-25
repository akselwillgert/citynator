package se.subsurface.citynator;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.display.RoundedBitmapDisplayer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import de.psdev.licensesdialog.LicensesDialogFragment;
import se.subsurface.citynator.Model.GameType;

public class MainActivity extends AppCompatActivity {


    private final static String TAG = "MainActivity";
    private final AtomicInteger totalScore = new AtomicInteger(0);
    //Facebook
    private CallbackManager callbackManager;
    private AccessTokenTracker accessTokenTracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppEventsLogger.activateApp(this);
        setContentView(R.layout.activity_main);
        //Facebook
        callbackManager = CallbackManager.Factory.create();

        LoginButton loginButton = (LoginButton) findViewById(R.id.login_button);

        loginButton.setReadPermissions(Collections.singletonList("user_friends"));

        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            final String TAG = "FacebookCallback";

            @Override
            public void onCancel() {
                Log.e(TAG, "onCancel");
            }

            @Override
            public void onError(FacebookException error) {
                Log.e(TAG, "onError");
            }

            @Override
            public void onSuccess(LoginResult loginResult) {

                Log.e(TAG, "onSuccess result=" + loginResult.getRecentlyGrantedPermissions());
                showFacebookHighscores();
                setupJoinHighscoreButton();
                putFacebook(totalScore.get());

            }
        });

        ViewGroup areaView = (ViewGroup) findViewById(R.id.area_list);

        for (final GameType gameType: FlagItApplication.getInstance().gameTypes) {
            RelativeLayout gameTypeView = (RelativeLayout)LayoutInflater.from(this).inflate(R.layout.game_type, areaView, false);

            TextView areaTV = (TextView) gameTypeView.findViewById(R.id.game_type_name);
            TextView scoreTV = (TextView) gameTypeView.findViewById(R.id.game_type_score);
            TextView bestDistanceTV = (TextView) gameTypeView.findViewById(R.id.game_type_best_distance);
            ImageView flagView = (ImageView) gameTypeView.findViewById(R.id.game_type_img);
            flagView.setImageResource(gameType.imageId);

            areaTV.setText(gameType.name);

            int bestScore = FlagItApplication.getInstance().getHighscore(gameType.name);
            double bestDistance = FlagItApplication.getInstance().getDistance(gameType.name);

            if (bestScore != -1) {
                scoreTV.setText(getResources().getString(R.string.points_p, bestScore));
                totalScore.addAndGet(bestScore);
            }
            if (bestDistance != -1) {
                String bestDistanceString = FlagItUtils.round(bestDistance, 1) + "km";
                bestDistanceTV.setText(bestDistanceString);
            } else {
                bestDistanceTV.setText(R.string.main_not_played_yet);
            }

            areaView.addView(gameTypeView);
            gameTypeView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(MainActivity.this, MapsActivity.class);
                    intent.putExtra("Area", gameType.name);
                    startActivity(intent);

                }
            });
        }
        if (isLoggedIn()) {
            showFacebookHighscores();
            setupJoinHighscoreButton();
            putFacebook(totalScore.get());
        }
        accessTokenTracker = new AccessTokenTracker() {
            @Override
            protected void onCurrentAccessTokenChanged(
                    AccessToken oldAccessToken,
                    AccessToken currentAccessToken) {

                if (currentAccessToken == null) {
                    hideFacebookScores();
                    setupJoinHighscoreButton();
                }
            }
        };
        findViewById(R.id.show_license_dlg_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final LicensesDialogFragment fragment = LicensesDialogFragment.newInstance(R.raw.notices, false, true);
                fragment.show(getSupportFragmentManager(), null);
            }
        });
    }

    private void setupJoinHighscoreButton() {
        final Button joinHighscore = (Button) findViewById(R.id.join_facebook_highscore);
        if (isLoggedIn() && !hasFacebookWrite()) {
            joinHighscore.setVisibility(View.VISIBLE);
            joinHighscore.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.e(TAG, "joinHighscore.onClick");
                    requestPermissions(totalScore.get());
                    if (hasFacebookWrite()) {
                        joinHighscore.setVisibility(View.GONE);
                    }
                }
            });
        } else {
            joinHighscore.setVisibility(View.GONE);
        }

    }

    private boolean isLoggedIn() {
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        return accessToken != null;
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Logs 'install' and 'app activate' App Events.
        AppEventsLogger.activateApp(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Logs 'app deactivate' App Event.
        AppEventsLogger.deactivateApp(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        accessTokenTracker.stopTracking();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.e(TAG, "onActivityResult data=" + data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    private void showFacebookHighscores() {
        Bundle params = new Bundle();
        params.putInt("score", 0);
        String app_id = getResources().getString(R.string.facebook_app_id);
        new GraphRequest(
                AccessToken.getCurrentAccessToken(),
                "/" + app_id + "/scores",
                null,
                HttpMethod.GET,
                new GraphRequest.Callback() {
                    public void onCompleted(GraphResponse response) {
                        Log.e(TAG, "response=" + response);
                        TableLayout highScoreTbl = (TableLayout) findViewById(R.id.tbl_high_scores);
                        highScoreTbl.setVisibility(View.VISIBLE);
                        View header = highScoreTbl.getChildAt(0);
                        highScoreTbl.removeAllViews();
                        highScoreTbl.addView(header);
                        try {
                            JSONObject obj = response.getJSONObject();
                            JSONArray array = obj.getJSONArray("data");
                            for (int i = 0; i < array.length(); i++) {

                                //Inflate new tablerow
                                TableRow row = (TableRow) LayoutInflater.from(MainActivity.this).inflate(R.layout.high_score_item, highScoreTbl, false);
                                TextView nameTV = (TextView) row.findViewById(R.id.high_score_name);
                                TextView scoreTV = (TextView) row.findViewById(R.id.high_score_points);
                                ImageView profileImage = (ImageView) row.findViewById(R.id.high_score_profile_picture);
                                highScoreTbl.addView(row);

                                //Parse json
                                JSONObject userObj = array.getJSONObject(i);
                                int score = userObj.getInt("score");
                                String name = userObj.getJSONObject("user").getString("name");

                                String id = userObj.getJSONObject("user").getString("id");
                                String url = "https://graph.facebook.com/" + id + "/picture?type=normal&width=100&height=100";

                                //Update views
                                nameTV.setText(name);
                                scoreTV.setText(String.format("%d",score));
                                ImageLoader imageLoader = ImageLoader.getInstance(); // Get singleton instance
                                DisplayImageOptions options = new DisplayImageOptions.Builder()
                                        .cacheInMemory(true)
                                        .cacheOnDisk(true)
                                        .displayer(new RoundedBitmapDisplayer(25)) // default
                                        .build();
                                imageLoader.displayImage(url, profileImage, options);
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "e=" + e.getMessage());
                        }

                    }
                }
        ).executeAsync();
    }

    private void hideFacebookScores() {
        findViewById(R.id.tbl_high_scores).setVisibility(View.GONE);
    }

    private boolean hasFacebookWrite() {
        Set<String> permissions = AccessToken.getCurrentAccessToken().getPermissions();
        for (String permission : permissions) {
            if (permission.equals("publish_actions")) {
                return true;
            }
        }
        return false;
    }

    private void requestPermissions(int score) {
        if (!hasFacebookWrite()) {
            LoginManager.getInstance().logInWithPublishPermissions(
                    this,
                    Collections.singletonList("publish_actions"));
        }

        putFacebook(score);
    }

    private void putFacebook(int score) {
        if (hasFacebookWrite()) {
            Bundle params = new Bundle();
            params.putInt("score", score);

            new GraphRequest(
                    AccessToken.getCurrentAccessToken(),
                    "/me/scores",
                    params,
                    HttpMethod.POST,
                    new GraphRequest.Callback() {
                        public void onCompleted(GraphResponse response) {
                            Log.e(TAG, "Response=" + response);
                            showFacebookHighscores();
                        }
                    }
            ).executeAsync();
        } else {
            Log.e(TAG, "No Facebook write permissions");
        }
    }
}