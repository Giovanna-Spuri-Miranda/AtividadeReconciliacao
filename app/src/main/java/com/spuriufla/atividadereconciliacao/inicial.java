package com.spuriufla.atividadereconciliacao;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class inicial extends AppCompatActivity {

    private Button myButton1;
    private Button myButton2;
    private LocationManager locationManager;

    // Constante para o código de solicitação de permissão de localização.
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inicial);

        // Obtém referências para os botões definidos no layout.
        myButton1 = findViewById(R.id.BTSimples);
        myButton2 = findViewById(R.id.BTCrossDocking);

        // Configuração de OnClickListener para o botão myButton1.
        myButton1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Quando o botão myButton1 é clicado, cria uma nova Intent para ir para a classe MainActivity.
                startMainActivity();
            }
        });

        // Configuração de OnClickListener para o botão myButton2.
        myButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Quando o botão myButton2 é clicado, cria uma nova Intent para ir para a classe MainActivityCross.
                startMainActivityCross();
            }
        });
    }

    // Método para iniciar a MainActivity se a permissão de localização for concedida.
    private void startMainActivity() {
        if (checkLocationPermission()) {
            // Se a permissão de localização foi concedida, chama o método para capturar a última localização válida e inicia a MainActivity.
            capturarUltimaLocalizacaoValida();
            Intent i = new Intent(inicial.this, MainActivity.class);
            startActivity(i);
        }
    }

    // Método para iniciar a MainActivityCross se a permissão de localização for concedida.
    private void startMainActivityCross() {
        if (checkLocationPermission()) {
            // Se a permissão de localização foi concedida, chama o método para capturar a última localização válida e inicia a MainActivityCross.
            capturarUltimaLocalizacaoValida();
            Intent i = new Intent(inicial.this, MainActivityCross.class);
            startActivity(i);
        }
    }

    // Método para verificar se a permissão de localização foi concedida ou não.
    private boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Se a permissão não foi concedida, solicita a permissão ao usuário.
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return false;
        } else {
            // Caso a permissão já tenha sido concedida, retorna verdadeiro.
            return true;
        }
    }

    // Método para capturar a última localização válida do dispositivo.
    private void capturarUltimaLocalizacaoValida() {
        // Verifica se o GPS está ativo.
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null && !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            // Se o GPS estiver desligado, exibe um diálogo de alerta para o usuário ativá-lo.
            showGPSDisabledDialog();
            return;
        }

        // Se o GPS estiver ativado e a permissão de localização for concedida, solicita atualizações de localização ao LocationManager usando a classe AtualizaLocalizacao.
        AtualizaLocalizacao atualizaLoc = new AtualizaLocalizacao();
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, atualizaLoc);
    }

    // Método para exibir um diálogo de alerta se o GPS estiver desligado.
    private void showGPSDisabledDialog() {
        // Cria um objeto AlertDialog.Builder para construir o diálogo de alerta.
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        // Define a mensagem que será exibida no diálogo.
        alertDialogBuilder.setMessage("O GPS está desligado. Deseja ir para as configurações para ativá-lo?")

                // Impede que o usuário possa fechar o diálogo ao tocar fora da caixa de diálogo.
                .setCancelable(false)

                // Configura o botão "Configurações" do diálogo e define o que acontece ao clicá-lo.
                .setPositiveButton("Configurações", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Cria um objeto Intent para abrir a tela de configurações do GPS.
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);

                        // Inicia a atividade usando a Intent criada, abrindo as configurações do GPS.
                        startActivity(intent);
                    }
                })

                // Configura o botão "Cancelar" do diálogo e define o que acontece ao clicá-lo.
                .setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Cancela o diálogo, fechando-o.
                        dialog.cancel();
                    }
                });

        // Cria o objeto AlertDialog a partir do builder configurado acima.
        AlertDialog alert = alertDialogBuilder.create();

        // Exibe o diálogo de alerta.
        alert.show();
    }
}
