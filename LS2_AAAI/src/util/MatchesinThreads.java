package util;

import java.util.ArrayList;
import java.util.HashSet;

import ai.synthesis.DslLeague.Runner.SettingsAlphaDSL;
import ai.synthesis.dslForScriptGenerator.DSLCommandInterfaces.ICommand;
import ai.synthesis.grammar.dslTree.interfacesDSL.iDSL;
import ai.synthesis.grammar.dslTree.utils.ReduceDSLController;
import ai.synthesis.twophasessa.TradutorDSL;
import rts.GameState;
import rts.PhysicalGameState;
import rts.units.UnitTypeTable;


public class MatchesinThreads {
	private static BuilderGrammars builder;
	private static boolean typePlayout=false;

	public static void main(String[] args) {
		
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
		
		long startTime=System.currentTimeMillis();
		
		for(int i=0;i<1;i++)
		{
			// TODO Auto-generated method stub
			builder = BuilderGrammars.getInstance();//Z
//			iDSL iSc1 = builder.buildS1Grammar();
//			iDSL iSc2 = builder.buildS1Grammar();
			TradutorDSL td = new TradutorDSL("train(Ranged,50,Left) build(Barrack,50,Down) attack(Worker,closest) for(u) (if(HaveQtdUnitsAttacking(Worker,12,u)) then(train(Worker,50,Right,u)) else(harvest(1,u) attack(Ranged,closest,u)) if(HaveQtdUnitsbyType(Worker,4,u)) then(build(Barrack,50,Down,u)) else(build(Barrack,50,Down,u) attack(Worker,closest,u) train(Worker,50,Up,u))) if(HaveUnitsToDistantToEnemy(Ranged,5)) then(harvest(1) train(Worker,50,Right) attack(Worker,closest)) else(build(Barrack,50,Down) (Z)) ");
			iDSL iSc1 =td.getAST();
			TradutorDSL td2 = new TradutorDSL("build(Barrack,1,Down) harvest(1) train(Worker,1,EnemyDir)  train(Ranged,1,EnemyDir) attack(Ranged,closest)");
			iDSL iSc2 =td2.getAST();
			
//			System.out.println("firsst script "+iSc2.translate());
//			System.out.println("Second script "+iSc1.translate());

			//Enable this block to run playouts in threads
			if(typePlayout)
			{

				evaluate_thread_playouts(iSc1, iSc2, gs, pgs, utt);
			}
			else
			{
				evaluate_thread_scripts(iSc1, iSc2, gs, pgs, utt);
			}

		}
		long endTime=System.currentTimeMillis();
		long duration =(endTime - startTime);
		System.out.println("Total duration "+duration);
	}

