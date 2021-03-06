package ga.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.security.auth.kerberos.DelegationPermission;

import ai.synthesis.grammar.dslTree.interfacesDSL.iDSL;
import ga.ScriptTableGenerator.ScriptsTable;
import ga.config.ConfigurationsLS2;
import ga.model.Chromosome;
import ga.model.Population;
import ga.util.Evaluation.RatePopulation;
import ga.util.Evaluation.RoundRobinEliteandSampleEval;
import ga.util.Evaluation.RoundRobinEliteandSampleIterativeEval;
import util.sqlLite.Log_Facade;

public class RunGA {
	
	String curriculumportfolio;
	
	public RunGA(String curriculumportfolio)
	{
		this.curriculumportfolio=curriculumportfolio;
	}

	private Population population;
	private Instant timeInicial;
	private int generations = 0;
	private ScriptsTable scrTable;
	private HashMap<Chromosome, BigDecimal> eliteIndividuals=new HashMap<Chromosome, BigDecimal>();
	public ArrayList<iDSL> scriptsAST;

	private final String pathTableScripts = System.getProperty("user.dir").concat("/Table/");
	private final String pathLogs = System.getProperty("user.dir").concat("/Tracking/");
	private final String pathInitialPopulation = System.getProperty("user.dir").concat("/InitialPopulation/");
	private final String pathUsedCommands = System.getProperty("user.dir").concat("/commandsUsed/");
	private final String pathTableScriptsAST = System.getProperty("user.dir").concat("/Table/");
	
	static int [] frequencyIdsRulesForUCB= new int[ConfigurationsLS2.QTD_RULES];
	static int numberCallsUCB11=0;
	
	
	//private final String pathTableScripts = "/home/rubens/cluster/TesteNewGASG/Table/";

