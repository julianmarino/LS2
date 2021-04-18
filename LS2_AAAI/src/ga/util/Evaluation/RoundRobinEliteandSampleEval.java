package ga.util.Evaluation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import ai.core.AI;
import ai.synthesis.DslLeague.Runner.SettingsAlphaDSL;
import ai.synthesis.dslForScriptGenerator.DslAI;
import ai.synthesis.dslForScriptGenerator.DSLCommandInterfaces.ICommand;
import ai.synthesis.dslForScriptGenerator.DSLCompiler.IDSLCompiler;
import ai.synthesis.dslForScriptGenerator.DSLCompiler.MainDSLCompiler;
import ai.synthesis.grammar.dslTree.interfacesDSL.iDSL;
import ai.synthesis.grammar.dslTree.utils.ReduceDSLController;

import java.util.Random;

import ga.ScriptTableGenerator.ScriptsTable;
import ga.config.ConfigurationsGA;
import ga.model.Chromosome;
import ga.model.Population;
import ga.util.PreSelection;
import model.EvalResult;
import rts.GameState;
import rts.PhysicalGameState;
import rts.units.UnitTypeTable;
import util.LeitorLog;

public class RoundRobinEliteandSampleEval implements RatePopulation {

	private static final int TOTAL_PARTIDAS_ROUND = 1;
	private static final int BATCH_SIZE = 2;

	//private static final String pathSOA = "/home/rubens/cluster/TesteNewGASG/configSOA/";
	private static final String pathSOA = System.getProperty("user.dir").concat("/configSOA/");

	//private static final String pathCentral = "/home/rubens/cluster/TesteNewGASG/centralSOA";
	private static final String pathCentral = System.getProperty("user.dir").concat("/centralSOA");

	/*
	 * key - name of the file
	 * value - data of the file
	 */
	private final LinkedHashMap<String, String> battleFiles = new LinkedHashMap<String, String>();

	// Classes de informaÃ§Ã£o
	private int atualGeneration = 0;

	// Atributos locais
	ArrayList<String> SOA_Folders = new ArrayList<>();
	ArrayList<String> SOA_arqs = new ArrayList<>();

	ArrayList<Chromosome> ChromosomeSample = new ArrayList<>();
	HashMap<Chromosome, BigDecimal> eliteIndividuals;
	ArrayList<iDSL> scriptsAST;
	ScriptsTable scrTable;
	
	IDSLCompiler compiler = new MainDSLCompiler();  

	public RoundRobinEliteandSampleEval() {
		super();
	}

	@Override
	public Population evalPopulation(Population population, int generation, ScriptsTable scriptsTable) {
		System.out.println("Size astTable "+scriptsAST.size());
		this.atualGeneration = generation;
		//SOA_Folders.clear();
		// clean existent values in the population
		population.clearValueChromosomes();

		//Run the matches
		population=runBattles(buildBattles(population),population);

		// used to run in cluster.
		//controllExecute();

		// used to run in cluster.
		//removeLogsEmpty();

		// ler resultados
//		ArrayList<EvalResult> resultados = lerResultados();
//		//check if all files were read
//		while(resultados.size() < this.battleFiles.size()) {
//			//record missing files
//			generatedMissingFiles(resultados);
//			//continue with the iterative controll
//			controllExecute();
//			ArrayList<EvalResult> missResultados = lerResultados();
//			resultados.addAll(missResultados);
//		}
//
//		System.out.println("Number of matchs necessary "+ this.battleFiles.size());
//		System.out.println("Total of matchs read "+resultados.size());
//
//		// atualizar valores das populacoes
//		updatePopulationValue(resultados, population);

		return population;
	}

	private void removeLogsEmpty() {
		LeitorLog log = new LeitorLog();
		log.removeNoResults();
	}

	public Population updatePopulationValue(ArrayList<EvalResult> results, Population pop) {
		//ArrayList<EvalResult> resultsNoDraw = removeDraw(results);
		ArrayList<EvalResult> resultsNoDraw = results;

		/*
		 * System.out.println("AvaliaÃ§Ãµes sem Draw"); for (EvalResult evalResult
		 * : resultsNoDraw) { evalResult.print(); }
		 */

		for (EvalResult evalResult : resultsNoDraw) {
			updateChomoPopulation(evalResult, pop);
		}

		return pop;
	}

