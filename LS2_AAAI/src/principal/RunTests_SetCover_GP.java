package principal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import SetCoverSampling.DataRecollection;
import SetCoverSampling.GameSampling;
import SetCoverSampling.RunSampling;
import Standard.StrategyTactics;
import ai.RandomBiasedAI;
import ai.CMAB.A3NWithin;
import ai.ScriptsGenerator.TableGenerator.FunctionsforGrammar;
import ai.abstraction.pathfinding.AStarPathFinding;
import ai.ahtn.AHTNAI;
import ai.asymmetric.PGS.LightPGSSCriptChoice;
import ai.asymmetric.SSS.LightSSSmRTSScriptChoice;
import ai.competition.dropletGNS.Droplet;
import ai.configurablescript.BasicExpandedConfigurableScript;
import ai.configurablescript.ScriptsCreator;
import ai.core.AI;
import ai.evaluation.SimpleSqrtEvaluationFunction3;
import ai.puppet.PuppetSearchMCTS;
import ai.synthesis.DslLeague.Runner.SettingsAlphaDSL;
import ai.synthesis.dslForScriptGenerator.DSLCommandInterfaces.ICommand;
import ai.synthesis.grammar.dslTree.interfacesDSL.iDSL;
import ai.synthesis.grammar.dslTree.utils.ReduceDSLController;
import ai.synthesis.runners.roundRobinLocal.SmartRRGxGRunnable;
import ga.ScriptTableGenerator.ScriptsTable;
import ga.config.ConfigurationsGA;
import ga.model.Chromosome;
import ga.model.Population;
import ga.util.PreSelection;
import ga.util.RunGA;
import ga.util.RunScriptByState;
import ga.util.Evaluation.RatePopulation;
import ga.util.Evaluation.RoundRobinEliteandSampleEval;
import ga.util.Evaluation.RoundRobinEliteandSampleIterativeEval;
import ga.util.Evaluation.RoundRobinEval;
import ga.util.Evaluation.RoundRobinIterativeEval;
import ga.util.Evaluation.RoundRobinSampleEval;
import ga.util.Evaluation.SetCoverEval;
import ga.util.Evaluation.TestSingleMatch;
import rts.GameState;
import rts.PhysicalGameState;
import rts.units.UnitTypeTable;
import setcoverCalculation.RunSetCoverCalculation;
import util.TestSingleMatchLeft;
import util.TestSingleMatchRight;
import ga.util.Evaluation.FixedScriptedEval;

public class RunTests_SetCover_GP {

	private final static String pathTableScriptsInit = System.getProperty("user.dir").concat("/TableInitialPortfolio/");
	//private static final String pathTableScriptsInit = "TableInitialPortfolio/";
	private static final String pathTable = System.getProperty("user.dir").concat("/Table/");
	private final static String pathLogsBestPortfolios = System.getProperty("user.dir").concat("/TrackingPortfolios/TrackingPortfolios.txt");
	private final static String pathFixedTrace = System.getProperty("user.dir").concat("/FixedTrace/FixedTrace.txt");
	private final static String pathLasi = System.getProperty("user.dir").concat("/lasiCommands/SCcommands.txt");
	private final static String dirPathPlayer = System.getProperty("user.dir").concat("/logs_game/logs_states/");


