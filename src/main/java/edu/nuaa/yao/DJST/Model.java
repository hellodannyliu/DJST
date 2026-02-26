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


/**
 * Base model class for the Dynamic Joint Sentiment-Topic (dJST) model.
 *
 * Array dimensions follow the dJST paper notation:
 *   L = number of sentiment labels (field: S)
 *   T = number of topics (field: K)
 *   V = vocabulary size
 *   M = number of documents
 *
 * Field index conventions:
 *   nw[V][K][S]         - word × topic × sentiment count
 *   nd[M][K][S]         - doc × topic × sentiment count
 *   nwsum[K][S]         - topic × sentiment total count
 *   ndsum[M][S]         - doc × sentiment total count
 *   nsum[M]             - doc total word count
 *   z[M][N_d]           - topic assignment per word
 *   s[M][N_d]           - sentiment assignment per word
 *   theta[M][S][K]      - doc × sentiment × topic distribution
 *   phi[S][K][V]        - sentiment × topic × word distribution
 *   p[K][S]             - sampling probability matrix
 */
public class Model {
	
	//---------------------------------------------------------------
	//	Class Variables
	//---------------------------------------------------------------
	
	public static String tassignSuffix;
	public static String thetaSuffix;
	public static String phiSuffix;
	public static String othersSuffix;
	public static String twordsSuffix;
	
	//---------------------------------------------------------------
	//	Model Parameters and Variables
	//---------------------------------------------------------------
	
	public String wordMapFile;
	public String trainlogFile;

	public String dir;
	public String dfile;
	public String modelName;
	public int modelStatus;
	public Corpus data;
	
	public int K;
	public int V;
	public int M;
	public int S;
	public double alpha;
	public double beta;
	public int niters;
	public int liter;
	public int savestep;
	public int twords;
	public int withrawdata;
	public int docnum;
	public double gama;
	
	public double[][][] theta; // M × S × K
	public double[][][] phi;   // S × K × V
	
	public int z[][];   // topic assignments: M × N_d
	public int s[][];   // sentiment assignments: M × N_d

	public int[][][] nw;    // V × K × S
	public int[][][] nd;    // M × K × S
	public int[][] nwsum;   // K × S
	public int[][] ndsum;   // M × S
	int[] nsum;             // M

	double[][] thetasum;
	double[][] phisum;

	public double[][] p; // K × S
 	
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
		S = 2;
		alpha = 50.0 / K;
		beta = 0.01;
		niters = 2000;
		liter = 0;
		
