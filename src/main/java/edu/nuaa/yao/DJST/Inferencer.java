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



public class Inferencer {	
	// Train model
	public Model trnModel;
	public Vocabulary globalVoc;
	private LdaArgs option;
	
	private Model newModel;
	public int niters = 100;
	
	public Inferencer(LdaArgs option) {
		this.option = option;
		trnModel = new Model();
	}
	
	//-----------------------------------------------------
	// Init method
	//-----------------------------------------------------
	public boolean init() {
		
		if (!trnModel.initEstimatedModel(option)) {
			return false;		
		}
		
		globalVoc = trnModel.data.localVoc;
		computeTrnTheta();
		computeTrnPhi();
		
		return true;
	}
	
	/**
	 * inference new model ~ getting data from a specified dataset
	 * @param newData
	 * @return
	 */
	public Model inference(Corpus newData) {
		System.out.println("init new model");
		Model newModel = new Model();		
		
		newModel.initNewModel(option, newData, trnModel);		
		this.newModel = newModel;		
		
		System.out.println("Sampling " + niters + " iteration for inference!");		
		for (newModel.liter = 1; newModel.liter <= niters; newModel.liter++){
			//System.out.println("Iteration " + newModel.liter + " ...");
			
			// for all newz_i
			for (int m = 0; m < newModel.M; ++m){
				for (int n = 0, l = newModel.data.docs.get(m).length; n < l; n++){
					// (newz_i = newz[m][n]
					// sample from p(z_i|z_-1,w)
					int topic = infSample(m, n);
					newModel.z[m][n] = topic;
				}
			}//end foreach new doc
			
		}// end iterations
		
		System.out.println("Gibbs sampling for inference completed!");
		
		computeNewTheta();
		computeNewPhi();
		newModel.liter--;
		return this.newModel;
	}
	
	public Model inference(String [] strs){
		//System.out.println("read dataset");
		Corpus corpus = Corpus.loadCorpus(strs, globalVoc);
		
		return inference(corpus);
	}
	
	/**
	 * inference new model ~ getting dataset from file specified in option
	 * @return
	 * @throws IOException 
	 */
	public Model inference() throws IOException{	
		//System.out.println("inference");
		
		newModel = new Model();
		if (!newModel.initNewModel(option, trnModel)) {
			return null;
		}
		
		System.out.println("Sampling " + niters + " iteration for inference!");
		
		for (newModel.liter = 1; newModel.liter <= niters; newModel.liter++){
			//System.out.println("Iteration " + newModel.liter + " ...");
			
			// for all newz_i
			for (int m = 0; m < newModel.M; ++m){
				for (int n = 0, N = newModel.data.docs.get(m).length; n < N; n++){
					// (newz_i = newz[m][n]
					// sample from p(z_i|z_-1,w)
					int topic = infSample(m, n);
					newModel.z[m][n] = topic;
				}
			}//end foreach new doc
			
		}// end iterations
		
		System.out.println("Gibbs sampling for inference completed!");		
		System.out.println("Saving the inference outputs!");
		
		computeNewTheta();
		
		int count = 0;
		for (int i = 0; i < newModel.M; i++) {
			List<Pair<Integer, Double>> list = new ArrayList<Pair<Integer, Double>>();
			for (int j = 0; j < newModel.K; j++) {
				Pair<Integer, Double> pair = new Pair<Integer, Double>(j, newModel.theta[i][j]);
				list.add(pair);
			}
			Collections.sort(list);
			System.out.print("docid:" + (i+1) + "\t");
			/*Domain domain = DomainService.getByDomainId(newModel.data.docs.get(i).domain);
			List<Integer> domains = domain.getTopics();
			System.out.print(domain.getDomain() + "\t");
			for (Integer integer : domains) {
				System.out.print(integer + "\t");
			}
//			for (int j = 0; j < 3; j++) {
			if (domains.contains(list.get(0).first) 
					|| domains.contains(list.get(1).first) 
					|| domains.contains(list.get(2).first)
						) {
				count++;
				System.out.print("T");
			} else {
				System.out.print("F");
			}
			System.out.print("\t" + list.get(0).first);
			System.out.print("\t" + list.get(1).first);
			System.out.print("\t" + list.get(2).first);
//			}
//			for (Integer integer : domains) {
//				System.out.print("\t" + integer);
//			}*/
			System.out.println();
		}
		System.out.println("correct:" + count + ",accuracy:" + (double)count/newModel.M);
		computeNewPhi();
		newModel.liter--;
		newModel.saveModel(newModel.dfile + "." + newModel.modelName);		
		
		return newModel;
	}
	
