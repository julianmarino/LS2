package util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;

import ai.synthesis.grammar.dslTree.interfacesDSL.iDSL;


public class comparingTables {
	
	private final static String pathTableScriptsAST = System.getProperty("user.dir").concat("/Table/");

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		ArrayList<iDSL> scriptsTableAST=readTableAST();
		ArrayList<String> tabl=readTableString();
		
		System.out.println("sizeAST"+scriptsTableAST.size());
		System.out.println("sizeString"+tabl.size());
		for (int i=0; i<scriptsTableAST.size(); i++)
		{
			if(!((scriptsTableAST.get(i).translate().trim()).equals(tabl.get(i).trim())))
			{
				System.out.println("something wrong ");
				System.out.println(scriptsTableAST.get(i).translate());
				System.out.println(tabl.get(i));
			}
		}

	}
	
    public static ArrayList<iDSL> readTableAST() {
    	ArrayList<iDSL> scriptsTableAST=new ArrayList<iDSL>();
    	FileInputStream fis;
		try {
			fis = new FileInputStream(pathTableScriptsAST+"/ScriptsTableAST.txt");
	    	ObjectInputStream ois = new ObjectInputStream(fis);
	    	
	    	scriptsTableAST = (ArrayList<iDSL>) ois.readObject();
	    	ois.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return scriptsTableAST;

    }
    
    public static ArrayList<String> readTableString() {
    	ArrayList<String> tabl = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(pathTableScriptsAST + "/ScriptsTable.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String code = line.substring(line.indexOf(" "), line.length());
                String[] strArray = line.split(" ");
                int idScript = Integer.decode(strArray[0]);
                tabl.add(code);
            }
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return tabl;
    }
    
    

}
