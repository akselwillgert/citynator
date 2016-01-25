package se.subsurface.citynator;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.plattysoft.leonids.ParticleSystem;

import de.greenrobot.event.EventBus;
import se.subsurface.citynator.Model.FlagMatch;
import se.subsurface.citynator.Model.GameType;
import se.subsurface.citynator.Model.Place;
import se.subsurface.citynator.Model.RoundResult;


public class MapsActivity extends FragmentActivity implements GoogleMap.OnMapClickListener, OnMapReadyCallback, View.OnClickListener {

    private static final String TAG = "MapsActivity";


    private static final String PRE_ROUND = "PreRound";
    private static final String TIMER = "Timer";
    private static final String BOUNDS = "Bounds";


    private GameType gameType;
    //Views
    private ImageView flagView;
    private ViewGroup mapOverlay;
    private GameTimer gameTimer;

    private MotionEvent ev;
    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private TextView mCountDown;
    private TextView mCityName;
    private TextView mAdmin;
    private TextView mCountry;
    private int padding;
    private View btnResults;
    private View btnGo;

    private FlagMatch match;
    private boolean resumed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate Enter savedInstanceState=" + savedInstanceState);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        //Find view stuff
        padding = this.getResources().getDimensionPixelSize(R.dimen.map_marker_padding);
        mapOverlay = (ViewGroup) findViewById(R.id.map_overlay);
        flagView = (ImageView) findViewById(R.id.flag);
        mCountDown = (TextView) findViewById(R.id.count_down);
        mCityName = (TextView) findViewById(R.id.city_name);
        mAdmin = (TextView) findViewById(R.id.admin_name);
        mCountry = (TextView) findViewById(R.id.country);

        btnResults = findViewById(R.id.maps_show_results);
        btnGo = findViewById(R.id.maps_btn_go);

        btnResults.setOnClickListener(this);
        btnGo.setOnClickListener(this);

        if (savedInstanceState != null) {
            boolean preRound = savedInstanceState.getBoolean(PRE_ROUND);
            long timeRemaining = savedInstanceState.getLong(TIMER);
            Log.d(TAG, "preRound=" + preRound + ", timeRemaining=" + timeRemaining);
            gameTimer = new GameTimer(timeRemaining, preRound);
            match = FlagItApplication.getInstance().match;
        } else {
            //this should propably be done in some destruct cycle, instead of startup
            FlagItApplication.getInstance().match = null;
        }

        EventBus.getDefault().register(this);

        //Area is used to create match
        String area = getIntent().getStringExtra("Area");
        gameType = FlagItApplication.getInstance().getGameType(area);
        Log.d(TAG, "onCreate Return");

        setUpMapIfNeeded();
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        FlagItApplication.getInstance().match = null;
        match = null;
        if (gameTimer != null) {
            gameTimer.cancel();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    private void updateCenterText() {
        if (gameTimer.preRound) {
            updateCenterText("Get Ready!", "", "", "");
        } else {
            Place place = match.places.get(match.currentRound);
            updateCenterText(place.name, place.admin, place.country, place.countryCode);
        }
    }

    private void updateCenterText(String city, String admin, String country, String countryCode) {
        int id = this.getResources().getIdentifier(countryCode, "drawable",
                this.getPackageName());
        flagView.setImageResource(id);
        mCityName.setText(city);
        mAdmin.setText(admin);
        mCountry.setText(country);
    }

    @Override
    public void onMapClick(LatLng click) {
        if (match != null && match.roundInProgress && !match.clicked) {
            match.clicked = true;
            RoundResult result = match.roundCompleted(click, gameTimer.timeEllapsed);
            FlagItUtils.drawPoly(result, mMap, this);
            int particleSize = 0;
            switch (result.accuracy) {
                case RED:
                    particleSize = 100;
                    break;
                case ORANGE:
                    particleSize = 30;
                    break;
                case YELLOW:
                    particleSize = 20;
                    break;
                case BLACK:
                    Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake2);
                    mapOverlay.startAnimation(shake);
                    break;
            }

            //Draw the explosion
            if (ev != null) {
                new ParticleSystem(this, particleSize, R.drawable.star_white, 800)
                        .setSpeedRange(0.1f, 0.25f)
                        .emit((int) ev.getX(), (int) ev.getY(), 1000, 100);
            }
            gameTimer.cancel();

            nextRound();
        }
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
        setUpMapIfNeeded();
    }

