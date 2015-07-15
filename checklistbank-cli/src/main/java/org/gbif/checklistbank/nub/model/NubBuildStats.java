package org.gbif.checklistbank.nub.model;

public class NubBuildStats {

    private int addedUsages;
    private int deletedUsages;
    private int updatedUsages;

    public int getAddedUsages() {
        return addedUsages;
    }

    public void setAddedUsages(int addedUsages) {
        this.addedUsages = addedUsages;
    }

    public int getDeletedUsages() {
        return deletedUsages;
    }

    public void setDeletedUsages(int deletedUsages) {
        this.deletedUsages = deletedUsages;
    }

    public int getUpdatedUsages() {
        return updatedUsages;
    }

    public void setUpdatedUsages(int updatedUsages) {
        this.updatedUsages = updatedUsages;
    }
}
