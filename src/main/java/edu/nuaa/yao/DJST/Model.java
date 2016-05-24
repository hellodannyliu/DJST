package edu.nuaa.yao.DJST;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;



public class Model {
	
	//---------------------------------------------------------------
	//	Class Variables
	//---------------------------------------------------------------
	
	public static String tassignSuffix;	//suffix for topic assignment file
	public static String thetaSuffix;		//suffix for theta (topic - document distribution) file
	public static String phiSuffix;		//suffix for phi file (topic - word distribution) file
	public static String othersSuffix; 	//suffix for containing other parameters
	public static String twordsSuffix;		//suffix for file containing words-per-topics
	
	//---------------------------------------------------------------
	//	Model Parameters and Variables
	//---------------------------------------------------------------
	
	public String wordMapFile; 		//file that contain word to id map
	public String trainlogFile; 	//training log file	
	
	public String dir;
	public String dfile;
	public String modelName;
	public int modelStatus; 		//see Constants class for status of model
	public Corpus data;			// link to a dataset
	
	public int K;	//number of topic
	public int V;	//vocabulary size
	public int M;	//number of document
	public double alpha;
	public double beta;
	public int niters; //number of Gibbs sampling iteration
	public int liter; //the iteration at which the model was saved	
	public int savestep; //saving period
	public int twords; //print out top words per each topic
	public int withrawdata;
	public int docnum;
	// Estimated/Inferenced parameters
	public double[][] theta;
	public double[][] phi;
	
	// Temp variables while sampling
	
	/**
     * topic assignments for each word.<br>
     * 每个词语的主题 z[i][j] := 文档i的第j个词语的主题编号
     */
    public int z[][];

    /**
     * cwt[i][j] number of instances of word i (term?) assigned to topic j.<br>
     * 计数器，nw[i][j] := 词语i归入主题j的次数
     */
    public int[][] nw;

    /**
     * na[i][j] number of words in document i assigned to topic j.<br>
     * 计数器，nd[i][j] := 文档[i]中归入主题j的词语的个数
     */
    public int[][] nd;

    /**
     * nwsum[j] total number of words assigned to topic j.<br>
     * 计数器，nwsum[j] := 归入主题j词语的个数
     */
    public int[] nwsum;

    /**
     * nasum[i] total number of words in document i.<br>
     * 计数器,ndsum[i] := 文档i中全部词语的数量
     */
    public int[] ndsum;
    
    // temp variables for sampling
    public double [] p; 
 	
    public Model() {
    	this.wordMapFile = "wordmap.txt";
    	this.trainlogFile = "trainlog.txt";
    	tassignSuffix = ".tassign";
    	thetaSuffix = ".theta";
    	phiSuffix = ".phi";
    	othersSuffix = ".others";
    	twordsSuffix = ".txt";
		
    	this.dir = "./";
    	this.dfile = "trndocs.dat";
    	this.modelName = "model-final";
    	this.modelStatus = Constants.MODEL_STATUS_UNKNOWN;		
		
		M = 0;
		V = 0;
		K = 100;
		alpha = 50.0 / K;
		beta = 0.1;
		niters = 2000;
		liter = 0;
		
		z = null;
		nw = null;
		nd = null;
		nwsum = null;
		ndsum = null;
		theta = null;
		phi = null;
    }
    