    private void setUpMapIfNeeded() {
        if (mMap != null) {
            resumeMatch();
        }
        SupportMapFragment mapFragment = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map));
        mapFragment.getMapAsync(this);


    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        googleMap.setOnMapClickListener(this);
        googleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            public boolean onMarkerClick(Marker marker) {
                //disable marker center click
                return true;
            }
        });
        googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        // To Disable Zoom you can do2 the following.
        googleMap.getUiSettings().setScrollGesturesEnabled(false);
        googleMap.getUiSettings().setZoomControlsEnabled(false);
        googleMap.getUiSettings().setZoomGesturesEnabled(false);

        resumeMatch();
    }

    private void resumeMatch() {
        Log.d(TAG, "resumeMatch() resumed=" + resumed + ", match=" + match);
        if (!resumed) {
            resumed = true;

            if (match == null) {
                btnGo.setVisibility(View.VISIBLE);
                btnResults.setVisibility(View.GONE);
                EventBus.getDefault().post(new EventStartMatch(gameType));
            } else {
                mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
                    @Override
                    public void onCameraChange(CameraPosition arg0) {
                        // Move camera.
                        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(match.bounds, padding));
                        // Remove listener to prevent position reset on camera move.
                        mMap.setOnCameraChangeListener(null);
                    }
                });
                switch (match.matchState) {
                    case NOT_STARTED:
                        btnGo.setVisibility(View.VISIBLE);
                        break;
                    case IN_PROGRESS:
                        btnGo.setVisibility(View.GONE);
                        btnResults.setVisibility(View.GONE);
                        //      setBounds(match.bounds, true);
                        updateCenterText();
                        //only start timer if match was started before
                        gameTimer.startIfNotStarted();
                        break;
                    case COMPLETED:
                        btnResults.setVisibility(View.VISIBLE);
                        btnGo.setVisibility(View.GONE);
                        break;
                }
            }
        } else {
            //Trigger the timer here, if it exists. needed when shutdown screen and return on
            if (gameTimer != null && match.matchState == FlagMatch.MatchState.IN_PROGRESS) {
                gameTimer.startIfNotStarted();
            }
        }
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (gameTimer != null) {
            gameTimer.cancel();
            gameTimer.started = false;
            if (gameTimer.millisUntilFinished > 0) {
                outState.putBoolean(PRE_ROUND, gameTimer.preRound);
                outState.putLong(TIMER, gameTimer.millisUntilFinished);
            }

            Log.d(TAG, "onSaveInstanceState() outstate=" + outState);
        }
        if (match != null) {
            outState.putParcelable(BOUNDS, match.bounds);
        }
    }

    public void onClick(View v) {
        Log.d(TAG, "onClick");
        switch (v.getId()) {
            case R.id.maps_show_results:
                Log.d(TAG, "matchComplete()");
                Intent intent = new Intent(this, MatchResultActivity.class);
                intent.putExtra("FlagMatch", match);
                intent.putExtra("Bounds", match.bounds);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                FlagItApplication.getInstance().match = null;
                match = null;
                startActivity(intent);
                break;
            case R.id.maps_btn_go:
                match.matchState = FlagMatch.MatchState.IN_PROGRESS;
                nextRound();
                v.setVisibility(View.GONE);
                break;
            default:
                new ParticleSystem(this, 100, R.drawable.star_white, 800)
                        .setSpeedRange(0.1f, 0.25f)
                        .oneShot(v, 100);

        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        this.ev = ev;
        return super.dispatchTouchEvent(ev);
    }

    @SuppressWarnings("unused")
    public void onEventBackgroundThread(EventStartMatch event) {
        Log.d(TAG, "ENTER onEventBackgroundThread");


        FlagMatch match = new FlagMatch(getResources(), event.gameType);
        EventBus.getDefault().post(new EventStartMatchResponse(match));
        Log.d(TAG, "RETURN onEventBackgroundThread");
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(EventStartMatchResponse event) {
        FlagItApplication.getInstance().match = event.match;
        this.match = event.match;

        //animate camera to show new area
        final CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(match.bounds, padding);
        mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                mMap.animateCamera(cu);
            }
        });
    }

    private void nextRound() {
        Log.d(TAG, "nextRound");
        if (match.matchState == FlagMatch.MatchState.COMPLETED) {
            btnResults.setVisibility(View.VISIBLE);
        } else {
            match.clicked = false;
            gameTimer = new GameTimer(match.count_down_ms, true);
            gameTimer.startIfNotStarted();
            updateCenterText();
        }

    }

    private class EventStartMatch {
        final GameType gameType;

        public EventStartMatch(GameType gameType) {
            this.gameType = gameType;
        }
    }

    private class EventStartMatchResponse {
        final FlagMatch match;

        public EventStartMatchResponse(FlagMatch match) {
            this.match = match;
        }
    }

    private class GameTimer extends CountDownTimer {
        public final boolean preRound;
        private final String TAG = "GameTimer";
        public long timeEllapsed = 0;
        public long millisUntilFinished;
        boolean started = false;

        public GameTimer(long time, boolean preRound) {
            super(time, match.tick_interval_ms);
            millisUntilFinished = time;
            this.preRound = preRound;
        }

        synchronized public void startIfNotStarted() {
            if (!started) {
                started = true;
                super.start();
            }

        }

        public void onTick(long millisUntilFinished) {

            timeEllapsed += match.tick_interval_ms;
            this.millisUntilFinished = millisUntilFinished;
            mCountDown.setText(FlagItUtils.getSecondsAndDecimal(millisUntilFinished));
        }

        @Override
        public void onFinish() {
            Log.d(TAG, "onFinish()");
            if (preRound) {
                Log.d(TAG, "StartRound()");
                match.roundInProgress = true;
                mMap.clear();
                gameTimer = new GameTimer(match.round_time_ms, false);
                gameTimer.startIfNotStarted();
                updateCenterText();
            } else {
                //No click in time, report timeout result
                match.roundTimeout();
                //drawPoly(result);
                nextRound();
            }
        }

        @Override
        public String toString() {
            return "GameTimer{" +
                    "timeEllapsed=" + timeEllapsed +
                    ", millisUntilFinished=" + millisUntilFinished +
                    ", preRound=" + preRound +
                    ", started=" + started +
                    "} " + super.toString();
        }
    }

}