	public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException {
		PrintWriter writer = new PrintWriter("Best.txt", "UTF-8");
		
		
		for(int j=0; j<ConfigurationsGA.numbersystematicTests; j++)
		{


		String curriculumportfolio="empty";	
		
		File logsBestPortfolios=new File(pathLogsBestPortfolios);
		GameSampling.deleteFolder(logsBestPortfolios);
		File logsGames=new File(dirPathPlayer);
		GameSampling.deleteFolder(logsGames);
		deleteAllSubfolders();


		if(!ConfigurationsGA.fixedTrace)
		{
			//Here we play with a search-based algorithm and save the path
			for(int i=0;i<ConfigurationsGA.numberA3Ngames;i++)
			{
				try {
					RunSampling sampling=new RunSampling(i,pathTableScriptsInit,curriculumportfolio);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}


		int bestFinalIndividual=0;

		for(int i=1;i<ConfigurationsGA.LOOPS_SELFPLAY;i++)
		{
			String scriptsSetCover="";
			HashSet<String> booleansUsedRedefined=new HashSet<>();
			if(!ConfigurationsGA.fixedTrace)
			{			
				//SC
				RunScriptByState sc = new RunScriptByState();

				RunSetCoverCalculation scCalculation = new RunSetCoverCalculation(sc.dataH);
				List<Integer> setCover=scCalculation.getSetCover();
				scriptsSetCover=setCover.toString();
				booleansUsedRedefined=sc.booleansUsed;
			}

			if(Files.exists(Paths.get(pathTable+"ScriptsTable.txt"))) { 
				Path source = Paths.get(pathTable+"ScriptsTable.txt");
				try {
					Files.move(source, source.resolveSibling("ScriptsTable"+i+".txt"));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			//* 
			//applying the GP
			RunGA ga = new RunGA(curriculumportfolio);

			//escolhemos uma função de avaliação
			//RatePopulation fEval = new RoundRobinEval();
			//RatePopulation fEval = new RoundRobinSampleEval();
			RoundRobinEliteandSampleEval fEval = new RoundRobinEliteandSampleEval();
			//RatePopulation fEval = new RoundRobinIterativeEval();
			//RoundRobinEliteandSampleIterativeEval fEval = new RoundRobinEliteandSampleIterativeEval();
			//fEval = new SetCoverEval();

			//rodamos o GA
			if(ConfigurationsGA.fixedTrace)
			{
				File arqTour = new File(pathFixedTrace);

				//Review this code, its possible to being saving Additionals "]"
				try {
					FileReader arq = new FileReader(arqTour);
					BufferedReader bf = new BufferedReader(arq);

					scriptsSetCover = bf.readLine();
					String booleansUsedLine= bf.readLine();
					String [] parts=booleansUsedLine.split("\\s+");

					for(String element:parts)
					{	
						if(element.substring(element.length() - 1).equals(","))
							element=element.substring(0, element.length() - 1);

						booleansUsedRedefined.add(element.trim());
					}
					arq.close();

				} catch (Exception e) {
					System.out.println(e.toString());
				}

			}

			//System.out.println("Commands from Lasi: "+scriptsSetCover);

			//System.out.println("Booleans from Lasi: "+booleansUsedRedefined.toString());

			//Testing functionsForGrammars
			//		FunctionsforGrammar fg = new FunctionsforGrammar();
			//		fg.reducingCommandsFromFile(pathLasi);
			Population popFinal = ga.run(fEval,scriptsSetCover,booleansUsedRedefined);

			//popFinal.printWithValue();

			//Here we chose the best individual
			HashMap<Chromosome, BigDecimal> elite=(HashMap<Chromosome, BigDecimal>)PreSelection.sortByValueBest(popFinal.getChromosomes());
			for (Chromosome ch : elite.keySet()) {

				ArrayList<Integer> Genes=(ArrayList<Integer>) ch.getGenes().clone();
				curriculumportfolio=Genes.toString();

			}		

			bestFinalIndividual=Integer.parseInt(curriculumportfolio.replace("[", "").replace("]", ""));
			System.out.println("Final best individual "+bestFinalIndividual);
			System.out.println("best script "+ga.scriptsAST.get(bestFinalIndividual).translate());

			float finalAvalation;
			try {
				finalAvalation = finalAvaliation(ga.scriptsAST.get(bestFinalIndividual));
				System.out.println("Final Avalation "+finalAvalation);
				writer.println(ga.scriptsAST.get(bestFinalIndividual).translate()+" "+finalAvalation);
				writer.flush();
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}			
			

			//		//Here we play with a search-based algorithm and save the path
			//		try {
			//			RunSampling sampling=new RunSampling(i,pathTable,curriculumportfolio);
			//		} catch (IOException er) {
			//			// TODO Auto-generated catch block
			//			er.printStackTrace();
			//		}
			//		
			//		curriculumportfolio=ga.recoverScriptGenotype(curriculumportfolio).trim();


		}

		}

		writer.close();

	}

	private static void deleteAllSubfolders() {
		// TODO Auto-generated method stub
		String dir1 = System.getProperty("user.dir").concat("/TrackingPortfolios/");
		String dir2 = System.getProperty("user.dir").concat("/Tracking/");
		String dir3 = System.getProperty("user.dir").concat("/TableInitialPortfolio/");
		String dir4 = System.getProperty("user.dir").concat("/Table/");
		//String dir5 = System.getProperty("user.dir").concat("/maps/");
		String dir6 = System.getProperty("user.dir").concat("/LogsGrammars/");
		String dir7 = System.getProperty("user.dir").concat("/logs_game/");
		String dir8 = System.getProperty("user.dir").concat("/logs/");
		//String dir9 = System.getProperty("user.dir").concat("/lib/");
		String dir10 = System.getProperty("user.dir").concat("/configSOA/");
		String dir11 = System.getProperty("user.dir").concat("/commandsUsed/");
		String dir12 = System.getProperty("user.dir").concat("/centralSOA/");
		
		GameSampling.deleteSubFolders(new File(dir1));
		GameSampling.deleteSubFolders(new File(dir2));
		GameSampling.deleteSubFolders(new File(dir3));
		GameSampling.deleteSubFolders(new File(dir4));
		//GameSampling.deleteSubFolders(new File(dir5));
		GameSampling.deleteSubFolders(new File(dir6));
		GameSampling.deleteSubFolders(new File(dir7));
		GameSampling.deleteSubFolders(new File(dir8));
		//GameSampling.deleteSubFolders(new File(dir9));
		GameSampling.deleteSubFolders(new File(dir10));
		GameSampling.deleteSubFolders(new File(dir11));
		GameSampling.deleteSubFolders(new File(dir12));
		
		
	}

	public static float finalAvaliation(iDSL bestScript) throws Exception
	{
		String map = SettingsAlphaDSL.get_map();
		UnitTypeTable utt = new UnitTypeTable();
		PhysicalGameState pgs=new PhysicalGameState(30, 30);
		try {
			pgs = PhysicalGameState.load(map, utt);


		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		GameState gs = new GameState(pgs, utt);

		TestSingleMatchLeft runner1 = new TestSingleMatchLeft(bestScript, new Droplet(utt), gs.clone(), pgs, utt, "");
		TestSingleMatchRight runner2 = new TestSingleMatchRight(new Droplet(utt), bestScript, gs.clone(), pgs, utt, "");

		TestSingleMatchLeft runner3 = new TestSingleMatchLeft(bestScript, new PuppetSearchMCTS(utt), gs.clone(), pgs, utt, "");
		TestSingleMatchRight runner4 = new TestSingleMatchRight(new PuppetSearchMCTS(utt), bestScript, gs.clone(), pgs, utt, "");

		TestSingleMatchLeft runner5 = new TestSingleMatchLeft(bestScript, new StrategyTactics(utt), gs.clone(), pgs, utt, "");
		TestSingleMatchRight runner6 = new TestSingleMatchRight(new StrategyTactics(utt), bestScript, gs.clone(), pgs, utt, "");

		TestSingleMatchLeft runner7 = new TestSingleMatchLeft(bestScript, new LightPGSSCriptChoice(utt, decodeScripts(utt, "0;1;2;3;"), 200, "PGSR_LIGHT"), gs.clone(), pgs, utt, "");
		TestSingleMatchRight runner8 = new TestSingleMatchRight(new LightPGSSCriptChoice(utt, decodeScripts(utt, "0;1;2;3;"), 200, "PGSR_LIGHT"), bestScript, gs.clone(), pgs, utt, "");

		TestSingleMatchLeft runner9 = new TestSingleMatchLeft(bestScript, new LightSSSmRTSScriptChoice(utt, decodeScripts(utt, "0;1;2;3;"), 200, "SSSR_LIGHT"), gs.clone(), pgs, utt, "");
		TestSingleMatchRight runner10 = new TestSingleMatchRight(new LightSSSmRTSScriptChoice(utt, decodeScripts(utt, "0;1;2;3;"), 200, "SSSR_LIGHT"), bestScript, gs.clone(), pgs, utt, "");

		TestSingleMatchLeft runner11 = new TestSingleMatchLeft(bestScript, new BasicExpandedConfigurableScript(utt, new AStarPathFinding(), 18, 0, 0, 1, 2, 2, -1, -1, 4), gs.clone(), pgs, utt, "");
		TestSingleMatchRight runner12 = new TestSingleMatchRight(new BasicExpandedConfigurableScript(utt, new AStarPathFinding(), 18, 0, 0, 1, 2, 2, -1, -1, 4), bestScript, gs.clone(), pgs, utt, "");

		TestSingleMatchLeft runner13 = new TestSingleMatchLeft(bestScript, new BasicExpandedConfigurableScript(utt, new AStarPathFinding(), 18, 0, 0, 1, 2, 2, -1, -1, 5), gs.clone(), pgs, utt, "");
		TestSingleMatchRight runner14 = new TestSingleMatchRight(new BasicExpandedConfigurableScript(utt, new AStarPathFinding(), 18, 0, 0, 1, 2, 2, -1, -1, 5), bestScript, gs.clone(), pgs, utt, "");

		TestSingleMatchLeft runner15 = new TestSingleMatchLeft(bestScript, new BasicExpandedConfigurableScript(utt, new AStarPathFinding(), 18, 0, 0, 1, 2, 2, -1, -1, 6), gs.clone(), pgs, utt, "");
		TestSingleMatchRight runner16 = new TestSingleMatchRight(new BasicExpandedConfigurableScript(utt, new AStarPathFinding(), 18, 0, 0, 1, 2, 2, -1, -1, 6), bestScript, gs.clone(), pgs, utt, "");

		TestSingleMatchLeft runner17 = new TestSingleMatchLeft(bestScript, new BasicExpandedConfigurableScript(utt, new AStarPathFinding(), 18, 0, 0, 1, 2, 2, -1, -1, 3), gs.clone(), pgs, utt, "");
		TestSingleMatchRight runner18 = new TestSingleMatchRight(new BasicExpandedConfigurableScript(utt, new AStarPathFinding(), 18, 0, 0, 1, 2, 2, -1, -1, 3), bestScript, gs.clone(), pgs, utt, "");

		TestSingleMatchLeft runner19 = new TestSingleMatchLeft(bestScript, new A3NWithin(100, -1, 100, 8, 0.3F, 0.0F, 0.4F, 0, new RandomBiasedAI(utt),
				new SimpleSqrtEvaluationFunction3(), true, utt, "ManagerClosestEnemy", 3,
				decodeScripts(utt, "1;2;3;"), "A3N"), gs.clone(), pgs, utt, "");
		TestSingleMatchRight runner20 = new TestSingleMatchRight(new A3NWithin(100, -1, 100, 8, 0.3F, 0.0F, 0.4F, 0, new RandomBiasedAI(utt),
				new SimpleSqrtEvaluationFunction3(), true, utt, "ManagerClosestEnemy", 3,
				decodeScripts(utt, "1;2;3;"), "A3N"), bestScript, gs.clone(), pgs, utt, "");


		try {
			runner1.start();
			runner2.start();
			runner3.start();
			runner4.start();
			runner5.start();
			runner6.start();
			runner7.start();
			runner8.start();
			runner9.start();
			runner10.start();
			runner11.start();
			runner12.start();
			runner13.start();
			runner14.start();
			runner15.start();
			runner16.start();
			runner17.start();
			runner18.start();
			runner19.start();
			runner20.start();

			runner1.join();
			runner2.join();
			runner3.join();
			runner4.join();
			runner5.join();
			runner6.join();
			runner7.join();
			runner8.join();
			runner9.join();
			runner10.join();
			runner11.join();
			runner12.join();
			runner13.join();
			runner14.join();
			runner15.join();
			runner16.join();
			runner17.join();
			runner18.join();
			runner19.join();
			runner20.join();

			float totalScript = 0.0f;
			if (runner1.getWinner() == 0) {
				totalScript += runner1.getResult();
			} else if (runner1.getWinner() == -1) {
				totalScript += runner1.getResult();
			}

			if (runner2.getWinner() == 1) {
				totalScript += runner2.getResult();
			} else if (runner2.getWinner() == -1) {
				totalScript += runner2.getResult();
			}

			//System.out.println("runner3 "+runner3.getResult());
			if (runner3.getWinner() == 0) {
				totalScript += runner3.getResult();
			} else if (runner3.getWinner() == -1) {
				totalScript += runner3.getResult();
			}

			if (runner4.getWinner() == 1) {
				totalScript += runner4.getResult();
			} else if (runner4.getWinner() == -1) {
				totalScript += runner4.getResult();
			}

			if (runner5.getWinner() == 0) {
				totalScript += runner5.getResult();
			} else if (runner5.getWinner() == -1) {
				totalScript += runner5.getResult();
			}

			if (runner6.getWinner() == 1) {
				totalScript += runner6.getResult();
			} else if (runner6.getWinner() == -1) {
				totalScript += runner6.getResult();
			}

			if (runner7.getWinner() == 0) {
				totalScript += runner7.getResult();
			} else if (runner7.getWinner() == -1) {
				totalScript += runner7.getResult();
			}

			if (runner8.getWinner() == 1) {
				totalScript += runner8.getResult();
			} else if (runner8.getWinner() == -1) {
				totalScript += runner8.getResult();
			}

			if (runner9.getWinner() == 0) {
				totalScript += runner9.getResult();
			} else if (runner9.getWinner() == -1) {
				totalScript += runner9.getResult();
			}

			if (runner10.getWinner() == 1) {
				totalScript += runner10.getResult();
			} else if (runner10.getWinner() == -1) {
				totalScript += runner10.getResult();
			}

			if (runner11.getWinner() == 0) {
				totalScript += runner11.getResult();
			} else if (runner11.getWinner() == -1) {
				totalScript += runner11.getResult();
			}

			if (runner12.getWinner() == 1) {
				totalScript += runner12.getResult();
			} else if (runner12.getWinner() == -1) {
				totalScript += runner12.getResult();
			}

			if (runner13.getWinner() == 0) {
				totalScript += runner13.getResult();
			} else if (runner13.getWinner() == -1) {
				totalScript += runner13.getResult();
			}

			if (runner14.getWinner() == 1) {
				totalScript += runner14.getResult();
			} else if (runner14.getWinner() == -1) {
				totalScript += runner14.getResult();
			}

			if (runner15.getWinner() == 0) {
				totalScript += runner15.getResult();
			} else if (runner15.getWinner() == -1) {
				totalScript += runner15.getResult();
			}

			if (runner16.getWinner() == 1) {
				totalScript += runner16.getResult();
			} else if (runner16.getWinner() == -1) {
				totalScript += runner16.getResult();
			}

			if (runner17.getWinner() == 0) {
				totalScript += runner17.getResult();
			} else if (runner17.getWinner() == -1) {
				totalScript += runner17.getResult();
			}

			if (runner18.getWinner() == 1) {
				totalScript += runner18.getResult();
			} else if (runner18.getWinner() == -1) {
				totalScript += runner18.getResult();
			}

			if (runner19.getWinner() == 0) {
				totalScript += runner19.getResult();
			} else if (runner19.getWinner() == -1) {
				totalScript += runner19.getResult();
			}

			if (runner20.getWinner() == 1) {
				totalScript += runner20.getResult();
			} else if (runner20.getWinner() == -1) {
				totalScript += runner20.getResult();
			}


			//            HashSet<ICommand> uniqueCommands = new HashSet<>();
			//            uniqueCommands.addAll(runner1.getAllCommandIA2());
			//            uniqueCommands.addAll(runner2.getAllCommandIA2());
			//            uniqueCommands.addAll(runner3.getAllCommandIA1());
			//            uniqueCommands.addAll(runner4.getAllCommandIA1());
			//            ReduceDSLController.removeUnactivatedParts(script2, new ArrayList<>(uniqueCommands));

			return totalScript;
		} catch (Exception e) {
			System.err.println("ai.synthesis.localsearch.DoubleProgramSynthesis.processMatch() " + e.getMessage());
			return -5.0f;
		}
	}

	public static List<AI> decodeScripts(UnitTypeTable utt, String sScripts) {

		//decompõe a tupla
		ArrayList<Integer> iScriptsAi1 = new ArrayList<>();
		String[] itens = sScripts.split(";");

		for (String element : itens) {
			iScriptsAi1.add(Integer.decode(element));
		}

		List<AI> scriptsAI = new ArrayList<>();

		ScriptsCreator sc = new ScriptsCreator(utt, 300);
		ArrayList<BasicExpandedConfigurableScript> scriptsCompleteSet = sc.getScriptsMixReducedSet();

		iScriptsAi1.forEach((idSc) -> {
			scriptsAI.add(scriptsCompleteSet.get(idSc));
		});

		return scriptsAI;
	}

}