	//---------------------------------------------------------------
	//	I/O Methods
	//---------------------------------------------------------------
	/**
	 * read other file to get parameters
	 */
	protected boolean readOthersFile(String otherFile){
		//open file <model>.others to read:
		
		try {
			BufferedReader reader = new BufferedReader(new FileReader(otherFile));
			String line;
			while((line = reader.readLine()) != null){
				StringTokenizer tknr = new StringTokenizer(line,"= \t\r\n");
				
				int count = tknr.countTokens();
				if (count != 2)
					continue;
				
				String optstr = tknr.nextToken();
				String optval = tknr.nextToken();
				
				if (optstr.equalsIgnoreCase("alpha")){
					alpha = Double.parseDouble(optval);					
				}
				else if (optstr.equalsIgnoreCase("beta")){
					beta = Double.parseDouble(optval);
				}
				else if (optstr.equalsIgnoreCase("ntopics")){
					K = Integer.parseInt(optval);
				}
				else if (optstr.equalsIgnoreCase("liter")){
					liter = Integer.parseInt(optval);
				}
				else if (optstr.equalsIgnoreCase("nwords")){
					V = Integer.parseInt(optval);
				}
				else if (optstr.equalsIgnoreCase("ndocs")){
					M = Integer.parseInt(optval);
				}
				else {
					// any more?
				}
			}
			
			reader.close();
		}
		catch (Exception e){
			System.out.println("Error while reading other file:" + e.getMessage());
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	protected boolean readTAssignFile(String tassignFile){
		
		BufferedReader reader = null;
		try {
			int i,j;
			reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(tassignFile), "UTF-8"));
			
			String line;
			z = new int[M][];			
			data = new Corpus(M);
			data.V = V;			
			for (i = 0; i < M; i++){
				line = reader.readLine();
				StringTokenizer tknr = new StringTokenizer(line, " \t\r\n");
				
				int length = tknr.countTokens();
				
				int[] words = new int[length];
				int[] topics = new int[length];
				
				for (j = 0; j < length; j++){
					String token = tknr.nextToken();
					
					StringTokenizer tknr2 = new StringTokenizer(token, ":");
					if (tknr2.countTokens() != 2){
						System.out.println("Invalid word-topic assignment line\n");
						return false;
					}
					
					words[j] = Integer.parseInt(tknr2.nextToken());
					topics[j] = Integer.parseInt(tknr2.nextToken());
				}//end for each topic assignment
				
				//allocate and add new document to the corpus
				Document doc = new Document(length, words);
				data.setDoc(doc, i);
				
				//assign values for z
				z[i] = new int[length];
				for (j = 0; j < length; j++){
					z[i][j] = topics[j];
				}
				
			}//end for each doc
			
			
		}
		catch (Exception e){
			System.out.println("Error while loading model: " + e.getMessage());
			e.printStackTrace();
			return false;
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return true;
	}
	
	/**
	 * 2015-06-04 read model-final.theta
	 * @param thetaFile
	 * @return
	 */
	protected boolean readThetaFile(String thetaFile){
		
		BufferedReader reader = null;
		try {
			int i,j;
			reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(thetaFile), "UTF-8"));
			
			String line;
			theta = new double[M][K];
			for (i = 0; i < M; i++){
				line = reader.readLine();
				StringTokenizer tknr = new StringTokenizer(line, " \t\r\n");

				double[] ts = new double[K];
				for (j = 0; j < K; j++){
					String token = tknr.nextToken();
					ts[j] = Double.parseDouble(token);
				}//end for each topic assignment
				theta[i] = ts;
			}//end for each doc
			
		}
		catch (Exception e){
			System.out.println("Error while loading model: " + e.getMessage());
			e.printStackTrace();
			return false;
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return true;
	}
	
	
	/**
	 * 2015-06-04 read model-final.phi
	 * @param phiFile
	 * @return
	 */
	protected boolean readPhiFile(String phiFile){
		
		BufferedReader reader = null;
		try {
			int i,j;
			reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(phiFile), "UTF-8"));
			
