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
package org.gbif.checklistbank.model;

import java.util.Date;
import java.util.UUID;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

/**
 * Different model for a verbatim name usage record as it is stored in postgres and used in the mybatis DAO layer.
 */
public class RawUsage {
    private Integer usageKey;
    private UUID datasetKey;
    private String json;
    private Date lastCrawled;

    public Integer getUsageKey() {
        return usageKey;
    }

    public void setUsageKey(Integer usageKey) {
        this.usageKey = usageKey;
    }

    public UUID getDatasetKey() {
        return datasetKey;
    }

    public void setDatasetKey(UUID datasetKey) {
        this.datasetKey = datasetKey;
    }

    public Date getLastCrawled() {
        return lastCrawled;
    }

    public void setLastCrawled(Date lastCrawled) {
        this.lastCrawled = lastCrawled;
    }

    public String getJson() {
        return json;
    }

    public void setJson(String json) {
        this.json = json;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof RawUsage) {
            RawUsage that = (RawUsage) obj;
            return Objects.equal(this.usageKey, that.usageKey)
                    && Objects.equal(this.datasetKey, that.datasetKey)
                    && Objects.equal(this.lastCrawled, that.lastCrawled)
                    && Objects.equal(this.json, that.json);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(usageKey, datasetKey, json, lastCrawled);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("usageKey", usageKey)
                .add("datasetKey", datasetKey)
                .add("json", json)
                .add("lastCrawled", lastCrawled)
                .toString();
    }
}