	/**
	 * Este metodo aplicar?? todas as fases do processo de um algoritmo Gen??tico
	 * Fres
	 * @param evalFunction
	 *            Ser?? a fun????o de avalia????o que desejamos utilizar
	 */
	public Population run(RoundRobinEliteandSampleEval evalFunction, String scriptsSetCover, HashSet<String> booleansUsed) {
		// Creating the table of scripts
		scrTable = new ScriptsTable(pathTableScripts);
		
		removeExistentTableAST();
		
		
		//do {
			if(ConfigurationsLS2.portfolioSetCover)
			{
				scrTable = scrTable.generateScriptsTableFromSetCover(ConfigurationsLS2.SIZE_TABLE_SCRIPTS,scriptsSetCover,booleansUsed,curriculumportfolio);
			}
			else
			{
				if(!ConfigurationsLS2.recoverTable)
				{
					scrTable = scrTable.generateScriptsTable(ConfigurationsLS2.SIZE_TABLE_SCRIPTS);
				}
				else
				{
					scrTable = scrTable.generateScriptsTableRecover();
				}
			}
		   //}while(scrTable.checkDiversityofTypes());
		scrTable.setCurrentSizeTable(scrTable.getScriptTable().size());

		PrintWriter f0;
		try {
			f0 = new PrintWriter(new FileWriter(pathLogs+"Tracking.txt"));

		do {
			// Fase 1 = gerar a popula????o inicial
			if(!ConfigurationsLS2.curriculum)
			{
				population = Population.getInitialPopulation(ConfigurationsLS2.SIZE_POPULATION, scrTable, pathTableScripts);
			}
			else
			{
				population = Population.getInitialPopulationCurriculum(ConfigurationsLS2.SIZE_POPULATION, scrTable, pathInitialPopulation);
			}			
			

			// Fase 2 = avalia a popula????o
			Chromosome tChom=new Chromosome();;
			tChom.addGene(0);
			eliteIndividuals.put(tChom, BigDecimal.ZERO);
			//saveListScripts(scrTable.scriptsAST,pathTableScriptsAST);
			evalFunction.setEliteIndividuals(eliteIndividuals);
			evalFunction.setASTlist(scrTable.scriptsAST);
			evalFunction.setScrTable(scrTable);
			population = evalFunction.evalPopulation(population, this.generations, scrTable);			
			
			System.out.println("INITIAL POPULATION");
			population.printWithValue(f0);
			
			
			
			//Get all the used commands
			if(ConfigurationsLS2.removeRules==true)
				population.fillAllCommands(pathTableScripts);
//		    Iterator it = population.getAllCommandsperGeneration().entrySet().iterator();
//		    while (it.hasNext()) {
//		        Map.Entry pair = (Map.Entry)it.next();
//		        int id=(Integer)pair.getKey();
//		        List<String> scripts= (List<String>) pair.getValue();
//		        System.out.println("key "+id+" "+scripts);
//		    }
			//Choose the used commands
			if(ConfigurationsLS2.removeRules==true)
				population.chooseusedCommands(pathUsedCommands);
//		    Iterator it = population.getUsedCommandsperGeneration().entrySet().iterator();
//		    while (it.hasNext()) {
//		        Map.Entry pair = (Map.Entry)it.next();
//		        int id=(Integer)pair.getKey();
//		        List<String> scripts= (List<String>) pair.getValue();
//		        System.out.println("key "+id+" "+scripts);
//		        //it.remove(); // avoids a ConcurrentModificationException
//		    }
			
			//Remove used commands from all commands
			if(ConfigurationsLS2.removeRules==true)
				population.removeCommands(scrTable,pathTableScripts);
			
//		    Iterator it2 = population.getAllCommandsperGeneration().entrySet().iterator();
//		    while (it2.hasNext()) {
//		        Map.Entry pair = (Map.Entry)it2.next();
//		        int id=(Integer)pair.getKey();
//		        List<String> scripts= (List<String>) pair.getValue();
//		        System.out.println("key "+id+" "+scripts);
//		    }
			
			System.out.println("Log - Iteration = " + this.generations);
			f0.println("Log - Iteration = " + this.generations);
			population.printWithValue(f0);
			f0.flush();
		} while (resetPopulation(population));

		resetControls();
		// Fase 3 = crit??rio de parada
		while (continueProcess()) {

			// Fase 4 = Sele????o (Aplicar Cruzamento e Muta????o)
			Selection selecao = new Selection();
			System.out.println("newAST");
			population = selecao.applySelectionAST(population, scrTable, pathTableScripts,pathTableScripts,eliteIndividuals);
			eliteIndividuals=selecao.eliteIndividuals;
			
			//saveListScripts(scrTable.scriptsAST,pathTableScriptsAST);
			// Repete-se Fase 2 = Avalia????o da popula????o
			evalFunction.setEliteIndividuals(eliteIndividuals);
			evalFunction.setASTlist(scrTable.scriptsAST);
			evalFunction.setScrTable(scrTable);
			population = evalFunction.evalPopulation(population, this.generations, scrTable);
			
			//Get all the used commands
			if(ConfigurationsLS2.removeRules==true)
				population.fillAllCommands(pathTableScripts);
			
			//Remove the unused commands
			if(ConfigurationsLS2.removeRules==true)
				population.chooseusedCommands(pathUsedCommands);
//		    Iterator it = population.getUsedCommandsperGeneration().entrySet().iterator();
//		    while (it.hasNext()) {
//		        Map.Entry pair = (Map.Entry)it.next();
//		        int id=(Integer)pair.getKey();
//		        List<String> scripts= (List<String>) pair.getValue();
//		        System.out.println("key "+id+" "+scripts);
//		        //it.remove(); // avoids a ConcurrentModificationException
//		    }
			//Remove used commands from all commands
			if(ConfigurationsLS2.removeRules==true)
				population.removeCommands(scrTable, pathTableScripts);

			// atualiza a gera????o
			updateGeneration();

			System.out.println("Log - Iteration = " + this.generations);
			f0.println("Log - Iteration = " + this.generations);
			population.printWithValue(f0);
			f0.flush();
			
			if(ConfigurationsLS2.UCB1==true)
			{
				Log_Facade.shrinkRewardTable();
				System.out.println("call shrink");
			}
		}
		
		f0.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		scriptsAST=scrTable.scriptsAST;
		return population;
	}

	private boolean resetPopulation(Population population2) {
		if (ConfigurationsLS2.RESET_ENABLED) {
			if (population2.isPopulationValueZero()) {
				System.out.println("Population reset!");
				return true;
			}
		}
		return false;
	}

	private void updateGeneration() {
		this.generations++;
	}

	private boolean continueProcess() {
		switch (ConfigurationsLS2.TYPE_CONTROL) {
		case 0:
			return hasTime();

		case 1:
			return hasGeneration();

		default:
			return false;
		}

	}

	private boolean hasGeneration() {
		if (this.generations < ConfigurationsLS2.QTD_GENERATIONS) {
			return true;
		}
		return false;
	}

	/**
	 * Fun????o que inicia o contador de tempo para o crit??rio de parada
	 */
	protected void resetControls() {
		this.timeInicial = Instant.now();
		this.generations = 0;
	}

	protected boolean hasTime() {
		Instant now = Instant.now();

		Duration duracao = Duration.between(timeInicial, now);

		// System.out.println( "Horas " + duracao.toMinutes());

		if (duracao.toHours() < ConfigurationsLS2.TIME_GA_EXEC) {
			return true;
		} else {
			return false;
		}

	}
	
	public String recoverScriptGenotype(String portfolioIds)
	{
		String portfolioGenotype;
        ArrayList<Integer> iScriptsAi1 = new ArrayList<>();
        portfolioIds = portfolioIds.replaceAll("\\s+","");
        String[] itens = portfolioIds.replace("[", "").replace("]", "").split(",");

        for (String element : itens) {
            iScriptsAi1.add(Integer.decode(element));
        }
        
        portfolioGenotype=buildScriptsTable(pathTableScripts).get(BigDecimal.valueOf(iScriptsAi1.get(0)));
       
		return portfolioGenotype;
	}
	
    public HashMap<BigDecimal, String> buildScriptsTable(String pathTableScripts) {
    	HashMap<BigDecimal, String> scriptsTable = new HashMap<>();
        String line="";
        try (BufferedReader br = new BufferedReader(new FileReader(pathTableScripts + "ScriptsTable.txt"))) {
            while ((line = br.readLine()) != null) {
                String code = line.substring(line.indexOf(" "), line.length());
                String[] strArray = line.split(" ");
                int idScript = Integer.decode(strArray[0]);
                scriptsTable.put(BigDecimal.valueOf(idScript), code);
            }
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block            
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch(Exception e){
            System.out.println(line);
            System.out.println(e);
        }

        return scriptsTable;
    }
    
    public void saveListScripts(ArrayList<iDSL> scripts, String path) {
    	FileOutputStream fos;
		try {
			removeExistentTableAST();
			fos = new FileOutputStream(path+"ScriptsTableAST.txt");
	    	ObjectOutputStream oos = new ObjectOutputStream(fos);
	    	oos.writeObject(scripts);
	    	oos.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

    }
    
    public void removeExistentTableAST() {
    	System.out.println("fullpath tableasts "+pathTableScriptsAST+"ScriptsTableAST.txt");
		File existentASTtable = new File(pathTableScriptsAST+"ScriptsTableAST.txt");
		boolean result;
		
		try {
			result = Files.deleteIfExists(existentASTtable.toPath());
			if(!result)
			{
				//System.out.println("Smething is wrong deleting the AST trees file");
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
    }
    
    
	
}