		z = null;
		s = null;
		nw = null;
		nd = null;
		nwsum = null;
		ndsum = null;
		nsum = null;
		theta = null;
		phi = null;
    }
    
	//---------------------------------------------------------------
	//	I/O Methods
	//---------------------------------------------------------------

	protected boolean readOthersFile(String otherFile){
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
				else if (optstr.equalsIgnoreCase("nsentiments")){
					S = Integer.parseInt(optval);
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
			s = new int[M][];
			data = new Corpus(M);
			data.V = V;
			for (i = 0; i < M; i++){
				line = reader.readLine();
				StringTokenizer tknr = new StringTokenizer(line, " \t\r\n");
				
				int length = tknr.countTokens();
				
				int[] words = new int[length];
				int[] topics = new int[length];
				int[] sentiments = new int[length];
				
				for (j = 0; j < length; j++){
					String token = tknr.nextToken();
					StringTokenizer tknr2 = new StringTokenizer(token, ":");
					int tokenCount = tknr2.countTokens();
					if (tokenCount < 2){
						System.out.println("Invalid word-topic assignment line\n");
						return false;
					}
					
					words[j] = Integer.parseInt(tknr2.nextToken());
					topics[j] = Integer.parseInt(tknr2.nextToken());
					sentiments[j] = (tokenCount >= 3) ? Integer.parseInt(tknr2.nextToken()) : 0;
				}
				
				Document doc = new Document(length, words);
				data.setDoc(doc, i);
				
				z[i] = new int[length];
				s[i] = new int[length];
				for (j = 0; j < length; j++){
					z[i][j] = topics[j];
					s[i][j] = sentiments[j];
				}
			}
		}
		catch (Exception e){
			System.out.println("Error while loading model: " + e.getMessage());
			e.printStackTrace();
			return false;
		} finally {
			try {
				if (reader != null) reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return true;
	}
	
	protected boolean readThetaFile(String thetaFile){
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(thetaFile), "UTF-8"));
			
			theta = new double[M][S][K];
			for (int i = 0; i < M; i++){
				String line = reader.readLine();
				StringTokenizer tknr = new StringTokenizer(line, " \t\r\n|");
				for (int l = 0; l < S; l++) {
					for (int j = 0; j < K; j++){
						theta[i][l][j] = Double.parseDouble(tknr.nextToken());
					}
				}
			}
		}
		catch (Exception e){
			System.out.println("Error while loading model: " + e.getMessage());
			e.printStackTrace();
			return false;
		} finally {
			try {
				if (reader != null) reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return true;
	}
	
	protected boolean readPhiFile(String phiFile){
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(phiFile), "UTF-8"));
			
			phi = new double[S][K][V];
			for (int l = 0; l < S; l++) {
				for (int k = 0; k < K; k++){
					String line = reader.readLine();
					StringTokenizer tknr = new StringTokenizer(line, " \t\r\n");
					for (int j = 0; j < V; j++){
						phi[l][k][j] = Double.parseDouble(tknr.nextToken());
					}
				}
			}
		}
		catch (Exception e){
			System.out.println("Error while loading model: " + e.getMessage());
			e.printStackTrace();
			return false;
		} finally {
			try {
				if (reader != null) reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return true;
	}
	
	public boolean loadModel(){
		if (!readOthersFile(dir + File.separator + modelName + othersSuffix)) {
			return false;
		}
		if (!readTAssignFile(dir + File.separator + modelName + tassignSuffix)) {
			return false;
		}
		Vocabulary voc = new Vocabulary();
		if (!voc.readWordMap(dir + File.separator + wordMapFile)) {
			return false;
		}
		data.localVoc = voc;
		return true;
	}
    
	protected boolean saveModelTAssign(String filename){
		BufferedWriter bw = null;
		try {
			bw = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(filename), "UTF-8"));
			for (int m = 0; m < M; m++) {
				for (int n = 0, N = data.docs.get(m).length; n < N; n++) {
					bw.write(data.docs.get(m).words[n] + ":" + z[m][n] + ":" + s[m][n] + " ");
				}
				bw.write("\n");
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try { if (bw != null) bw.close(); } catch (IOException e) { e.printStackTrace(); }
		}
		return false;
	}
    
	protected boolean saveModelTheta(String filename){
		BufferedWriter writer = null;
		try{
			writer = new BufferedWriter(new FileWriter(filename));
			for (int i = 0; i < M; i++){
				for (int l = 0; l < S; l++) {
					for (int j = 0; j < K; j++){
						writer.write(theta[i][l][j] + " ");
					}
					if (l < S - 1) writer.write("| ");
				}
				writer.write("\n");
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try { if (writer != null) writer.close(); } catch (IOException e) { e.printStackTrace(); }
		}
		return false;
	}
	
	protected boolean saveModelPhi(String filename){
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(filename));
			for (int l = 0; l < S; l++) {
				for (int k = 0; k < K; k++){
					for (int j = 0; j < V; j++){
						writer.write(phi[l][k][j] + " ");
					}
					writer.write("\n");
				}
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try { if (writer != null) writer.close(); } catch (IOException e) { e.printStackTrace(); }
		}
		return false;
	}
	
	protected boolean saveModelOthers(String filename){
		BufferedWriter writer = null;
		try{
			writer = new BufferedWriter(new FileWriter(filename));
			writer.write("alpha=" + alpha + "\n");
			writer.write("beta=" + beta + "\n");
			writer.write("ntopics=" + K + "\n");
			writer.write("nsentiments=" + S + "\n");
			writer.write("ndocs=" + M + "\n");
			writer.write("nwords=" + V + "\n");
			writer.write("liters=" + liter + "\n");
			writer.write("dfile=" + dfile);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try { if (writer != null) writer.close(); } catch (IOException e) { e.printStackTrace(); }
		}
		return false;
	}
	
	protected boolean saveModelTwords(String filename){
		BufferedWriter bw = null;
		try {
			bw = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(filename), "UTF-8"));
			
			int tw = twords;
			if (tw > V || tw == 0){
				tw = V;
			}
			
			String[] sentimentLabels = new String[S];
			for (int l = 0; l < S; l++) {
				sentimentLabels[l] = "Sentiment " + l;
			}

			for (int l = 0; l < S; l++) {
				for (int k = 0; k < K; k++){
					List<Integer> tWordsIndexArray = new ArrayList<Integer>();
					for(int j = 0; j < V; j++){
						tWordsIndexArray.add(j);
					}
					final int fl = l, fk = k;
					Collections.sort(tWordsIndexArray, new Comparator<Integer>() {
						public int compare(Integer o1, Integer o2) {
							if (phi[fl][fk][o1] > phi[fl][fk][o2]) return -1;
							else if (phi[fl][fk][o1] < phi[fl][fk][o2]) return 1;
							return 0;
						}
					});
					bw.write(sentimentLabels[l] + " topic " + k + "\t:\n");
					for(int t = 0; t < tw; t++){
						bw.write(data.localVoc.id2word.get(tWordsIndexArray.get(t)) + " " 
							+ phi[l][k][tWordsIndexArray.get(t)] + "\n");
					}
					bw.write("\n");
				}
			}
			bw.close();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	protected boolean saveModelTwordsWithDomain(String filename) throws IOException{
		return saveModelTwords(filename);
	}
	
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
			if (!saveModelTwords(dir + File.separator + modelName + twordsSuffix))
				return false;
		}
		return true;
	}
	
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

    protected boolean init(LdaArgs option) {
    	if (option == null) {
    		return false;
    	}
    	
    	modelName = option.modelName;
		K = option.ntopics;
		S = option.S;
		
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
    
    public boolean initNewModel(LdaArgs option) {
    	if (!init(option)) {
    		return false;
    	}
    	
    	p = new double[K][S];
    	
    	data = Corpus.loadCorpus(dir + File.separator + dfile);
    	if (data == null) {
    		System.out.println("Fail to load training data into model!\n");
    		return false;
    	}
    	
    	M = data.M;
    	V = data.V;
    	savestep = option.savestep;
    	
    	nw = new int[V][K][S];
    	nd = new int[M][K][S];
    	nwsum = new int[K][S];
    	ndsum = new int[M][S];
    	nsum = new int[M];
    	z = new int[M][];
    	s = new int[M][];
    	
    	for (int m = 0; m < M; m++) {
			int N = data.docs.get(m).length;
			z[m] = new int[N];
			s[m] = new int[N];
			for(int n = 0; n < N; n++) {
				int topic = (int)(Math.random() * K);
				int sentiment = (int)(Math.random() * S);
				z[m][n] = topic;
				s[m][n] = sentiment;
				nw[data.docs.get(m).words[n]][topic][sentiment]++;
				nd[m][topic][sentiment]++;
				nwsum[topic][sentiment]++;
				ndsum[m][sentiment]++;
			}
			nsum[m] = N;
		}
    	
    	theta = new double[M][S][K];
    	phi = new double[S][K][V];
    	
    	return true;
    }
    
    public boolean initNewModel(LdaArgs option, Corpus newdata, Model trnModel){
		if (!init(option))
			return false;
		
		K = trnModel.K;
		S = trnModel.S;
		alpha = trnModel.alpha;
		beta = trnModel.beta;		
		
		p = new double[K][S];
		System.out.println("K:" + K);
		
		data = newdata;
		
		M = data.M;
		V = data.V;
		dir = option.dir;
		savestep = option.savestep;
		System.out.println("M:" + M);
		System.out.println("V:" + V);

		nw = new int[V][K][S];
		nd = new int[M][K][S];
		nwsum = new int[K][S];
		ndsum = new int[M][S];
		nsum = new int[M];
		z = new int[M][];
		s = new int[M][];

		for (int m = 0; m < data.M; m++){
			int N = data.docs.get(m).length;
			z[m] = new int[N];
			s[m] = new int[N];
			for (int n = 0; n < N; n++){
				int topic = (int)Math.floor(Math.random() * K);
				int sentiment = (int)Math.floor(Math.random() * S);
				z[m][n] = topic;
				s[m][n] = sentiment;
				nw[data.docs.get(m).words[n]][topic][sentiment]++;
				nd[m][topic][sentiment]++;
				nwsum[topic][sentiment]++;
				ndsum[m][sentiment]++;
			}
			nsum[m] = N;
		}
		
		theta = new double[M][S][K];
		phi = new double[S][K][V];
		
		return true;
	}
    
    public boolean initNewModel(LdaArgs option, Model trnModel){
		if (!init(option))
			return false;
		
		Corpus corpus = Corpus.loadCorpus(dir + File.separator + dfile, trnModel.data.localVoc);
		if (corpus == null){
			System.out.println("Fail to read corpus!\n");
			return false;
		}
		
		return initNewModel(option, corpus, trnModel);
	}
    
	public boolean initEstimatedModel(String dir) {
		if (!readOthersFile(dir + File.separator + modelName + othersSuffix)) {
			return false;
		}
		data = new Corpus(M);

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
	
    public boolean initEstimatedModel(LdaArgs option){
		if (!init(option)) {
			return false;
		}
		
		p = new double[K][S];
		
		if (!loadModel()){
			System.out.println("Fail to load word-topic assignment file of the model!\n");
			return false;
		}
		
		System.out.println("Model loaded:");
		System.out.println("\talpha:" + alpha);
		System.out.println("\tbeta:" + beta);
		System.out.println("\tM:" + M);
		System.out.println("\tV:" + V);		
		
		nw = new int[V][K][S];		
		nd = new int[M][K][S];
		nwsum = new int[K][S];
	    ndsum = new int[M][S];
	    nsum = new int[M];
	    
	    for (int m = 0; m < data.M; m++){
	    	int N = data.docs.get(m).length;
	    	
	    	for (int n = 0; n < N; n++){
	    		int w = data.docs.get(m).words[n];
	    		int topic = z[m][n];
	    		int sentiment = (s != null && s[m] != null) ? s[m][n] : 0;
	    		
	    		nw[w][topic][sentiment]++;
	    		nd[m][topic][sentiment]++;
	    		nwsum[topic][sentiment]++;
	    		ndsum[m][sentiment]++;
	    	}
	    	nsum[m] = N;
	    }
	    
	    theta = new double[M][S][K];
	    phi = new double[S][K][V];
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
		public double[] sortProb;
		
		public TwordsComparable(double[] sortProb){
			this.sortProb = sortProb;
		}

		public int compare(Integer o1, Integer o2) {
			if(sortProb[o1] > sortProb[o2]) return -1;
			else if(sortProb[o1] < sortProb[o2]) return 1;
			else return 0;
		}
	}
}