	private static float evaluate_thread_scripts(iDSL script1, iDSL script2, GameState gs, PhysicalGameState pgs, UnitTypeTable utt) {
//		System.out.println("Runnable Simulated Annealing Version");

		TestSingleMatchUnique runner1 = new TestSingleMatchUnique(script1, script2, gs, pgs, utt, "runner1", 2,3);
//		TestSingleMatch runner2 = new TestSingleMatch(script1, script2, gs, pgs, utt, "runner2", 3,2);
//		TestSingleMatch runner3 = new TestSingleMatch(script1, script2, gs, pgs, utt, "runner3" ,3,3);
//		TestSingleMatch runner4 = new TestSingleMatch(script1, script2, gs, pgs, utt, "runner4" ,3,4);
//		TestSingleMatch runner5 = new TestSingleMatch(script1, script2, gs, pgs, utt, "runner5 ");
//		TestSingleMatch runner6 = new TestSingleMatch(script1, script2, gs, pgs, utt, "runner6");
//		TestSingleMatch runner7 = new TestSingleMatch(script1, script2, gs, pgs, utt, "runner7" );
//		TestSingleMatch runner8 = new TestSingleMatch(script1, script2, gs, pgs, utt, "runner8" );
//		TestSingleMatch runner9 = new TestSingleMatch(script1, script2, gs, pgs, utt, "runner9" );
//		TestSingleMatch runner10 = new TestSingleMatch(script1, script2, gs, pgs, utt, "runner10 ");
//		TestSingleMatch runner11 = new TestSingleMatch(script1, script2, gs, pgs, utt, "runner1");
//		TestSingleMatch runner12 = new TestSingleMatch(script1, script2, gs, pgs, utt, "runner2" );
//		TestSingleMatch runner13 = new TestSingleMatch(script1, script2, gs, pgs, utt, "runner3" );
//		TestSingleMatch runner14 = new TestSingleMatch(script1, script2, gs, pgs, utt, "runner4" );
//		TestSingleMatch runner15 = new TestSingleMatch(script1, script2, gs, pgs, utt, "runner5 ");
//		TestSingleMatch runner16 = new TestSingleMatch(script1, script2, gs, pgs, utt, "runner6");
//		TestSingleMatch runner17 = new TestSingleMatch(script1, script2, gs, pgs, utt, "runner7" );
//		TestSingleMatch runner18 = new TestSingleMatch(script1, script2, gs, pgs, utt, "runner8" );
//		TestSingleMatch runner19 = new TestSingleMatch(script1, script2, gs, pgs, utt, "runner9" );
//		TestSingleMatch runner20 = new TestSingleMatch(script1, script2, gs, pgs, utt, "runner10 ");




		//		TestSinglePlayout runner1 = new TestSinglePlayout(script1, script2, gs, pgs, utt);
		//		TestSinglePlayout runner2 = new TestSinglePlayout(script1, script2, gs, pgs, utt);
		//		TestSinglePlayout runner3 = new TestSinglePlayout(script1, script2, gs, pgs, utt);
		//		TestSinglePlayout runner4 = new TestSinglePlayout(script1, script2, gs, pgs, utt);
		try {


			runner1.start();
//			runner2.start();
//			runner3.start();
//			runner4.start();
//			runner5.start();
//			runner6.start();
//			runner7.start();
//			runner8.start();
//			runner9.start();
//			runner10.start();
//			runner11.start();
//			runner12.start();
//			runner13.start();
//			runner14.start();
//			runner15.start();
//			runner16.start();
//			runner17.start();
//			runner18.start();
//			runner19.start();
//			runner20.start();


			runner1.join();
//			runner2.join();
//			runner3.join();
//			runner4.join();
//			runner5.join();
//			runner6.join();
//			runner7.join();
//			runner8.join();
//			runner9.join();
//			runner10.join();
//			runner11.join();
//			runner12.join();
//			runner13.join();
//			runner14.join();
//			runner15.join();
//			runner16.join();
//			runner17.join();
//			runner18.join();
//			runner19.join();
//			runner20.join();
			
			float totalScript2 = 0.0f;
			if (runner1.getWinner() == 1) {
				totalScript2 += runner1.getResult();
			} else if (runner1.getWinner() == -1) {
				totalScript2 += runner1.getResult();
			}
//			if (runner2.getWinner() == 1) {
//				totalScript2 += runner2.getResult();
//			} else if (runner2.getWinner() == -1) {
//				totalScript2 += runner2.getResult();
//			}
//
//			if (runner3.getWinner() == 0) {
//				totalScript2 += runner3.getResult();
//			} else if (runner3.getWinner() == -1) {
//				totalScript2 += runner3.getResult();
//			}
//			if (runner4.getWinner() == 0) {
//				totalScript2 += runner4.getResult();
//			} else if (runner4.getWinner() == -1) {
//				totalScript2 += runner4.getResult();
//			}

			HashSet<ICommand> uniqueCommands = new HashSet<>();
			uniqueCommands.addAll(runner1.getAllCommandIA2());
//			uniqueCommands.addAll(runner2.getAllCommandIA2());
//			uniqueCommands.addAll(runner3.getAllCommandIA1());
//			uniqueCommands.addAll(runner4.getAllCommandIA1());
			ReduceDSLController.removeUnactivatedParts(script2, new ArrayList<>(uniqueCommands));
			System.out.println("score second script "+totalScript2);
			return totalScript2;
		} catch (Exception e) {
			System.err.println("ai.synthesis.localsearch.DoubleProgramSynthesis.processMatch() " + e.getMessage());
			return -5.0f;
		}
	}

	private static float evaluate_thread_playouts(iDSL script1, iDSL script2, GameState gs, PhysicalGameState pgs, UnitTypeTable utt) {
		//System.out.println("Runnable Simulated Annealing Version");


		TestSinglePlayout runner1 = new TestSinglePlayout(script1, script2, gs, pgs, utt);
		TestSinglePlayout runner2 = new TestSinglePlayout(script1, script2, gs, pgs, utt);
		TestSinglePlayout runner3 = new TestSinglePlayout(script1, script2, gs, pgs, utt);
		TestSinglePlayout runner4 = new TestSinglePlayout(script1, script2, gs, pgs, utt);
		try {


			runner1.start();
			runner2.start();
			runner3.start();
			runner4.start();

			runner1.join();
			runner2.join();
			runner3.join();
			runner4.join();

			float totalScript2 = 0.0f;
			if (runner1.getWinner() == 1) {
				totalScript2 += runner1.getResult();
			} else if (runner1.getWinner() == -1) {
				totalScript2 += runner1.getResult();
			}
			if (runner2.getWinner() == 1) {
				totalScript2 += runner2.getResult();
			} else if (runner2.getWinner() == -1) {
				totalScript2 += runner2.getResult();
			}

			if (runner3.getWinner() == 0) {
				totalScript2 += runner3.getResult();
			} else if (runner3.getWinner() == -1) {
				totalScript2 += runner3.getResult();
			}
			if (runner4.getWinner() == 0) {
				totalScript2 += runner4.getResult();
			} else if (runner4.getWinner() == -1) {
				totalScript2 += runner4.getResult();
			}

			HashSet<ICommand> uniqueCommands = new HashSet<>();
			uniqueCommands.addAll(runner1.getAllCommandIA2());
			uniqueCommands.addAll(runner2.getAllCommandIA2());
			uniqueCommands.addAll(runner3.getAllCommandIA1());
			uniqueCommands.addAll(runner4.getAllCommandIA1());
			ReduceDSLController.removeUnactivatedParts(script2, new ArrayList<>(uniqueCommands));
			System.out.println("score second script "+totalScript2);
			return totalScript2;
		} catch (Exception e) {
			System.err.println("ai.synthesis.localsearch.DoubleProgramSynthesis.processMatch() " + e.getMessage());
			return -5.0f;
		}
	}
}
