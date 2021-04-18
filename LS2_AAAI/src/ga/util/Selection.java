package ga.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import ai.synthesis.grammar.dslTree.builderDSLTree.BuilderDSLTreeSingleton;
import ai.synthesis.grammar.dslTree.interfacesDSL.iDSL;
import ga.ScriptTableGenerator.ScriptsTable;
import ga.config.ConfigurationsGA;
import ga.model.Chromosome;
import ga.model.Population;

public class Selection {

	/**
	 * Este método será responsável por controlar o processo de seleção. 
	 * Acredito que nele poderá ser feitas as chamadas para cruzamento e para mutação.
	 * @param populacaoInicial que será utilizada para aplicarmos as alterações.
	 * @return Population com os devidos novos cromossomos.
	 */
	
	static Random rand = new Random();
	public HashMap<Chromosome, BigDecimal> eliteIndividuals;
	
	public Population applySelection(Population populacaoInicial,ScriptsTable scrTable, String pathTableScripts){

		//System.out.println("printing the initial population");
		//printMap(populacaoInicial.getChromosomes());

		//class preselection have the methods for selecting parents according the tournament
		PreSelection ps=new PreSelection(populacaoInicial);			
		List<Map.Entry<Chromosome, BigDecimal>> parents=ps.Tournament();		
		//System.out.println("printing the parents selected for reproduction ");
		//printList(parents);

		//Class Reproduction have the methods for getting new population according the parents obtained before
		//using crossover and mutation
		Reproduction rp=new Reproduction(parents,scrTable,pathTableScripts);
		//Population newPopulation=rp.UniformCrossover();
		
		Population newPopulation;
		if(ConfigurationsGA.evolvingScript)
		{
			newPopulation=rp.CrossoverSingleScriptLimitedSize();
		}
		else
		{
			newPopulation=rp.CrossoverLimitedSize();
		}
		
		//System.out.println("printing the new population after crossover");
		//printMap(newPopulation.getChromosomes());
		newPopulation=rp.mutation(newPopulation);
		if(ConfigurationsGA.INCREASING_INDEX==true)
		{
		newPopulation=rp.IncreasePopulation(newPopulation);
		newPopulation=rp.DecreasePopulation(newPopulation);
		}
		
		newPopulation=rp.invaders(newPopulation);
		
		//System.out.println("printing the new population after mutation");
		//printMap(newPopulation.getChromosomes());

		//in elite is saved the best guys from the last population
		HashMap<Chromosome, BigDecimal> elite=(HashMap<Chromosome, BigDecimal>)ps.sortByValue(populacaoInicial.getChromosomes());		
		eliteIndividuals=elite;
		System.out.println("printing elite last population (Selection)");
		printMap(elite);
		
		//here we mutate copy of the elite individuals and add to the population 
		newPopulation=rp.eliteMutated(newPopulation,elite);
		//joining elite and new sons in chromosomesNewPopulation, 
		HashMap<Chromosome, BigDecimal> chromosomesNewPopulation=new HashMap<Chromosome, BigDecimal>();
		chromosomesNewPopulation.putAll(newPopulation.getChromosomes());
		chromosomesNewPopulation.putAll(elite);
		//System.out.println("printing complete new population (elite+new population)");
		//printMap(chromosomesNewPopulation);
		newPopulation.setChromosomes(chromosomesNewPopulation);
		
		//if the number of the new pop is less than the initial pop, fill with random elements
		newPopulation=fillWithRandom(newPopulation,scrTable);
		//System.out.println("printing complete new population with new random elements If that's the case");
		//printMap(chromosomesNewPopulation);

		newPopulation=rp.RemoveCopies(newPopulation);
		return newPopulation;
	}
	
