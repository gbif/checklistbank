package org.gbif.nub.mapdb;

import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Store;

import java.io.File;

public class MapDbUtils {

    public static DBMaker.Maker fileDB(File file) {
        return configureFile(DBMaker.fileDB(file));
    }

    public static DBMaker.Maker tmpFileDB() {
        return configureFile(DBMaker.tempFileDB());
    }

    private static DBMaker.Maker configureFile(DBMaker.Maker maker) {
        return maker
            .fileMmapEnableIfSupported()
            .fileMmapPreclearDisable();
    }

    public static void compact(HTreeMap<?, ?> map) {
        for (Store st : map.getStores()) {
            st.compact();
        }
    }

}
