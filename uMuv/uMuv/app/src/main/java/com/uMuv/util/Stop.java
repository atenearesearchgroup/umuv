package com.uMuv.util;

public class Stop {
    private int idStop, codStop;
    private String name, url;
    private double lat, lon;

    public Stop(String idStop, String codStop, String name, String lat, String lon, String url) {
        this.idStop = Integer.parseInt(idStop);
        this.codStop = Integer.parseInt(codStop);
        this.name = name;
        this.lat = Double.parseDouble(lat);
        this.lon = Double.parseDouble(lon);
        this.url = url;
    }

    public int getIdStop() {
        return idStop;
    }

    public int getCodStop() {
        return codStop;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }


    @Override
    public String toString() {
        return "Stop{" +
                "idStop=" + idStop +
                ", codStop=" + codStop +
                ", name='" + name + '\'' +
                ", url='" + url + '\'' +
                ", lat=" + lat +
                ", lon=" + lon +
                '}';
    }
}
