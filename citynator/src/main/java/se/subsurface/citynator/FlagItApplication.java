package se.subsurface.citynator;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.util.Log;

import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import se.subsurface.citynator.Model.FlagMatch;
import se.subsurface.citynator.Model.GameType;


public class FlagItApplication extends Application {

    private static final String FLAGIT_PREFERENCES_V1 = "FlagIt_1";
    private static final String SCORE_PREFIX = "Score_";
    private static final String DISTANCE_PREFIX = "Distance_";
    private static final String TAG = "FlagItApplication";
    private static FlagItApplication INSTANCE;

    private final Map<String, String> countryCodesMap = new HashMap<>();
    private final Map<String, Integer> highscores = new HashMap<>();
    private final Map<String, Double> bestDistance = new HashMap<>();
    public FlagMatch match;
    public CityDatabase db;
    private SharedPreferences prefs;
    private int soundSuccessId = 0;
    private int soundMissId = 0;
    private int soundAlmostId = 0;
    private int soundTimeoutId = 0;
    private SoundPool mSounds;

    public static FlagItApplication getInstance() {
        if (INSTANCE == null)
            INSTANCE = new FlagItApplication();
        return INSTANCE;
    }

    public List<GameType> getCountries() {
        List<GameType> gameTypes = new ArrayList<>();
        //Countries
        gameTypes.add(new GameType("China", "country = 'China'" + popString(200000), R.drawable.cn));
        gameTypes.add(new GameType("Finland", "country = 'Finland'" + popString(3000), R.drawable.fi));
        gameTypes.add(new GameType("Norway", "country = 'Norway'" + popString(3000), R.drawable.no));
        gameTypes.add(new GameType("Sweden", "country = 'Sweden'" + popString(4000), R.drawable.se));
        gameTypes.add(new GameType("Poland", "country = 'Poland'" + popString(40000), R.drawable.pl));
        gameTypes.add(new GameType("Germany", "country = 'Germany'" + popString(60000), R.drawable.de));

        return gameTypes;
    }

    public List<GameType> getRegions() {
        List<GameType> gameTypes = new ArrayList<>();
        //Regions
        gameTypes.add(new GameType("Pacific", "latitude between -47 AND -2 AND longitude between 112 AND 180" + popString(30000), R.drawable.ic_oceania_orthographic_projection));
        gameTypes.add(new GameType("Africa", "timezone LIKE 'Africa%'" + popString(150000), R.drawable.ic_africa_orthographic_projection));
        gameTypes.add(new GameType("North America", "(country = 'United States' OR country = 'Canada')" + popString(100000), R.drawable.ic_northern_america_orthographic_projection));
        gameTypes.add(new GameType("Europe", "timezone LIKE 'Europe%' AND longitude < 44.7" + popString(150000), R.drawable.ic_europe_orthographic_projection));
        gameTypes.add(new GameType("Asia", "latitude between 5 AND 55 AND longitude between 60 AND 147" + popString(400000), R.drawable.ic_asia_orthographic_projection));
        gameTypes.add(new GameType("Middle East", "latitude between 12 AND 47 AND longitude between 32 AND 60" + popString(200000), R.drawable.ic_asia_orthographic_projection));
        gameTypes.add(new GameType("South America", "longitude between -82 AND -34 AND latitude between -56 AND 13" + popString(100000), R.drawable.ic_latin_america_orthographic_projection));
        gameTypes.add(new GameType("Central America", "latitude between 0 AND 32.4 AND longitude between -100 AND -59 AND country != 'United States'" + popString(50000), R.drawable.ic_latin_america_orthographic_projection));
        gameTypes.add(new GameType("South East Asia", "latitude between -12 AND 18 AND longitude between 95 AND 141" + popString(150000), R.drawable.ic_asia_orthographic_projection));
        return gameTypes;
    }

    private void initCountryCodeMap() {
        Cursor cursor = db.getCountryCodes();

        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            int isoIndex = cursor.getColumnIndex("ISO");
            int countryIndex = cursor.getColumnIndex("Country");
            String iso = cursor.getString(isoIndex);
            String country = cursor.getString(countryIndex);
            countryCodesMap.put(country, iso);
        }
        cursor.close();
    }

    public String getCountryCode(String country) {
        return countryCodesMap.get(country);
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate Enter");
        INSTANCE = this;

        super.onCreate();
        FacebookSdk.sdkInitialize(getApplicationContext());
        AppEventsLogger.activateApp(this);
        //Universal Image Loader
        DisplayImageOptions defaultOptions = new DisplayImageOptions.Builder()
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .build();
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(getApplicationContext())
                .defaultDisplayImageOptions(defaultOptions)
                .build();
        ImageLoader.getInstance().init(config);
        prefs = this.getSharedPreferences(FLAGIT_PREFERENCES_V1, Context.MODE_PRIVATE);

        List<GameType> gameTypes = getCountries();
        gameTypes.addAll(getRegions());
        for (GameType gameType : gameTypes) {
            int score = prefs.getInt(SCORE_PREFIX + gameType.name, -1);
            double distance = prefs.getFloat(DISTANCE_PREFIX + gameType.name, -1);
            bestDistance.put(gameType.name, distance);
            highscores.put(gameType.name, score);
        }

        db = new CityDatabase(this);

        initCountryCodeMap();
        initSounds(this);

        Log.d(TAG, "onCreate return");
    }

    private String popString(int popLimit) {
        return " AND population > " + popLimit;
    }

    public GameType getGameType(String name) {
        List<GameType> gameTypes = getCountries();
        gameTypes.addAll(getRegions());
        for (GameType gameType : gameTypes) {
            if (gameType.name.equals(name)) {
                return gameType;
            }
        }
        throw new RuntimeException("Failed to find gametype");
    }

    public boolean putHighscore(String name, int score, double distance) {
        boolean highscore = false;
        if (score >= getHighscore(name)) {
            if (score > getHighscore(name) //score is bigger than old, ignore previous distance
                    || getDistance(name) == -1 // no previous distance
                    || distance < getDistance(name)) { // better than old distance
                highscore = true;
                highscores.put(name, score);
                bestDistance.put(name, distance);

                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt(SCORE_PREFIX + name, score);
                editor.putFloat(DISTANCE_PREFIX + name, Double.valueOf(distance).floatValue());
                editor.apply();
            }
        }
        return highscore;
    }

    public Integer getHighscore(String name) {
        return highscores.get(name);
    }

    public Double getDistance(String name) {
        Double distance = bestDistance.get(name);
        if (distance == null) {
            return (double) -1;
        }
        return distance;

    }

    private void initSounds(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AudioAttributes attributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            mSounds = new SoundPool.Builder()
                    .setAudioAttributes(attributes)
                    .build();
        } else {
            //noinspection deprecation
            mSounds = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);
        }

        /** soundId for Later handling of sound pool **/
        soundSuccessId = mSounds.load(context, R.raw.correct, 1);
        soundMissId = mSounds.load(context, R.raw.miss, 1);
        soundAlmostId = mSounds.load(context, R.raw.close, 1);
        soundTimeoutId = mSounds.load(context, R.raw.disconnected, 1);
    }

    public void playSuccess() {
        mSounds.play(soundSuccessId, 1, 1, 0, 0, 1);
    }

    public void playMiss() {
        mSounds.play(soundMissId, 1, 1, 0, 0, 1);
    }

    public void playAlmost() {
        mSounds.play(soundAlmostId, 1, 1, 0, 0, 1);
    }

    public void playTimeout() {
        mSounds.play(soundTimeoutId, 1, 1, 0, 0, 1);
    }
}
