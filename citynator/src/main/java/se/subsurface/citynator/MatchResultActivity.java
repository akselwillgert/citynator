package se.subsurface.citynator;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.facebook.FacebookSdk;
import com.facebook.share.model.SharePhoto;
import com.facebook.share.model.SharePhotoContent;
import com.facebook.share.widget.ShareDialog;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;

import se.subsurface.citynator.Model.FlagMatch;
import se.subsurface.citynator.Model.RoundResult;


public class MatchResultActivity extends FragmentActivity implements View.OnClickListener, OnMapReadyCallback {

    private static final String TAG = MatchResultActivity.class.getName();

    private GoogleMap mMap;
    private FlagMatch match;
    private LatLngBounds bounds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(this);
        setContentView(R.layout.activity_match_result);
        findViewById(R.id.btn_match_completed_ok).setOnClickListener(this);
        findViewById(R.id.fb_share_button).setOnClickListener(this);
        findViewById(R.id.fb_share_button).setEnabled(true);
        match = (FlagMatch) getIntent().getSerializableExtra("FlagMatch");
        bounds = getIntent().getParcelableExtra("Bounds");
        printResults();
        ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMapAsync(this);
    }

    private void printResults() {
        TableLayout gameResultsTable = (TableLayout) findViewById(R.id.round_results);


        for (RoundResult roundResult : match.results) {
            TableRow resultRow = (TableRow) LayoutInflater.from(this).inflate(R.layout.match_result_table_item, gameResultsTable, false);
            gameResultsTable.addView(resultRow);

            TextView cityName = (TextView) resultRow.findViewById(R.id.city_name);
            cityName.setText(roundResult.place.name);


            ImageView flag = (ImageView) resultRow.findViewById(R.id.country_flag);
            int id = this.getResources().getIdentifier(roundResult.place.countryCode, "drawable",
                    this.getPackageName());
            flag.setImageResource(id);


            TextView distance = (TextView) resultRow.findViewById(R.id.game_result_distance);
            TextView time = (TextView) resultRow.findViewById(R.id.game_result_time);
            ImageView accurcy = (ImageView) resultRow.findViewById(R.id.game_result_accuracy);

            int accuracyColor = FlagItUtils.getAccuracyColor(this, roundResult.accuracy);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                accurcy.getDrawable().setTint(accuracyColor);
            } else {
                //pre API 21
                accurcy.setColorFilter(accuracyColor);
            }

            distance.setText(getResources().getString(R.string.distance_km, FlagItUtils.round(roundResult.distance, 0)));
            time.setText(getResources().getString(R.string.time_s, FlagItUtils.getSecondsAndDecimal(roundResult.time)));
            if (roundResult.distance == -1) {
                distance.setText("");
                time.setText("t/o");
            }
        }


        double distanceTotal = 0;
        long timeTotal = 0;
        int scoreTotal = 0;
        for (RoundResult roundResult : match.results) {
            distanceTotal += roundResult.distance;
            timeTotal += roundResult.time;
            switch (roundResult.accuracy) {
                case RED:
                    scoreTotal += 3;
                    break;
                case ORANGE:
                    scoreTotal += 2;
                    break;
                case YELLOW:
                    scoreTotal += 1;
                    break;
                case BLACK:
                    scoreTotal += 0;
                    break;
                case TIME_OUT:
                    scoreTotal += 0;
                    break;
            }
        }

        TextView distance = (TextView) gameResultsTable.findViewById(R.id.game_result_total_distance);
        TextView time = (TextView) gameResultsTable.findViewById(R.id.game_result_total_time);
        TextView score = (TextView) gameResultsTable.findViewById(R.id.game_result_total_score);
        distance.setText(getResources().getString(R.string.distance_km, FlagItUtils.round(distanceTotal, 0)));
        time.setText(getResources().getString(R.string.time_s, FlagItUtils.getSecondsAndDecimal(timeTotal)));

        score.setText(getResources().getString(R.string.points_p, scoreTotal));

        boolean highScore = FlagItApplication.getInstance().putHighscore(match.gameType.name, scoreTotal, distanceTotal);
        TextView highscoreTV = (TextView) findViewById(R.id.high_score);

        if (highScore) {

            highscoreTV.setVisibility(View.VISIBLE);
        }
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        googleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            public boolean onMarkerClick(Marker marker) {
                //disable marker center click
                return true;
            }
        });
        try {
            // Customise the styling of the base map using a JSON object defined
            // in a raw resource file.
            boolean success = mMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                            this, R.raw.mapstyle));

            if (!success) {
                Log.e("MapsActivityRaw", "Style parsing failed.");
            }
        } catch (Resources.NotFoundException e) {
            Log.e("MapsActivityRaw", "Can't find style.", e);
        }
        //googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        // To Disable Zoom you can do2 the following.
        googleMap.getUiSettings().setScrollGesturesEnabled(false);
        googleMap.getUiSettings().setZoomControlsEnabled(false);
        googleMap.getUiSettings().setZoomGesturesEnabled(false);


        // http://stackoverflow.com/a/13800112/1418643
        //Trick to set bounds when map is loaded,
        final int padding = this.getResources().getDimensionPixelSize(R.dimen.map_marker_padding);
        mMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {
                // Move camera.
                mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
                // Remove listener to prevent position reset on camera move.
                mMap.setOnCameraIdleListener(null);
            }
        });


        for (RoundResult result : match.results) {
            FlagItUtils.drawPoly(result, mMap, this);
        }


    }

    private void createDrawable() {
        mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                GoogleMap.SnapshotReadyCallback callback = new GoogleMap.SnapshotReadyCallback() {

                    @Override
                    public void onSnapshotReady(Bitmap snapshot) {

                        Drawable mDrawable = new BitmapDrawable(getResources(), snapshot);
                        LinearLayout container = (LinearLayout) findViewById(R.id.map_container);
                        View mapView = container.getChildAt(0);
                        container.removeAllViews();

                        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                            container.setBackground(mDrawable);
                        } else {
                            //pre API 16
                            //noinspection deprecation
                            container.setBackgroundDrawable(mDrawable);
                        }


                        ViewGroup view = ((ViewGroup) findViewById(R.id.results_map_and_score));
                        Bitmap image = getBitmapFromView(view, view.getHeight(), view.getWidth());
                        ShareDialog shareDialog = new ShareDialog(MatchResultActivity.this);
                        if (ShareDialog.canShow(SharePhotoContent.class)) {

                            SharePhoto photo = new SharePhoto.Builder()
                                    .setBitmap(image)
                                    .build();
                            SharePhotoContent content = new SharePhotoContent.Builder()
                                    .addPhoto(photo)
                                    .build();
                            shareDialog.show(content);
                        }
                        container.removeAllViews();
                        container.addView(mapView);
                    }
                };
                mMap.snapshot(callback);
            }
        });
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_match_completed_ok:
                Intent intent = new Intent(MatchResultActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                break;
            case R.id.fb_share_button:
                createDrawable();
                break;
        }
    }


    private Bitmap getBitmapFromView(View view, int totalHeight, int totalWidth) {

        Bitmap returnedBitmap = Bitmap.createBitmap(totalWidth, totalHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(returnedBitmap);
        Drawable bgDrawable = view.getBackground();
        if (bgDrawable != null)
            bgDrawable.draw(canvas);
        else
            canvas.drawColor(Color.WHITE);
        view.draw(canvas);
        return returnedBitmap;
    }
}
