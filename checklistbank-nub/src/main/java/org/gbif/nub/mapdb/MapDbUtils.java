/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.nub.mapdb;

import java.io.File;

import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Store;

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
