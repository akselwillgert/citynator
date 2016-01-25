package se.subsurface.citynator.Model;

import java.io.Serializable;
import java.util.Locale;

public class Place implements Serializable {
    public final String name;
    public final String country;
    public final String admin;
    public final double latitude;
    public final double longitude;
    public final String countryCode;

    public Place(String name, String admin, String country, double latitude, double longitude, String countryCode) {
        this.name = name;
        this.admin = admin;
        this.country = country;
        this.latitude = latitude;
        this.longitude = longitude;
        this.countryCode = countryCode.toLowerCase(Locale.getDefault());
    }
}
