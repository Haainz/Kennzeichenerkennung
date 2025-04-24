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

    public void setTyp(String typ) {
        this.typ = typ;
    }

    public boolean isNormal() {
        return typ.equals("normal");
    }

    public boolean isSonder() {
        return typ.equals("sonder");
    }

    public boolean isAuslaufend() {
        return typ.equals("auslaufend");
    }

    public boolean isEigene() {
        return typ.equals("eigene");
    }
}