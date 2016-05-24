package edu.nuaa.yao.DJST;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.StringTokenizer;


public class OldaArgs extends LdaArgs{
	
	public int delta = 1;
	public double[] omega = new double[delta];
	public int timeSlices;
	
	public static OldaArgs initOldaArgs(String configPath) {
		OldaArgs oldaArgs = new OldaArgs();
		try {
			InputStream is = new FileInputStream(configPath);
			properties.load(is);
			parseOldaProperties(oldaArgs);
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
//			oldaArgs.alpha = Double.parseDouble(_alpha);
//			oldaArgs.beta = Double.parseDouble(_beta);
//			oldaArgs.dfile= _dfile;
//			oldaArgs.dir = _dir;
//			oldaArgs.niters = Integer.parseInt(_niters);
//			oldaArgs.ntopics = Integer.parseInt(_ntopics);
//			oldaArgs.savestep = Integer.parseInt(_savestep);
//			oldaArgs.twords = Integer.parseInt(_twords);
//			oldaArgs.modelName = _modelname;
//			oldaArgs.wordMapFileName = _wordmap;
//			oldaArgs.withrawdata = Boolean.parseBoolean(_withrawdata);
//			oldaArgs.est = Integer.parseInt(_est);
//			oldaArgs.estc = Integer.parseInt(_estc);
//			oldaArgs.inf = Integer.parseInt(_inf);
//			oldaArgs.initflag = true;
		}
		catch (NullPointerException e)
		{
			oldaArgs.initflag = false;
			System.out.println("A config key cannot be found,Please check config.properties!");
			e.printStackTrace();
		}
		catch (FileNotFoundException e) {
			oldaArgs.initflag = false;
			System.out.println("Config file path not exists!");
			e.printStackTrace();
		} catch (IOException e) {
			oldaArgs.initflag = false;
			System.out.println("Load config file exception!");
			e.printStackTrace();
		}
		return oldaArgs;
	}
	
	public static void parseOldaProperties(OldaArgs oldaArgs) {
		parseProperties(oldaArgs);
		String _delta = properties.getProperty("delta");
		String _omega = properties.getProperty("omega");
		String _timeSlices = properties.getProperty("timeslices");
		StringTokenizer stk = new StringTokenizer(_omega,",");
		oldaArgs.delta = Integer.parseInt(_delta);
		oldaArgs.omega = new double[oldaArgs.delta];
		oldaArgs.timeSlices = Integer.parseInt(_timeSlices);
		for (int i = 0; i < oldaArgs.delta; i++) {
			if(stk.hasMoreTokens()) {
				oldaArgs.omega[i] = Double.parseDouble(stk.nextToken());
			} else {
				oldaArgs.omega[i] = 0;
			}
		}
	}
	
	public static void main(String[] args) {
		StringTokenizer stk = new StringTokenizer("11", ",");
		String token = "";
		while(stk.hasMoreTokens()){
			System.out.println(stk.nextToken());
		}
	}
}
