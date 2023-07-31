package com.spuriufla.atividadereconciliacao;

import org.ejml.data.DMatrixRMaj;
import org.ejml.simple.SimpleMatrix;

public class Reconciliation {
	// Declaração de variáveis membros da classe
	private double[] reconciledFlowDouble;
	private SimpleMatrix reconciledFlow;
	private SimpleMatrix adjustment;
	private SimpleMatrix rawMeasurement;
	private SimpleMatrix standardDeviation;
	private SimpleMatrix varianceMatrix;
	private SimpleMatrix incidenceMatrix;
	private SimpleMatrix diagonalMatrix;
	private SimpleMatrix weightsArray;


	// Construtor da classe para uma matriz de incidência
	public Reconciliation(double[] _rawMeasurement, double[] _standardDeviation, double[][] _incidenceMatrix) {
		// Verifica se os parâmetros são válidos e faz a inicialização das matrizes
		if ((_rawMeasurement != null) && (_standardDeviation != null) && (_incidenceMatrix != null)) {
			if ((_rawMeasurement.length == _standardDeviation.length)
					&& (_standardDeviation.length == _incidenceMatrix[0].length)) {
				// Inicializa a matriz de incidência com os valores passados como parâmetro
				this.incidenceMatrix = new SimpleMatrix(_incidenceMatrix);
				// Inicializa a matriz de medição com os valores passados como parâmetro
				// A matriz é representada como uma matriz coluna usando a classe SimpleMatrix
				this.rawMeasurement = new SimpleMatrix(_rawMeasurement.length, 1, true, _rawMeasurement);
				// Inicializa a matriz de desvio padrão com os valores passados como parâmetro
				// A matriz é representada como uma matriz coluna usando a classe SimpleMatrix
				this.standardDeviation = new SimpleMatrix(_standardDeviation.length, 1, true, _standardDeviation);

				// Cria uma cópia do vetor de desvio padrão para usar na construção da matriz de variância
				double[] aux_StandardDeviation = _standardDeviation.clone();

				// Cria a matriz de variância (matriz diagonal) a partir do vetor de desvio padrão
				// Os elementos fora da diagonal principal são definidos como zero
				double[][] aux_varianceMatrix = new double[aux_StandardDeviation.length][aux_StandardDeviation.length];
				for (int i = 0; i < aux_varianceMatrix.length; i++) {
					for (int j = 0; j < aux_varianceMatrix[0].length; j++) {
						if (i == j) {
							// Elementos da diagonal principal recebem o valor do desvio padrão
							aux_varianceMatrix[i][i] = aux_StandardDeviation[i];
						} else {
							// Elementos fora da diagonal são definidos como zero
							aux_varianceMatrix[i][j] = 0;
						}
					}
				}

				// Inicializa a matriz de variância com os valores calculados
				this.varianceMatrix = new SimpleMatrix(aux_varianceMatrix);

				// Realiza os cálculos para obter o fluxo reconciliado e o ajuste
				// A fórmula usada é: ajuste = ((varianceMatrix * incidenceMatrix^T) * (incidenceMatrix * (varianceMatrix * incidenceMatrix^T)^-1)) * rawMeasurement
				SimpleMatrix aux1 = this.varianceMatrix.mult(this.incidenceMatrix.transpose());
				aux1 = aux1.mult(this.incidenceMatrix.mult(aux1).invert());
				this.adjustment = aux1.mult(this.incidenceMatrix.mult(this.rawMeasurement));
				// Calcula o fluxo reconciliado a partir da medição original e do ajuste
				this.reconciledFlow = this.rawMeasurement.minus(this.adjustment);


				// Converte o resultado do fluxo reconciliado para um array de double
				// para permitir a obtenção dos resultados através do método getReconciledFlow()
				DMatrixRMaj temp = this.reconciledFlow.getMatrix();
				this.reconciledFlowDouble = temp.getData();

			} else {
				// Se os tamanhos dos arrays forem inconsistentes, imprime mensagem de erro
				System.out.println(
						"the rawMeasurement and/or standardDeviation and/or incidenceMatrix have inconsistent data/size.");
			}

		} else {
			// Se algum dos arrays passados como parâmetro for nulo, imprime mensagem de erro
			System.out.println("the rawMeasurement and/or standardDeviation and/or incidenceMatrix have null data.");
		}
	}

