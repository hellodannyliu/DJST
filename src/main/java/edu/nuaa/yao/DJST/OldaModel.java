package edu.nuaa.yao.DJST;

import java.io.File;


public class OldaModel extends Model{
	
	public int delta;
	public double[] omega;
	
	public boolean init(OldaArgs option) {
		if (option == null) {
    		return false;
    	}
    	
    	modelName = option.modelName;
		K = option.ntopics;
		
		alpha = option.alpha;
		if (alpha < 0.0) {
			alpha = 50.0 / K;
		}
		
		if (option.beta >= 0) {
			beta = option.beta;
		}
		
		niters = option.niters;
    	savestep = option.savestep;

		dir = option.dir;
		if (dir.endsWith(File.separator))
			dir = dir.substring(0, dir.length() - 1);
		
		dfile = option.dfile;
		twords = option.twords;
		wordMapFile = option.wordMapFileName;
		delta = option.delta;
		omega = option.omega;
		docnum=option.docnum;
		return true;    	
	}
	
	/**
	 * Init parameters for estimation
	 */
    public boolean initFirstOldaModel() {
    	
    	p = new double[K];
    	//System.out.println(dir + File.separator + dfile + File.separator + "1.txt");
		data = Corpus.loadCorpus(dir + File.separator + dfile + File.separator + "1.txt");
     	
		modelName += "-1";
		
     	if (data == null) {
    		System.out.println("Fail to load training data into model!\n");
    		return false;
    	}
    	
    	M = data.M;
    	V = data.V;
    	
    	nw = new int[V][K];
    	nd = new int[M][K];
    	nwsum = new int[K];
    	ndsum = new int[M];
    	z = new int[M][];
    	
    	for (int m = 0; m < M; m++) {
			int N = data.docs.get(m).length;
			z[m] = new int[N];
			
			//initialize z
			for(int n = 0; n < N; n++) {
				int topic = (int)(Math.random() * K);
				z[m][n] = topic;
				nw[data.docs.get(m).words[n]][topic]++;
				nd[m][topic]++;
				nwsum[topic]++;
			}
			ndsum[m] = N;
		}
    	
    	theta = new double[M][K];
    	phi = new double[K][V];
    	
    	return true;
    }
    
	/**
	 * Init parameters for estimation
	 */
    public boolean initNewOldaModel(OldaArgs option, Vocabulary globalVoc, int stream) {
    	
    	liter = 0;
    	modelName = option.modelName + "-" + stream;
    	
    	p = new double[K];
    	//System.out.println(dir + File.separator + dfile + File.separator + stream + ".txt");
		data = Corpus.loadCorpus(dir + File.separator + dfile + File.separator + stream + ".txt", globalVoc);
     	
     	if (data == null) {
    		System.out.println("Fail to load training data into model!\n");
    		return false;
    	}
    	
    	M = data.M;
    	V = data.V;
    	
    	nw = new int[V][K];
    	nd = new int[M][K];
    	nwsum = new int[K];
    	ndsum = new int[M];
    	z = new int[M][];
    	
    	for (int m = 0; m < M; m++) {
			int N = data.docs.get(m).length;
			z[m] = new int[N];
			
			//initialize z
			for(int n = 0; n < N; n++) {
				int topic = (int)(Math.random() * K);
				z[m][n] = topic;
				nw[data.docs.get(m).words[n]][topic]++;
				nd[m][topic]++;
				nwsum[topic]++;
			}
			ndsum[m] = N;
		}
    	
    	theta = new double[M][K];
    	phi = new double[K][V];
    	
    	return true;
    }
}
