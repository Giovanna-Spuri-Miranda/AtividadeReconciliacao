package com.spuriufla.atividadereconciliacao;

import android.location.Location;
import android.location.LocationListener;

public class AtualizaLocalizacao implements LocationListener {

    // Declaração das variáveis membros da classe para armazenar a latitude e a longitude
    private static double lat, longi;

    @Override
    public void onLocationChanged(Location location) {
        // Este método é chamado sempre que há uma mudança na localização do dispositivo
        // Ele é responsável por atualizar as variáveis lat e longi com a nova latitude e longitude.
        this.lat  = location.getLatitude();
        this.longi = location.getLongitude();
    }

    // Método getter para obter a latitude atual do dispositivo
    public double getLatitude(){
        return lat;
    }

    // Método getter para obter a longitude atual do dispositivo
    public double getLongitude(){
        return longi;
    }
}
