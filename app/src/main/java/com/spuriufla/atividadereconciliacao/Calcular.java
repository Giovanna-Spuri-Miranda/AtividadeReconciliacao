package com.spuriufla.atividadereconciliacao;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.Toast;
import java.text.DecimalFormat;

public class Calcular extends AppCompatActivity {

    // Declaração das variáveis de interface
    private EditText Ed_consumo, Ed_valorCombustivel, Ed_VelRecomendada, Ed_VelMedia;
    private EditText Ed_distanciaTotal, Ed_DistPercorrida, Ed_DistRestante;
    private EditText Ed_LocalAtual, Ed_LocalDestino, Ed_tempoDesejado;
    private double consumo, velRecomendada, velMedia;
    private double distanciaRestante, distanciaTotal;
    private static double latitudeNova, longitudeNova;
    private static double latitudeDestino, longitudeDestino;
    private boolean setInicio = true, gpsAtivo = true;
    private float valorAutonomia, valorCombustivel;
    private LocationManager locationManager;
    private Chronometer cronometro;
    private long tempoRestante;
    private int tempoTotal;

    // Função chamada quando a atividade é criada
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calculo);

        // Referencia o botão na interface pelo ID definido no layout (activity_calculo.xml)
        Button myButton = findViewById(R.id.BTFinalizarCross);
        myButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Quando o botão myButton é clicado, cria uma nova Intent para ir à atividade inicial
                Intent i = new Intent(Calcular.this, inicial.class);
                startActivity(i);
            }
        });

        // Obtém os valores passados da Intent da atividade anterior
        tempoTotal = getIntent().getIntExtra("tempoTotal", 0);
        valorAutonomia = getIntent().getFloatExtra("valorAutonomia", 0);
        valorCombustivel = getIntent().getFloatExtra("valorCombustivel", 0);

        // Define a latitude e longitude do destino (pode ser alterado conforme necessário)
        latitudeDestino = -21.225691;
        longitudeDestino = -44.978236;

        // Inicialização dos campos de texto na interface
        // Localização
        Ed_LocalAtual = findViewById(R.id.localAtual);
        Ed_LocalDestino = findViewById(R.id.localDestino);
        // Destino
        Ed_distanciaTotal = findViewById(R.id.DistTotal);
        Ed_DistPercorrida = findViewById(R.id.DistPercorrida);
        Ed_DistRestante = findViewById(R.id.DistRestante);
        // Tempo
        Ed_tempoDesejado = findViewById(R.id.tempoDesejado);
        Ed_tempoDesejado.setText(tempoTotal + " min");
        cronometro = findViewById(R.id.cronometro);
        // Otimização
        Ed_consumo = findViewById(R.id.consumo);
        Ed_valorCombustivel = findViewById(R.id.custo);
        Ed_VelRecomendada = findViewById(R.id.VelRecomendada);
        Ed_VelMedia = findViewById(R.id.velMedia);

        // Inicialização do LocationManager para obter informações de localização
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        gpsAtivo = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);



        // Verifica se o GPS está ativo e captura a última localização válida
        if (gpsAtivo) {
            capturarUltimaLocalizacaoValida();
        } else {
            // Caso o GPS não esteja disponível, define latitudeNova e longitudeNova como 0.00
            latitudeNova = 0.00;
            longitudeNova = 0.00;
            Toast.makeText(this, "GPS não Disponível", Toast.LENGTH_LONG).show();
        }

        // Exibe a latitude e longitude do destino nos campos de texto na interface
        Ed_LocalDestino.setText("Lat.: " + formatarGeopoint(latitudeDestino) + " Long.: " + formatarGeopoint(longitudeDestino));

        // Calcula a distância total entre a localização atual e o destino
        distanciaTotal = calculoDistancia(latitudeNova, longitudeNova, latitudeDestino, longitudeDestino);

        // Calcula o tempo restante em segundos com base no tempoTotal em minutos
        tempoRestante = tempoTotal * 60;

        // Inicia a atualização do cronômetro
        myThread.start();
    }

    private Thread myThread = new Thread(new Runnable() {
        @Override
        public void run() {
            // Prepara o Looper para o thread atual
            Looper.prepare();

            // Loop enquanto o tempo restante for maior que 0
            while (tempoRestante > 0) {
                // Captura a última localização válida do dispositivo
                capturarUltimaLocalizacaoValida();

                if (latitudeNova != 0) {
                    // Calcula a distância restante até o destino
                    distanciaRestante = calculoDistancia(latitudeNova, longitudeNova, latitudeDestino, longitudeDestino);
                    // Calcula o consumo estimado em litros com base na distância restante e na autonomia do veículo
                    consumo = (distanciaRestante / 1000) / valorAutonomia;
                    // Calcula a velocidade recomendada em Km/h com base na distância restante e no tempo restante
                    velRecomendada = (distanciaRestante / tempoRestante) * 3.6;
                    // Atualiza o tempo restante com base no tempoTotal e no tempo já decorrido desde o início da contagem
                    tempoRestante = tempoTotal * 60 - (SystemClock.elapsedRealtime() - cronometro.getBase()) / 1000;

                    // O bloco de código dentro do if (setInicio) será executado apenas uma vez no início do loop
                    if (setInicio) {
                        // Calcula a distância total entre a localização atual e o destino (distância percorrida)
                        distanciaTotal = calculoDistancia(latitudeNova, longitudeNova, latitudeDestino, longitudeDestino);
                        // Inicia o cronômetro com o tempo decorrido desde o início da contagem
                        cronometro.setBase(SystemClock.elapsedRealtime());
                        // Calcula a velocidade média em Km/h com base na distância total e no tempoTotal
                        velMedia = (distanciaTotal / (tempoTotal * 60)) * 3.6;
                        // Indica que a parte inicial do loop já foi executada e não precisa ser repetida
                        setInicio = false;
                    }
                    // Inicia ou reinicia o cronômetro
                    cronometro.start();

                    // Atualiza a interface gráfica com as informações calculadas
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Ed_LocalAtual.setText("Lat.: " + formatarGeopoint(latitudeNova) + " Long.: " + formatarGeopoint(longitudeNova));
                            Ed_distanciaTotal.setText(String.format("%.2f", distanciaTotal) + " m");
                            Ed_DistRestante.setText(String.format("%.2f", distanciaRestante) + " m");
                            Ed_DistPercorrida.setText(String.format("%.2f", distanciaTotal - distanciaRestante) + " m");
                            Ed_consumo.setText(String.format("%.2f", consumo) + " Litros");
                            Ed_valorCombustivel.setText("R$ " + String.format("%.2f", consumo * valorCombustivel));
                            Ed_VelMedia.setText(String.format("%.2f", velMedia) + " Km/h");
                            Ed_VelRecomendada.setText(String.format("%.2f", velRecomendada) + " Km/h");
                        }
                    });
                }

                // Aguarda 1 segundo antes de executar novamente o loop
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                    // Lança uma exceção caso ocorra um erro durante o sono da thread
                    throw new RuntimeException(e);
                }
            }

            // Quando o tempo restante for menor ou igual a 0, o loop é encerrado
            // O cronômetro é parado e exibe uma mensagem para o usuário
            cronometro.stop();

            if (tempoRestante <= 0) {
                if (distanciaRestante < 30) {
                    // O usuário chegou ao destino dentro do prazo
                    Toast.makeText(Calcular.this, "Parabéns! Você chegou ao Destino dentro do prazo!", Toast.LENGTH_LONG).show();
                } else {
                    // O usuário não chegou ao destino no tempo determinado
                    Toast.makeText(Calcular.this, "Você não chegou ao destino no tempo determinado.", Toast.LENGTH_LONG).show();
                }
            }
        }
    });

    // Método para capturar a última localização válida do dispositivo
    private void capturarUltimaLocalizacaoValida() {
        // Verifica se o GPS está ativado
        gpsAtivo = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (gpsAtivo) {
            // Verifica se a permissão de acesso à localização foi concedida
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED) {

                // Cria uma instância da classe AtualizaLocalizacao para atualizar a localização
                AtualizaLocalizacao atualizaLoc = new AtualizaLocalizacao();

                // Solicita atualizações de localização ao LocationManager através da classe AtualizaLocalizacao
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, atualizaLoc);

                // Obtém a latitude e longitude atualizadas
                latitudeNova = atualizaLoc.getLatitude();
                longitudeNova = atualizaLoc.getLongitude();

            } else {
                // Caso a permissão não tenha sido concedida, solicita permissão ao usuário
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }
    }

    // Método para formatar um valor double representando a latitude ou longitude
    private String formatarGeopoint(double valor) {
        DecimalFormat decimalFormat = new DecimalFormat("#.####");
        return decimalFormat.format(valor);
    }

    // Método para calcular a distância entre duas coordenadas geográficas (latitude e longitude)
    private double calculoDistancia(double lat1, double long1, double lat2, double long2) {
        // Vetor para armazenar a distância calculada
        float[] dist = new float[1];

        // Calcula a distância entre os pontos utilizando a função estática do Android Location
        Location.distanceBetween(lat1, long1, lat2, long2, dist);

        // A função retorna a distância em metros (índice 0 do vetor)
        return dist[0];
    }
}