	private void updateChomoPopulation(EvalResult evalResult, Population pop) {
		if (evalResult.getEvaluation() == 0) {
			//IAWinner = evalResult.getIA1();
			updateChromo(pop, evalResult.getIA1(), BigDecimal.ONE);
		} else if (evalResult.getEvaluation() == 1){
			updateChromo(pop, evalResult.getIA2(), BigDecimal.ONE);
		}else{
			updateChromo(pop, evalResult.getIA1(), new BigDecimal(0.5));
			updateChromo(pop, evalResult.getIA2(), new BigDecimal(0.5));
		}

	}

	private void updateChromo(Population pop, String IAWinner, BigDecimal value) {
		// buscar na população a IA compatível.
		Chromosome chrUpdate = null;
		for (Chromosome ch : pop.getChromosomes().keySet()) {
			if (convertBasicTuple(ch).equals(IAWinner)) {
				chrUpdate = ch;
			}
		}

		if (chrUpdate != null) {
			// atualizar valores.
			BigDecimal toUpdate = pop.getChromosomes().get(chrUpdate);
			if (toUpdate != null) {
				toUpdate = toUpdate.add(value);
				HashMap<Chromosome, BigDecimal> chrTemp = pop.getChromosomes();
				chrTemp.put(chrUpdate, toUpdate);
			}
		}
	}

	private ArrayList<EvalResult> removeDraw(ArrayList<EvalResult> results) {
		ArrayList<EvalResult> rTemp = new ArrayList<>();

		for (EvalResult evalResult : results) {
			if (evalResult.getEvaluation() != -1) {
				rTemp.add(evalResult);
			}
		}

		return rTemp;
	}

	public ArrayList<EvalResult> lerResultados() {
		LeitorLog leitor = new LeitorLog();
		ArrayList<EvalResult> resultados = leitor.processar();
		/*
		 * for (EvalResult evalResult : resultados) { evalResult.print(); }
		 */
		return resultados;
	}

