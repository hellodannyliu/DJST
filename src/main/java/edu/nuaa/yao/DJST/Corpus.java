package edu.nuaa.yao.DJST;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

public class Corpus {
	
	public Vocabulary localVoc;
	public List<Document> docs;
	public int M;
	public int V;
	
	// map from local coordinates (id) to global ones 
	// null if the global dictionary is not set
	public Map<Integer, Integer> lid2gid; 
	
	//link to a global dictionary (optional), null for train data, not null for test data
	public Vocabulary globalVoc;	 	
	
	public Corpus() {
		localVoc = new Vocabulary();
		M = 0;
		V = 0;
		docs = new ArrayList<Document>();
		lid2gid = null;
		globalVoc = null;
	}
	
	public Corpus(int M) {
		localVoc = new Vocabulary();
		this.M = M;
		V = 0;
		docs = new ArrayList<Document>(M);
		lid2gid = null;
		globalVoc = null;
	}
	
	public Corpus(int M, Vocabulary globalVoc) {
		localVoc = new Vocabulary();	
		this.M = M;
		this.V = 0;
		docs = new ArrayList<Document>(M);	
		
		this.globalVoc = globalVoc;
		lid2gid = new HashMap<Integer, Integer>();
	}
	
	//-------------------------------------------------------------
	//Public Instance Methods
	//-------------------------------------------------------------
	/**
	 * set the document at the index idx if idx is greater than 0 and less than M
	 * @param doc document to be set
	 * @param idx index in the document array
	 */	
	public void setDoc(Document doc, int idx){
		if (0 <= idx && idx < M){
			docs.add(idx, doc);;
		}
	}
	
	/**
	 * set the document at the index idx if idx is greater than 0 and less than M
	 * @param str string contains doc
	 * @param idx index in the document array
	 */
	public void setDoc(String line) {
		
		String domain = null;
		
		StringTokenizer st = new StringTokenizer(line, " \t\r\n");
		//domain = st.nextToken();
		//st.nextToken();
		//st.nextToken();
		//st.nextToken();
		
		int nid = st.countTokens();
		int[] ids = new int[nid];
		for (int i = 0; i < nid; i++) {
			String word = st.nextToken();
			int _id = localVoc.id2word.size();
			if (localVoc.contains(word)) {
				_id = localVoc.getId(word);
			}
			if (globalVoc != null){
				//get the global id					
				Integer id = globalVoc.word2id.get(word);
				//System.out.println(id);
				
				if (id != null){
					localVoc.addWord(word);					
				}
				else {
					//not in global dictionary
					//do nothing currently
					localVoc.addWord(word);					
					globalVoc.addWord(word);
				}
				lid2gid.put(_id, id);
				ids[i] = _id;
			}
			else {
				localVoc.addWord(word);
				ids[i] = _id;
			}			
		}
		Document doc = new Document(ids, line, domain);
		docs.add(doc);
	}
	
	/**
	 *  read a dataset from a stream, create new dictionary
	 *  @return dataset if success and null otherwise
	 */
	public static Corpus loadCorpus(String filePath) {
		
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(
					new FileInputStream(filePath), "UTF-8"));
			Corpus corpus = new Corpus();
			String line = null;
			int ndoc = 0;
			while ((line = br.readLine()) != null) {
				corpus.setDoc(line);
				ndoc++;
			}
			corpus.M = ndoc;
			corpus.V = corpus.localVoc.id2word.size();
			return corpus;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	
	/**
	 * read a dataset from a file with a preknown vocabulary
	 * @param filename file from which we read dataset
	 * @param dict the dictionary
	 * @return dataset if success and null otherwise
	 */
	public static Corpus loadCorpus(String filename, Vocabulary voc){
		
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(
					new FileInputStream(filename), "UTF-8"));
			//read number of document
			Corpus corpus = new Corpus(64 , voc);
			
			String line = null;
			int ndoc = 0;
			while ((line = br.readLine()) != null) {
				corpus.setDoc(line);
				ndoc++;
			}
			corpus.M = ndoc;
			corpus.V = corpus.localVoc.id2word.size();
			return corpus;
		}
		catch (Exception e){
			System.out.println("Read Dataset Error: " + e.getMessage());
			e.printStackTrace();
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	
	/**
	 * read a dataset from a string with respect to a specified dictionary
	 * @param str String from which we get the dataset, documents are seperated by newline character	
	 * @param dict the dictionary
	 * @return dataset if success and null otherwise
	 */
	public static Corpus loadCorpus(String[] strs, Vocabulary vocabulary){
		//System.out.println("readDataset...");
		Corpus data = new Corpus(strs.length, vocabulary);
		
		for (int i = 0 ; i < strs.length; ++i){
			//System.out.println("set doc " + i);
			data.setDoc(strs[i]);
		}
		return data;
	}
}
