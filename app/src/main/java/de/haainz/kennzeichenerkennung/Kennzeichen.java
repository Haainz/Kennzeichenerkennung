package de.haainz.kennzeichenerkennung;

import java.io.Serializable;

/**
 * Klasse Kennzeichen zum Speichern des Unterscheidungszeichens (Ortskürzel) und den
 * zugehörigen Ort bzw. die Behörde
 *
 * @author (Stefan Seegerer, Peter Brichzin)
 * @version (18.5.23)
 */
public class Kennzeichen implements Serializable
{
    public String oertskuerzel;
    public String stadtkreis;
    public String ort;
    public String bundesland;
    public String nationalitaetskuerzel;
    public String bundeslandiso;
    public String bemerkungen;
    public String fussnote = "6";
    public String saved;
    private String typ;

    /**
     * Konstruktor für Objekte der Klasse Kennzeichen
     */
    public Kennzeichen() {
        nationalitaetskuerzel = "---";
        bundeslandiso = "---";
        bundesland = "---";
        bemerkungen = "---";
        oertskuerzel = "";
        stadtkreis = "";
        ort = "-";
        typ = "";
    }

    public String OertskuerzelGeben(){
        return oertskuerzel;
    }

    public String StadtKreisGeben(){
        return stadtkreis;
    }

    public String BundeslandGeben(){
        return bundesland;
    }

    public String OrtGeben(){
        return ort;
    }

    public String getTyp() {
        return typ;
    }

    public String LandGeben(){
        return nationalitaetskuerzel;
    }

    public String BundeslandIsoGeben() {
        return bundeslandiso;
    }

    public String BemerkungenGeben() {
        return bemerkungen;
    }

    public String FussnoteGeben() {
        return fussnote;
    }

    public String SavedGeben() {
        return saved;
    }

    public void setTyp(String typ) {
        this.typ = typ;
    }

    public boolean isNormalDE() {
        return typ.equals("normal_de");
    }

    public boolean isSonderDE() {
        return typ.equals("sonder_de");
    }

    public boolean isAuslaufendDE() {
        return typ.equals("auslaufend_de");
    }

    public boolean isEigene() {
        return typ.equals("eigene");
    }

    public boolean isSaved() {
        return saved.equals("ja");
    }
}