	/**
	 * Verifica se os jobs jÃ¡ foram encerrados no cluster.
	 */
	private void controllExecute() {

		// look for clients and share the data.
		while (hasSOACentralFile()) {
			// update the quantity of SOA Clients.
			updateSOAClients();
			// update the file to process
			updateFiles();
			// share the files between SOA Clients
			shareFiles();

			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		while (hasSOAArq()) {
			try {
				Thread.sleep(50000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	private void shareFiles() {
		for (String folder : this.SOA_Folders) {

			for (int i = 0; i < BATCH_SIZE; i++) {

				if (SOA_arqs.size() == 0) {
					return;
				}
				String nFile = SOA_arqs.get(0);
				File f = new File(nFile);
				try {
					copyFileUsingStream(f, new File(folder + "/" + f.getName()));
					SOA_arqs.remove(nFile);
					f.delete();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		}

	}

	private void updateFiles() {
		this.SOA_arqs.clear();
		File CentralFolder = new File(pathCentral + "/");
		for (File file : CentralFolder.listFiles()) {
			SOA_arqs.add(file.getAbsolutePath());
		}
	}

	private void updateSOAClients() {
		this.SOA_Folders.clear();
		File configSOAFolder = new File(pathSOA);
		if (configSOAFolder != null) {
			for (File folder : configSOAFolder.listFiles()) {
				if (folder.listFiles().length == 0) {
					SOA_Folders.add(folder.getAbsolutePath());
				}
			}
		}

	}

	/**
	 * irÃ¡ verificar se todas as pastas SOA estÃ£o vazias
	 * 
	 * @return True se estiver vazias
	 */
	private boolean hasSOAArq() {
		updateSOACLientFull();
		for (String soaFolder : this.SOA_Folders) {
			String strConfig = soaFolder;
			File f = new File(strConfig);
			String[] children = f.list();
			if (children.length > 0) {
				return true;
			}

		}

		return false;
	}

	private void updateSOACLientFull() {
		this.SOA_Folders.clear();
		File configSOAFolder = new File(pathSOA);
		for (File folder : configSOAFolder.listFiles()) {
			SOA_Folders.add(folder.getAbsolutePath());
		}

	}

	/**
	 * IrÃ¡ verificar a pasta central nÃ£o tem mais arquivos.
	 * 
	 * @return
	 */
	private boolean hasSOACentralFile() {
		File centralF = new File(pathCentral);
		if (centralF.list().length > 0) {
			return true;
		}
		return false;
	}

	/**
	 * MetÃ³do para enviar todas as batalhas ao cluster.
	 * 
	 * @param population
	 *            Que contÃ©m as configuracoes para a IA
	 */
	private ArrayList<String> buildBattles(Population population) {
		ArrayList<String> matches =new ArrayList<String>();
		int numberSOA = 1;
		this.battleFiles.clear();
		// montar a lista de batalhas que irÃ£o ocorrer


		defineChromosomeSample(population);
		defineRandomSet(population);

		for (int i = 0; i < TOTAL_PARTIDAS_ROUND; i++) {

			for (Chromosome cIA1 : population.getChromosomes().keySet()) {


				for (Chromosome cIA2 : this.ChromosomeSample) {

					// first position
					String match1 = convertBasicTuple(cIA1) + "#(" + convertBasicTuple(cIA2) + ")#" + i + "#"
							+ atualGeneration;
					matches.add(match1);
					// second position
					String match2 = "(" + convertBasicTuple(cIA2) + ")#" + convertBasicTuple(cIA1) + "#" + i
							+ "#" + atualGeneration;
					matches.add(match2);
					//}
				}
			}
		}
		System.out.println("size of matches here "+matches.size());
		return matches;

	}

	private Population runBattles(ArrayList <String> matches, Population population) 
	{
		ArrayList<TestSingleMatch> singleMatches=new ArrayList<TestSingleMatch>();
		HashMap<Integer, HashSet<ICommand>> uniqueCommandsPopulation=new HashMap<Integer, HashSet<ICommand>>();
		int currentmatchesPerformed=0;
		int TotalmatchesPerformed=0;

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
		HashMap<Integer, AI> iasPopulation=new HashMap<Integer, AI>();;
		
		for(int i=0;i<matches.size();i++)
		{				
			String[] itens = matches.get(i).split("#");	
			int idIA1=convertToInt(itens[0]);
			int idIA2=convertToInt(itens[1]);
			iDSL sIA1 = convertToDSL(itens[0]);
			iDSL sIA2 = convertToDSL(itens[1]);
			AI ai1 = buildCommandsIA(utt, sIA1);
	        AI ai2 = buildCommandsIA(utt, sIA2);
	        iasPopulation.put(idIA1, ai1);
	        iasPopulation.put(idIA2, ai2);
		}
		
		System.out.println("starting matches");
		while(currentmatchesPerformed<matches.size())
		{
			int limitProcesses;
			if(matches.size()-currentmatchesPerformed>=ConfigurationsGA.nProcessorsThreads)
			{
				limitProcesses=ConfigurationsGA.nProcessorsThreads;
			}
			else
			{
				limitProcesses=matches.size()-currentmatchesPerformed;
			}
			for(int i=TotalmatchesPerformed;i<TotalmatchesPerformed+limitProcesses;i++)
			{								
				
				String[] itens = matches.get(i).split("#");	
				//System.out.println("match line "+matches.get(i));
				iDSL sIA1 = convertToDSL(itens[0]);
				iDSL sIA2 = convertToDSL(itens[1]);
				int idIA1=convertToInt(itens[0]);
				int idIA2=convertToInt(itens[1]);
				HashSet<ICommand> uniqueCommandsS1 = new HashSet<>();
				HashSet<ICommand> uniqueCommandsS2 = new HashSet<>();
				//uniqueCommandsPopulation.put(idIA1, uniqueCommandsS1);
				//uniqueCommandsPopulation.put(idIA2, uniqueCommandsS2);
				//System.out.println("first script "+sIA1.translate());
				//System.out.println("second script "+sIA2.translate());
		        
				singleMatches.add(new TestSingleMatch(sIA1, sIA2, gs.clone(), pgs, utt, Integer.toString(i), idIA1, idIA2, iasPopulation));
				
			}

			
			for(int i=TotalmatchesPerformed;i<TotalmatchesPerformed+limitProcesses;i++)
			{
				singleMatches.get(i).playGame();
				currentmatchesPerformed++;
			}			
			
//			try {
//				for(int i=TotalmatchesPerformed;i<TotalmatchesPerformed+limitProcesses;i++)
//				{
//					singleMatches.get(i).join();
//				}
//
//			} catch (Exception e) {
//				System.err.println("ai.synthesis.localsearch.DoubleProgramSynthesis.processMatch() " + e.getMessage());
//
//			}
		

			TotalmatchesPerformed=currentmatchesPerformed;
		}
		System.out.println("ending matches");

		for(int i=0;i<matches.size();i++)
		{				
			population=updateChromosomes(singleMatches.get(i).getWinner(), matches.get(i), population);
			String[] itens = matches.get(i).split("#");	
			int idIA1=convertToInt(itens[0]);
			int idIA2=convertToInt(itens[1]);
			//uniqueCommandsPopulation.get(idIA1).addAll(singleMatches.get(i).getAllCommandIA1());
			//uniqueCommandsPopulation.get(idIA2).addAll(singleMatches.get(i).getAllCommandIA2()); 
		}

		if(ConfigurationsGA.removeRulesAST)
		{
			population=updatePopulationRemotionRules(population,iasPopulation);
		}


		return population;
	}
	
    private AI buildCommandsIA(UnitTypeTable utt, iDSL code) {
        HashMap<Long, String> counterByFunction = new HashMap<Long, String>();
        List<ICommand> commandsDSL = compiler.CompilerCode(code, utt);
        AI aiscript = new DslAI(utt, commandsDSL, "P1", code, counterByFunction);
        return aiscript;
    }

	private Population updateChromosomes(int winner, String lineMatch, Population population) {
		String[] itens = lineMatch.split("#");	
		String player0=itens[0];
		String player1=itens[1];

		
		if(winner==0 && !(player0.contains("(")))
		{
			population=updatePopulationASTs(population,player0, BigDecimal.ONE);
		}
		else if(winner==1 && !(player1.contains("(")))
		{
			population=updatePopulationASTs(population,player1, BigDecimal.ONE);
		}
		else if(winner==-1)
		{
			if(!(player0.contains("(")))
			{
				population=updatePopulationASTs(population,player0, new BigDecimal(0.5));
			}
			else if(!(player1.contains("(")))
			{
				population=updatePopulationASTs(population,player1, new BigDecimal(0.5));
			}
		}
		return population;	
		
	}
	
	private Population updatePopulationASTs(Population population,String IAWinner, BigDecimal value) {
		
		Chromosome chrUpdate = null;
		for (Chromosome ch : population.getChromosomes().keySet()) {
			if (convertBasicTuple(ch).equals(IAWinner)) {
				chrUpdate = ch;
			}
		}

		if (chrUpdate != null) {
			BigDecimal toUpdate = population.getChromosomes().get(chrUpdate);
			if (toUpdate != null) {
				toUpdate = toUpdate.add(value);
				HashMap<Chromosome, BigDecimal> chrTemp = population.getChromosomes();
				chrTemp.put(chrUpdate, toUpdate);
			}
		}
		
		return population;
	}
	
	private Population updatePopulationRemotionRules(Population population, HashMap<Integer, AI> iasPopulation)
	{
		for (Chromosome ch : population.getChromosomes().keySet()) {
			int idScript=Integer.parseInt(convertBasicTuple(ch).replace(";", ""));
			BigDecimal toUpdate = population.getChromosomes().get(ch);
			iDSL originalScript=(iDSL) scriptsAST.get(idScript);
			String originalScriptStr=originalScript.translate();
			System.out.println("before remotion "+originalScriptStr);
			ReduceDSLController.removeUnactivatedParts(originalScript, new ArrayList<>(((DslAI) iasPopulation.get(idScript)).getCommands()));
			System.out.println("after remotion "+originalScript.translate());
			//updateReferencesforScript(originalScriptStr,originalScript.translate(),idScript);
			//population=addToPopulation(newScript,population,toUpdate);
		}
		return population;
		
	}
//	private void updateReferencesforScript(String originalScript, String reducedString, int idScriptOriginal)
//	{
//		
//		if(!originalScript.equals(reducedString))
//		{
//			System.out.println("changing id "+idScriptOriginal+ "current id "+scrTable.getScriptTable().get(originalScript));
//			System.out.println("orig "+originalScript);
//			System.out.println("new "+reducedString);
//			BigDecimal idScript=scrTable.getScriptTable().get(originalScript);
//			scrTable.getScriptTable().remove(originalScript);
//			scrTable.getScriptTable().put(reducedString, idScript);
//		}
//	}

	private iDSL convertToDSL(String script) {

		ArrayList<Integer> iScriptsAi1 = new ArrayList<>();
		String[] itens = script.replace("(", "").replace(")", "").split(";");

		for (String element : itens) {
			iScriptsAi1.add(Integer.decode(element));
		}

		return scriptsAST.get(iScriptsAi1.get(0));
	}
	
	private int convertToInt(String script) {

		ArrayList<Integer> iScriptsAi1 = new ArrayList<>();
		String[] itens = script.replace("(", "").replace(")", "").split(";");

		for (String element : itens) {
			iScriptsAi1.add(Integer.decode(element));
		}

		return iScriptsAi1.get(0);
	}

	private void defineRandomSet(Population population) {


		int totalPop = population.getChromosomes().size();
		Random rand = new Random();
		HashSet<Chromosome> samples = new HashSet<>();
		ArrayList<Chromosome> temp = new ArrayList<>(population.getChromosomes().keySet());
		//System.out.print("Random set ");
		while (samples.size() < ConfigurationsGA.QTD_ENEMIES_SAMPLE_RANDOM) {

			Chromosome cTemp;
			do {
				cTemp = temp.get(rand.nextInt(totalPop));
			}while(ChromosomeSample.contains(cTemp));
			//System.out.print("Random set "+cTemp.getGenes().toString());
			samples.add(cTemp);
		}
		System.out.println("");
		this.ChromosomeSample.addAll(samples);

	}

	private void defineChromosomeSample(Population population) {

		this.ChromosomeSample.clear();
		PreSelection ps=new PreSelection(population);	
		HashMap<Chromosome, BigDecimal> elite=(HashMap<Chromosome, BigDecimal>)ps.sortByValue(population.getChromosomes());
		ArrayList<Entry<Chromosome, BigDecimal>> arrayElite = new ArrayList<>();

		if(getEliteIndividuals().size()>0)
		{
			arrayElite.addAll(getEliteIndividuals().entrySet());
		}
		else
		{
			arrayElite.addAll(elite.entrySet());
		}

		System.out.println("Elite last iteration (Eval function)");
		HashSet<Chromosome> eliteH = new HashSet<>();
		for(int i=0;i<arrayElite.size();i++)
		{
			eliteH.add(arrayElite.get(i).getKey());
			System.out.print(arrayElite.get(i).getKey().getGenes().toString()+" ");
		}
		System.out.println("");
		this.ChromosomeSample.addAll(eliteH);
	}

	private String convertTuple(Chromosome cromo) {
		String tuple = "'";

		for (Integer integer : cromo.getGenes()) {
			tuple += integer + ";";
		}

		return tuple += "'";
	}

	private String convertBasicTuple(Chromosome cromo) {
		String tuple = "";

		for (Integer integer : cromo.getGenes()) {
			tuple += integer + ";";
		}

		return tuple;
	}

	private void copyFileUsingStream(File source, File dest) throws IOException {
		InputStream is = null;
		OutputStream os = null;
		try {
			is = new FileInputStream(source);
			os = new FileOutputStream(dest);
			byte[] buffer = new byte[1024];
			int length;
			while ((length = is.read(buffer)) > 0) {
				os.write(buffer, 0, length);
			}
		} finally {
			is.close();
			os.close();
		}
	}

	/**
	 * Envia o sinal de exit para todos os SOA clientes
	 */
	@Override
	public void finishProcess() {
		for (String soaFolder : this.SOA_Folders) {
			String strConfig = soaFolder;
			File f = new File(strConfig + "/exit");
			try {
				f.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

	}

	private void generatedMissingFiles(ArrayList<EvalResult> resultados) {
		HashMap<String, String> intersect = new HashMap<String, String>();
		//check by data
		for(String key : this.battleFiles.keySet()) {
			String fileData = this.battleFiles.get(key);
			String[] itens = fileData.split("#");
			if(!thereIsMatch(itens[0],itens[1], resultados)) {
				intersect.put(key, fileData);
			}

		}
		//record the necessary files
		int cont = 1;
		System.out.println("Missed Files found "+ intersect.size()+ " missed files to be generated");
		for(String key : intersect.keySet()) {
			String data = intersect.get(key);
			String newName = key.substring(0, key.lastIndexOf("/")+1).concat("file"+cont).concat(".txt");
			System.out.println("File: "+key+" data: "+ data+" generated with name "+newName);
			saveNewBattle(newName,data);
			cont++;
		}
	}

	private boolean thereIsMatch(String ia1, String ia2, ArrayList<EvalResult> resultados) {
		for (EvalResult evalResult : resultados) {
			if(evalResult.getIA1().equals(ia1) && evalResult.getIA2().equals(ia2)) {
				return true;
			}
		}
		return false;
	}

	private void saveNewBattle(String miss, String data) {
		File arqConfig = new File(miss);
		if (!arqConfig.exists()) {
			try {
				arqConfig.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		// escreve a configuração de teste
		try {
			FileWriter arq = new FileWriter(arqConfig, false);
			PrintWriter gravarArq = new PrintWriter(arq);			
			gravarArq.println(data);
			gravarArq.flush();
			gravarArq.close();
			arq.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public HashMap<Chromosome, BigDecimal> getEliteIndividuals() {
		return eliteIndividuals;
	}

	public void setEliteIndividuals(HashMap<Chromosome, BigDecimal> eliteIndividuals) {
		this.eliteIndividuals = eliteIndividuals;
	}

	public void setASTlist(ArrayList<iDSL> scriptsAST) {
		this.scriptsAST = scriptsAST;
	}
	
	public void setScrTable(ScriptsTable scrTable) {
		this.scrTable = scrTable;
	}

}
