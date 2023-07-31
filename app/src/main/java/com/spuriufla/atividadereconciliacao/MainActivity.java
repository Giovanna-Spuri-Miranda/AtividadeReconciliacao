package com.spuriufla.atividadereconciliacao;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    // Declaração das variáveis de interface
    private Button myButton;
    private EditText tempDesejado, autonomia, combustivel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Referencia os campos de texto na interface pelos IDs definidos no layout (activity_main.xml)
        tempDesejado = findViewById(R.id.tempDesejado);
        autonomia = findViewById(R.id.autonomia);
        combustivel = findViewById(R.id.combustivel);

        // Referencia o botão na interface pelo ID definido no layout (activity_main.xml)
        myButton = findViewById(R.id.BTSimples);
        myButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Quando o botão myButton é clicado, cria uma nova Intent para ir à atividade Calcular
                Intent i = new Intent(MainActivity.this, Calcular.class);

                // Extrai os valores inseridos pelo usuário nos campos de texto e adiciona-os como extras na Intent
                String desiredTimeString = tempDesejado.getText().toString();
                String autonomyString = autonomia.getText().toString();
                String fuelCostString = combustivel.getText().toString();

                // Validação dos campos de texto
                if (desiredTimeString.isEmpty() || autonomyString.isEmpty() || fuelCostString.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Preencha todos os campos", Toast.LENGTH_SHORT).show();
                    return; // Sai do método para evitar exceções
                }

                try {
                    // Conversão dos valores para int e float
                    int desiredTime = Integer.parseInt(desiredTimeString);
                    float autonomy = Float.parseFloat(autonomyString);
                    float fuelCost = Float.parseFloat(fuelCostString);

                    // Adiciona os valores como extras na Intent
                    i.putExtra("tempoTotal", desiredTime);
                    i.putExtra("valorAutonomia", autonomy);
                    i.putExtra("valorCombustivel", fuelCost);

                    // Inicia a atividade Calcular passando os valores como parâmetros
                    startActivity(i);
                } catch (NumberFormatException e) {
                    // Tratamento de exceções caso os valores inseridos não sejam números válidos
                    Toast.makeText(MainActivity.this, "Valores inválidos. Certifique-se de digitar apenas números.", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }
        });
    }
}