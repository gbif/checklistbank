package org.gbif.checklistbank.model;

import java.util.Objects;

public class Citation {
    private Integer key;
    private String citation;
    private String link;
    private String doi;

    public Citation() {
    }

    public Citation(String citation) {
        this.citation = citation;
    }

    public Integer getKey() {
        return key;
    }

    public void setKey(Integer key) {
        this.key = key;
    }

    public String getCitation() {
        return citation;
    }

    public void setCitation(String citation) {
        this.citation = citation;
    }

    public String getDoi() {
        return doi;
    }

    public void setDoi(String doi) {
        this.doi = doi;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, citation, link, doi);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final Citation other = (Citation) obj;
        return Objects.equals(this.key, other.key)
                && Objects.equals(this.citation, other.citation)
                && Objects.equals(this.link, other.link)
                && Objects.equals(this.doi, other.doi);
    }
}
