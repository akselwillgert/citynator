package se.subsurface.citynator;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.ui.IconGenerator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import se.subsurface.citynator.Model.Place;
import se.subsurface.citynator.Model.RoundResult;

public abstract class FlagItUtils {

    private static final String TAG = "FlagUtils";


    public static double CalculationByDistance(LatLng start, LatLng end) {
        return CalculationByDistance(start.latitude, start.longitude, end.latitude, end.longitude);
    }

    public static double CalculationByDistance(double StartLat, double StartLong, double EndLat, double EndLong) {
        int Radius = 6371;//radius of earth in Km

        double dLat = Math.toRadians(EndLat - StartLat);
        double dLon = Math.toRadians(EndLong - StartLong);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(StartLat)) * Math.cos(Math.toRadians(EndLat)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.asin(Math.sqrt(a));
        double valueResult = Radius * c;
        double km = valueResult / 1;
        DecimalFormat newFormat = new DecimalFormat("####");
        int kmInDec = Integer.valueOf(newFormat.format(km));
        double meter = valueResult % 1000;
        int meterInDec = Integer.valueOf(newFormat.format(meter));
        //   Log.i("Radius Value", "" + valueResult + "   KM  " + kmInDec + " Meter   " + meterInDec);

        return Radius * c;
    }

