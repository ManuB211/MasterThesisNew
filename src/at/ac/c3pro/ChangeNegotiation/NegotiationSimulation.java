package at.ac.c3pro.ChangeNegotiation;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import at.ac.c3pro.chormodel.Choreography;
import at.ac.c3pro.chormodel.Role;
import au.com.bytecode.opencsv.CSVWriter;

public class NegotiationSimulation {

	
	Choreography choreo;
	public ArrayList<OpaqueNegotiation> scenarios = new ArrayList<OpaqueNegotiation>();
	public ArrayList<ArrayList<OpaqueNegotiationAlternative>> alternatives = new ArrayList<ArrayList<OpaqueNegotiationAlternative>>();
	//public ArrayList<ArrayList<OpaqueNegotiationAlternative>> Rankedalternatives = new ArrayList<ArrayList<OpaqueNegotiationAlternative>>();
	
	
	public NegotiationSimulation(Choreography choreo, int scenariosNumber,int maxalternativeperpartner) throws IOException {
		System.out.println(choreo);
		this.choreo = choreo;
		this.GenerateSimulationData(scenariosNumber,2);
		this.generateNegotiationAlternatives(scenariosNumber, maxalternativeperpartner);
		System.out.println("Additive score = "+ this.additiveUtility(this.alternatives.get(0).get(0),scenarios.get(0)));
		System.out.println("NashBargaining score = "+ this.nashBargainUtility(this.alternatives.get(0).get(0),scenarios.get(0),0));
		System.out.println("RKS score = "+ this.RKSUtility(this.alternatives.get(0).get(0),scenarios.get(0)));
		System.out.println("Alternative= "+ this.alternatives.get(0).get(0));
		if(this.alternatives.get(0).size()>1){
			System.out.println("Additive score = "+ this.additiveUtility(this.alternatives.get(0).get(1),scenarios.get(0)));
			System.out.println("NashBargaining score = "+ this.nashBargainUtility(this.alternatives.get(0).get(1),scenarios.get(0),0));
			System.out.println("RKS score = "+ this.RKSUtility(this.alternatives.get(0).get(1),scenarios.get(0)));
			System.out.println("Alternative= "+ this.alternatives.get(0).get(1));
		}
		this.serializev2();
	}
	
	
	public void serialize() throws IOException{
		int index =0;
        for(ArrayList<OpaqueNegotiationAlternative> opa : alternatives){        	
        	String logfilename = "Negotiationresults/NegotiationLog"+index+".csv";        	
            CSVWriter writer = new CSVWriter(new FileWriter(logfilename), ';');
            List<String[]> logStrings= new ArrayList<String[]>();
            String[] Heading = new String[]{ "Utility_Bank", "Utility_Insurance", "AdditiveScore", "NashBargainScore", "RKSscore"}; 
           // String[] Heading = new String[]{ "Utility_Bank", "Utility_Insurance", "Type", "Score"}; 
    		logStrings.add(Heading);		            
            for(OpaqueNegotiationAlternative spa : opa){
            	double additivescore = Math.round(this.additiveUtility(spa,scenarios.get(index))*1000);
            	double nashbargainscore = Math.round(this.nashBargainUtility(spa,scenarios.get(index),0)*1000);
            	double rksscore = Math.round(this.RKSUtility(spa,scenarios.get(index))*1000);
            	String[] oneRowData = ( (int)Math.round(scenarios.get(index).utility*1000)+ "#" +(int)Math.round(spa.decompose().get(0).utility(0.33,0.33,0.33)*1000) + "#" + (int)additivescore +"#"+ (int)nashbargainscore +"#"+(int)rksscore).split("#");           	
            	logStrings.add(oneRowData); 
//            	String[] oneRowData = ( scenarios.get(index).utility+ "#" +spa.decompose().get(0).utility(0.33,0.33,0.33) + "#" + "additiveScore" +"#"+ additivescore).split("#");           	
//            	logStrings.add(oneRowData);  
//            	oneRowData = ( scenarios.get(index).utility+ "#" +spa.decompose().get(0).utility(0.33,0.33,0.33) + "#" + "nashbargainScore" +"#"+ nashbargainscore).split("#");           	
//            	logStrings.add(oneRowData);  
//            	oneRowData = ( scenarios.get(index).utility+ "#" +spa.decompose().get(0).utility(0.33,0.33,0.33) + "#" + "rksScore" +"#"+ rksscore).split("#");           	
//            	logStrings.add(oneRowData);  
        	}
            writer.writeAll(logStrings);
            writer.close();
            index++;
        }
        
	}
	
	
	
