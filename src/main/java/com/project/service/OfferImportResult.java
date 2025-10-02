package com.project.service;

import java.util.ArrayList;
import java.util.List;

public class OfferImportResult {
    private int offresCreees;
    private int lignesTraitees;
    private List<String> errors = new ArrayList<>();

    public int getOffresCreees() { return offresCreees; }
    public void setOffresCreees(int offresCreees) { this.offresCreees = offresCreees; }

    public int getLignesTraitees() { return lignesTraitees; }
    public void setLignesTraitees(int lignesTraitees) { this.lignesTraitees = lignesTraitees; }

    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }
}
