package com.spuriufla.atividadereconciliacao;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivityCross extends AppCompatActivity {

    // Declaração das variáveis de interface
    private Button myButton;
    private LocationManager locationManager;
    private EditText numIdentXross, motoristaCross, tempoDesejadoCross, autonomiaCross, combustivelXross, itensSerXross;

    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_cross);

        // Referência à instância da atividade para ser usada dentro do OnClickListener
        final Activity activity = this;

        // Referencia o botão na interface pelo ID definido no layout (activity_main_cross.xml)
        myButton = findViewById(R.id.BTCalcularCross);
        myButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Verifica se os campos estão preenchidos corretamente
                if (validateFields()) {
                    // Quando o botão myButton é clicado, cria uma nova Intent para ir à atividade CalcularCross
                    Intent i = new Intent(MainActivityCross.this, CalcularCross.class);

                    // Extrai os valores inseridos pelo usuário nos campos de texto e adiciona-os como extras na Intent
                    int desiredTime = Integer.parseInt(tempoDesejadoCross.getText().toString());
                    float autonomy = Float.parseFloat(autonomiaCross.getText().toString());
                    float fuelCost = Float.parseFloat(combustivelXross.getText().toString());
                    int identificationNumber = Integer.parseInt(numIdentXross.getText().toString());
                    String driver = motoristaCross.getText().toString();
                    String serviceItems = itensSerXross.getText().toString();

                    // Adiciona os valores como extras na Intent
                    i.putExtra("tempoTotal", desiredTime);
                    i.putExtra("valorAutonomia", autonomy);
                    i.putExtra("valorCombustivel", fuelCost);
                    i.putExtra("numeroIdenticacao", identificationNumber);
                    i.putExtra("motorista", driver);
                    i.putExtra("itensServico", serviceItems);

                    // Inicia a atividade CalcularCross passando os valores como parâmetros
                    startActivity(i);

                    // Captura a última localização válida do dispositivo
                    // Verifica se o aplicativo tem permissão para acessar a localização
                    if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        // Se a permissão ainda não foi concedida, solicite-a
                        ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_LOCATION);
                    } else {
                        // Se a permissão já foi concedida, continue com a captura da localização
                        capturarUltimaLocalizacaoValida();
                    }
                }
            }
        });

        // Referencia os campos de texto na interface pelos IDs definidos no layout (activity_main_cross.xml)
        numIdentXross = findViewById(R.id.numIdentXross);
        motoristaCross = findViewById(R.id.motoristaCross);
        tempoDesejadoCross = findViewById(R.id.tempoDesejadoCross);
        autonomiaCross = findViewById(R.id.autonomiaCross);
        combustivelXross = findViewById(R.id.combustivelXross);
        itensSerXross = findViewById(R.id.itensSerXross);
    }

    // Método para validar os campos de texto
    private boolean validateFields() {
        String desiredTimeString = tempoDesejadoCross.getText().toString();
        String autonomyString = autonomiaCross.getText().toString();
        String fuelCostString = combustivelXross.getText().toString();
        String identificationNumberString = numIdentXross.getText().toString();
        String driverString = motoristaCross.getText().toString();
        String serviceItemsString = itensSerXross.getText().toString();

        if (desiredTimeString.isEmpty() || autonomyString.isEmpty() || fuelCostString.isEmpty() ||
                identificationNumberString.isEmpty() || driverString.isEmpty() || serviceItemsString.isEmpty()) {
            // Exibe uma mensagem informando ao usuário que todos os campos devem ser preenchidos
            Toast.makeText(MainActivityCross.this, "Preencha todos os campos", Toast.LENGTH_SHORT).show();
            return false;
        }

        try {
            // Tenta converter os valores para int e float uma vez e armazena-os em variáveis
            int desiredTime = Integer.parseInt(desiredTimeString);
            float autonomy = Float.parseFloat(autonomyString);
            float fuelCost = Float.parseFloat(fuelCostString);
            int identificationNumber = Integer.parseInt(identificationNumberString);

            // Os valores foram validados com sucesso, retorne true
            return true;
        } catch (NumberFormatException e) {
            // Tratamento de exceções caso os valores inseridos não sejam números válidos
            Toast.makeText(MainActivityCross.this, "Valores inválidos. Certifique-se de digitar apenas números válidos.", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            return false;
        }
    }

    // Método para capturar a última localização válida do dispositivo
    private void capturarUltimaLocalizacaoValida() {
        // Verifica se a permissão de acesso à localização foi concedida
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {

            // Verifica se o provedor de localização GPS está disponível
            locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                AtualizaLocalizacao atualizaLoc = new AtualizaLocalizacao();
                // Solicita atualizações de localização ao LocationManager através da classe AtualizaLocalizacao
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, atualizaLoc);
            } else {
                // Exibe uma mensagem informativa ao usuário caso o GPS não esteja disponível
                Toast.makeText(MainActivityCross.this, "O provedor de localização GPS não está disponível.", Toast.LENGTH_SHORT).show();
            }

        } else {
            // Caso a permissão não tenha sido concedida, solicita permissão ao usuário
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }
}