	public void serializev2() throws IOException{
		int index =0;
		String logfilename = "Negotiationresults/NegotiationLog"+index+".csv";        	
        CSVWriter writer = new CSVWriter(new FileWriter(logfilename), ';');
        List<String[]> logStrings= new ArrayList<String[]>();
        String[] Heading = new String[]{ "Utility_Bank", "Utility_Insurance", "Type", "Score"}; 
		logStrings.add(Heading);		            
        for(ArrayList<OpaqueNegotiationAlternative> opa : alternatives){        	        	
           // String[] Heading = new String[]{ "Utility_Bank", "Utility_Insurance", "AdditiveScore", "NashBargainScore", "RKSscore"};            
            for(OpaqueNegotiationAlternative spa : opa){
            	double additivescore = this.additiveUtility(spa,scenarios.get(index));
            	double nashbargainscore = this.nashBargainUtility(spa,scenarios.get(index),0);
            	double rksscore = this.RKSUtility(spa,scenarios.get(index));            	
            	String[] oneRowData = ( scenarios.get(index).utility+ "#" +spa.decompose().get(0).utility(0.33,0.33,0.33) + "#" + "additiveScore" +"#"+ additivescore).split("#");           	
            	logStrings.add(oneRowData);  
            	oneRowData = ( scenarios.get(index).utility+ "#" +spa.decompose().get(0).utility(0.33,0.33,0.33) + "#" + "nashbargainScore" +"#"+ nashbargainscore).split("#");           	
            	logStrings.add(oneRowData);  
            	oneRowData = ( scenarios.get(index).utility+ "#" +spa.decompose().get(0).utility(0.33,0.33,0.33) + "#" + "rksScore" +"#"+ rksscore).split("#");           	
            	logStrings.add(oneRowData);  
        	}
           
            index++;
        }
        writer.writeAll(logStrings);
        writer.close();
        
	}
	
	private OpaqueNegotiation RandomScenario(Role sourcepartner, ArrayList<Role> targetpartners){
				
		CombinedCost source = new CombinedCost(sourcepartner);
		ArrayList<PublicCost> targets = new ArrayList<PublicCost>();
		for(Role targetpartner :targetpartners){
			targets.add(new PublicCost(targetpartner));
		}
		CommonCost commoncost = new CommonCost();		
		return new OpaqueNegotiation(sourcepartner, source, targets,commoncost);
	}
	