			String line;
			phi = new double[K][V];
			for (i = 0; i < K; i++){
				line = reader.readLine();
				StringTokenizer tknr = new StringTokenizer(line, " \t\r\n");

				double[] ts = new double[V];
				for (j = 0; j < V; j++){
					String token = tknr.nextToken();
					ts[j] = Double.parseDouble(token);
				}//end for each topic assignment
				phi[i] = ts;
			}//end for each doc
			
		}
		catch (Exception e){
			System.out.println("Error while loading model: " + e.getMessage());
			e.printStackTrace();
			return false;
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return true;
	}
	
	/**
	 * load saved model
	 */
	public boolean loadModel(){
		
		if (!readOthersFile(dir + File.separator + modelName + othersSuffix)) {
			return false;
		}
		
		if (!readTAssignFile(dir + File.separator + modelName + tassignSuffix)) {
			return false;
		}
		
		// read dictionary
		Vocabulary voc = new Vocabulary();
		if (!voc.readWordMap(dir + File.separator + wordMapFile)) {
			return false;
		}
		data.localVoc = voc;
		
		return true;
	}
    
    /**
	 * Save word-topic assignments for this model
	 */
	protected boolean saveModelTAssign(String filename){
		
		BufferedWriter bw = null;
		try {
			bw = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(filename), "UTF-8"));
			for (int m = 0; m < M; m++) {
				for (int n = 0, N = data.docs.get(m).length; n < N; n++) {
					bw.write(data.docs.get(m).words[n] + ":" + z[m][n] + " ");
				}
				bw.write("\n");
			}
			return true;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return false;
	}
    
	/**
	 * Save theta (topic distribution) for this model
	 */
	protected boolean saveModelTheta(String filename){
		
		BufferedWriter writer = null;
		try{
			writer = new BufferedWriter(new FileWriter(filename));
			for (int i = 0; i < M; i++){
				for (int j = 0; j < K; j++){
					writer.write(theta[i][j] + " ");
				}
				writer.write("\n");
			}
			return true;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return false;
	}
	
	/**
	 * Save word-topic distribution
	 */
	
	protected boolean saveModelPhi(String filename){
		
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(filename));
			
			for (int i = 0; i < K; i++){
				for (int j = 0; j < V; j++){
					writer.write(phi[i][j] + " ");
				}
				writer.write("\n");
			}
			return true;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return false;
	}
	
	/**
	 * Save other information of this model
	 */
	protected boolean saveModelOthers(String filename){
		
		BufferedWriter writer = null;
		try{
			writer = new BufferedWriter(new FileWriter(filename));
			
			writer.write("alpha=" + alpha + "\n");
			writer.write("beta=" + beta + "\n");
			writer.write("ntopics=" + K + "\n");
			writer.write("ndocs=" + M + "\n");
			writer.write("nwords=" + V + "\n");
			writer.write("liters=" + liter + "\n");
			writer.write("dfile=" + dfile);
			return true;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return false;
	}
	
	/**
	 * Save theta (topic distribution) for this model
	 */
	protected boolean saveModelTwords(String filename){
		
		BufferedWriter bw = null;
		try {
			bw = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(filename), "UTF-8"));
			
			if (twords > V || 0 == twords){
				twords = V;
			}
			
			for (int k = 0; k < K; k++){
				Map<Integer, Double> wordsProbsMap = new HashMap<Integer, Double>(V);
				for (int w = 0; w < V; w++) {
					wordsProbsMap.put(w, phi[k][w]);
				}
				List<Entry<Integer, Double>> wordsProbsList = 
						new ArrayList<Entry<Integer, Double>>(wordsProbsMap.entrySet());
				Collections.sort(wordsProbsList, new Comparator<Entry<Integer, Double>>() {

					public int compare(Entry<Integer, Double> o1, Entry<Integer, Double> o2) {
						return -o1.getValue().compareTo(o2.getValue());
					}
					
				});			
				bw.write("Topic " + k + "th:\n");
				for (int i = 0; i < twords; i++) {
					String word = data.localVoc.getWord(wordsProbsList.get(i).getKey());
					bw.write("\t" + word + " " + wordsProbsList.get(i).getValue() + "\n");
				}
			}
			return true;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return false;
	}
	
	protected boolean saveModelTwordsWithDomain(String filename) throws IOException{
		
		BufferedWriter bw = null;
		
			bw = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(filename), "UTF-8"));
			
			if (twords > V || 0 == twords){
				twords = V;
			}
			
			//boolean[] domainExists = new boolean[Tool.DOMAINS.size()];

			for (int k = 0; k < K; k++){
				Map<Integer, Double> wordsProbsMap = new HashMap<Integer, Double>(V);
				for (int w = 0; w < V; w++) {
					wordsProbsMap.put(w, phi[k][w]);
				}
				List<Integer> tWordsIndexArray = new ArrayList<Integer>(); 
				for(int j = 0; j < V; j++){
					tWordsIndexArray.add(new Integer(j));
				}
				Collections.sort(tWordsIndexArray, new Model.TwordsComparable(phi[k]));
				bw.write("topic " + k + "\t:\n");
				for(int t = 0; t < twords; t++){
					bw.write(data.localVoc.id2word.get(tWordsIndexArray.get(t)) + " " + phi[k][tWordsIndexArray.get(t)] + "\n");
				}
				bw.write("\n");
			}
			bw.close();
				/*list<Entry<Integer, Double>> wordsProbsList = 
						new ArrayList<Entry<Integer, Double>>(wordsProbsMap.entrySet());
				Collections.sort(wordsProbsList, new Comparator<Entry<Integer, Double>>() {

					public int compare(Entry<Integer, Double> o1, Entry<Integer, Double> o2) {
						return -o1.getValue().compareTo(o2.getValue());
					}
					
				});
				
				List<Pair<Integer, Double>> docsProbsList = new ArrayList<Pair<Integer, Double>>();
				//double[] docsProbs = new double[Tool.DOMAINS.size()];
				//for (int i = 0; i < M; i++) {
				//	docsProbs[Tool.DOMAINS.indexOf(data.docs.get(i).domain)] += theta[i][k];
				//}
				for (int i = 0; i < docsProbs.length; i++) {
					Pair<Integer, Double> pair = new Pair<Integer, Double>(i, docsProbs[i]);
					docsProbsList.add(pair);
				}
				Collections.sort(docsProbsList);
				
				int d1 = docsProbsList.get(0).first;
				int d2 = docsProbsList.get(1).first;
				domainExists[d1] = true;
				domainExists[d2] = true;
				bw.write("Topic " + k + "th:" + Tool.DOMAINS.get(d1) 
						+ " " + Tool.DOMAINS.get(d2) + "\n");
				for (int i = 0; i < twords; i++) {
					String word = data.localVoc.getWord(wordsProbsList.get(i).getKey());
					bw.write("\t" + word + " " + wordsProbsList.get(i).getValue() + "\n");
				}
			}
			int count = 0;
			bw.write("Topic:-------------------------------------------------------------\n");
			bw.write("Topic:domain don't exists:");
			for (int i = 0; i < domainExists.length; i++) {
				if (domainExists[i]) {
					count++;
				} else {
					bw.write(Tool.DOMAINS.get(i) + "\t");
				}
			}
			bw.write("\n");
			bw.write("Topic:domain exists num:" + count + "/" + domainExists.length + "\n");
			bw.write("Topic:doamin exists percentage:" + (double)count / domainExists.length);
			return true;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		*/
			
		
		return false;
	}
	
	/**
	 * Save model
	 */
	public boolean saveModelWithDomain(String modelName){
		if (!saveModelTAssign(dir + File.separator + modelName + tassignSuffix)) {
			return false;
		}
		
		if (!saveModelOthers(dir + File.separator + modelName + othersSuffix)) {			
			return false;
		}
		
		if (!saveModelTheta(dir + File.separator + modelName + thetaSuffix)) {
			return false;
		}
		
		if (!saveModelPhi(dir + File.separator + modelName + phiSuffix)) {
			return false;
		}
		
		if (twords > 0) {
			if (!saveModelWithDomain(dir + File.separator + modelName + twordsSuffix))
				return false;
		}
		return true;
	}
	
    /**
	 * Save model
     * @throws IOException 
	 */
	public boolean saveModel(String modelName) throws IOException{
		if (!saveModelTAssign(dir + File.separator + modelName + tassignSuffix)) {
			return false;
		}
		
		if (!saveModelOthers(dir + File.separator + modelName + othersSuffix)) {			
			return false;
		}
		
		if (!saveModelTheta(dir + File.separator + modelName + thetaSuffix)) {
			return false;
		}
		
		if (!saveModelPhi(dir + File.separator + modelName + phiSuffix)) {
			return false;
		}
		
		if (twords > 0) {
			if (!saveModelTwordsWithDomain(dir + File.separator + modelName + twordsSuffix))
				return false;
		}
		return true;
	}
	
	//---------------------------------------------------------------
	//	Init Methods
	//---------------------------------------------------------------
    /**
     * initialize model parameters
     * @param option
     * @return 
     */
    protected boolean init(LdaArgs option) {
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
		
		dir = option.dir;
		if (dir.endsWith(File.separator))
			dir = dir.substring(0, dir.length() - 1);
		
		dfile = option.dfile;
		twords = option.twords;
		wordMapFile = option.wordMapFileName;
		
		return true;    	
    }
    
    /**
	 * Init parameters for estimation
	 */
    public boolean initNewModel(LdaArgs option) {
    	
    	if (!init(option)) {
    		return false;
    	}
    	
    	p = new double[K];
    	
    	data = Corpus.loadCorpus(dir + File.separator + dfile);
    	if (data == null) {
    		System.out.println("Fail to load training data into model!\n");
    		return false;
    	}
    	
    	M = data.M;
    	V = data.V;
    	savestep = option.savestep;
    	
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
	 * Init parameters for inference
	 * @param newData DataSet for which we do inference
	 */
	public boolean initNewModel(LdaArgs option, Corpus newdata, Model trnModel){
		if (!init(option))
			return false;
		
		int m, n, w, k;
		
		K = trnModel.K;
		alpha = trnModel.alpha;
		beta = trnModel.beta;		
		
		p = new double[K];
		System.out.println("K:" + K);
		
		data = newdata;
		
		//+ allocate memory and assign values for variables		
		M = data.M;
		V = data.V;
		dir = option.dir;
		savestep = option.savestep;
		System.out.println("M:" + M);
		System.out.println("V:" + V);
		
		// K: from command line or default value
	    // alpha, beta: from command line or default values
	    // niters, savestep: from command line or default values

		nw = new int[V][K];
		
		nd = new int[M][K];
				
		nwsum = new int[K];
				
		ndsum = new int[M];
				
		z = new int[M][];
		for (m = 0; m < data.M; m++){
			int N = data.docs.get(m).length;
			z[m] = new int[N];
			
			//initilize for z
			for (n = 0; n < N; n++){
				int topic = (int)Math.floor(Math.random() * K);
				z[m][n] = topic;
				
				// number of instances of word assigned to topic j
				nw[data.docs.get(m).words[n]][topic] += 1;
				// number of words in document i assigned to topic j
				nd[m][topic] += 1;
				// total number of words assigned to topic j
				nwsum[topic] += 1;
			}
			// total number of words in document i
			ndsum[m] = N;
		}
		
		theta = new double[M][K];		
		phi = new double[K][V];
		
		return true;
	}
    
    /**
	 * Init parameters for inference
	 * reading new dataset from file
	 */
	public boolean initNewModel(LdaArgs option, Model trnModel){
		if (!init(option))
			return false;
		
		Corpus corpus = Corpus.loadCorpus(dir + File.separator + dfile, trnModel.data.localVoc);
		if (corpus == null){
			System.out.println("Fail to read corpus!\n");
			return false;
		}
		
		return initNewModel(option, corpus , trnModel);
	}
    
	public boolean initEstimatedModel(String dir) {
		
		if (!readOthersFile(dir + File.separator + modelName + othersSuffix)) {
			return false;
		}
		data = new Corpus(M);

		// read dictionary
		Vocabulary voc = new Vocabulary();
		if (!voc.readWordMap(dir + File.separator + wordMapFile)) {
			return false;
		}
		data.localVoc = voc;
		
		if (!readThetaFile(dir + File.separator + modelName + thetaSuffix)) {
			return false;
		}
		if (!readPhiFile(dir + File.separator + modelName + phiSuffix)) {
			return false;
		}
		
		System.out.println("Model loaded:");
		System.out.println("\talpha:" + alpha);
		System.out.println("\tbeta:" + beta);
		System.out.println("\tM:" + M);
		System.out.println("\tV:" + V);		
		
	    this.dir = dir;
	    
		return true;
	}
	
    /**
	 * init parameter for continue estimating or for later inference
	 */
	public boolean initEstimatedModel(LdaArgs option){
		
		if (!init(option)) {
			return false;
		}
		
		int m, n, w;
		
		p = new double[K];
		
		// load model, i.e., read z and trndata
		if (!loadModel()){
			System.out.println("Fail to load word-topic assignment file of the model!\n");
			return false;
		}
		
		System.out.println("Model loaded:");
		System.out.println("\talpha:" + alpha);
		System.out.println("\tbeta:" + beta);
		System.out.println("\tM:" + M);
		System.out.println("\tV:" + V);		
		
		//初始化默认值为0
		nw = new int[V][K];		
		nd = new int[M][K];
		nwsum = new int[K];    
	    ndsum = new int[M];
	    
	    for (m = 0; m < data.M; m++){
	    	int N = data.docs.get(m).length;
	    	
	    	// assign values for nw, nd, nwsum, and ndsum
	    	for (n = 0; n < N; n++){
	    		w = data.docs.get(m).words[n];
	    		int topic = z[m][n];
	    		
	    		// number of instances of word i assigned to topic j
	    		nw[w][topic] += 1;
	    		// number of words in document i assigned to topic j
	    		nd[m][topic] += 1;
	    		// total number of words assigned to topic j
	    		nwsum[topic] += 1;	    		
	    	}
	    	// total number of words in document i
	    	ndsum[m] = N;
	    }
	    
	    theta = new double[M][K];
	    phi = new double[K][V];
	    dir = option.dir;
		savestep = option.savestep;
	    
		return true;
	}
	public static void main(String[] args) {
		Model model1 = new Model();
		model1.initEstimatedModel("./models/0603/1");

		Model model2 = new Model();
		model2.initEstimatedModel("./models/0603/2");
		System.out.println(model1.data.localVoc.word2id.get("yeahkw"));
	}
public class TwordsComparable implements Comparator<Integer> {
		
		public double [] sortProb; // Store probability of each word in topic k
		
		public TwordsComparable (double[] sortProb){
			this.sortProb = sortProb;
		}

		public int compare(Integer o1, Integer o2) {
			// TODO Auto-generated method stub
			//Sort topic word index according to the probability of each word in topic k
			if(sortProb[o1] > sortProb[o2]) return -1;
			else if(sortProb[o1] < sortProb[o2]) return 1;
			else return 0;
		}
	}
}
