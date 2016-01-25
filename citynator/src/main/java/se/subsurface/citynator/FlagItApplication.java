package se.subsurface.citynator;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.util.Log;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import se.subsurface.citynator.Model.FlagMatch;
import se.subsurface.citynator.Model.GameType;


public class FlagItApplication extends Application {

    private static final String FLAGIT_PREFERENCES_V1 = "FlagIt_1";
    private static final String SCORE_PREFIX = "Score_";
    private static final String DISTANCE_PREFIX = "Distance_";
    private static final String TAG = "FlagItApplication";
    private static FlagItApplication INSTANCE;
    public final Set<GameType> gameTypes = new HashSet<>();
    private final Map<String, String> countryCodesMap = new HashMap<>();
    private final Map<String, Integer> highscores = new HashMap<>();
    private final Map<String, Double> bestDistance = new HashMap<>();
    public FlagMatch match;
    public CityDatabase db;
    private SharedPreferences prefs;

    public static FlagItApplication getInstance() {
        if (INSTANCE == null)
            INSTANCE = new FlagItApplication();
        return INSTANCE;
    }

    private void initGameTypes() {
        gameTypes.add(new GameType("Finland", "country = 'Finland'" + popString(5000), R.drawable.fi));
        gameTypes.add(new GameType("Sweden", "country = 'Sweden'" + popString(5000), R.drawable.se));
        gameTypes.add(new GameType("Pacific", "(timezone LIKE 'Pacific%' OR country = 'Australia')" + popString(50000), R.drawable.ic_oceania_orthographic_projection));
        gameTypes.add(new GameType("Africa", "timezone LIKE 'Africa%'" + popString(150000), R.drawable.ic_africa_orthographic_projection));
        gameTypes.add(new GameType("North America", "(country = 'United States' OR country = 'Canada')" + popString(150000), R.drawable.ic_northern_america_orthographic_projection));
        gameTypes.add(new GameType("Europe", "timezone LIKE 'Europe%'" + popString(150000), R.drawable.ic_europe_orthographic_projection));
        gameTypes.add(new GameType("Asia", "timezone LIKE 'Asia%'" + popString(400000), R.drawable.ic_asia_orthographic_projection));
        gameTypes.add(new GameType("South America", "timezone LIKE 'America%' AND (country != 'United States' AND country != 'Canada')" + popString(150000), R.drawable.ic_latin_america_orthographic_projection));
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
        initGameTypes();
        super.onCreate();

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

        for (GameType gameType : gameTypes) {
            int score = prefs.getInt(SCORE_PREFIX + gameType.name, -1);
            double distance = prefs.getFloat(DISTANCE_PREFIX + gameType.name, -1);
            bestDistance.put(gameType.name, distance);
            highscores.put(gameType.name, score);
        }

        db = new CityDatabase(this);

        initCountryCodeMap();


        Log.d(TAG, "onCreate return");
    }

    private String popString(int popLimit) {
        return " AND population > " + popLimit;
    }


    public GameType getGameType(String name) {
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

}