	private Role pickRandomPartnerSource(){		
		int randompartnerindex = (int)(Math.random() * ((choreo.collaboration.roles.size())));
		return (Role)choreo.collaboration.roles.toArray()[randompartnerindex];		
	}
	
private Role pickRandomPartnerTarget(ArrayList<Role> partners){
	Role chosenpartner;
	do{
		int randompartnerindex = (int)(Math.random() * ((choreo.collaboration.roles.size())));
		chosenpartner = (Role)choreo.collaboration.roles.toArray()[randompartnerindex];
	} while(partners.contains(chosenpartner));		
	
	return chosenpartner;
	}
	
public void generateNegotiationAlternatives(int scenariosNumber, int maxalternativeperpartner){
		
	System.out.println("**********Several Scenarios: propagations with alternatives");
	for(OpaqueNegotiation opn : scenarios){
		System.out.println("***Scenario: Source = "+"("+opn.source.publiccost.partner + "," + opn.source.toString()+")");
		ArrayList<ArrayList<SinglePartnerAlternative>> allPartnerAlternatives = new ArrayList<ArrayList<SinglePartnerAlternative>>();
		for(PublicCost target : opn.targets){
			ArrayList<SinglePartnerAlternative> onePartnerAlternatives = new ArrayList<SinglePartnerAlternative>();
			for(int i = 0; i<(int) (Math.random()*maxalternativeperpartner+1);i++){
				SinglePartnerAlternative tmp = new SinglePartnerAlternative(opn.source.publiccost,target, target.partner);
				tmp.setcommoncost(opn.commoncost);
				onePartnerAlternatives.add(tmp);
			}
			allPartnerAlternatives.add(onePartnerAlternatives);
		}	
		
		for(ArrayList<SinglePartnerAlternative> alt : allPartnerAlternatives){			
			for(SinglePartnerAlternative salt : alt)
				System.out.println(salt.toString());
		}
		System.out.println("****************************");
		ArrayList<OpaqueNegotiationAlternative> alternativPerScenario = new ArrayList<OpaqueNegotiationAlternative>();
		permute(allPartnerAlternatives, 0, alternativPerScenario);
		for(OpaqueNegotiationAlternative opa: alternativPerScenario){
			opa.setcommoncost(opn.commoncost);
		}
		this.alternatives.add(alternativPerScenario);
	}
		
}



public static void permute(ArrayList<ArrayList<SinglePartnerAlternative>> allPartnerAlternatives, int index, ArrayList<OpaqueNegotiationAlternative> allscenarioAlternatives){
    if(index == allPartnerAlternatives.size()){
    	System.out.println("**Number of generated alternatives = " + allscenarioAlternatives.size());
    	for(OpaqueNegotiationAlternative op : allscenarioAlternatives)		
    		System.out.println(op.toString());
    }
    else{
    	if(allscenarioAlternatives.isEmpty()){    
    		for(int i=0 ; i<allPartnerAlternatives.get(index).size(); i++){   
    			OpaqueNegotiationAlternative tmp = new OpaqueNegotiationAlternative(allPartnerAlternatives.get(index).get(i).source);
    			tmp.addtarget(allPartnerAlternatives.get(index).get(i).target);
        		allscenarioAlternatives.add(tmp);	
        	}
    		permute(allPartnerAlternatives,index+1,allscenarioAlternatives);
    	} else{
    		ArrayList<OpaqueNegotiationAlternative> tmp = new ArrayList<OpaqueNegotiationAlternative>();
    		for(int i=0 ; i<allPartnerAlternatives.get(index).size(); i++){
    			for(int j = 0; j<allscenarioAlternatives.size();j++){
    				OpaqueNegotiationAlternative  alt = new OpaqueNegotiationAlternative(allscenarioAlternatives.get(j).source);
    						alt.addtargets(allscenarioAlternatives.get(j).targets);
    				alt.addtarget(allPartnerAlternatives.get(index).get(i).target);
    				tmp.add(alt);
    			}
    		}	
        	permute(allPartnerAlternatives,index+1,tmp);        	        	
        }
    }
}

	
public void GenerateSimulationData(int scenariosNumber, int nbpartners){	
		for(int i=0;i<scenariosNumber; i++){
			ArrayList<Role> partners = new ArrayList<Role>();
			Role source = pickRandomPartnerSource();
			source.setweight(1/nbpartners);
			partners.add(source);
			for(int j=0; j<nbpartners-1;j++){
				Role target = pickRandomPartnerTarget(partners);
				target.setweight(1/nbpartners);
				partners.add(target);
			}
			partners.remove(0);
			scenarios.add(RandomScenario(source,partners));
		}		
	}


	public double additiveUtility(OpaqueNegotiationAlternative opna, OpaqueNegotiation opn){
		double sum = 0;
		int nbpartners =0;
		for(SinglePartnerAlternative spa: opna.decompose()){
			//sum = sum + spa.target.publiccost.partner.weight*spa.utility(0.33,0.33,0.33);
			//fix weights
			sum = sum + spa.utility(0.33,0.33,0.33);
			nbpartners++;
		}
		//return (sum + opn.source.publiccost.partner.weight*opn.utility(0.33,0.33,0.33));
		return (sum + opn.utility(0.33,0.33,0.33))/(nbpartners+1);
	}
	
	
	
	public double nashBargainUtility(OpaqueNegotiationAlternative opna, OpaqueNegotiation opn, double disagreement){
		double prod = 1;
		for(SinglePartnerAlternative spa: opna.decompose()){
			prod = prod * (spa.utility(0.33,0.33,0.33) - disagreement);
		}
		return (prod * (opn.utility(0.33,0.33,0.33)- disagreement));
	}
	
	public double RKSUtility(OpaqueNegotiationAlternative opna, OpaqueNegotiation opn){
		double max = opn.utility(0.33,0.33,0.33);
		double min = opn.utility(0.33,0.33,0.33);
		for(SinglePartnerAlternative spa: opna.decompose()){
			if(max < spa.utility(0.33,0.33,0.33))
			max = spa.utility(0.33,0.33,0.33);
		}		
		for(SinglePartnerAlternative spa: opna.decompose()){
			if(min > spa.utility(0.33,0.33,0.33))
			min = spa.utility(0.33,0.33,0.33);
		}
		return min/max;
	}
	
	public double efficiency(OpaqueNegotiationAlternative opna, OpaqueNegotiation opn){
		double sum = 0;
		int nbpartners =0;
		for(SinglePartnerAlternative spa: opna.decompose()){
			sum = sum + spa.utility(0.33,0.33,0.33);
			nbpartners++;
		}
		return ((sum + opn.utility(0.33,0.33,0.33))/(nbpartners+1));
	}
	

	public ArrayList<OpaqueNegotiation> RankOpaqueScenarios(){
	return null;
	}

}
