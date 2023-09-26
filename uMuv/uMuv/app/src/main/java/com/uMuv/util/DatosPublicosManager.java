package com.uMuv.util;

import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.uMuv.app.MainActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public class DatosPublicosManager {
    private static DatosPublicosManager instance = null;
    private static final String PARADAS_LINEAS = "https://datosabiertos.malaga.eu/recursos/transporte/EMT/EMTLineasYParadas/lineasyparadas.geojson";
    private static final String BUSES = "https://datosabiertos.malaga.eu/recursos/transporte/EMT/EMTlineasUbicaciones/lineasyubicaciones.geojson";
    private static final String CSV_PARADAS_LINEAS = "https://datosabiertos.malaga.eu/recursos/transporte/EMT/lineasYHorarios/stops.csv";
    private static final int EARTH_R = 6371 * 1000;

    private String paradasLineas, buses;
    private List<String[]> paradasParsed, busesParsed;
    private List<Stop> stopList;
    private List<Bus> busList;
    private DatosPublicosManager (){
        this.paradasLineas = new String();
        this.buses = new String();
        this.paradasParsed = null;
        this.busesParsed = null;
        this.busList = new ArrayList<>();
        this.stopList = new ArrayList<>();
    }

    public static DatosPublicosManager getSharedInstance() {
        if (instance == null) {
            instance = new DatosPublicosManager();
        }

        return instance;
    }

    protected void setParadasLineas(String s){
        this.paradasLineas = s;
    }
    public String getParadasLineas(){
        return this.paradasLineas;
    }

    protected void setParadasParsed(List<String[]> s){
        this.paradasParsed = s;
    }
    public List<String[]> getParadasParsed(){
        return this.paradasParsed;
    }

    protected void setBuses(String s){
        this.buses = s;
    }

    public String getBuses(){
        return this.buses;
    }

    public void getUrlParadasLineas(){
        StringRequest request = new StringRequest(CSV_PARADAS_LINEAS, new Response.Listener<String>() {
            @Override
            public void onResponse(String string) {
                CSVReader reader = new CSVReader(new StringReader(string));

                DatosPublicosManager.getSharedInstance().setParadasLineas(string);
                try {
                    List<String []> all = reader.readAll();
                    for(String[] stop : all.subList(1, all.size())){
                        Stop newStop = new Stop(stop[0], stop[1], stop[2], stop[3], stop[4], stop[5]);
                        stopList.add(newStop);
                    }
                    DatosPublicosManager.getSharedInstance().setParadasParsed(all.subList(1,all.size()));
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (CsvException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
            }
        });

        RequestQueue rQueue = Volley.newRequestQueue(MyApplication.getAppContext());
        rQueue.add(request);
    }

    public void getUrlParadasLineasWithCallback(final VolleyCallBack callBack){
        StringRequest request = new StringRequest(CSV_PARADAS_LINEAS, new Response.Listener<String>() {
            @Override
            public void onResponse(String string) {
                CSVReader reader = new CSVReader(new StringReader(string));

                DatosPublicosManager.getSharedInstance().setParadasLineas(string);
                try {
                    List<String []> all = reader.readAll();
                    for(String[] stop : all.subList(1, all.size())){
                        Stop newStop = new Stop(stop[0], stop[1], stop[2], stop[3], stop[4], stop[5]);
                        stopList.add(newStop);
                    }
                    DatosPublicosManager.getSharedInstance().setParadasParsed(all.subList(1,all.size()));
                    callBack.onSuccess();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (CsvException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
            }
        });

        RequestQueue rQueue = Volley.newRequestQueue(MyApplication.getAppContext());
        rQueue.add(request);
    }

    public void getUrlBuses(final VolleyCallBack callBack){
        StringRequest request = new StringRequest(BUSES, new Response.Listener<String>() {
            @Override
            public void onResponse(String string) {
                DatosPublicosManager.getSharedInstance().setBuses(string);
                try {
                    JSONArray allBuses = new JSONArray(string);
                    List<String[]> busesP = new ArrayList<>();
                    if(getBusList() != null && getBusList().size() > 0)
                        setBusList(new ArrayList<>());
                    for(int i = 0; i < allBuses.length(); i++){
                        JSONObject busJSON = allBuses.getJSONObject(i);
                        String codBus = busJSON.getString("codBus");
                        String codLinea = busJSON.getString("codLinea");
                        JSONObject geometry = busJSON.getJSONObject("geometry");
                        JSONArray coordinates = geometry.getJSONArray("coordinates");
                        String lat = coordinates.get(1).toString();
                        String lon = coordinates.get(0).toString();
                        
                        Bus bus = new Bus(codBus, codLinea, lat, lon);
                        addBus(bus);

                    }
                    callBack.onSuccess();

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
            }
        });

        RequestQueue rQueue = Volley.newRequestQueue(MyApplication.getAppContext());
        rQueue.add(request);
    }


    public Stop getParadaInfo(int stopId){
        return stopList.stream().filter(stopToFind -> stopId == stopToFind.getIdStop()).findFirst().orElse(null);
    }

    public Bus getBusInfo(int busId){
        return busList.stream().filter(busToFind -> busId == busToFind.getCodBus()).findFirst().orElse(null);
    }

    public String parseBus(Bus bus){
        return "Bus Code: " + bus.getCodBus() + "\nLine: " + bus.getCodLinea() +  "\n(lat,lon): (" + bus.getLat() + ", " + bus.getLon() + ")";
    }

    public void addBus(Bus bus){
        this.busList.add(bus);
    }

    public List<Bus> getBusList() {
        return busList;
    }

    public void setBusList(List<Bus> busList) {
        this.busList = busList;
    }

    public void refreshBusList(final VolleyCallBack callBack){
        this.busList = new ArrayList<>();
        this.getUrlBuses(callBack);
        Log.e("DatosPublicosManager", "Refreshed Bus List " + this.getBusList().size());
    }

    public List<Stop> getStopList() {
        return stopList;
    }

    protected double getDistance(double lat1, double lon1, double lat2, double lon2){

        double dLat = deg2rad(lat2 - lat1);
        double dLon = deg2rad(lon2 - lon1);
        double a =
                Math.sin(dLat/2) * Math.sin(dLat/2) +
                        Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) *
                                Math.sin(dLon/2) * Math.sin(dLon/2)
                ;
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double d = this.EARTH_R * c;
        return d;
    }
    private double deg2rad(double deg){
        return deg * (Math.PI/180);
    }

}
