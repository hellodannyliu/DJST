package edu.nuaa.yao.DJST;

import java.io.IOException;

/**
 * Dynamic Joint Sentiment-Topic (dJST) trainer.
 * Implements the online Gibbs sampling procedure described in:
 * "Dynamic Joint Sentiment-Topic Model" (He et al., 2012)
 *
 * Gibbs sampling formula (Eq 6 in paper):
 *   P(z=j, l=k | ...) ∝
 *     (N_{k,j,w} + β_{k,j,w}) / (N_{k,j} + Σ_w β_{k,j,w})
 *   × (N_{d,k,j} + α_{k,j}) / (N_{d,k} + Σ_j α_{k,j})
 *   × (N_{d,k} + γ) / (N_d + L·γ)
 *
 * Array index conventions (matching OldaModel):
 *   nw[word][topic][sentiment], nd[doc][topic][sentiment],
 *   nwsum[topic][sentiment], ndsum[doc][sentiment], nsum[doc],
 *   phi[sentiment][topic][word], theta[doc][sentiment][topic],
 *   p[topic][sentiment]
 */
public class OldaTrainer {
	
	public OldaArgs option;
	public Vocabulary globalVoc;	
	public OldaModel trnModel;

	/** Historical phi matrices for each time window: B[delta][S][K][V] */
	public double[][][][] B;

	/** Evolved Dirichlet prior for sentiment-topic-word: obeta[S][K][V] */
	public double[][][] obeta;

	public Vocabulary[] vocWindow;
	public double Kalpha;
	public double Vbeta;

	/** Sum of obeta over vocab for each (sentiment, topic): Vobeta[S][K] */
	public double[][] Vobeta;

	public int docnum;
	public double gama;

	public boolean init(OldaArgs option) {
		this.option = option;
		trnModel = new OldaModel();
		if (!trnModel.init(option)) {
			return false;
		}
		if (!trnModel.initFirstOldaModel()) {
			System.out.println("init olda trainer failed!");
			return false;
		}
		
		B = new double[option.delta][][][];
		obeta = new double[trnModel.S][trnModel.K][trnModel.V];
		globalVoc = trnModel.data.localVoc;
		vocWindow = new Vocabulary[option.delta];
		Kalpha = trnModel.K * trnModel.alpha;
		Vbeta = trnModel.V * trnModel.beta;
		Vobeta = new double[trnModel.S][trnModel.K];
		docnum = trnModel.docnum;
		gama = trnModel.gama;
		return true;
	}
	
	/**
	 * Gibbs sampling training for the first epoch (basic JST, no evolution).
	 * Uses symmetric beta as Dirichlet prior.
	 */
	public void train() throws IOException {
		System.out.println("Sampling " + trnModel.niters + " iteration!");
		
		int lastIter = trnModel.liter;
		for (trnModel.liter = lastIter + 1; trnModel.liter < trnModel.niters; trnModel.liter++) {
			for (int m = 0; m < trnModel.M; m++) {
				for (int n = 0, N = trnModel.data.docs.get(m).length; n < N; n++) {
					int[] result = sample(m, n);
					trnModel.z[m][n] = result[0];
					trnModel.s[m][n] = result[1];
				}
			}
			
			if (option.savestep > 0) {
				if (trnModel.liter % option.savestep == 0){
					System.out.println("Saving the model at iteration " + trnModel.liter + " ...");
					computeTheta();
					computePhi();
					trnModel.saveModel("model-" + LdaUtil.converseZeroPad(trnModel.liter, 5));
				}
			}
		}
		
		System.out.println("Gibbs sampling completed!\n");
		System.out.println("Saving the final model!\n");
		computeTheta();
		computePhi();
		trnModel.liter--;
		trnModel.saveModel(trnModel.modelName);
	}
	
	/**
	 * Main dJST training loop across all time windows.
	 */
	public void trainOlda() throws IOException {
		
		System.out.println("Train NO:" + 1 + " model");
		train();
		B[0] = trnModel.phi;
		vocWindow[0] = trnModel.data.localVoc;
		
		int i = 0;
		
		for (i = 2; i <= option.delta; i++) {
			System.out.println("Train NO:" + i + " model");
			if (!trnModel.initNewOldaModel(option, globalVoc, i)) {
				System.out.println("train NO:" + i + " model failed!");
				return;
			}
			Vbeta = trnModel.V * trnModel.beta;
			train();
			B[i-1] = trnModel.phi;
			vocWindow[i-1] = trnModel.data.localVoc;
		}
		
		while((i <= docnum) && trnModel.initNewOldaModel(option, globalVoc, i)) {
			Vbeta = trnModel.V * trnModel.beta;
			obeta = new double[trnModel.S][trnModel.K][trnModel.V];
			Vobeta = new double[trnModel.S][trnModel.K];
			computeObeta();
			System.out.println("Train NO:" + i + " model");
			trainNext();
			adjustTimeWindow(trnModel);
			++i;
		}
	}
	
