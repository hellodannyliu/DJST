package edu.nuaa.yao.DJST;

import java.io.IOException;



public class OldaTrainer {
	
	public OldaArgs option;
	public Vocabulary globalVoc;	
	public OldaModel trnModel;
	public double[][][] B;
	public double[][] obeta;
	public Vocabulary[] vocWindow;
	public double Kalpha;
	public double Vbeta;
	public double[] Vobeta;
	public int docnum;
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
		B = new double[option.delta][][];
		obeta = new double[trnModel.K][];
		globalVoc = trnModel.data.localVoc;
		vocWindow = new Vocabulary[option.delta];
		Kalpha = trnModel.K * trnModel.alpha;
		Vbeta = trnModel.V * trnModel.beta;
		Vobeta = new double[trnModel.K];
		docnum=trnModel.docnum;
		return true;
	}
	
	public void train() throws IOException {
		System.out.println("Sampling " + trnModel.niters + " iteration!");
		
		int lastIter = trnModel.liter;
		for (trnModel.liter = lastIter + 1; trnModel.liter < trnModel.niters; trnModel.liter++) {
//			System.out.println("Iteration " + trnModel.liter + " ...");
			
			for (int m = 0; m < trnModel.M; m++) {
				for (int n = 0, N = trnModel.data.docs.get(m).length; n < N; n++) {
					int topic = sample(m, n);
					trnModel.z[m][n] = topic;
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
	
	public void trainOlda() throws IOException {
		
		System.out.println("Train NO:" + 1 + " model");

		// train first time window
		train();
		B[0] = trnModel.phi;
		vocWindow[0] = trnModel.data.localVoc;
		
		int i=0;
		//System.out.println(docnum);
		
		for (i = 2; i <= option.delta; i++) {
			System.out.println("Train NO:" + i + " model");
			if (!trnModel.initNewOldaModel(option, globalVoc, i)) {
				System.out.println("train NO:" + i + " model failed!");
				return;
			}
			train();
			B[i-1] = trnModel.phi;
			vocWindow[i-1] = trnModel.data.localVoc;
		}
		
		while((i<=docnum)&&trnModel.initNewOldaModel(option, globalVoc, i)) {
			computeObeta();
			System.out.println("Train NO:" + i + " model");
			trainNext();
			adjustTimeWindow(trnModel);
			++i;
		}
	}
	
	public void trainNext() throws IOException {
		System.out.println("Sampling " + trnModel.niters + " iteration!");
		
		int lastIter = trnModel.liter;
		for (trnModel.liter = lastIter + 1; trnModel.liter < trnModel.niters; trnModel.liter++) {
//			System.out.println("Iteration " + trnModel.liter + " ...");
			
			for (int m = 0; m < trnModel.M; m++) {
				for (int n = 0, N = trnModel.data.docs.get(m).length; n < N; n++) {
					int topic = sampleOlda(m, n);
					trnModel.z[m][n] = topic;
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
	
	protected void computeObeta() {
		for (int i = 0; i < trnModel.K; ++i) {
			obeta[i] = new double[trnModel.V];
			double sum = 0;
			for (int j = 0; j < trnModel.V; j++) {
				double tmpSum = 0;
				for (int j2 = 0; j2 < option.delta; j2++) {
					Integer k = vocWindow[j2].word2id.get(trnModel.data.localVoc.id2word.get(j));
					if (k != null && B[j2][i].length > k) {
						tmpSum += trnModel.omega[j2] * B[j2][i][k];
					}
				}
				if(0 == tmpSum) {
					obeta[i][j] = option.beta;
				} else {
					obeta[i][j] = tmpSum;
				}
				sum += tmpSum;
			}
			Vobeta[i] = sum;
		}
	}
	
	/**
     * Sample a topic z_i from the full conditional distribution: p(z_i = j |
     * z_-i, w) = (n_-i,j(w_i) + beta)/(n_-i,j(.) + W * beta) * (n_-i,j(d_i) +
     * alpha)/(n_-i,.(d_i) + K * alpha) <br>
     * 根据上述公式计算文档m中第n个词语的主题的完全条件分布，输出最可能的主题
     *
     * @param m document
     * @param n word
     */
	protected int sample(int m, int n) {
		// remove z_i from the count variables  先将这个词从计数器中抹掉
		int topic = trnModel.z[m][n];
		int w = trnModel.data.docs.get(m).words[n];
		trnModel.nw[w][topic]--;
		trnModel.nd[m][topic]--;
		trnModel.nwsum[topic]--;
		trnModel.ndsum[m]--;
		
        // do multinomial sampling via cumulative method: 通过多项式方法采样多项式分布
		for (int k = 0; k < trnModel.K; k++) {
			trnModel.p[k] = (trnModel.nd[m][k] + trnModel.alpha) / (trnModel.ndsum[m] + Kalpha) * 
					(trnModel.nw[w][k] + trnModel.beta) / (trnModel.nwsum[k] + Vbeta);
		}
		
        // cumulate multinomial parameters  累加多项式分布的参数
		for (int k = 1; k < trnModel.K; k++) {
			trnModel.p[k] += trnModel.p[k - 1];
		}
		
        // scaled sample because of unnormalised p[] 正则化
		double u = Math.random() * trnModel.p[trnModel.K - 1];

		for (topic = 0; topic < trnModel.K; topic++) {
			if (trnModel.p[topic] > u) {
				break;
			}
		}
		trnModel.nw[w][topic]++;
		trnModel.nd[m][topic]++;
		trnModel.nwsum[topic]++;
		trnModel.ndsum[m]++;
		
		return topic;
	}
	
	/**
     * Sample a topic z_i from the full conditional distribution: p(z_i = j |
     * z_-i, w) = (n_-i,j(w_i) + beta)/(n_-i,j(.) + W * beta) * (n_-i,j(d_i) +
     * alpha)/(n_-i,.(d_i) + K * alpha) <br>
     * 根据上述公式计算文档m中第n个词语的主题的完全条件分布，输出最可能的主题
     *
     * @param m document
     * @param n word
     */
	protected int sampleOlda(int m, int n) {
		// remove z_i from the count variables  先将这个词从计数器中抹掉
		int topic = trnModel.z[m][n];
		int w = trnModel.data.docs.get(m).words[n];
		trnModel.nw[w][topic]--;
		trnModel.nd[m][topic]--;
		trnModel.nwsum[topic]--;
		trnModel.ndsum[m]--;
		
        // do multinomial sampling via cumulative method: 通过多项式方法采样多项式分布
		for (int k = 0; k < trnModel.K; k++) {
			trnModel.p[k] = (trnModel.nd[m][k] + trnModel.alpha) / (trnModel.ndsum[m] + Kalpha) * 
					(trnModel.nw[w][k] + obeta[k][w]) / (trnModel.nwsum[k] + Vobeta[k]);
		}
		
        // cumulate multinomial parameters  累加多项式分布的参数
		for (int k = 1; k < trnModel.K; k++) {
			trnModel.p[k] += trnModel.p[k - 1];
		}
		
        // scaled sample because of unnormalised p[] 正则化
		double u = Math.random() * trnModel.p[trnModel.K - 1];

		for (topic = 0; topic < trnModel.K; topic++) {
			if (trnModel.p[topic] > u) {
				break;
			}
		}
		trnModel.nw[w][topic]++;
		trnModel.nd[m][topic]++;
		trnModel.nwsum[topic]++;
		trnModel.ndsum[m]++;
		
		return topic;
	}
	
	protected void computePhi() {
		for (int k = 0; k < trnModel.K; k++) {
			for (int w = 0; w < trnModel.V; w++) {
				trnModel.phi[k][w] = (trnModel.nw[w][k] + trnModel.beta) /
						(trnModel.nwsum[k] + Vbeta);
			}
		}
	}

	protected void computeTheta() {
		for (int m = 0; m < trnModel.M; m++) {
			for (int k = 0; k < trnModel.K; k++) {
				trnModel.theta[m][k] = (trnModel.nd[m][k] + trnModel.alpha) /
						(trnModel.ndsum[m] + Kalpha);
			}
		}		
	}
}
