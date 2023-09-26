package com.uMuv.util;

public class Bus {
    private int codBus, codLinea;
    private double lat, lon;

    public Bus (String codBus, String codLinea, String lat, String lon){
        this.codBus = Integer.parseInt(codBus);
        this.codLinea = (int)
                Double.parseDouble(codLinea);
        this.lat = Double.parseDouble(lat);
        this.lon = Double.parseDouble(lon);
    }

    public int getCodBus() {
        return codBus;
    }

    public void setCodBus(int codBus) {
        this.codBus = codBus;
    }

    public int getCodLinea() {
        return codLinea;
    }

    public void setCodLinea(int codLinea) {
        this.codLinea = codLinea;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    @Override
    public String toString() {
        return "Bus code:"  + getCodBus() + "\nLine: " + getCodLinea() +  "\n(lat,lon): (" + getLat() + ", " + getLon() + ")";
    }
}
