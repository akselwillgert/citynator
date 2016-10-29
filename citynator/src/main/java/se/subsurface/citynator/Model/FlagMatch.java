package se.subsurface.citynator.Model;


import android.content.res.Resources;
import android.database.Cursor;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import se.subsurface.citynator.FlagItApplication;
import se.subsurface.citynator.FlagItUtils;
import se.subsurface.citynator.R;

public class FlagMatch implements Serializable {

    private static final String TAG = "FlagMatch";

    public final List<RoundResult> results = new ArrayList<>();
    transient final public LatLngBounds bounds;
    final public List<Place> places;
    public final int round_time_ms;
    public final int count_down_ms;
    public final int tick_interval_ms;
    public final GameType gameType;
    private final int numRounds;
    public int currentRound = 0;
    public boolean clicked = false;
    public boolean roundInProgress = false;
    public MatchState matchState = MatchState.NOT_STARTED;

    public FlagMatch(Resources res, GameType gameType) {
        this.gameType = gameType;
        Cursor cityCursor = FlagItApplication.getInstance().db.sql(gameType.Sql);
        Log.d(TAG, "cityCursor.count=" + cityCursor.getCount());
        bounds = FlagItUtils.getBounds(cityCursor);
        double boundsDistance = FlagItUtils.CalculationByDistance(bounds.southwest, bounds.northeast);
        double toCloseKm = boundsDistance * 0.05;
        Log.d(TAG, "toCloseKm=" + toCloseKm);
        this.numRounds = res.getInteger(R.integer.num_rounds);
        this.round_time_ms = res.getInteger(R.integer.round_time_ms);
        this.count_down_ms = res.getInteger(R.integer.round_count_down_ms);
        this.tick_interval_ms = res.getInteger(R.integer.tick_intervall_millis);
        places = FlagItUtils.getRandomPlaces(cityCursor, this.numRounds, toCloseKm);
        cityCursor.close();
    }

    public RoundResult roundCompleted(LatLng click, long timeEllapsed) {
        roundInProgress = false;
        RoundResult result = new RoundResult(places.get(currentRound), click.latitude, click.longitude, timeEllapsed, bounds);
        currentRound++;
        results.add(result);
        if (currentRound == this.numRounds) {
            matchState = MatchState.COMPLETED;
        }
        return result;
    }

    public RoundResult roundTimeout() {
        roundInProgress = false;
        RoundResult result = new RoundResult(places.get(currentRound));
        currentRound++;
        results.add(result);
        if (currentRound == this.numRounds) {
            matchState = MatchState.COMPLETED;
        }
        return result;
    }

    @Override
    public String toString() {
        return "FlagMatch{" +
                ", bounds=" + bounds +
                ", currentRound=" + currentRound +
                ", clicked=" + clicked +
                ", roundInProgress=" + roundInProgress +
                ", matchState=" + matchState +
                '}';
    }


    public enum MatchState {
        NOT_STARTED, IN_PROGRESS, COMPLETED
    }
}
