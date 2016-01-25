package se.subsurface.citynator;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.util.Log;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

public class CityDatabase extends SQLiteAssetHelper {

    private static final String TAG = CityDatabase.class.getSimpleName();
    private static final String DATABASE_NAME = "geonames.db";
    private static final int DATABASE_VERSION = 2;

    public CityDatabase(Context context) {

        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        setForcedUpgrade();
    }

    public Cursor getCountryCodes() {
        SQLiteDatabase db = getReadableDatabase();
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        String sqlTables = "Country";
        String[] sqlSelect = {"geonameid",
                "Country",
                "ISO"};

        qb.setTables(sqlTables);
        return qb.query(db, sqlSelect, null, null,
                null, null, null);
    }

    public Cursor sql(String where) {
        Log.d(TAG, "where=" + where);
        String[] sqlSelect = {"geonameid",
                "asciiname",
                "admin1",
                "country",
                "population",
                "latitude",
                "longitude",
                "timezone"};
        SQLiteDatabase db = getReadableDatabase();
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        String sqlTables = "geoname_fulltext";
        String orderBy = "population DESC";
        String limitString = String.valueOf(500);
        qb.setTables(sqlTables);
        Cursor c = qb.query(db, sqlSelect, where, null,
                null, null, orderBy, limitString);

        c.moveToFirst();
        return c;
    }
}