	/**
	 * Gibbs sampling training for subsequent epochs with evolutionary prior (dJST).
	 * Uses obeta (evolved Dirichlet prior) instead of symmetric beta.
	 */
	public void trainNext() throws IOException {
		System.out.println("Sampling " + trnModel.niters + " iteration!");
		
		int lastIter = trnModel.liter;
		for (trnModel.liter = lastIter + 1; trnModel.liter < trnModel.niters; trnModel.liter++) {
			for (int m = 0; m < trnModel.M; m++) {
				for (int n = 0, N = trnModel.data.docs.get(m).length; n < N; n++) {
					int[] result = sampleOlda(m, n);
					trnModel.z[m][n] = result[0];
					trnModel.s[m][n] = result[1];
				}
			}
			
			if (option.savestep > 0) {
				if (trnModel.liter % option.savestep == 0){
					System.out.println("Saving the model at iteration " + trnModel.liter + " ...");
					computeTheta();
					computePhi();
					trnModel.saveModel("model-" + LdaUtil.converseZeroPad(trnModel.liter, 5));
				}
			}
		}
		
		System.out.println("Gibbs sampling completed!\n");
		System.out.println("Saving the final model!\n");
		computeTheta();
		computePhi();
		trnModel.liter--;
		trnModel.saveModel(trnModel.modelName);
	}
	
	private void adjustTimeWindow(OldaModel trnModel) {
		for (int i = option.delta - 1; i > 0; --i) {
			B[i-1] = B[i];
			vocWindow[i-1] = vocWindow[i];
		}
		B[option.delta - 1] = trnModel.phi;
		vocWindow[option.delta - 1] = trnModel.data.localVoc;
	}
	
	/**
	 * Compute evolutionary Dirichlet prior obeta from historical phi (B).
	 * Implements Eq 1 in paper: β^t_{l,z} = μ^t_{l,z} · E^t_{l,z}
	 * Using sliding window approach where each time slice maps to one epoch.
	 */
	protected void computeObeta() {
		for (int l = 0; l < trnModel.S; l++) {
			for (int k = 0; k < trnModel.K; k++) {
				double sum = 0;
				for (int w = 0; w < trnModel.V; w++) {
					double tmpSum = 0;
					for (int t = 0; t < option.delta; t++) {
						if (B[t] == null) continue;
						Integer wid = vocWindow[t].word2id.get(
							trnModel.data.localVoc.id2word.get(w));
						if (wid != null && l < B[t].length && k < B[t][l].length
								&& wid < B[t][l][k].length) {
							tmpSum += trnModel.omega[t] * B[t][l][k][wid];
						}
					}
					if (tmpSum == 0) {
						obeta[l][k][w] = option.beta;
					} else {
						obeta[l][k][w] = tmpSum;
					}
					sum += obeta[l][k][w];
				}
				Vobeta[l][k] = sum;
			}
		}
	}
	
	/**
	 * Sample a (topic, sentiment) pair from the full conditional distribution
	 * for the first epoch using symmetric beta prior.
	 *
	 * P(z=j, l=k | ...) ∝
	 *   (nw[w][j][k] + β) / (nwsum[j][k] + V·β)
	 * × (nd[m][j][k] + α) / (ndsum[m][k] + K·α)
	 * × (ndsum[m][k] + γ) / (nsum[m] + S·γ)
	 *
	 * @return int[]{topic, sentiment}
	 */
	protected int[] sample(int m, int n) {
		int topic = trnModel.z[m][n];
		int sentiment = trnModel.s[m][n];
		int w = trnModel.data.docs.get(m).words[n];

		trnModel.nw[w][topic][sentiment]--;
		trnModel.nd[m][topic][sentiment]--;
		trnModel.nwsum[topic][sentiment]--;
		trnModel.ndsum[m][sentiment]--;
		trnModel.nsum[m]--;
		
		double Sgama = trnModel.S * gama;
		double psum = 0;
		for (int k = 0; k < trnModel.K; k++) {
			for (int l = 0; l < trnModel.S; l++) {
				trnModel.p[k][l] =
					(trnModel.nw[w][k][l] + trnModel.beta) /
					(trnModel.nwsum[k][l] + Vbeta) *
					(trnModel.nd[m][k][l] + trnModel.alpha) /
					(trnModel.ndsum[m][l] + Kalpha) *
					(trnModel.ndsum[m][l] + gama) /
					(trnModel.nsum[m] + Sgama);
				psum += trnModel.p[k][l];
			}
		}
		
		double u = Math.random() * psum;
		double cumsum = 0;
		topic = 0;
		sentiment = 0;
		boolean found = false;
		for (int k = 0; k < trnModel.K && !found; k++) {
			for (int l = 0; l < trnModel.S && !found; l++) {
				cumsum += trnModel.p[k][l];
				if (cumsum > u) {
					topic = k;
					sentiment = l;
					found = true;
				}
			}
		}
		
		trnModel.nw[w][topic][sentiment]++;
		trnModel.nd[m][topic][sentiment]++;
		trnModel.nwsum[topic][sentiment]++;
		trnModel.ndsum[m][sentiment]++;
		trnModel.nsum[m]++;
		
		return new int[]{topic, sentiment};
	}
	
