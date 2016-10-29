package se.subsurface.citynator.Model;

import com.google.android.gms.maps.model.LatLngBounds;

import java.io.Serializable;

import se.subsurface.citynator.FlagItUtils;

public class RoundResult implements Serializable {
    public final Place place;
    public final Accuracy accuracy;
    public long time;
    public double clickLat = 0;
    public double clickLong = 0;
    public double distance = -1;
    public boolean below = false;

    RoundResult(Place place) {
        this.place = place;
        accuracy = Accuracy.TIME_OUT;
        distance = -1;
        time = -1;
    }

    RoundResult(Place place, double clickLat, double clickLong, long time, LatLngBounds bounds) {

        this.place = place;
        this.clickLat = clickLat;
        this.clickLong = clickLong;
        this.time = time;
        if (clickLong != 0) {
            this.distance = FlagItUtils.CalculationByDistance(clickLat, clickLong, place.latitude, place.longitude);
        }

        double boundsDistance = FlagItUtils.CalculationByDistance(bounds.southwest, bounds.northeast);
        if (clickLat < place.latitude) {
            below = true;
        }
        if (clickLat == -1 && clickLong == -1) {
            distance = -1;
            this.time = -1;
            accuracy = Accuracy.TIME_OUT;
        } else if (distance < boundsDistance * 0.05) {
            accuracy = Accuracy.RED;
        } else if (distance < boundsDistance * 0.2) {
            accuracy = Accuracy.ORANGE;
        } else if (distance < boundsDistance * 0.4) {
            accuracy = Accuracy.YELLOW;
        } else {
            accuracy = Accuracy.BLACK;
        }
    }

    @Override
    public String toString() {
        return "RoundResult{" +
                "place=" + place +
                ", time=" + time +
                ", distance=" + distance +
                '}';
    }

    public enum Accuracy {
        RED, ORANGE, YELLOW, BLACK, TIME_OUT
    }
}
