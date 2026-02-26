/*
 * Copyright (C) 2007 by
 * 
 * 	Xuan-Hieu Phan
 *	hieuxuan@ecei.tohoku.ac.jp or pxhieu@gmail.com
 * 	Graduate School of Information Sciences
 * 	Tohoku University
 * 
 *  Cam-Tu Nguyen
 *  ncamtu@gmail.com
 *  College of Technology
 *  Vietnam National University, Hanoi
 *
 * JGibbsLDA is a free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 *
 * JGibbsLDA is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JGibbsLDA; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 */

package edu.nuaa.yao.DJST;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Inferencer for the dJST model.
 * Updated to work with 3D arrays for joint sentiment-topic inference.
 */
public class Inferencer {	
	public Model trnModel;
	public Vocabulary globalVoc;
	private LdaArgs option;
	
	private Model newModel;
	public int niters = 100;
	
	public Inferencer(LdaArgs option) {
		this.option = option;
		trnModel = new Model();
	}
	
	public boolean init() {
		if (!trnModel.initEstimatedModel(option)) {
			return false;		
		}
		
		globalVoc = trnModel.data.localVoc;
		computeTrnTheta();
		computeTrnPhi();
		
		return true;
	}
	
	public Model inference(Corpus newData) {
		System.out.println("init new model");
		Model newModel = new Model();		
		
		newModel.initNewModel(option, newData, trnModel);		
		this.newModel = newModel;		
		
		System.out.println("Sampling " + niters + " iteration for inference!");		
		for (newModel.liter = 1; newModel.liter <= niters; newModel.liter++){
			for (int m = 0; m < newModel.M; ++m){
				for (int n = 0, l = newModel.data.docs.get(m).length; n < l; n++){
					int[] result = infSample(m, n);
					newModel.z[m][n] = result[0];
					newModel.s[m][n] = result[1];
				}
			}
		}
		
		System.out.println("Gibbs sampling for inference completed!");
		
		computeNewTheta();
		computeNewPhi();
		newModel.liter--;
		return this.newModel;
	}
	
	public Model inference(String[] strs){
		Corpus corpus = Corpus.loadCorpus(strs, globalVoc);
		return inference(corpus);
	}
	
	public Model inference() throws IOException{	
		newModel = new Model();
		if (!newModel.initNewModel(option, trnModel)) {
			return null;
		}
		
		System.out.println("Sampling " + niters + " iteration for inference!");
		
		for (newModel.liter = 1; newModel.liter <= niters; newModel.liter++){
			for (int m = 0; m < newModel.M; ++m){
				for (int n = 0, N = newModel.data.docs.get(m).length; n < N; n++){
					int[] result = infSample(m, n);
					newModel.z[m][n] = result[0];
					newModel.s[m][n] = result[1];
				}
			}
		}
		
		System.out.println("Gibbs sampling for inference completed!");		
		System.out.println("Saving the inference outputs!");
		
		computeNewTheta();
		computeNewPhi();
		newModel.liter--;
		newModel.saveModel(newModel.dfile + "." + newModel.modelName);		
		
		return newModel;
	}
	
	/**
	 * Sample (topic, sentiment) pair for inference.
	 * Combines training model counts with new model counts.
	 * @return int[]{topic, sentiment}
	 */
	protected int[] infSample(int m, int n){
		int topic = newModel.z[m][n];
		int sentiment = newModel.s[m][n];
		int _w = newModel.data.docs.get(m).words[n];
		Integer wObj = newModel.data.lid2gid.get(_w);
		int w = (wObj != null) ? wObj : _w;

		newModel.nw[_w][topic][sentiment]--;
		newModel.nd[m][topic][sentiment]--;
		newModel.nwsum[topic][sentiment]--;
		newModel.ndsum[m][sentiment]--;
		newModel.nsum[m]--;
		
		double Kalpha = newModel.K * newModel.alpha;
		double Vbeta = trnModel.V * newModel.beta;
		double Sgama = newModel.S * newModel.gama;

		double psum = 0;
		for (int k = 0; k < newModel.K; k++){
			for (int l = 0; l < newModel.S; l++) {
				double nwTrn = (w < trnModel.V) ? trnModel.nw[w][k][l] : 0;
				double nwsumTrn = trnModel.nwsum[k][l];
				newModel.p[k][l] = 
					(nwTrn + newModel.nw[_w][k][l] + newModel.beta) /
					(nwsumTrn + newModel.nwsum[k][l] + Vbeta) *
					(newModel.nd[m][k][l] + newModel.alpha) /
					(newModel.ndsum[m][l] + Kalpha) *
					(newModel.ndsum[m][l] + newModel.gama) /
					(newModel.nsum[m] + Sgama);
				psum += newModel.p[k][l];
			}
		}
		
		double u = Math.random() * psum;
		double cumsum = 0;
		topic = 0;
		sentiment = 0;
		boolean found = false;
		for (int k = 0; k < newModel.K && !found; k++){
			for (int l = 0; l < newModel.S && !found; l++) {
				cumsum += newModel.p[k][l];
				if (cumsum > u) {
					topic = k;
					sentiment = l;
					found = true;
				}
			}
		}
		
		newModel.nw[_w][topic][sentiment]++;
		newModel.nd[m][topic][sentiment]++;
		newModel.nwsum[topic][sentiment]++;
		newModel.ndsum[m][sentiment]++;
		newModel.nsum[m]++;
		
		return new int[]{topic, sentiment};
	}
	
	protected void computeNewTheta(){
		for (int m = 0; m < newModel.M; m++){
			for (int l = 0; l < newModel.S; l++) {
				for (int k = 0; k < newModel.K; k++){
					newModel.theta[m][l][k] = (newModel.nd[m][k][l] + newModel.alpha) /
						(newModel.ndsum[m][l] + newModel.K * newModel.alpha);
				}
			}
		}
	}
	
	protected void computeNewPhi(){
		for (int l = 0; l < newModel.S; l++) {
			for (int k = 0; k < newModel.K; k++){
				for (int _w = 0; _w < newModel.V; _w++){
					Integer id = newModel.data.lid2gid.get(_w);
					if (id != null && id < trnModel.V){
						newModel.phi[l][k][_w] = (trnModel.nw[id][k][l] + newModel.nw[_w][k][l] + newModel.beta) /
							(trnModel.nwsum[k][l] + newModel.nwsum[k][l] + trnModel.V * newModel.beta);
					}
				}
			}
		}
	}
	
	protected void computeTrnTheta(){
		for (int m = 0; m < trnModel.M; m++){
			for (int l = 0; l < trnModel.S; l++) {
				for (int k = 0; k < trnModel.K; k++){
					trnModel.theta[m][l][k] = (trnModel.nd[m][k][l] + trnModel.alpha) /
						(trnModel.ndsum[m][l] + trnModel.K * trnModel.alpha);
				}
			}
		}
	}
	
	protected void computeTrnPhi(){
		for (int l = 0; l < trnModel.S; l++) {
			for (int k = 0; k < trnModel.K; k++){
				for (int w = 0; w < trnModel.V; w++){
					trnModel.phi[l][k][w] = (trnModel.nw[w][k][l] + trnModel.beta) /
						(trnModel.nwsum[k][l] + trnModel.V * trnModel.beta);
				}
			}
		}
	}
}
