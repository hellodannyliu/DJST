package edu.nuaa.yao.DJST;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimlarityAnalyser {

	public Model big;
	public Model small;
	public double[] smallAvg;
	public double[] bigAvg;
	public Map<Integer, Integer> small2big;
	
	public void init(String bigPath, String smallPath) {
		big = new Model();
		big.initEstimatedModel(bigPath);
		
		small = new Model();
		small.initEstimatedModel(smallPath);
		
		small2big = new HashMap<Integer, Integer>(small.V);
		
		smallAvg = new double[small.K];
		bigAvg = new double[big.K];
		
		int n = 0;
		for (int i = 0, h = small.V; i < h; i++) {
			String aword = small.data.localVoc.getWord(i);
			Integer bigid = big.data.localVoc.word2id.get(aword);
			if (null != bigid) {
				small2big.put(i, bigid);
				n++;
				for (int j = 0; j < small.K; j++) {
					smallAvg[j] += small.phi[j][i];
				}
				for (int j = 0; j < big.K; j++) {
					bigAvg[j] += big.phi[j][bigid];
				}
			}
			
		}
		
		for (int i = 0; i < small.K; i++) {
			smallAvg[i] = smallAvg[i] / n;
		}
		for (int i = 0; i < big.K; i++) {
			bigAvg[i] = bigAvg[i] / n;
		}
	}
	
	public double computeSimilarity(int tsmall, int tbig) {
		
		double num = 0;
		double dem1 = 0;
		double dem2 = 0;
		for (Map.Entry<Integer, Integer> entry : small2big.entrySet()) {
			int sid = entry.getKey();
			int bid = entry.getValue();
			num += (small.phi[tsmall][sid] - smallAvg[tsmall]) * 
					(big.phi[tbig][bid] - bigAvg[tbig]);
			dem1 += (small.phi[tsmall][sid] - smallAvg[tsmall]) * (small.phi[tsmall][sid] - smallAvg[tsmall]);
			dem2 += (big.phi[tbig][bid] - bigAvg[tbig]) * (big.phi[tbig][bid] - bigAvg[tbig]);
		}
		return num / (Math.sqrt(dem1) * Math.sqrt(dem2));
	}
	
	public double[] computeSimilarity(int tsmall) {
		double[] result = new double[small.K];
		List<Pair<Integer, Double>> list = new ArrayList<Pair<Integer, Double>>();
		for (int i = 0; i < big.K; i++) {
			result[i] = computeSimilarity(tsmall, i);		
			Pair<Integer, Double> p = new Pair<Integer, Double>(i, result[i]);
			list.add(p);
		}
		Collections.sort(list);
		for (int j = 0; j < 3; j++) {
			System.out.print(list.get(j).first + ":" + list.get(j).second + " ");
		}
		System.out.println();
		return result;
	}
	
	public double[][] computeSimilarity() {
		double[][] result = new double[small.K][big.K];
		for (int i = 0; i < small.K; i++) {
			List<Pair<Integer, Double>> list = new ArrayList<Pair<Integer, Double>>();
			for (int j = 0; j < big.K; j++) {
				result[i][j] = computeSimilarity(i, j);
				Pair<Integer, Double> p = new Pair<Integer, Double>(j, result[i][j]);
				list.add(p);
			}
			Collections.sort(list);
			System.out.print(i + ": ");
			for (int j = 0; j < 3; j++) {
				System.out.print(list.get(j).first + ":" + list.get(j).second + " ");
			}
			System.out.println();
		}
		return result;
	}
	public static void main(String[] args) {
		SimlarityAnalyser simlarityAnalyser = new SimlarityAnalyser();
		simlarityAnalyser.init("./models/0527", "./models/0603/1");
		simlarityAnalyser.computeSimilarity();
	}
}