	public Reconciliation(double[] _rawMeasurement, double[] _standardDeviation, double[] _incidenceMatrix) {
		// Verifica se os parâmetros são válidos e faz a inicialização das matrizes
		if ((_rawMeasurement != null) && (_standardDeviation != null) && (_incidenceMatrix != null)) {
			// Verifica se os tamanhos dos arrays são consistentes para os cálculos
			if ((_rawMeasurement.length == _standardDeviation.length)
					&& (_standardDeviation.length == _incidenceMatrix.length)) {

				// Inicializa a matriz de incidência com os valores passados como parâmetro
				// A matriz é representada como uma matriz coluna usando a classe SimpleMatrix
				this.incidenceMatrix = new SimpleMatrix(_incidenceMatrix.length, 1, true, _incidenceMatrix);
				// Inicializa a matriz de medição com os valores passados como parâmetro
				// A matriz é representada como uma matriz coluna usando a classe SimpleMatrix
				this.rawMeasurement = new SimpleMatrix(_rawMeasurement.length, 1, true, _rawMeasurement);
				// Inicializa a matriz de desvio padrão com os valores passados como parâmetro
				// A matriz é representada como uma matriz coluna usando a classe SimpleMatrix
				this.standardDeviation = new SimpleMatrix(_standardDeviation.length, 1, true, _standardDeviation);

				// Cria uma matriz diagonal a partir dos vetores de medição e desvio padrão
				// E também um vetor que será usado nos cálculos
				double[][] auxDiagonalMatrix = new double[_rawMeasurement.length + 1][_rawMeasurement.length + 1];
				double[] auxWeightsArray = new double[_rawMeasurement.length + 1];
				for (int i = 0; i < _rawMeasurement.length; i++) {
					double auxMP = Math.pow((_rawMeasurement[i] * _standardDeviation[i]), 2);
					// Cria a matriz diagonal com os elementos (2 / (rawMeasurement[i] * standardDeviation[i])^2) na diagonal principal
					auxDiagonalMatrix[i][i] = 2 / auxMP;
					// Preenche o vetor auxWeightsArray com os valores (2 * rawMeasurement[i]) / (rawMeasurement[i] * standardDeviation[i])^2
					auxWeightsArray[i] = (2 * _rawMeasurement[i]) / auxMP;
				}

				// Inicializa a matriz diagonal com os valores calculados
				this.diagonalMatrix = new SimpleMatrix(auxDiagonalMatrix);
				// Insere o vetor de incidência na última coluna da matriz diagonal
				this.diagonalMatrix.setColumn(auxDiagonalMatrix.length - 1, 0, _incidenceMatrix);
				// Insere o vetor de incidência na última linha da matriz diagonal
				this.diagonalMatrix.setRow(auxDiagonalMatrix.length - 1, 0, _incidenceMatrix);
				// Inicializa o vetor de pesos com os valores calculados
				this.weightsArray = new SimpleMatrix(auxWeightsArray.length, 1, true, auxWeightsArray);

				// Realiza os cálculos para obter o fluxo reconciliado
				// A fórmula usada é: fluxoReconciliado = diagonalMatrix^-1 * weightsArray
				this.reconciledFlow = this.diagonalMatrix.invert().mult(this.weightsArray);
				// Converte o resultado do fluxo reconciliado para um array de double
				// para permitir a obtenção dos resultados através do método getReconciledFlow()
				DMatrixRMaj temp = this.reconciledFlow.getMatrix();
				this.reconciledFlowDouble = temp.getData();

			} else {
				// Se os tamanhos dos arrays forem inconsistentes, imprime mensagem de erro
				System.out.println(
						"the rawMeasurement and/or standardDeviation and/or incidenceMatrix have inconsistent data/size.");
			}

		} else {
			// Se algum dos arrays passados como parâmetro for nulo, imprime mensagem de erro
			System.out.println("the rawMeasurement and/or standardDeviation and/or incidenceMatrix have null data.");
		}
	}

	public void printMatrix(double[][] _m) {

		// Verifica se a matriz passada como parâmetro é válida (não nula)
		if (_m != null) {
			// Itera pelas linhas da matriz
			for (int i = 0; i < _m.length; i++) {
				System.out.print("| ");
				// Itera pelas colunas da matriz
				for (int j = 0; j < _m[0].length; j++) {
					// Imprime o elemento (_m[i][j]) da posição (i, j)
					System.out.print(_m[i][j] + " ");
				}
				// Após imprimir todos os elementos da linha, imprime o caractere '|' e pula para a próxima linha
				System.out.println("|");
			}
			// Imprime uma linha vazia após a impressão da matriz
			System.out.println("");

		} else {
			// Se a matriz for nula, imprime uma mensagem de erro
			System.out.println("A matriz tem dados nulos.");
		}
	}

	public void printMatrix(double[] _m) {

		// Verifica se o array passado como parâmetro é válido (não nulo)
		if (_m != null) {
			// Itera pelos elementos do array
			for (int i = 0; i < _m.length; i++) {
				// Imprime o elemento do array na posição i, separado por '|' no início e fim
				System.out.println("| " + _m[i] + " | ");
			}
			// Imprime uma linha vazia após a impressão do array
			System.out.println("");
		} else {
			// Se o array for nulo, imprime uma mensagem de erro
			System.out.println("the array has null data.");
		}
	}


	public double[] getReconciledFlow() {
		// Retorna o fluxo conciliado como um array de double[]
		return this.reconciledFlowDouble;
	}

	public SimpleMatrix getAdjustment() {
		// Retorna a matriz de ajuste (SimpleMatrix) que representa as correções aplicadas ao fluxo de medição original
		return this.adjustment;
	}

	public SimpleMatrix getRawMeasurement() {
		// Retorna a matriz de medição bruta (SimpleMatrix) que contém as medições originais sem correções
		return this.rawMeasurement;
	}

	public SimpleMatrix getStandardDeviation() {
		// Retorna a matriz de desvio padrão (SimpleMatrix) que contém os valores de desvio padrão para cada medição
		return this.standardDeviation;
	}

	public SimpleMatrix getVarianceMatrix() {
		// Retorna a matriz de variância (SimpleMatrix) que representa a covariância entre as medições
		return this.varianceMatrix;
	}

	public SimpleMatrix getIncidenceMatrix() {
		// Retorna a matriz de incidência (SimpleMatrix) que representa as relações entre as medições e as variáveis desconhecidas
		return this.incidenceMatrix;
	}

	public SimpleMatrix getDiagonalMatrix() {
		// Retorna a matriz diagonal (SimpleMatrix) utilizada em um dos cálculos do método de construção para o fluxo conciliado
		return this.diagonalMatrix;
	}

	public SimpleMatrix getWeightsArray() {
		// Retorna o vetor de pesos (SimpleMatrix) que é parte dos cálculos do método de construção para o fluxo conciliado
		return this.weightsArray;
	}
}