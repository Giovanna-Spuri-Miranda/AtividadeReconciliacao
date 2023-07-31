package com.spuriufla.atividadereconciliacao;


import android.Manifest;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.Semaphore;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.Toast;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.HashMap;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import android.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class CalcularCross extends AppCompatActivity {

    // Declaração de variáveis para armazenar informações e dados
    // relacionados ao cálculo e reconciliação de rotas.
    // Além disso, alguns componentes da interface do usuário (EditText)
    private static final String EXTRA_TEMPO_TOTAL = "tempoTotal";
    private static final String EXTRA_VALOR_AUTONOMIA = "valorAutonomia";
    private static final String EXTRA_VALOR_COMBUSTIVEL = "valorCombustivel";
    private static final String EXTRA_NUMERO_IDENTIFICACAO = "numeroIdenticacao";
    private static final String EXTRA_MOTORISTA = "motorista";
    private static final String EXTRA_ITENS_SERVICO = "itensServico";

    // Declaração de botões para interação do usuário com a atividade.
    private Button myButton1; // Botão para finalizar a atividade
    private Button myButton2; // Botão para trocar de veículo

    // Variáveis para armazenar informações sobre o veículo e a rota.
    private float valorAutonomia, valorCombustivel;
    private double consumo, velRecomendada, velMedia;
    private int tempoTotal, numeroIdenticacao;
    private static float tempoRestante;

    // Cronômetro para medir o tempo gasto na rota.
    private Chronometer cronometro;

    // Informações sobre o motorista e itens de serviço.
    private String motorista, itensServico;

    // Variáveis para armazenar informações sobre a localização do veículo.
    private double distanciaRestante, distanciaTotal;
    private static double latitudeNova, longitudeNova;
    private static double latitudeDestino, longitudeDestino;

    // Declaração de diversos campos de texto (EditText) para exibir os resultados e detalhes do cálculo.
    EditText num_Identificacao, tempoDesejadoCross, passageiroCross, pontoEncontroCross;
    EditText motorista01Cross, localAtual01Cross, locDestino01Cross;
    EditText DistTotal01Cross, DistRestante01Cross, DistPercorrida01Cross;
    EditText consumo01Cross, custo01Cross, VelRecomendada01Cross, velMedia01Cross;
    EditText motorista02Cross, localAtual02Cross, locDestino02Cross;
    EditText DistTotal02Cross, DistRestante02Cross, DistPercorrida02Cross;
    EditText consumo02Cross, custo02Cross, VelRecomendada02Cross, velMedia02Cross;
    private String valMotorista02, valLocAtual02, valLocDest02;
    private Double valConsumo02, valVelMed02, valVelRec02;
    private Double valDisTotal02, valDistPerc02, valDistRest02;

    // Referências ao Realtime Database do Firebase para leitura e escrita de dados.
    private DatabaseReference meusDados, motorista2Dados;

    // Variáveis para controlar a lógica do cálculo e do fluxo do aplicativo.
    private boolean setInicio = true; // Variável para indicar se o início do cálculo foi definido.
    private boolean gpsAtivo = true; // Indica se o GPS está ativado no dispositivo.
    private LocationManager locationManager; // Gerenciador de localização do dispositivo.
    private HashMap Dados; // Estrutura de dados para armazenar informações do cálculo.
    private HandlerThread handlerThread;

    // Matriz e vetores utilizados em cálculos e reconciliação.
    private double[][] A = new double[][] { { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, -1 } };
    private double[] v = new double[] { 0.0001, 0.0001, 0.0001, 0.0001, 0.0001, 0.0001, 0.0001, 0.0001,
            0.0001, 0.0001, 0.0001, 0.0001, 0.0001, 0.0001, 0.0001, 0.0001, 0.0001, 0.0001,
            0.0001, 0.0001, 0.01 };
    private static double tempInterval = tempoRestante / 20;
    private static double[] y = new double[] {tempInterval, tempInterval, tempInterval, tempInterval,
            tempInterval, tempInterval, tempInterval, tempInterval, tempInterval, tempInterval, tempInterval,
            tempInterval, tempInterval, tempInterval, tempInterval, tempInterval, tempInterval, tempInterval,
            tempInterval, tempInterval, tempoRestante }; // Valores Medidos Tempo

    // Matrizes e vetores auxiliares utilizados durante os cálculos de reconciliação.
    private static double[][] aux_A = new double[1][y.length - 1];
    private static double[] aux_Y = new double[y.length - 1];
    private static double[] aux_V = new double[y.length - 1];

    // Objeto da classe Reconciliation
    private Reconciliation reconciliacao;

    // Objeto para manipulação de datas e horários.
    private Calendar data;

    // Variáveis para controlar o tempo de percurso e fluxo do aplicativo.
    private static float tempoPercurso = 0;
    private int tempInicio;
    private static int medidorDeFluxo = 1;
    private static double tempoReconciliado = 0;


    // Método chamado quando a atividade é criada.
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calculo_cross);

        initializeButtons(); //Inicialização dos botões e definição de suas ações ao serem clicados.
        getExtras(); // Recebe informações extras (tempoTotal, valorAutonomia, valorCombustivel, etc.) passadas pela atividade anterior.
        initializeEditTexts(); // Inicialização e associação de diversos campos de texto (EditText) para exibir os dados recebidos.
        setupFirebaseReferences(); // Configuração das referências ao Realtime Database do Firebase para leitura e escrita de dados.

        // Inicialize e inicie o HandlerThread
        // O Handler é uma classe do Android que nos permite manipular as mensagens e tarefas em
        // threads específicas. Ao associá-lo ao HandlerThread usando handlerThread.getLooper(),
        // todas as mensagens enviadas para este Handler serão executadas na fila de mensagens do HandlerThread.
        handlerThread = new HandlerThread("LocationThread");
        handlerThread.start();

        // Cria um Handler associado ao HandlerThread
        Handler handler = new Handler(handlerThread.getLooper());

        // Executa o método capturarUltimaLocalizacaoValida() no HandlerThread
        handler.post(new Runnable() {
            @Override
            public void run() {
                capturarUltimaLocalizacaoValida();
            }
        });

        myButton2 = findViewById(R.id.BTtrocarDeVeiculo);
        myButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Ação para trocar para a atividade Calcular (Calcular.class).
                Intent i = new Intent(CalcularCross.this, Calcular.class);
                startActivity(i);
            }
        });


        // Definição das coordenadas de destino (latitude e longitude) da rota.
        latitudeDestino = -21.225691;
        longitudeDestino = -44.978236;

        // Inicialização do gerenciador de localização e verificação se o GPS está ativado.
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        gpsAtivo = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        // Verifica se o GPS está ativo e, se estiver, captura a última localização válida do dispositivo.
        if (gpsAtivo) {
            if (checkLocationPermissions()) {
                // Permissões de localização concedidas, captura a última localização válida do dispositivo
                capturarUltimaLocalizacaoValida();
            } else {
                // Permissões de localização não foram concedidas, solicitar permissões
                requestLocationPermissions();
            }
        } else {
            // Caso o GPS esteja desativado, define as coordenadas de localização como (0, 0) e exibe uma mensagem ao usuário.
            latitudeNova = 0.00;
            longitudeNova = 0.00;
            Toast.makeText(this, "GPS não Disponível", Toast.LENGTH_LONG).show();
        }

        // Exibe as coordenadas de destino na interface do usuário.
        locDestino01Cross.setText("Lat.: " + formatarGeopoint(latitudeDestino) + " Long.: " + formatarGeopoint(longitudeDestino));

        // Realiza cálculos iniciais necessários para a reconciliação e inicia uma nova thread (myThread).
        distanciaTotal = calculoDistancia(latitudeNova, longitudeNova, latitudeDestino, longitudeDestino);
        tempoRestante = tempoTotal * 60;
        MandaBancodeDados();
        LerBancodeDados();
        myThread.start();
    }



    // Verificar se as permissões de localização foram concedidas
    private boolean checkLocationPermissions() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private static final int REQUEST_LOCATION_PERMISSIONS = 1;

    // Método para solicitar permissões de localização ao usuário
    private void requestLocationPermissions() {
        // Solicita permissões de localização usando o método requestPermissions da classe ActivityCompat
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                REQUEST_LOCATION_PERMISSIONS);
    }

    // Método que lida com o resultado do pedido de permissão
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Verifica se o pedido de permissão corresponde à solicitação de localização
        if (requestCode == REQUEST_LOCATION_PERMISSIONS) {
            // Verifica se as permissões foram concedidas
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                // Permissões de localização concedidas, captura a última localização válida do dispositivo
                capturarUltimaLocalizacaoValida();
            } else {
                // Permissões de localização não foram concedidas, lidar com o comportamento adequado
                Toast.makeText(this, "As permissões de localização são necessárias para o funcionamento do aplicativo.", Toast.LENGTH_LONG).show();
            }
        }
    }


    //Inicialização dos botões e definição de suas ações ao serem clicados.
    private void initializeButtons() {
        // Referencia o botão "BTFinalizarCross" na interface pelo ID definido no layout
        myButton1 = findViewById(R.id.BTFinalizarCross);

        // Define o que acontece quando o botão "BTFinalizarCross" é clicado
        myButton1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Chama o método "navigateToActivity" para navegar para a atividade "inicial"
                navigateToActivity(inicial.class);
            }
        });

        // Referencia o botão "BTtrocarDeVeiculo" na interface pelo ID definido no layout
        myButton2 = findViewById(R.id.BTtrocarDeVeiculo);

        // Define o que acontece quando o botão "BTtrocarDeVeiculo" é clicado
        myButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Chama o método "navigateToActivity" para navegar para a atividade "Calcular"
                navigateToActivity(Calcular.class);
            }
        });
    }


    // Método para navegar para outra atividade com base na classe da atividade de destino.
    private void navigateToActivity(Class<?> destinationActivityClass) {
        // Cria uma nova instância da classe Intent, que é usada para iniciar uma nova atividade
        // O primeiro argumento é o contexto atual (this), e o segundo é a classe da atividade de destino
        Intent intent = new Intent(this, destinationActivityClass);

        // Inicia a nova atividade usando o Intent criado
        startActivity(intent);
    }

    //Recebe informações extras (tempoTotal, valorAutonomia, valorCombustivel, etc.) passadas pela atividade anterior.
    private void getExtras() {
        tempoTotal = getIntent().getIntExtra(EXTRA_TEMPO_TOTAL, 0);
        valorAutonomia = getIntent().getFloatExtra(EXTRA_VALOR_AUTONOMIA, 0);
        valorCombustivel = getIntent().getFloatExtra(EXTRA_VALOR_COMBUSTIVEL, 0);
        numeroIdenticacao = getIntent().getIntExtra(EXTRA_NUMERO_IDENTIFICACAO, 0);
        motorista = getIntent().getStringExtra(EXTRA_MOTORISTA);
        itensServico = getIntent().getStringExtra(EXTRA_ITENS_SERVICO);
    }

    //Inicialização e associação de diversos campos de texto (EditText) para exibir os dados recebidos.
    private void initializeEditTexts() {
        Log.d("CalcularCross", "initializeEditTexts");
        num_Identificacao = findViewById(R.id.num_Identificacao);
        tempoDesejadoCross = findViewById(R.id.tempoDesejadoCross);
        passageiroCross = findViewById(R.id.passageiroCross);
        pontoEncontroCross = findViewById(R.id.pontoEncontroCross);
        motorista01Cross = findViewById(R.id.motorista01Cross);

        // Definição dos valores dos campos de texto com base nos dados recebidos.
        num_Identificacao.setText(String.valueOf(numeroIdenticacao));
        tempoDesejadoCross.setText(tempoTotal + " min.");
        passageiroCross.setText(itensServico);
        motorista01Cross.setText(motorista);
        pontoEncontroCross.setText("Lat.: -22.8434 Long.: -21.8778");

        // Veículo 1
        localAtual01Cross = findViewById(R.id.localAtual01Cross);
        locDestino01Cross = findViewById(R.id.locDestino01Cross);
        DistTotal01Cross = findViewById(R.id.DistTotal01Cross);
        DistRestante01Cross = findViewById(R.id.DistRestante01Cross);
        DistPercorrida01Cross = findViewById(R.id.DistPercorrida01Cross);
        consumo02Cross = findViewById(R.id.consumo02Cross);
        custo02Cross = findViewById(R.id.custo02Cross);
        VelRecomendada01Cross = findViewById(R.id.VelRecomendada01Cross);
        velMedia01Cross = findViewById(R.id.velMedia01Cross);

        // Veículo 2
        motorista02Cross = findViewById(R.id.motorista02Cross);
        localAtual02Cross = findViewById(R.id.localAtual02Cross);
        locDestino02Cross = findViewById(R.id.locDestino02Cross);
        DistTotal02Cross = findViewById(R.id.DistTotal02Cross);
        DistRestante02Cross = findViewById(R.id.DistRestante02Cross);
        DistPercorrida02Cross = findViewById(R.id.DistPercorrida02Cross);
        consumo01Cross = findViewById(R.id.consumo01Cross);
        custo01Cross = findViewById(R.id.custo01Cross);
        VelRecomendada02Cross = findViewById(R.id.VelRecomendada02Cross);
        velMedia02Cross = findViewById(R.id.velMedia02Cross);
    }

    // Configuração das referências ao Realtime Database do Firebase para leitura e escrita de dados.
    private void setupFirebaseReferences() {
        // Obtém uma referência para o banco de dados do Firebase
        FirebaseDatabase database = FirebaseDatabase.getInstance();

        // Define as referências para os nós específicos no banco de dados do Firebase
        // O método `getReference()` retorna a referência para o nó raiz do banco de dados
        // Em seguida, é utilizado o método `child()` para navegar para nós específicos (por exemplo, "Motorista01Cross" e "Motorista02Cross")
        // Isso nos permite acessar e manipular os dados armazenados em cada nó no banco de dados do Firebase
        meusDados = database.getReference().child("Motorista01Cross");
        motorista2Dados = database.getReference().child("Motorista02Cross");
    }


    private Handler uiHandler = new Handler(Looper.getMainLooper());
    private Semaphore semaphore = new Semaphore(1);

    // Criação de uma thread que será executada em segundo plano.
    // Essa thread é responsável por realizar cálculos e atualizações periódicas enquanto a atividade está em execução.
    private Thread myThread = new Thread(new Runnable() {

        @Override
        public void run() {
            // Cálculo do intervalo de fluxo necessário para percorrer a distância restante em 20 etapas
            double intervaloFluxo = (distanciaRestante * medidorDeFluxo) / 20;

            // Loop que será executado enquanto ainda houver tempo restante na viagem
            while (tempoRestante > 0) {
                capturarUltimaLocalizacaoValida();

                // Verifica se a latitude é diferente de zero, o que indica que a localização foi obtida com sucesso
                if (latitudeNova != 0) {
                    // Verifica se a distância restante é igual ao intervalo de fluxo calculado anteriormente
                    if (distanciaRestante == intervaloFluxo) {
                        try {
                            // Adquire o semáforo para garantir exclusão mútua nos cálculos
                            acquireSemaphore();

                            distanciaRestante = calculoDistancia(latitudeNova, longitudeNova, latitudeDestino, longitudeDestino);
                            consumo = (distanciaRestante / 1000) / valorAutonomia;
                            tempoPercurso = tempoPercurso + ((data.get(Calendar.HOUR_OF_DAY) * 60 + data.get(Calendar.MINUTE)) - tempInicio);

                            // Aplica tratamentos de reconciliação nos dados obtidos para melhorar a precisão
                            tratamentosDeReconciliacao();
                            // Atualiza o tempo restante considerando o tempo reconciliado
                            tempoRestante = tempoRestante - (float) tempoReconciliado;
                            // Calcula a velocidade recomendada com base na distância restante e tempo restante
                            velRecomendada = (distanciaRestante / tempoRestante) * 3.6;

                            // Atualiza os dados no banco de dados (Firebase) com os resultados dos cálculos
                            MandaBancodeDados();
                        } catch (InterruptedException e) {
                            Log.d("CalcularCross", "catch");
                            e.printStackTrace();
                        } finally {
                            Log.d("CalcularCross", "finally");
                            // Libera o semáforo no bloco finally para garantir que ele seja sempre liberado
                            semaphore.release();
                        }
                    }
                    // Verifica se é a primeira vez executando o aplicativo
                    if (setInicio) {
                        try {
                            // Adquire o semáforo para garantir exclusão mútua nos cálculos
                            acquireSemaphore();

                            // Calcula valores fixos depois de iniciada a aplicação
                            setInicio = false;
                            tempInicio = (data.get(Calendar.HOUR_OF_DAY) * 60 + data.get(Calendar.MINUTE));
                            distanciaTotal = calculoDistancia(latitudeNova, longitudeNova, latitudeDestino, longitudeDestino);
                            cronometro.setBase(SystemClock.elapsedRealtime());
                            velMedia = (distanciaTotal / (tempoTotal * 60)) * 3.6;
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } finally {
                            // Libera o semáforo no bloco finally para garantir que ele seja sempre liberado
                            releaseSemaphore();
                        }
                    }

                    cronometro.start();

                    // Atualiza a interface do usuário com os resultados dos cálculos e informações sobre a rota
                    uiHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            // Atualiza os campos de texto exibindo informações sobre a rota.
                            localAtual01Cross.setText("Lat.: " + formatarGeopoint(latitudeNova) + " Long.: " + formatarGeopoint(longitudeNova));
                            DistTotal01Cross.setText(String.format("%.2f", distanciaTotal) + " m");
                            DistRestante01Cross.setText(String.format("%.2f", distanciaRestante) + " m");
                            DistPercorrida01Cross.setText(String.format("%.2f", distanciaTotal - distanciaRestante) + " m");
                            consumo01Cross.setText(String.format("%.2f", consumo) + " Litros");
                            custo01Cross.setText("R$ " + String.format("%.2f", consumo * valorCombustivel));
                            velMedia01Cross.setText(String.format("%.2f", velMedia) + " Km/h");
                            VelRecomendada01Cross.setText(String.format("%.2f", velRecomendada) + " Km/h");

                            // Atualiza os campos de texto exibindo informações sobre a segunda atividade.
                            motorista02Cross.setText(valMotorista02 + " ");
                            localAtual02Cross.setText(valLocAtual02 + " ");
                            locDestino02Cross.setText(valLocDest02 + " ");
                            DistTotal02Cross.setText(String.format("%.2f", valDisTotal02) + "m");
                            DistRestante02Cross.setText(String.format("%.2f", valDistRest02) + "m");
                            DistPercorrida02Cross.setText(String.format("%.2f", valDistPerc02) + "m");
                            consumo02Cross.setText(String.format("%.2f", valConsumo02) + " Litros");
                            custo02Cross.setText("R$ " + String.format("%.2f", valConsumo02));
                            VelRecomendada02Cross.setText(String.format("%.2f", valVelRec02) + " Km/h");
                            velMedia02Cross.setText(String.format("%.2f", valVelMed02) + " Km/h");
                        }

                    });
                }

                try {
                    // Faz a thread dormir por 1 segundo antes de realizar a próxima iteração do loop
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // Para o cronômetro quando o tempo restante é igual ou inferior a zero
            cronometro.stop();

            // Verifica se o tempo restante é menor ou igual a zero e se a distância restante é menor que 30 metros (erro)
            // Se sim, exibe uma mensagem de parabéns para o usuário, indicando que ele chegou ao ponto de CrossDocking
            if (tempoRestante <= 0) {
                if (distanciaRestante < 30) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // Exibe um Toast (mensagem temporária) para o usuário com a mensagem de parabéns
                            Toast.makeText(CalcularCross.this, "Parabéns! Você chegou ao ponto de CrossDocking!", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }

        // Método para adquirir o semáforo. Ele é responsável por bloquear a thread caso o semáforo não esteja disponível.
        // Quando o semáforo estiver disponível, a thread pode prosseguir normalmente.
        // O método utiliza a chamada "semaphore.acquire();" para adquirir o semáforo.
        // Caso a aquisição seja bem-sucedida, a thread continua sua execução.
        // Caso contrário, a thread fica bloqueada até que o semáforo seja liberado por outra thread.
        // O método lança uma exceção "InterruptedException" caso ocorra alguma interrupção durante o bloqueio da thread.
        private void acquireSemaphore() throws InterruptedException {
            semaphore.acquire();
        }

        // Método para liberar o semáforo. Ele é responsável por liberar uma permissão do semáforo,
        // permitindo que outra thread bloqueada possa prosseguir.
        // O método utiliza a chamada "semaphore.release();" para liberar o semáforo.
        // Quando uma permissão é liberada, uma thread bloqueada pode continuar sua execução.
        // Esse método não lança nenhuma exceção, pois a liberação do semáforo é uma operação simples e não apresenta riscos de falhas.
        private void releaseSemaphore() {
            semaphore.release();
        }

    });

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Interrompe a execução da thread e libera os recursos associados.
        myThread.interrupt();
    }


    private double calcularDesvioPadrao(double tempoPlanejado, double tempoRealPercurso) {
        // Calcula o desvio padrão entre o tempo planejado e o tempo real do percurso.
        // Calcula a média entre o tempo planejado e o tempo real.
        double media = (tempoPlanejado + tempoRealPercurso) / 2;

        // Calcula a soma dos quadrados das diferenças entre cada valor e a média.
        double somaQuadradosDiferencas = Math.pow(tempoPlanejado - media, 2) + Math.pow(tempoRealPercurso - media, 2);

        // Verifica se há mais de uma amostra para evitar divisão por zero.
        int numeroAmostras = 2;
        if (numeroAmostras > 1) {
            // Calcula o desvio padrão dividindo a soma dos quadrados das diferenças por (n - 1) e tirando a raiz quadrada.
            double desvioPadrao = Math.sqrt(somaQuadradosDiferencas / (numeroAmostras - 1));
            return desvioPadrao;
        } else {
            // Se há apenas uma amostra, o desvio padrão é indefinido, retornar NaN (Not a Number).
            return Double.NaN;
        }
    }

    private double desvioPadrao(double tempoDesejado, float tempoReal) {
        Double med = (tempoDesejado + tempoReal) / 2;
        Double sum = Math.pow(tempoDesejado - med, 2) + Math.pow(tempoReal - med, 2);
        Double result = Math.sqrt(sum / (2 - 1));
        return result;
    }
    private void MandaBancodeDados(){
        try {
            // Envia dados para o banco de dados Firebase.
            // Recupera as informações necessárias da interface do usuário.
            String localAtual = localAtual01Cross.getText().toString();
            String localDestino = locDestino01Cross.getText().toString();
            String distTotal = Double.toString(distanciaTotal);
            String distPercorrida = Double.toString(distanciaTotal - distanciaRestante);
            String DistRestante = Double.toString(distanciaRestante);
            String cons = Double.toString(consumo);
            String velRec = Double.toString(velRecomendada);
            String velMed = Double.toString(velMedia);

            // Verifica se os objetos localAtual01Cross e locDestino01Cross não são nulos antes de acessar seus valores.
            if (localAtual01Cross != null && locDestino01Cross != null) {
                // Cria um HashMap para armazenar os dados a serem enviados para o banco de dados.
                HashMap<String, String> dados = new HashMap<>();
                dados.put("motorista", Criptografa(motorista)); // Adiciona o motorista criptografado ao HashMap.
                dados.put("localAtual", Criptografa(localAtual)); // Adiciona o local atual criptografado ao HashMap.
                dados.put("localDestino", Criptografa(localDestino)); // Adiciona o local de destino criptografado ao HashMap.
                dados.put("distTotal", Criptografa(distTotal)); // Adiciona a distância total criptografada ao HashMap.
                dados.put("distPercorrida", Criptografa(distPercorrida)); // Adiciona a distância percorrida criptografada ao HashMap.
                dados.put("DistRestante", Criptografa(DistRestante)); // Adiciona a distância restante criptografada ao HashMap.
                dados.put("cons", Criptografa(cons)); // Adiciona o consumo criptografado ao HashMap.
                dados.put("velRec", Criptografa(velRec)); // Adiciona a velocidade recomendada criptografada ao HashMap.
                dados.put("velMed", Criptografa(velMed)); // Adiciona a velocidade média criptografada ao HashMap.

                // Converta o HashMap<String, String> para Map<String, Object> através do cast.
                Map<String, Object> dadosConvertidos = (Map<String, Object>) (Map<?, ?>) dados;

                // Atualiza os dados no banco de dados Firebase usando o objeto "meusDados".
                meusDados.updateChildren(dadosConvertidos);

            }
        } catch (Exception e) {
        }
    }

    private void LerBancodeDados(){
        // Função para ler dados do banco de dados Firebase.

        // Cada trecho de código abaixo é um listener para um dado específico no banco de dados.
        // Eles ficam aguardando mudanças nos valores dos dados especificados e respondem quando ocorrem alterações.

        // Listener para o valor do nó "motorista" no banco de dados.
        motorista2Dados.child("motorista").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Quando o valor de "motorista" é alterado, esse método é chamado.

                if(snapshot.exists()){
                    valMotorista02 = Descriptografa(snapshot.getValue(String.class)); // Decifra o valor do snapshot e armazena na variável valMotorista02.
                } else {
                    valMotorista02 = "None"; // Se o nó "motorista" não existir no banco de dados, atribui "None" à variável valMotorista02.
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Esse método é chamado quando ocorre um erro no acesso aos dados.
                // Neste caso, não realizamos nenhuma ação adicional.
            }
        });

        motorista2Dados.child("localAtual").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    valLocAtual02 = Descriptografa(snapshot.getValue(String.class));
                }else {
                    valMotorista02 = "None";
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

        motorista2Dados.child("localDestino").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    valLocDest02 = Descriptografa(snapshot.getValue(String.class));
                }else {
                    valLocDest02 = "None";
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

        motorista2Dados.child("distTotal").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    valDisTotal02 = Double.parseDouble(Descriptografa(snapshot.getValue(String.class)));
                }else {
                    valDisTotal02 = 0.0;
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

        motorista2Dados.child("distPercorrida").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    valDistPerc02 = Double.parseDouble(Descriptografa(snapshot.getValue(String.class)));
                }else {
                    valDistPerc02 = 0.0;
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

        motorista2Dados.child("DistRestante").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    valDistRest02 = Double.parseDouble(Descriptografa(snapshot.getValue(String.class)));
                }else {
                    valDistRest02 = 0.0;
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

        motorista2Dados.child("cons").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    valConsumo02 = Double.parseDouble(Descriptografa(snapshot.getValue(String.class)));
                }else {
                    valConsumo02 = 0.0;
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

        motorista2Dados.child("velRec").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    valVelRec02 = Double.parseDouble(Descriptografa(snapshot.getValue(String.class)));
                }else {
                    valVelRec02 = 0.0;
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

        motorista2Dados.child("velMed").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    valVelMed02 = Double.parseDouble(Descriptografa(snapshot.getValue(String.class)));
                }else {
                    valVelMed02 = 0.0;
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void capturarUltimaLocalizacaoValida() {
        // Verifica se o GPS está ativo no dispositivo.
        gpsAtivo = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        // Se o GPS estiver ativo, prossegue com a captura da localização.
        if (gpsAtivo) {
            // Verifica se o aplicativo possui as permissões necessárias para acessar a localização.
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                // Obtem uma instância do LocationManager, que é o gerenciador de localização do sistema,
                // caso ainda não exista.
                if (locationManager == null) {
                    locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                }

                // Cria uma instância de AtualizaLocalizacao, que é uma classe que implementa LocationListener.
                // Essa classe será responsável por receber as atualizações da localização.
                AtualizaLocalizacao atualizaLoc = new AtualizaLocalizacao();

                // Registra o AtualizaLocalizacao para receber atualizações da localização.
                // O parâmetro "LocationManager.GPS_PROVIDER" indica que queremos receber as atualizações do GPS.
                // Os valores "0, 0" indicam que não temos preferência por intervalo de tempo ou distância mínima
                // entre as atualizações.
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, atualizaLoc);

                // Obtém a última localização registrada pelo GPS.
                Location ultimaLocalizacao = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (ultimaLocalizacao != null) {
                    latitudeNova = ultimaLocalizacao.getLatitude();
                    longitudeNova = ultimaLocalizacao.getLongitude();
                } else {}
            } else {
                // Caso o aplicativo não tenha as permissões necessárias para acessar a localização,
                // solicita essas permissões ao usuário.
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        } else {}
    }

    private String formatarGeopoint(double valor){
        // Cria uma instância da classe DecimalFormat com o formato "#.####".
        // O formato "#.####" indica que o número será formatado com quatro casas decimais.
        DecimalFormat decimalFormat = new DecimalFormat("#.####");

        // Formata o valor de entrada usando o formato criado acima.
        // O valor é convertido em uma string com quatro casas decimais após o ponto.
        String valorFormatado = decimalFormat.format(valor);

        // Retorna a string resultante após a formatação.
        return valorFormatado;
    }

    private double calculoDistancia(double lat1, double long1, double lat2, double long2) {
        // Cria um array de floats com um único elemento para armazenar o resultado do cálculo da distância.
        float[] dist = new float[1];

        // Utiliza o método estático distanceBetween da classe Location para calcular a distância entre as coordenadas.
        // O método recebe as coordenadas de latitude e longitude dos dois pontos e armazena o resultado no array dist.
        Location.distanceBetween(lat1, long1,  lat2, long2, dist);

        // Retorna a distância calculada, que está armazenada no primeiro elemento do array dist.
        // O resultado é retornado como um valor de ponto flutuante (float), pois é a convenção do método distanceBetween.
        return dist[0];
    }

    private SecretKey gerarChaveAleatoria() throws Exception {
        // Cria uma instância do gerador de chaves usando o algoritmo AES (Advanced Encryption Standard).
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");

        // Inicializa o gerador de chaves com um tamanho de chave de 256 bits.
        SecureRandom secureRandom = new SecureRandom();
        keyGenerator.init(256, secureRandom);

        // Gera a chave secreta.
        return keyGenerator.generateKey();
    }

    private String Criptografa(String texto) {
        try {
            // Gera uma chave aleatória para a criptografia.
            SecretKey chave = gerarChaveAleatoria();

            // Cria uma instância do objeto Cipher para a criptografia usando o algoritmo AES.
            Cipher cipher = Cipher.getInstance("AES");

            // Configura o objeto Cipher para a operação de criptografia usando a chave secreta gerada.
            cipher.init(Cipher.ENCRYPT_MODE, chave);

            // Codifica o texto para um array de bytes e, em seguida, realiza a criptografia.
            byte[] textoCriptografado = cipher.doFinal(texto.getBytes());

            // Converte o array de bytes criptografado para uma string usando Base64.
            String textoCodificado = Base64.encodeToString(textoCriptografado, Base64.DEFAULT);

            // Retorna o texto codificado em Base64.
            return textoCodificado;
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private String Descriptografa(String textoCod) {
        try {
            // Gera uma chave aleatória para a criptografia.
            SecretKey chave = gerarChaveAleatoria();


            // Cria uma instância do objeto Cipher para a decodificação usando o algoritmo AES.
            Cipher cipher = Cipher.getInstance("AES");

            // Configura o objeto Cipher para a operação de decodificação usando a chave secreta gerada.
            cipher.init(Cipher.DECRYPT_MODE, chave);

            // Decodifica o texto codificado em Base64 para obter o array de bytes criptografado.
            byte[] textoCriptografado = Base64.decode(textoCod, Base64.DEFAULT);

            // Realiza a decodificação dos dados criptografados para obter o texto original em bytes.
            byte[] bytesTexto = cipher.doFinal(textoCriptografado);

            // Cria uma nova string a partir do array de bytes decodificado.
            String textoDecodificado = new String(bytesTexto);

            // Retorna o texto original decodificado.
            return textoDecodificado;
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private void novosVetRec(int len) {
        // Inicia um loop para percorrer o vetor de dados (y) até a penúltima posição (len - 1).
        for (int j = 0; j < len - 1; j++) {
            // Verifica se o índice j é igual a 0.
            if (j == 0) {
                // Se for o primeiro elemento (j == 0), calcula a soma entre o primeiro
                // e o segundo elemento e atribui ao vetor aux_Y.
                aux_Y[j] = y[j] + y[j + 1];
            } else {
                // Caso contrário, atribui ao vetor aux_Y o valor do segundo elemento (j + 1).
                aux_Y[j] = y[j + 1];}

            // Atribui ao vetor aux_V o valor do segundo elemento (j + 1) do vetor original v.
            aux_V[j] = v[j + 1];
            // Atribui à matriz aux_A[0][j] o valor do segundo elemento (j + 1) da primeira linha
            // (índice 0) da matriz original A.
            aux_A[0][j] = A[0][j + 1];
        }

        // Atualiza os vetores y, v e a matriz A com os dados armazenados em seus respectivos
        // vetores auxiliares.
        y = aux_Y;
        v = aux_V;
        A = aux_A;
    }

    private void tratamentosDeReconciliacao() {
        // Realiza o tratamento de reconciliação para os dados do vetor y e vetor v.
        // Os tratamentos envolvem ajustes e combinações dos dados para garantir a integridade e consistência das informações.

        // Ajusta o primeiro elemento (índice 0) do vetor y somando-o com o segundo elemento (índice 1).
        y[0] = y[0] + y[1];

        // Calcula o desvio padrão usando a função desvioPadrao() para o primeiro elemento (índice 0) do vetor y
        // e armazena o resultado no primeiro elemento (índice 0) do vetor v.
        v[0] = desvioPadrao(y[0], tempoPercurso);

        // Incrementa o valor do medidor de fluxo em 1 unidade.
        medidorDeFluxo = medidorDeFluxo + 1;

        // Atualiza o último elemento (índice length-1) do vetor y usando o mínimo entre o tempoRestante
        // e uma razão calculada entre valDistRest02 e valVelMed02.
        y[y.length - 1] = Math.min(tempoRestante, valDistRest02 / valVelMed02);

        // Cria um objeto de reconciliação (classe Reconciliation) passando os vetores y, v e a matriz A.
        // A classe Reconciliation realiza tratamentos específicos para reconciliar os dados.
        reconciliacao = new Reconciliation(y, v, A);

        // Atualiza o vetor y com os dados reconciliados obtidos a partir do objeto de reconciliação.
        for (int i = 0; i < reconciliacao.getReconciledFlow().length; i++) {
            y[i] = reconciliacao.getReconciledFlow()[i];
        }

        // Atualiza a variável tempoReconciliado com o primeiro elemento (índice 0) do vetor y.
        tempoReconciliado = y[0];

        // Realiza a função novosVetRec() para ajustar os vetores y, v e a matriz A.
        // Esse tratamento de reconciliação pode envolver alterações adicionais nos dados.
        novosVetRec(y.length);
    }

}
