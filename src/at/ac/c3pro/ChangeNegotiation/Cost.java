package at.ac.c3pro.ChangeNegotiation;

public abstract class Cost {

	protected double cost = 0;
	
	public Cost() {
		// TODO Auto-generated constructor stub
		cost = this.generateRandomCost(1, 100)/100;
		
	}
	
	protected double generateRandomCost(int min, int max){
		
		return (min + (int)(Math.random() * ((max - min) + 1)));
	}
	public String toString(){
		
		return Double.toString(cost);			
	}
}