	public Population applySelectionAST(Population populacaoInicial,ScriptsTable scrTable, String pathTableScripts, String pathTable, HashMap<Chromosome, BigDecimal> eliteIndividualsOld){
		Reproduction rp=new Reproduction(scrTable,pathTableScripts);
		
		PreSelection ps=new PreSelection(populacaoInicial);	
		HashMap<Chromosome, BigDecimal> newChromosomes = new HashMap<>();
		//in elite is saved the best guys from the last population
		HashMap<Chromosome, BigDecimal> elite=(HashMap<Chromosome, BigDecimal>)ps.sortByValue(populacaoInicial.getChromosomes());
		if(validateAllareTies(populacaoInicial.getChromosomes()))
		{
			eliteIndividuals=eliteIndividualsOld;
		}
		else
		{
			eliteIndividuals=elite;
		}
		
		List<Chromosome> listElite = new ArrayList<Chromosome>(elite.keySet());
		System.out.println("printing elite last population (Selection)");
		printMap(elite);
		
//		Chromosome tChom=new Chromosome();
//		tChom = new Chromosome();
//		tChom.addGene(listElite.get(0).getGenes().get(0));
		newChromosomes.put(listElite.get(0), BigDecimal.ZERO);
		
		Chromosome tChom = new Chromosome();
		int idNewScript;
//		System.out.println("Size asts "+scrTable.scriptsAST.size()+" size table "+scrTable.getScriptTable().size());
//		System.out.println("idOriginalScript "+listElite.get(0).getGenes().get(0));
		System.out.println("Elite (Best script last iteration) "+scrTable.scriptsAST.get(listElite.get(0).getGenes().get(0)).translate() );
//		System.out.println("looking in the original table "+scrTable.getScriptTable().get(scrTable.scriptsAST.get(listElite.get(0).getGenes().get(0)).translate()));
		//System.out.println("Starting genration mutations");
		while (newChromosomes.size()<ConfigurationsGA.SIZE_POPULATION-ConfigurationsGA.SIZE_INVADERS) {
			//System.out.println("sizes matchs2 "+scrTable.getScriptTable().size()+" "+scrTable.scriptsAST.size());
			
			iDSL sc_cloned = (iDSL) scrTable.scriptsAST.get(listElite.get(0).getGenes().get(0)).clone();
			iDSL iSc1=BuilderDSLTreeSingleton.changeNeighbourPassively(sc_cloned,scrTable.allBasicFunctionsRedefined,scrTable.allBooleansFunctionsRedefined);
			String newScript=iSc1.translate();
			//System.out.println("mutated "+newScript);
			int idCandidate=Population.verifyIfExistsIndividualInTable(scrTable.scriptsAST,newScript);
			if(idCandidate!=-1)
			{
				idNewScript=idCandidate;		
			}
			else
			{
//				System.out.println("beforeMutateScript "+cromScriptOriginal);
//				System.out.println("afterMutateScript "+cromScript);
//				int newId=scrTable.getScriptTable().size();
//				scrTable.getScriptTable().put(newScript, BigDecimal.valueOf(newId));
//				scrTable.setCurrentSizeTable(scrTable.getScriptTable().size());
//				addLineFile(newId+" "+newScript,pathTable);
//				idNewScript=newId;
				
//				if(scrTable.scriptsAST.size()!=newId)
//				{
//					System.out.println("Something is broken4");
//				}
				
				scrTable.scriptsAST.add(iSc1);
				idNewScript=scrTable.scriptsAST.size()-1;
				
			}
			//gerar o novo cromossomo com base no tamanho
			if(!Population.verifyIfExistsIndividualInPopulation(newChromosomes,idNewScript))
			{
				tChom = new Chromosome();
				tChom.addGene(idNewScript);
				newChromosomes.put(tChom, BigDecimal.ZERO);
			}
//			int sizeCh=rand.nextInt(ConfigurationsGA.SIZE_CHROMOSOME)+1;
//			for (int j = 0; j < sizeCh; j++) {
//				tChom.addGene(rand.nextInt(scrTable.getCurrentSizeTable()));
//			}
		}
		//System.out.println("Ending generation mutations");
		Population pop = new Population(newChromosomes);
		//System.out.println("sizebeforeinvaders "+pop.getChromosomes().size());
		pop=rp.invadersAST(pop);
		//System.out.println("sizeafterinvaders "+pop.getChromosomes().size());
		return pop;
	}

	private boolean validateAllareTies(HashMap<Chromosome, BigDecimal> population) {
		 
		for (Chromosome ch: population.keySet()){

			String key =ch.getGenes().toString();
			String value = population.get(ch).toString();  
			Double currentValue=Double.parseDouble(value);
			//validate if all are ties
			if(currentValue>1)
			{	
				return false;
			}
		}
		return true;
	}

	public void printMap(HashMap<Chromosome, BigDecimal> m)
	{
		for (Chromosome ch: m.keySet()){

			String key =ch.getGenes().toString();
			String value = m.get(ch).toString();  
			System.out.println(key + " " + value);  


		} 
	}
	public void printList(List<Map.Entry<Chromosome, BigDecimal>> l)
	{
		for (Map.Entry<Chromosome, BigDecimal> it: l){

			String key =it.getKey().getGenes().toString();
			String value = it.getValue().toString(); 
			System.out.println(key + " " + value);

		} 
	}
	public Population fillWithRandom(Population p,ScriptsTable scrTable)
	{
		while(p.getChromosomes().size()<ConfigurationsGA.SIZE_POPULATION)
		{
			Chromosome tChom = new Chromosome();
			int sizeCh=rand.nextInt(ConfigurationsGA.SIZE_CHROMOSOME)+1;
			for (int j = 0; j < sizeCh; j++) {
				tChom.addGene(rand.nextInt(scrTable.getCurrentSizeTable()));
			}
			p.getChromosomes().put(tChom, BigDecimal.ZERO);			
		}
		return p;
	}

	public static void addLineFile(String data, String pathTableScripts) {
	    try{    
	        File file =new File(pathTableScripts+"ScriptsTable.txt");    

	        //if file doesnt exists, then create it    
	        if(!file.exists()){    
	            file.createNewFile();      
	        }    

	        //true = append file    
	            FileWriter fileWritter = new FileWriter(file,true);        
	            BufferedWriter bufferWritter = new BufferedWriter(fileWritter);
	            bufferWritter.write(data);
	            bufferWritter.newLine();
	            bufferWritter.close();
	            fileWritter.close();  

	    }catch(Exception e){  
	        e.printStackTrace();    
	    } 
		}
}
