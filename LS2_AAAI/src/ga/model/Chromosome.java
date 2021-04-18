package ga.model;

import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * 
 * @author rubens
 * Os genes do cromossomos serão correspondentes aos scripts que serão utilizados no portfólio.
 * Os genes serão representados como números inteiros iniciando em 0. 
 * Cada gene corresponde à um script da classe BasicExpandedConfigurableScript
 */
public class Chromosome {
	private ArrayList<Integer> Genes;

	public Chromosome() {
		this.Genes = new ArrayList<>();
	}

	public ArrayList<Integer> getGenes() {
		return Genes;
	}

	public void setGenes(ArrayList<Integer> genes) {
		Genes = genes;
	}
	
	public void addGene(Integer gene){
		this.Genes.add(gene);
	}
	
	public void print(PrintWriter f0){
		System.out.print("Script ");
		f0.print("Script ");
		for (Integer gene : Genes) {
			System.out.print(gene+" ");
			f0.print(gene+" ");
		}
		System.out.println("");
		f0.println("");
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((Genes == null) ? 0 : Genes.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Chromosome other = (Chromosome) obj;
		if (Genes == null) {
			if (other.Genes != null)
				return false;
		} else if (!Genes.equals(other.Genes))
			return false;
		return true;
	}
	
	
	
}
