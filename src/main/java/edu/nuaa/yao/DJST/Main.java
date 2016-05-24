package edu.nuaa.yao.DJST;



public class Main {
	public static void main(String[] args) {
		String configPath = "./cfg/olda.properties";
		OldaArgs option = OldaArgs.initOldaArgs(configPath);
		try {
			if (!option.initflag){
				System.out.println("Args init failed!");
				return;
			}
			
			if (1 == option.est || 1 == option.estc){
				OldaTrainer trainer = new OldaTrainer();
				if (!trainer.init(option)) {
					System.out.println("Trainer init failed!");
					return;
				}
				trainer.trainOlda();
			}
			/*else if (1 == option.inf){
				Inferencer inferencer = new Inferencer(option);
				if (!inferencer.init()) {
					System.out.println("Inferencer init failed!");
					return;
				}
				
				inferencer.inference();
			}*/
		}
		catch (Exception e){
			System.out.println("Error in main: " + e.getMessage());
			e.printStackTrace();
			return;
		}
	}
}