	/**
	 * Sample a (topic, sentiment) pair from the full conditional distribution
	 * for subsequent epochs using evolved beta (obeta) prior.
	 *
	 * P(z=j, l=k | ...) ∝
	 *   (nw[w][j][k] + obeta[k][j][w]) / (nwsum[j][k] + Vobeta[k][j])
	 * × (nd[m][j][k] + α) / (ndsum[m][k] + K·α)
	 * × (ndsum[m][k] + γ) / (nsum[m] + S·γ)
	 *
	 * @return int[]{topic, sentiment}
	 */
	protected int[] sampleOlda(int m, int n) {
		int topic = trnModel.z[m][n];
		int sentiment = trnModel.s[m][n];
		int w = trnModel.data.docs.get(m).words[n];

		trnModel.nw[w][topic][sentiment]--;
		trnModel.nd[m][topic][sentiment]--;
		trnModel.nwsum[topic][sentiment]--;
		trnModel.ndsum[m][sentiment]--;
		trnModel.nsum[m]--;
		
		double Sgama = trnModel.S * gama;
		double psum = 0;
		for (int k = 0; k < trnModel.K; k++) {
			for (int l = 0; l < trnModel.S; l++) {
				trnModel.p[k][l] =
					(trnModel.nw[w][k][l] + obeta[l][k][w]) /
					(trnModel.nwsum[k][l] + Vobeta[l][k]) *
					(trnModel.nd[m][k][l] + trnModel.alpha) /
					(trnModel.ndsum[m][l] + Kalpha) *
					(trnModel.ndsum[m][l] + gama) /
					(trnModel.nsum[m] + Sgama);
				psum += trnModel.p[k][l];
			}
		}
		
		double u = Math.random() * psum;
		double cumsum = 0;
		topic = 0;
		sentiment = 0;
		boolean found = false;
		for (int k = 0; k < trnModel.K && !found; k++) {
			for (int l = 0; l < trnModel.S && !found; l++) {
				cumsum += trnModel.p[k][l];
				if (cumsum > u) {
					topic = k;
					sentiment = l;
					found = true;
				}
			}
		}
		
		trnModel.nw[w][topic][sentiment]++;
		trnModel.nd[m][topic][sentiment]++;
		trnModel.nwsum[topic][sentiment]++;
		trnModel.ndsum[m][sentiment]++;
		trnModel.nsum[m]++;
		
		return new int[]{topic, sentiment};
	}
	
	/**
	 * Compute phi: sentiment-topic-word distribution.
	 * phi[l][k][w] = (nw[w][k][l] + β) / (nwsum[k][l] + V·β)
	 */
	protected void computePhi() {
		for (int l = 0; l < trnModel.S; l++) {
			for (int k = 0; k < trnModel.K; k++) {
				for (int w = 0; w < trnModel.V; w++) {
					trnModel.phi[l][k][w] = (trnModel.nw[w][k][l] + trnModel.beta) /
							(trnModel.nwsum[k][l] + Vbeta);
				}
			}
		}
	}

	/**
	 * Compute theta: document-sentiment-topic distribution.
	 * theta[m][l][k] = (nd[m][k][l] + α) / (ndsum[m][l] + K·α)
	 */
	protected void computeTheta() {
		for (int m = 0; m < trnModel.M; m++) {
			for (int l = 0; l < trnModel.S; l++) {
				for (int k = 0; k < trnModel.K; k++) {
					trnModel.theta[m][l][k] = (trnModel.nd[m][k][l] + trnModel.alpha) /
							(trnModel.ndsum[m][l] + Kalpha);
				}
			}
		}		
	}
}
