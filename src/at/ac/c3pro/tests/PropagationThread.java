package at.ac.c3pro.tests;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import at.ac.c3pro.ImpactAnalysis.ImpactAnalysisUtil.Pair;
import at.ac.c3pro.change.IChangeOperation;
import at.ac.c3pro.change.Replace;
import at.ac.c3pro.changePropagation.ChangePropagationUtil;
import at.ac.c3pro.changePropagation.Stats;
import at.ac.c3pro.changePropagation.ChangePropagationUtil.ChgOpType;
import at.ac.c3pro.chormodel.Choreography;
import at.ac.c3pro.chormodel.IRole;
import at.ac.c3pro.chormodel.Role;
import at.ac.c3pro.io.Bpmn2ChoreographyModel;
import at.ac.c3pro.util.ChoreographyGenerator;
import at.ac.c3pro.util.FragmentUtil;

public class PropagationThread implements Runnable {

    private Map<IRole, Map<ChgOpType, Set<IChangeOperation>>> operations;
    private String xls_filename;
    public Map<String, Long> executionTimeMap = new HashMap<String, Long>();
    public Map<String, Stats> chgOpId2Stats = new HashMap<String, Stats>();
    public Choreography choreography = null;
    public List<IChangeOperation> all_ops = new LinkedList<IChangeOperation>();
    //logs all propagation instances
    public  Map<IChangeOperation, Map<Role, List<IChangeOperation>>> ChangePropagationLogMap = new HashMap<IChangeOperation, Map<Role, List<IChangeOperation>>>();

    public static Map<IRole,Integer> centrality = new HashMap<IRole,Integer>();
    public static Map<Pair<IRole,IRole>, Integer> PropagationGraphMetrics = new HashMap<Pair<IRole,IRole>, Integer>();
    public int maxRequests =0;
    public int minRequests = 10000000;
    public int maxDelete =0;
    public int minDelete = 10000000;
    public int maxReplace =0;
    public int minReplace = 10000000;
    public int maxInsert =0;
    public int minInsert = 10000000;
    
    
    
    public PropagationThread(String choreoPath, List<String> roleNames) throws Exception {
    	 Bpmn2ChoreographyModel BookTripChoreoModel = new Bpmn2ChoreographyModel(choreoPath, "BookTripOperationExtended");
         Choreography Booking  = ChoreographyGenerator.generateChoreographyFromModel(BookTripChoreoModel.choreoModel);
        Set<Role> listroles = new HashSet<Role>();
         for(Role role : Booking.collaboration.roles)
        	 if(roleNames.contains(role.name))
        		 listroles.add(role);        	 
       	 operations = ChangePropagationUtil.generateChangeOperationsForPublicModel(Booking, listroles);
       	 
         
         for(IRole role : operations.keySet()) {
             Map<ChgOpType, Set<IChangeOperation>> role2map1 = operations.get(role);
             int aux = 0;
             for(ChgOpType type : role2map1.keySet()){
            	 int size = operations.get(role).get(type).size();
            	 aux = aux + size;
            	 if(type.equals(ChgOpType.Delete)){
            		 if(size > maxDelete)
            			 maxDelete = size;
            		 if(size < minDelete)
            			 minDelete = size;
            	 }
            	 if(type.equals(ChgOpType.Replace)){
            		 if(size > maxReplace)
            			 maxReplace= size;
            		 if(size < minReplace)
            			 minReplace = size;
            	 }
            	 if(type.equals(ChgOpType.Insert)){
            		 if(size > maxInsert)
            			 maxInsert = size;
            		 if(size < minInsert)
            			 minInsert = size;
            	 }          	 
            	 
                 for(IChangeOperation op :operations.get(role).get(type)){
                     all_ops.add(op);
                    
                 }                
             }
             if(maxRequests < aux)
            	 maxRequests= aux;
             if(minRequests > aux)
            	 minRequests = aux;
         }
        		this.init(Booking); 
    }

    @Override
    public void run() {
        int nb = 0;
        int nb_total = all_ops.size();
        System.out.println(Thread.currentThread().getName()+" Starting...");
        //for (IChangeOperation op : operations) {
        //    op.setChoreography(choreography);
        //}
        
       
        
        for (IChangeOperation op : all_ops) {
            nb++;
            long before = System.currentTimeMillis();
            // write stats to a different variable instantiated here -> don't
            // use static variable ChgOpId2Stats inside ChangePropagationUtil
            //  TODO
            Map<Role, List<IChangeOperation>> partner2propagatedOperationsMap = ChangePropagationUtil.propagate(op, null, null, chgOpId2Stats, this.centrality, this.PropagationGraphMetrics);
            this.ChangePropagationLogMap.put(op, partner2propagatedOperationsMap);
            long after = System.currentTimeMillis();
            long diff = after - before;
            executionTimeMap.put(op.getId(), diff);
            System.out.println("====== " + Thread.currentThread().getName() + " (" + nb + "/" + nb_total + ") " + diff + "ms " + op.getType());
            //System.out.println(op);
            //System.out.println(partner2propagatedOperationsMap);
        }
    }
    
    public void init(Choreography c ){
    	//UNCOMMENT
    	
    	for(IRole r : c.collaboration.roles)
            centrality.put(r, 0);
        for(IRole r1 : c.collaboration.roles)
            for(IRole r2 : c.collaboration.roles){
                if(r1!=r2)
                    PropagationGraphMetrics.put(new Pair<IRole,IRole>(r1,r2) ,0);
            }
        System.out.println("---centrality");
        System.out.println(centrality);
        System.out.println("---Propagation graph");
        System.out.println(PropagationGraphMetrics);
       
    }

}