    public static String round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return Integer.toString(bd.intValue());
    }

    public static String getSecondsAndDecimal(long millisUntilFinished) {
        DecimalFormat df = new DecimalFormat("#.#"); //import java.text.DecimalFormat;
        return df.format(millisUntilFinished / 1000.0);
    }

    private static int randInt(int max) {
        int min = 0;
        // NOTE: Usually this should be a field rather than a method
        // variable so that it is not re-seeded every call.
        Random rand = new Random();

        // nextInt is normally exclusive of the top value,
        // so add 1 to make it inclusive

        return rand.nextInt((max - min) + 1) + min;
    }


    public static LatLngBounds getBounds(Cursor cursor) {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            int latitudeIndex = cursor.getColumnIndex("latitude");
            int longitudeIndex = cursor.getColumnIndex("longitude");
            double latitude = cursor.getDouble(latitudeIndex);
            double longitude = cursor.getDouble(longitudeIndex);
            builder.include(new LatLng(latitude, longitude));
        }

        return builder.build();
    }

    public static List<Place> getRandomPlaces(Cursor cursor, int numPlaces, double notCloserThanKm) {
        List<Place> places = new ArrayList<>();
        Log.d(TAG, "cursor.getCount()=" + cursor.getCount());

        while (places.size() < numPlaces) {
            int randomNumber = randInt(cursor.getCount() - 1);
            cursor.moveToPosition(randomNumber);
            int nameIndex = cursor.getColumnIndex("asciiname");
            int adminIndex = cursor.getColumnIndex("admin1");
            // int cc2Index = cursor.getColumnIndex("cc2");
            int countryIndex = cursor.getColumnIndex("country");
            //  int populationIndex = cursor.getColumnIndex("population");

            int latitudeIndex = cursor.getColumnIndex("latitude");
            int longitudeIndex = cursor.getColumnIndex("longitude");

            String name = cursor.getString(nameIndex);
            String admin = cursor.getString(adminIndex);
            String country = cursor.getString(countryIndex);
            //    int population = cursor.getInt(populationIndex);
            double latitude = cursor.getDouble(latitudeIndex);
            double longitude = cursor.getDouble(longitudeIndex);
            String countryCode = FlagItApplication.getInstance().getCountryCode(country);


            //Add if not to near already picked places
            boolean toClose = false;
            for (Place place : places) {
                if (checkIfToClose(latitude, longitude, place.latitude, place.longitude, notCloserThanKm)) {
                    toClose = true;
                    break;
                }
            }
            if (!toClose) {
                Place place = new Place(name, admin, country, latitude, longitude, countryCode);
                places.add(place);
            }

        }

        return places;
    }

    private static boolean checkIfToClose(double lat1, double long1, double lat2, double long2, double notCloserThenKM) {
        double distance = CalculationByDistance(lat1, long1, lat2, long2);
        return distance < notCloserThenKM;
    }

    public static int getAccuracyColor(Context context, RoundResult.Accuracy accuracy) {
        int accuracyColor = 0;
        switch (accuracy) {
            case RED:
                accuracyColor = R.color.red_900;
                break;
            case ORANGE:
                accuracyColor = R.color.orange_900;
                break;
            case YELLOW:
                accuracyColor = R.color.yellow_500;
                break;
            case BLACK:
                accuracyColor = R.color.Black;
                break;
            case TIME_OUT:
                accuracyColor = R.color.grey_400;
                break;
        }
        return ContextCompat.getColor(context, accuracyColor);
    }


    public static void drawPoly(RoundResult roundResult, GoogleMap mMap, Context context) {
        LatLng cityLocation = new LatLng(roundResult.place.latitude, roundResult.place.longitude);
        IconGenerator iconFactory = new IconGenerator(context);
        iconFactory.setBackground(new ColorDrawable(Color.TRANSPARENT));
        //Distance
        double distance = FlagItUtils.CalculationByDistance(roundResult.clickLat, roundResult.clickLong, roundResult.place.latitude, roundResult.place.longitude);


        @SuppressLint("InflateParams")//ignoring nullparent here, as view is used for creating bitmap
                TextView cityView = (TextView) LayoutInflater.from(context).inflate(R.layout.city_marker, null);
        cityView.setText(roundResult.place.name);
        iconFactory.setContentView(cityView);
        //place City marker if not already created
        MarkerOptions placeMarkerOptions = new MarkerOptions().
                position(cityLocation).
                flat(true).
                anchor(0.5f, 0.75f).
                icon(BitmapDescriptorFactory.fromBitmap(iconFactory.makeIcon(roundResult.place.name)));
        mMap.addMarker(placeMarkerOptions);

        //Click marker
        int accuracyColor = FlagItUtils.getAccuracyColor(context, roundResult.accuracy);
        @SuppressLint("InflateParams") //ignoring nullparent here, as view is used for creating bitmap
                TextView clickMarkerTextView = (TextView) LayoutInflater.from(context).inflate(R.layout.click_marker, null);
        clickMarkerTextView.setText(FlagItUtils.round(distance, 0));
        clickMarkerTextView.setTextColor(accuracyColor);

        Drawable[] drawables = clickMarkerTextView.getCompoundDrawables();
        for (Drawable drawable : drawables) {
            if (drawable != null) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    drawable.setTint(accuracyColor);
                } else {
                    Drawable wrapedDrawable = DrawableCompat.wrap(drawable);
                    DrawableCompat.setTint(wrapedDrawable, accuracyColor);
                    clickMarkerTextView.setCompoundDrawables(null, null, null, drawable);
                }
            }
        }

        iconFactory.setContentView(clickMarkerTextView);
        iconFactory.setBackground(new ColorDrawable(Color.TRANSPARENT));
        Bitmap bm = iconFactory.makeIcon();
        MarkerOptions clickMarkerOptions = new MarkerOptions()
                .flat(true)
                .icon(BitmapDescriptorFactory.fromBitmap(bm))
                .position(new LatLng(roundResult.clickLat, roundResult.clickLong))
                .anchor(0.5f, 0.75f);

        mMap.addMarker(clickMarkerOptions);

        //The line between
        if (roundResult.accuracy != RoundResult.Accuracy.TIME_OUT) {
            PolylineOptions line =
                    new PolylineOptions()
                            .add(new LatLng(roundResult.clickLat, roundResult.clickLong), cityLocation)
                            .width(5)
                            .color(accuracyColor)
                            .geodesic(true);

            mMap.addPolyline(line);
        }

    }

}

