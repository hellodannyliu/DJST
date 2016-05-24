package edu.nuaa.yao.DJST;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class LdaArgs {
	
	public static Properties properties = new Properties();
	
	public double beta = 0.01;
	public String dfile = "";
	public String dir = "";
	public int niters = 1000;
	public int ntopics = 100;
	public double alpha = 0.5;
	public int savestep = 100;
	public int twords = 100;
	public String wordMapFileName = "wordmap.txt";
	public String modelName= "model-final";
	public boolean withrawdata = false;
	public int est = 0;
	public int estc = 0;
	public int inf = 0;
	public boolean initflag = false;
	public int docnum;
	
	public static LdaArgs initLdaArgs(String configPath) {

		LdaArgs ldaArgs = new LdaArgs();
		try {
			InputStream is = new FileInputStream(configPath);
			properties.load(is);
			parseProperties(ldaArgs);
//			String _alpha = properties.getProperty("alpha");
//			String _beta = properties.getProperty("beta");
//			String _dfile = properties.getProperty("dfile");
//			String _dir = properties.getProperty("dir");
//			String _niters = properties.getProperty("niters");
//			String _ntopics = properties.getProperty("ntopics");
//			String _savestep = properties.getProperty("savestep");
//			String _twords = properties.getProperty("twords");
//			String _wordmap = properties.getProperty("wordmap");
//			String _modelname = properties.getProperty("modelname");
//			String _withrawdata = properties.getProperty("withrawdata");
//			String _est = properties.getProperty("est");
//			String _estc = properties.getProperty("estc");
//			String _inf = properties.getProperty("inf");
//			
//			ldaArgs.alpha = Double.parseDouble(_alpha);
//			ldaArgs.beta = Double.parseDouble(_beta);
//			ldaArgs.dfile= _dfile;
//			ldaArgs.dir = _dir;
//			ldaArgs.niters = Integer.parseInt(_niters);
//			ldaArgs.ntopics = Integer.parseInt(_ntopics);
//			ldaArgs.savestep = Integer.parseInt(_savestep);
//			ldaArgs.twords = Integer.parseInt(_twords);
//			ldaArgs.modelName = _modelname;
//			ldaArgs.wordMapFileName = _wordmap;
//			ldaArgs.withrawdata = Boolean.parseBoolean(_withrawdata);
//			ldaArgs.est = Integer.parseInt(_est);
//			ldaArgs.estc = Integer.parseInt(_estc);
//			ldaArgs.inf = Integer.parseInt(_inf);
//			ldaArgs.initflag = true;
		}
		catch (NullPointerException e)
		{
			ldaArgs.initflag = false;
			System.out.println("A config key cannot be found,Please check config.properties!");
			e.printStackTrace();
		}
		catch (FileNotFoundException e) {
			ldaArgs.initflag = false;
			System.out.println("Config file path not exists!");
			e.printStackTrace();
		} catch (IOException e) {
			ldaArgs.initflag = false;
			System.out.println("Load config file exception!");
			e.printStackTrace();
		}
		return ldaArgs;
	}
	
	public static void parseProperties(LdaArgs ldaArgs) {
		String _alpha = properties.getProperty("alpha");
		String _beta = properties.getProperty("beta");
		String _dfile = properties.getProperty("dfile");
		String _dir = properties.getProperty("dir");
		String _niters = properties.getProperty("niters");
		String _ntopics = properties.getProperty("ntopics");
		String _savestep = properties.getProperty("savestep");
		String _twords = properties.getProperty("twords");
		String _wordmap = properties.getProperty("wordmap");
		String _modelname = properties.getProperty("modelname");
		String _withrawdata = properties.getProperty("withrawdata");
		String _est = properties.getProperty("est");
		String _estc = properties.getProperty("estc");
		String _inf = properties.getProperty("inf");
		String _docnum=properties.getProperty("docnum");
		
		ldaArgs.alpha = Double.parseDouble(_alpha);
		ldaArgs.beta = Double.parseDouble(_beta);
		ldaArgs.dfile= _dfile;
		ldaArgs.dir = _dir;
		ldaArgs.niters = Integer.parseInt(_niters);
		ldaArgs.ntopics = Integer.parseInt(_ntopics);
		ldaArgs.savestep = Integer.parseInt(_savestep);
		ldaArgs.twords = Integer.parseInt(_twords);
		ldaArgs.modelName = _modelname;
		ldaArgs.wordMapFileName = _wordmap;
		ldaArgs.withrawdata = Boolean.parseBoolean(_withrawdata);
		ldaArgs.est = Integer.parseInt(_est);
		ldaArgs.estc = Integer.parseInt(_estc);
		ldaArgs.inf = Integer.parseInt(_inf);
		ldaArgs.initflag = true;
		ldaArgs.docnum= Integer.parseInt(_docnum);
	}
	
	public void increaseNtopic(int i) {
		ntopics = i * 10;
		alpha = 50d / (i * 10); 
		modelName = "model-final-" + i * 10;
	}
	
	public static void main(String[] args) {
		System.out.println();
	}
}	