	/**
	 * do sampling for inference
	 * m: document number
	 * n: word number?
	 */
	protected int infSample(int m, int n){
		// remove z_i from the count variables
		int topic = newModel.z[m][n];
		int _w = newModel.data.docs.get(m).words[n];
		int w = newModel.data.lid2gid.get(_w);
		newModel.nw[_w][topic] -= 1;
		newModel.nd[m][topic] -= 1;
		newModel.nwsum[topic] -= 1;
		newModel.ndsum[m] -= 1;
		
		// do multinomial sampling via cummulative method		
		for (int k = 0; k < newModel.K; k++){			
			newModel.p[k] = (trnModel.nw[w][k] + newModel.nw[_w][k] + newModel.beta) /
					(trnModel.nwsum[k] +  newModel.nwsum[k] + trnModel.V * newModel.beta) *
					(newModel.nd[m][k] + newModel.alpha) /
					(newModel.ndsum[m] + trnModel.K * newModel.alpha);
		}
		
		// cummulate multinomial parameters
		for (int k = 1; k < newModel.K; k++){
			newModel.p[k] += newModel.p[k - 1];
		}
		
		// scaled sample because of unnormalized p[]
		double u = Math.random() * newModel.p[newModel.K - 1];
		
		for (topic = 0; topic < newModel.K; topic++){
			if (newModel.p[topic] > u)
				break;
		}
		
		// add newly estimated z_i to count variables
		newModel.nw[_w][topic] += 1;
		newModel.nd[m][topic] += 1;
		newModel.nwsum[topic] += 1;
		newModel.ndsum[m] += 1;
		
		return topic;
	}
	
	protected void computeNewTheta(){
		for (int m = 0; m < newModel.M; m++){
			for (int k = 0; k < newModel.K; k++){
				newModel.theta[m][k] = (newModel.nd[m][k] + newModel.alpha) / (newModel.ndsum[m] + newModel.K * newModel.alpha);
			}//end foreach topic
		}//end foreach new document
	}
	
	protected void computeNewPhi(){
		for (int k = 0; k < newModel.K; k++){
			for (int _w = 0; _w < newModel.V; _w++){
				Integer id = newModel.data.lid2gid.get(_w);
				
				if (id != null){
					newModel.phi[k][_w] = (trnModel.nw[id][k] + newModel.nw[_w][k] + newModel.beta) / (newModel.nwsum[k] + newModel.nwsum[k] + trnModel.V * newModel.beta);
				}
			}//end foreach word
		}// end foreach topic
	}
	
	protected void computeTrnTheta(){
		for (int m = 0; m < trnModel.M; m++){
			for (int k = 0; k < trnModel.K; k++){
				trnModel.theta[m][k] = (trnModel.nd[m][k] + trnModel.alpha) / (trnModel.ndsum[m] + trnModel.K * trnModel.alpha);
			}
		}
	}
	
	protected void computeTrnPhi(){
		for (int k = 0; k < trnModel.K; k++){
			for (int w = 0; w < trnModel.V; w++){
				trnModel.phi[k][w] = (trnModel.nw[w][k] + trnModel.beta) / (trnModel.nwsum[k] + trnModel.V * trnModel.beta);
			}
		}
	}
}
