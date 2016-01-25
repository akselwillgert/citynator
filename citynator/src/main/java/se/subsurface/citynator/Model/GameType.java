package se.subsurface.citynator.Model;

import java.io.Serializable;

public class GameType implements Serializable {

    public final String name;
    public final String Sql;
    public final int imageId;


    public GameType(String name, String sql, int imageId) {
        this.name = name;
        Sql = sql;
        this.imageId = imageId;
    }
}
