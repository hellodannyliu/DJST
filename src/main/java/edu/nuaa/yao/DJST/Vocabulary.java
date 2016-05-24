package edu.nuaa.yao.DJST;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

public class Vocabulary {
	public Map<String, Integer> word2id;
	public List<String> id2word;
	
	public Vocabulary() {
		this.word2id = new HashMap<String, Integer>();
		this.id2word = new ArrayList<String>(1024);
	}
	
	public String getWord(int id) {
		return id2word.get(id);
	}
	
	public int getId(String word) {
		return word2id.get(word);
	}
	
	public boolean contains(String word) {
		return word2id.containsKey(word);
	}
	
	public int addWord(String word) {
		if (contains(word)) {
			return word2id.get(word);
		} else {
			int id = id2word.size();
			word2id.put(word, id);
			id2word.add(word);
			return id;
		}
	}
	
	public boolean readWordMap(String filePath) {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(
					new FileInputStream(filePath), "UTF-8"));
			String line;
			line = br.readLine();
			int nword = Integer.parseInt(line);
			for (int i = 0; i < nword; i++) {
				line = br.readLine();
				StringTokenizer st = new StringTokenizer(line, " \t\n\r");
				if (st.countTokens() != 2) {
					continue;
				}
				String word = st.nextToken();
				int id = Integer.parseInt(st.nextToken());
				word2id.put(word, id);
				id2word.add(word);
			}
			return true;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
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
		return false;
	}
	
	public boolean writeWordMap(String filePath) {
		BufferedWriter bw = null;
		try {
			bw = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(filePath),"UTF-8"));
			int nword = id2word.size();
			bw.write(nword + "\n");
			for (int i = 0; i < nword; i++) {
				bw.write(id2word.get(i) + "\t" + i + "\n");
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
}
