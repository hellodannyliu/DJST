package edu.nuaa.yao.DJST;

import java.io.File;


public class OldaModel extends Model{
	
	public int delta;
	public double[] omega;
	public int S;
	public boolean init(OldaArgs option) {
		if (option == null) {
    		return false;
    	}
    	gama=option.gama;
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
		S=option.S;
		return true;    	
	}
	
	/**
	 * Init parameters for estimation
	 */
    public boolean initFirstOldaModel() {
    	
    	p = new double[K][S];
    	//System.out.println(dir + File.separator + dfile + File.separator + "1.txt");
		data = Corpus.loadCorpus(dir + File.separator + dfile + File.separator + "1.txt");
     	
		modelName += "-1";
		
     	if (data == null) {
    		System.out.println("Fail to load training data into model!\n");
    		return false;
    	}
    	
    	M = data.M;
    	V = data.V;
    	
    	nw = new int[V][K][S];
    	nd = new int[M][K][S];
    	nwsum = new int[K][S];
    	ndsum = new int[M][S];
    	nsum=new int[M];
    	z = new int[M][];
    	s=new int[M][];
    	theta=new double[M][S][K]; //theta: document - topic distributions, size M x K
	 	phi=new double [S][K][V]; // phi: topic-word distributions, size K x V
    	for (int m = 0; m < M; m++) {
			int N = data.docs.get(m).length;
			z[m] = new int[N];
			s[m]=new int[N];
			//initialize z
			for(int n = 0; n < N; n++) {
				int topic = (int)(Math.random() * K);
				int sentiment =(int)(Math.random()*S);
				s[m][n]=sentiment;
				z[m][n] = topic;
				nw[data.docs.get(m).words[n]][topic][S]++;
				nd[m][topic][S]++;
				nwsum[topic][S]++;
				ndsum[m][S] ++;
				nsum[m]++;
			}
			
		}
    	
    	theta = new double[M][S][K];
    	phi = new double[S][K][V];
    	
    	return true;
    }
    
	/**
	 * Init parameters for estimation
	 */
   public boolean initNewOldaModel(OldaArgs option, Vocabulary globalVoc, int stream) {
    	
    	liter = 0;
    	modelName = option.modelName + "-" + stream;
    	
    	p = new double[K][S];
    	//System.out.println(dir + File.separator + dfile + File.separator + stream + ".txt");
		data = Corpus.loadCorpus(dir + File.separator + dfile + File.separator + stream + ".txt", globalVoc);
     	
     	if (data == null) {
    		System.out.println("Fail to load training data into model!\n");
    		return false;
    	}
    	

    	M = data.M;
    	V = data.V;
    	
    	nw = new int[V][K][S];
    	nd = new int[M][K][S];
    	nwsum = new int[K][S];
    	ndsum = new int[M][S];
    	nsum=new int[M];
    	z = new int[M][];
    	s=new int[M][];
    	theta=new double[M][S][K]; //theta: document - topic distributions, size M x K
	 	phi=new double [S][K][V]; // phi: topic-word distributions, size K x V
    	for (int m = 0; m < M; m++) {
			int N = data.docs.get(m).length;
			z[m] = new int[N];
			s[m]=new int[N];
			//initialize z
			for(int n = 0; n < N; n++) {
				int topic = (int)(Math.random() * K);
				int sentiment =(int)(Math.random()*S);
				s[m][n]=sentiment;
				z[m][n] = topic;
				nw[data.docs.get(m).words[n]][topic][S]++;
				nd[m][topic][S]++;
				nwsum[topic][S]++;
				ndsum[m][S] ++;
				nsum[m]++;
			}
			
		}
    	
    	theta = new double[M][S][K];
    	phi = new double[S][K][V];
    	
    	return true;
    }
}
