package pea.reqCheck;

import java.util.ArrayList;
import java.util.List;

import pea.*;
import pea.modelchecking.*;

/**
 * The class <code>PatternToPEA</code> offers functionality to
 * transform requirements, formalized as instantiated Patterns, via Countertraces (ct) to PEAS. 
 * 
 * 
 * @author Amalinda Oertel
 * April 2010
 * 
 * @see pea.CDD
 */

public class PatternToPEA {
    Trace2PEACompiler compiler = new Trace2PEACompiler();
    CDD entry = EventDecision.create("S1");
    //CDD entry = CDD.FALSE;
    CDD exit = EventDecision.create("S2");
    //CDD exit = CDD.TRUE;
    CDD missing = CDD.TRUE;
    
    int nameindex=0; //this index is needed so that the counters in the peas for the quantitative patterns 
    //do not have the same names


    //%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
    //AbsencePattern
    //komplett und validiert
    //Scope Globally
    public PhaseEventAutomata absencePattern(CDD P, CDD Q, CDD R, String scope) {
    	PhaseEventAutomata ctA;
    	if (scope.contains("Globally")){
		CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
			    new CounterTrace.DCPhase(),
			    new CounterTrace.DCPhase(P),
			    new CounterTrace.DCPhase()
			});
			ctA = compiler.compile("AbsenceGlob", ct); //ctA.dump();
			return ctA;
	}
    	else 
    	if (scope.contains("Before")){
    	CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
    		    new CounterTrace.DCPhase(R.negate()),
    		    new CounterTrace.DCPhase(P.and(R.negate())),
    		    new CounterTrace.DCPhase(R.negate()),
    		    //new CounterTrace.DCPhase(R),
    		    new CounterTrace.DCPhase()
    		 });    	
    		ctA = compiler.compile("TAbsenceBefore", ct); //ctA.dump();
    		return ctA;
    		}
    	else
    	if (scope.contains("until")){
        	CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
           	    new CounterTrace.DCPhase(),
           	    new CounterTrace.DCPhase(Q.and(R.negate())),
           	    new CounterTrace.DCPhase(R.negate()),
           	    new CounterTrace.DCPhase(P.and(R.negate())),
           	    new CounterTrace.DCPhase()
           	});    	
           	ctA = compiler.compile("TAbsenceUntil", ct); //ctA.dump();
           	return ctA;
        	}
    	else	
    	if (scope.contains("After")){
    		CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
    	    	    new CounterTrace.DCPhase(),
    	    	    new CounterTrace.DCPhase(Q),
    	    	    new CounterTrace.DCPhase(),
    	    	    new CounterTrace.DCPhase(P),
    	    	    new CounterTrace.DCPhase()
    	    	});    	
    	    	ctA = compiler.compile("TAbsenceAfter", ct); //ctA.dump();
    	    	return ctA;
    	}
    	else
    		if(scope.contains("Between")){
    		CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
        	  	    new CounterTrace.DCPhase(),
        	   	    new CounterTrace.DCPhase(Q.and(R.negate())),
        	   	    new CounterTrace.DCPhase(R.negate()),
        	   	    new CounterTrace.DCPhase(P.and(R.negate())),
        	   	    new CounterTrace.DCPhase(R.negate()),
        	   	    new CounterTrace.DCPhase(R),
        	  	  new CounterTrace.DCPhase()
        	   	});    	
               	ctA = compiler.compile("TAbsBet", ct); //ctA.dump();
        	   	return ctA;
    		}
    	CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
	    	    new CounterTrace.DCPhase()});
    	ctA = compiler.compile("NoKnownScope", ct);
    	return ctA;
    }
    
    //%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
    //Universality Pattern
    //komplett und validiert
    //Scope Globally
    public PhaseEventAutomata universalityPattern(CDD P, CDD Q, CDD R, String scope) {
    	PhaseEventAutomata ctA;
    	if (scope.contains("Globally")){
		CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
			    new CounterTrace.DCPhase(),
			    new CounterTrace.DCPhase(P.negate()),
			    new CounterTrace.DCPhase()
			});
			ctA = compiler.compile("univG", ct); //ctA.dump();
			return ctA;
	}
    	else 
    	if (scope.contains("Before")){
    	CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
    		    new CounterTrace.DCPhase(R.negate()),
    		    new CounterTrace.DCPhase(P.negate().and(R.negate())),
    		    //new CounterTrace.DCPhase(R.negate()),
    		    //new CounterTrace.DCPhase(R),
    		    new CounterTrace.DCPhase()
    		 });    	
    		ctA = compiler.compile("univB", ct); //ctA.dump();
    		return ctA;
    		}
    	else
    	if (scope.contains("until")){
        	CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
           	    new CounterTrace.DCPhase(),
           	    new CounterTrace.DCPhase(Q.and(R.negate())),
           	    new CounterTrace.DCPhase(R.negate()),
           	    new CounterTrace.DCPhase(P.negate().and(R.negate())),
           	    new CounterTrace.DCPhase()
           	});    	
           	ctA = compiler.compile("univU", ct); //ctA.dump();
           	return ctA;
        	}
    	else	
    	if (scope.contains("After")){
    		CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
    	    	    new CounterTrace.DCPhase(),
    	    	    new CounterTrace.DCPhase(Q),
    	    	    new CounterTrace.DCPhase(),
    	    	    new CounterTrace.DCPhase(P.negate()),
    	    	    new CounterTrace.DCPhase()
    	    	});    	
    	    	ctA = compiler.compile("univA", ct); //ctA.dump();
    	    	return ctA;
    	}
    	else
    		if(scope.contains("Between")){
    			CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
        	    	    new CounterTrace.DCPhase(),
        	    	    new CounterTrace.DCPhase(Q.and(R.negate())),
        	    	    new CounterTrace.DCPhase(R.negate()),
        	    	    new CounterTrace.DCPhase(P.negate().and(R.negate())),
        	    	    new CounterTrace.DCPhase(R.negate()),
        	    	    new CounterTrace.DCPhase(R),
        	    	    new CounterTrace.DCPhase()
        	    	});    	
        	    	ctA = compiler.compile("univBet", ct); //ctA.dump();
        	    	return ctA;
    		
    		}
    	CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
	    	    new CounterTrace.DCPhase()});
    	ctA = compiler.compile("NoKnownScope", ct);
    	return ctA;
    }
    
    //%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
    //Existence Pattern
    //mu� noch f�r 3scopes erweitert werden
    //Scope Globally
    public PhaseEventAutomata existencePattern(CDD P, CDD Q, CDD R, String scope) {
    	PhaseEventAutomata ctA;
    	if (scope.contains("Globally")){
    		System.out.println("Existence-Globally: Hier mu� die Methode noch erweitert werden");
		//CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
			//    new CounterTrace.DCPhase(P.negate()),
			    //new CounterTrace.DCPhase(),
			    //new CounterTrace.DCPhase()
			//});
			//ctA = compiler.compile("ExistenceGlob", ct); ctA.dump();
			//return ctA;
	}
    	else 
    	if (scope.contains("Before")){
    	CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
    		    new CounterTrace.DCPhase(P.negate().and(R.negate())),
    		    new CounterTrace.DCPhase(R),
    		    new CounterTrace.DCPhase()
    		 });    	
    		ctA = compiler.compile("TExistenceBefore", ct); //ctA.dump();
    		return ctA;
    		}
    	else
    	if (scope.contains("until")){
    		System.out.println("Existence-Until: Hier mu� die Methode noch erweitert werden");
        	}
    	else	
    	if (scope.contains("After")){
    		System.out.println("Existence-After: Hier mu� die Methode noch erweitert werden");
    	}
    	else
    		if(scope.contains("Between")){
    			CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
        	    	    new CounterTrace.DCPhase(),
        	    	    new CounterTrace.DCPhase(Q.and(R.negate())),
        	    	    new CounterTrace.DCPhase(P.negate().and(R.negate())),
        	    	    new CounterTrace.DCPhase(R),
        	    	    new CounterTrace.DCPhase()
        	    	});    	
        	    	ctA = compiler.compile("TExistenceBetween", ct); //ctA.dump();
        	    	return ctA;
    		
    		}
    	CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
	    	    new CounterTrace.DCPhase()});
    	ctA = compiler.compile("NoKnownScope", ct);
    	return ctA;
    }
    
  
    
  //%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
    //Bounded Existence Pattern
    //komplett und validiert
    //Scope Globally
    public PhaseEventAutomata bndExistencePattern(CDD P, CDD Q, CDD R, String scope) {
    	PhaseEventAutomata ctA;
    	if (scope.contains("Globally")){
		CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
			new CounterTrace.DCPhase(),
			new CounterTrace.DCPhase(P),
		    new CounterTrace.DCPhase(P.negate()),
		    new CounterTrace.DCPhase(P),
			new CounterTrace.DCPhase(P.negate()),
			new CounterTrace.DCPhase(P),
			new CounterTrace.DCPhase()
			});
			ctA = compiler.compile("BndExistenceGlob", ct); //ctA.dump();
			return ctA;
	}
    	else 
    	if (scope.contains("Before")){
    	CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
    			new CounterTrace.DCPhase(R.negate()),
     		    new CounterTrace.DCPhase(P.and(R.negate())),
     		    new CounterTrace.DCPhase(P.negate().and(R.negate())),
     		    new CounterTrace.DCPhase(P.and(R.negate())),
     		    new CounterTrace.DCPhase(P.negate().and(R.negate())),
     		    new CounterTrace.DCPhase(P.and(R.negate())),
     		    //new CounterTrace.DCPhase(R.negate()),
    		    //new CounterTrace.DCPhase(R),
    		    new CounterTrace.DCPhase()
    		 });    	
    		ctA = compiler.compile("TBndExistenceBefore", ct); //ctA.dump();
    		return ctA;
    		}
    	else
    	if (scope.contains("until")){
    		CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
        			new CounterTrace.DCPhase(),
         		    new CounterTrace.DCPhase(Q.and(R.negate())),
         		    new CounterTrace.DCPhase(R.negate()),
         		    new CounterTrace.DCPhase(P.and(R.negate())),
         		    new CounterTrace.DCPhase(P.negate().and(R.negate())),
         		    new CounterTrace.DCPhase(P.and(R.negate())),
         		    new CounterTrace.DCPhase(P.negate().and(R.negate())),
        		    new CounterTrace.DCPhase(P.and(R.negate())),
         		    new CounterTrace.DCPhase(R.negate()),
        		    new CounterTrace.DCPhase()
        		 });    	
        		ctA = compiler.compile("TBndExistenceUntil", ct); //ctA.dump();
        		return ctA;
        	}
    	else	
    	if (scope.contains("After")){
    		CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
        			new CounterTrace.DCPhase(),
         		    new CounterTrace.DCPhase(Q),
         		    new CounterTrace.DCPhase(),
         		    new CounterTrace.DCPhase(P),
         		    new CounterTrace.DCPhase(P.negate()),
         		    new CounterTrace.DCPhase(P),
         		    new CounterTrace.DCPhase(P.negate()),
        		    new CounterTrace.DCPhase(P),
        		    new CounterTrace.DCPhase()
        		 });   
        		ctA = compiler.compile("TBndExistenceAfter", ct); //ctA.dump();
        		return ctA;
    	}
    	else
    		if(scope.contains("Between")){
    			CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
    					new CounterTrace.DCPhase(),
             		    new CounterTrace.DCPhase(Q.and(R.negate())),
             		    new CounterTrace.DCPhase(R.negate()),
             		    new CounterTrace.DCPhase(P.and(R.negate())),
             		    new CounterTrace.DCPhase(P.negate().and(R.negate())),
             		    new CounterTrace.DCPhase(P.and(R.negate())),
             		    new CounterTrace.DCPhase(P.negate().and(R.negate())),
            		    new CounterTrace.DCPhase(P.and(R.negate())),
             		    new CounterTrace.DCPhase(R.negate()),
            		    new CounterTrace.DCPhase(R),
            		    new CounterTrace.DCPhase()
        	    	});    	
        	    	ctA = compiler.compile("TBndExistenceBetween", ct); //ctA.dump();
        	    	return ctA;
    		
    		}
    	CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
	    	    new CounterTrace.DCPhase()});
    	ctA = compiler.compile("NoKnownScope", ct);
    	return ctA;
    }
    
    //%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
    //Precedence Pattern
    //komplett und validiert
    //Scope Globally
    public PhaseEventAutomata precedencePattern(CDD P, CDD Q, CDD R, CDD S, String scope) {
    	PhaseEventAutomata ctA;
    	if (scope.contains("Globally")){
		CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
			new CounterTrace.DCPhase(S.negate()),
			new CounterTrace.DCPhase(P),
			new CounterTrace.DCPhase()
			});
			ctA = compiler.compile("precedenceGlob", ct); //ctA.dump();
			return ctA;
	}
    	else 
    	if (scope.contains("Before")){
    	CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
    			new CounterTrace.DCPhase(R.negate().and(S.negate())),
     		    new CounterTrace.DCPhase(P.and(R.negate())),
     		    //new CounterTrace.DCPhase(R.negate()),
    		    //new CounterTrace.DCPhase(R),
    		    new CounterTrace.DCPhase()
    		 });    	
    		ctA = compiler.compile("precedenceBefore", ct); //ctA.dump();
    		return ctA;
    		}
    	else
    	if (scope.contains("until")){
    		CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
					new CounterTrace.DCPhase(),
         		    new CounterTrace.DCPhase(Q.and(R.negate()).and(S.negate())),
         		    new CounterTrace.DCPhase(S.negate().and(R.negate())),
         		    new CounterTrace.DCPhase(P.and(R.negate())),
        		    new CounterTrace.DCPhase()
    	    	});    	
        		ctA = compiler.compile("precedenceUntil", ct); //ctA.dump();
        		return ctA;
        	}
    	else	
    	if (scope.contains("After")){
    		CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
        			new CounterTrace.DCPhase(),
         		    new CounterTrace.DCPhase(Q.and(S.negate())),
         		    new CounterTrace.DCPhase(S.negate()),
         		    new CounterTrace.DCPhase(P),
        		    new CounterTrace.DCPhase()
        		 });   
        		ctA = compiler.compile("precedenceAfter", ct); //ctA.dump();
        		return ctA;
    	}
    	else
    		if(scope.contains("Between")){
    			CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
    					new CounterTrace.DCPhase(),
             		    new CounterTrace.DCPhase(Q.and(S.negate()).and(R.negate())),
             		    new CounterTrace.DCPhase(S.negate().and(R.negate())),
             		    new CounterTrace.DCPhase(P.and(R.negate()).and(S.negate())),
             		    new CounterTrace.DCPhase(R.negate()),
             		    new CounterTrace.DCPhase(R),
            		    new CounterTrace.DCPhase()
        	    	});    	
        	    	ctA = compiler.compile("precBet", ct); //ctA.dump();
        	    	return ctA;
    		
    		}
    	CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
	    	    new CounterTrace.DCPhase()});
    	ctA = compiler.compile("NoKnownScope", ct);
    	return ctA;
    }
    
  //%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
    //Precedence Chain Pattern 12
    //Scope Globally
    public PhaseEventAutomata precedenceChainPattern12(CDD P, CDD Q, CDD R, CDD S, CDD T, String scope) {
    	PhaseEventAutomata ctA;
    	if (scope.contains("Globally")){
		CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
			new CounterTrace.DCPhase(P.negate()),
			new CounterTrace.DCPhase(S),
			new CounterTrace.DCPhase(),
			new CounterTrace.DCPhase(T),
			new CounterTrace.DCPhase()
			});
			ctA = compiler.compile("precCh12G", ct); //ctA.dump();
			return ctA;
	}
    	else 
    	if (scope.contains("Before")){
    	CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
    			new CounterTrace.DCPhase(P.negate().and(R.negate())),
    			new CounterTrace.DCPhase(S.and(R.negate()).and(P.negate())),
    			new CounterTrace.DCPhase(R.negate()),
    			new CounterTrace.DCPhase(T.and(R.negate())),
    			//new CounterTrace.DCPhase(R.negate()),
    			//new CounterTrace.DCPhase(R),
    			new CounterTrace.DCPhase()
    		 });    	
    		ctA = compiler.compile("precCh12B", ct); //ctA.dump();
    		return ctA;
    		}
    	else
    	if (scope.contains("until")){
    		CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
    				new CounterTrace.DCPhase(P.negate()),
        			new CounterTrace.DCPhase(Q.and(P.negate()).and(R.negate())),
        			new CounterTrace.DCPhase(P.negate().and(R.negate())),
        			new CounterTrace.DCPhase(S.and(P.negate()).and(R.negate())),
        			new CounterTrace.DCPhase(R.negate()),
        			new CounterTrace.DCPhase(T.and(R.negate())),
        			new CounterTrace.DCPhase()
        		 });    	 	
        		ctA = compiler.compile("precCh12U", ct); //ctA.dump();
        		return ctA;
        	}
    	else	
    	if (scope.contains("After")){
    		CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
    				new CounterTrace.DCPhase(P.negate()),
        			new CounterTrace.DCPhase(Q.and(P.negate())),
        			new CounterTrace.DCPhase(P.negate()),
        			new CounterTrace.DCPhase(S.and(P.negate())),
        			new CounterTrace.DCPhase(),
        			new CounterTrace.DCPhase(T),
        			new CounterTrace.DCPhase()
        		 });    	
        		ctA = compiler.compile("precCh12A", ct); //ctA.dump();
        		return ctA;
    	}
    	else
    		if(scope.contains("Between")){
    			CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
    					new CounterTrace.DCPhase(P.negate()),
            			new CounterTrace.DCPhase(Q.and(P.negate()).and(R.negate())),
            			new CounterTrace.DCPhase(P.negate().and(R.negate())),
            			new CounterTrace.DCPhase(S.and(P.negate()).and(R.negate())),
            			new CounterTrace.DCPhase(R.negate()),
            			new CounterTrace.DCPhase(T.and(R.negate())),
            			new CounterTrace.DCPhase(R.negate()),
            			new CounterTrace.DCPhase(R),
            			new CounterTrace.DCPhase()
        	    	});    	
        	    	ctA = compiler.compile("precCh12Bet", ct); //ctA.dump();
        	    	return ctA;
    		
    		}
    	CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
	    	    new CounterTrace.DCPhase()});
    	ctA = compiler.compile("NoKnownScope", ct);
    	return ctA;
    }
    
  //%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
    //Precedence Chain Pattern 21
    //Scope Globally
    //Klappt noch gar nicht
    public PhaseEventAutomata precedenceChainPattern21(CDD P, CDD Q, CDD R, CDD S, CDD T, String scope) {
    	PhaseEventAutomata ctA, ctA1, ctA2, ctA3;
    	if (scope.contains("Globally")){
		CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
			new CounterTrace.DCPhase(S.negate()),
			new CounterTrace.DCPhase(S.and(T.negate())),
			new CounterTrace.DCPhase(T.negate()),
			new CounterTrace.DCPhase(P),
			new CounterTrace.DCPhase()
			});
		
		CounterTrace ct1 = new CounterTrace(new CounterTrace.DCPhase[] {
				new CounterTrace.DCPhase(T.negate()),
				new CounterTrace.DCPhase(P),
				new CounterTrace.DCPhase()
				});
		
//		CounterTrace ct3 = new CounterTrace(new CounterTrace.DCPhase[] {
//				new CounterTrace.DCPhase(S.and(T.negate())),
//				new CounterTrace.DCPhase(T.negate()),
//				new CounterTrace.DCPhase(P),
//				new CounterTrace.DCPhase()
//				});
		
			ctA1 = compiler.compile("precCh21G", ct1); //ctA1.dump();
			
			ctA = compiler.compile("precCh21G2", ct); //ctA.dump();
			//ctA = (ctA).parallel(ctA1);
			
			
			return ctA;
	}
    	else 
    	if (scope.contains("Before")){
    	CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
    			new CounterTrace.DCPhase()
    		 });    	
    		System.out.println("not yet provided");
    		ctA = compiler.compile("precCh12B", ct); //ctA.dump();
    		return ctA;
    		}
    	else
    	if (scope.contains("until")){
    		CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
//    				new CounterTrace.DCPhase(P.negate()),
//        			new CounterTrace.DCPhase(Q.and(P.negate()).and(R.negate())),
//        			new CounterTrace.DCPhase(P.negate().and(R.negate())),
//        			new CounterTrace.DCPhase(S.and(P.negate()).and(R.negate())),
//        			new CounterTrace.DCPhase(R.negate()),
//        			new CounterTrace.DCPhase(T.and(R.negate())),
        			new CounterTrace.DCPhase()
        		 });    	 	
    			System.out.println("not yet provided");
        		ctA = compiler.compile("precCh12U", ct); //ctA.dump();
        		return ctA;
        	}
    	else	
    	if (scope.contains("After")){
    		CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
//    				new CounterTrace.DCPhase(P.negate()),
//        			new CounterTrace.DCPhase(Q.and(P.negate())),
//        			new CounterTrace.DCPhase(P.negate()),
//        			new CounterTrace.DCPhase(S.and(P.negate())),
//        			new CounterTrace.DCPhase(),
//        			new CounterTrace.DCPhase(T),
        			new CounterTrace.DCPhase()
        		 });    	
    		    System.out.println("not yet provided");
        		ctA = compiler.compile("precCh12A", ct); //ctA.dump();
        		return ctA;
    	}
    	else
    		if(scope.contains("Between")){
    			CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
//    					new CounterTrace.DCPhase(P.negate()),
//            			new CounterTrace.DCPhase(Q.and(P.negate()).and(R.negate())),
//            			new CounterTrace.DCPhase(P.negate().and(R.negate())),
//            			new CounterTrace.DCPhase(S.and(P.negate()).and(R.negate())),
//            			new CounterTrace.DCPhase(R.negate()),
//            			new CounterTrace.DCPhase(T.and(R.negate())),
//            			new CounterTrace.DCPhase(R.negate()),
//            			new CounterTrace.DCPhase(R),
            			new CounterTrace.DCPhase()
        	    	});    	
    			System.out.println("not yet provided");
        	    ctA = compiler.compile("precCh12Bet", ct); //ctA.dump();
        	    return ctA;
    		
    		}
    	CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
	    	    new CounterTrace.DCPhase()});
    	ctA = compiler.compile("NoKnownScope", ct);
    	return ctA;
    }
    
  //%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
    //Response Chain Pattern 21
    //Scope Globally
    //Klappt noch gar nicht
    public PhaseEventAutomata responseChainPattern21(CDD P, CDD Q, CDD R, CDD S, CDD T, String scope) {
    	PhaseEventAutomata ctA, ctA1, ctA2, ctA3;
    	if (scope.contains("Globally")){
		CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
				new CounterTrace.DCPhase()
			});
				
			ctA = compiler.compile("respCh21G", ct); //ctA.dump();
			return ctA;
	}
    	else 
    	if (scope.contains("Before")){
    	CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
    			new CounterTrace.DCPhase(R.negate()),
    			new CounterTrace.DCPhase(S.and(R.negate()).and(T.negate())),
    			new CounterTrace.DCPhase(R.negate()),
    			new CounterTrace.DCPhase(T.and(R.negate())),
    			new CounterTrace.DCPhase(P.negate().and(R.negate())),
    			new CounterTrace.DCPhase(R),
    			new CounterTrace.DCPhase()
    		 });    	
    		ctA = compiler.compile("respCh21B", ct); //ctA.dump();
    		return ctA;
    		}
    	else
    	if (scope.contains("until")){
    		CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
        			new CounterTrace.DCPhase()
        		 });    	 	
        		ctA = compiler.compile("respCh21U", ct); //ctA.dump();
        		return ctA;
        	}
    	else	
    	if (scope.contains("After")){
    		CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
        			new CounterTrace.DCPhase()
        		 });    	
        		ctA = compiler.compile("respCh21A", ct); //ctA.dump();
        		return ctA;
    	}
    	else
    		if(scope.contains("Between")){
    			CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
    					new CounterTrace.DCPhase(),
            			new CounterTrace.DCPhase(Q.and(R.negate())),
            			new CounterTrace.DCPhase(R.negate()),
            			new CounterTrace.DCPhase(S.and(R.negate()).and(T.negate())),
            			new CounterTrace.DCPhase(R.negate()),
            			new CounterTrace.DCPhase(T.and(R.negate())),
            			new CounterTrace.DCPhase(P.negate().and(R.negate())),
            			new CounterTrace.DCPhase(R),
            			new CounterTrace.DCPhase()
        	    	});    	
        	    	ctA = compiler.compile("respCh21Bet", ct); //ctA.dump();
        	    	return ctA;
    		
    		}
    	CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
	    	    new CounterTrace.DCPhase()});
    	ctA = compiler.compile("NoKnownScope", ct);
    	return ctA;
    }
    
    public PhaseEventAutomata responseChainPattern12(CDD P, CDD Q, CDD R, CDD S, CDD T, String requestedLogic){
    	System.out.println("Not yet implemented");
    	PhaseEventAutomata ctA;
    	CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
	    	    new CounterTrace.DCPhase()});
    	ctA = compiler.compile("NoKnownScope", ct);
    	return ctA;
    }
    
  //%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
    //minimum Duration Pattern
    //komplett und validiert
    public PhaseEventAutomata minDurationPattern(CDD P, CDD Q, CDD R, int timebound, String scope) {
    	PhaseEventAutomata ctA,ctA2;
    	if (scope.contains("Globally")){
    	CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
    	    new CounterTrace.DCPhase(),
    	    new CounterTrace.DCPhase(P.negate()),
    	    new CounterTrace.DCPhase(P, CounterTrace.BOUND_LESS, timebound),
    	    new CounterTrace.DCPhase(P.negate()),
    	    new CounterTrace.DCPhase()
    	 });    
   
    	 ctA = compiler.compile("minDurG"+nameindex, ct);     	
    	
    	 nameindex++;
    	 return ctA;
	}
    	else 
    	if (scope.contains("Before")){
    	CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
    			new CounterTrace.DCPhase(R.negate()),
    			new CounterTrace.DCPhase(R.negate().and(P.negate())),
     		    new CounterTrace.DCPhase(P.and(R.negate()), CounterTrace.BOUND_LESS, timebound),
     		    new CounterTrace.DCPhase(R.negate().and(P.negate())),
    		    //new CounterTrace.DCPhase(R.negate()),
    		    //new CounterTrace.DCPhase(R),
    		    new CounterTrace.DCPhase()
    		 });    	
    		ctA = compiler.compile("minDurB"+nameindex, ct); //ctA.dump();
    		nameindex++;
    		return ctA;
    		}
    	else
    	if (scope.contains("until")){
    		CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
					new CounterTrace.DCPhase(),
         		    new CounterTrace.DCPhase(Q.and(R.negate())),
        		    new CounterTrace.DCPhase(R.negate()),
        		    new CounterTrace.DCPhase(P.negate().and(R.negate())),
        		    new CounterTrace.DCPhase(P.and(R.negate()), CounterTrace.BOUND_LESS, timebound),
         		    new CounterTrace.DCPhase(R.negate().and(P.negate())),
         		    new CounterTrace.DCPhase(R.negate()),
        		    new CounterTrace.DCPhase()
    	    	});    	
        		ctA = compiler.compile("minDurU"+nameindex, ct); //ctA.dump();
        		nameindex++;
        		return ctA;
        	}
    	else	
    	if (scope.contains("After")){
    		CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
        			new CounterTrace.DCPhase(),
         		    new CounterTrace.DCPhase(Q),
         		    new CounterTrace.DCPhase(),
         		    new CounterTrace.DCPhase(P.negate()),
         		    new CounterTrace.DCPhase(P, CounterTrace.BOUND_LESS, timebound),
         		    new CounterTrace.DCPhase(P.negate()),
        		    new CounterTrace.DCPhase()
        		 });          		 
        		ctA = compiler.compile("minDurA"+nameindex, ct); 
        		nameindex++;
        		//ctA.dump();
        		return ctA;
    	}
    	else
    		if(scope.contains("Between")){
    			CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
    					new CounterTrace.DCPhase(),
             		    new CounterTrace.DCPhase(Q.and(R.negate())),
            		    new CounterTrace.DCPhase(R.negate()),
            		    new CounterTrace.DCPhase(P.negate().and(R.negate())),
            		    new CounterTrace.DCPhase(P.and(R.negate()), CounterTrace.BOUND_LESS, timebound),
            		    new CounterTrace.DCPhase(R.negate().and(P.negate())),
             		    new CounterTrace.DCPhase(R.negate()),
            		    new CounterTrace.DCPhase(R),
            		    new CounterTrace.DCPhase()
        	    	});    	
        	    	ctA = compiler.compile("minDurBetween"+nameindex, ct); //ctA.dump();
        	    	nameindex++;
        	    	return ctA;
    		
    		}
    	CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
	    	    new CounterTrace.DCPhase()});
    	ctA = compiler.compile("NoKnownScope", ct);
    	return ctA;
    }
    
  //%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
    //maximum Duration Pattern
    //in Entwicklung
    public PhaseEventAutomata maxDurationPattern(CDD P, CDD Q, CDD R, int timebound, String scope) {
    	PhaseEventAutomata ctA, ctA2;
    	//mit der auskommentierten Zeile sind wir n�her an der Semantik von Konrad/Cheng, aber in der Benutzung ist 
    	//diese Version die einfachere
    	if (scope.contains("Globally")){
    	CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
    	    new CounterTrace.DCPhase(),
    	    //new CounterTrace.DCPhase(P.negate()),
    	    new CounterTrace.DCPhase(P, CounterTrace.BOUND_GREATEREQUAL, timebound),
    	    new CounterTrace.DCPhase()
    	    	
    	 });
    	 ctA = compiler.compile("maxDurG"+nameindex, ct); //ctA.dump();
    	 nameindex++;
    	 return ctA;
	}
    	else 
    	if (scope.contains("Before")){
    	CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
    			new CounterTrace.DCPhase(R.negate()),
    			new CounterTrace.DCPhase(R.negate().and(P.negate())),
     		    new CounterTrace.DCPhase(P.and(R.negate()), CounterTrace.BOUND_GREATEREQUAL, timebound),
    		    //new CounterTrace.DCPhase(R.negate()),
    		    //new CounterTrace.DCPhase(R),
    		    new CounterTrace.DCPhase()
    		 });    	
    		ctA = compiler.compile("maxDurB"+nameindex, ct); //ctA.dump();
    		nameindex++;
    		return ctA;
    		}
    	else
    	if (scope.contains("until")){
    		CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
					new CounterTrace.DCPhase(),
         		    new CounterTrace.DCPhase(Q.and(R.negate())),
        		    new CounterTrace.DCPhase(R.negate()),
        		    //new CounterTrace.DCPhase(P.negate().and(R.negate())),
        		    new CounterTrace.DCPhase(P.and(R.negate()), CounterTrace.BOUND_GREATEREQUAL, timebound),
         		    //new CounterTrace.DCPhase(R.negate()),
        		    new CounterTrace.DCPhase()
    	    	});    	
        		ctA = compiler.compile("maxDurU"+nameindex, ct); //ctA.dump();
        		nameindex++;
        		return ctA;
        	}
    	else	
    	if (scope.contains("After")){
    		CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
        			new CounterTrace.DCPhase(),
         		    new CounterTrace.DCPhase(Q),
         		    new CounterTrace.DCPhase(),
         		    new CounterTrace.DCPhase(P.negate()),
         		    new CounterTrace.DCPhase(P, CounterTrace.BOUND_GREATEREQUAL, timebound),
        		    new CounterTrace.DCPhase()
        		 });          		 
        		ctA = compiler.compile("maxDurA"+nameindex, ct); 
        		nameindex++;
        		//ctA.dump();
        		return ctA;
    	}
    	else
    		if(scope.contains("Between")){
    			CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
    					new CounterTrace.DCPhase(),
             		    new CounterTrace.DCPhase(Q.and(R.negate())),
            		    new CounterTrace.DCPhase(R.negate()),
            		    new CounterTrace.DCPhase(P.and(R.negate()), CounterTrace.BOUND_GREATEREQUAL, timebound),
             		    new CounterTrace.DCPhase(R.negate()),
             		   new CounterTrace.DCPhase(R),
            		    new CounterTrace.DCPhase()
        	    	});    	  	
        	    	ctA = compiler.compile("maxDurBet"+nameindex, ct); //ctA.dump();
        	    	nameindex++;
        	    	return ctA;
    		
    		}
    	CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
	    	    new CounterTrace.DCPhase()});
    	ctA = compiler.compile("NoKnownScope", ct);
    	return ctA;
    }
    
  //%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
    //bounded Response Pattern
    //(au�er between) validiert
    public PhaseEventAutomata bndResponsePattern(CDD P, CDD Q, CDD R, CDD S, int timebound, String scope) {
    	PhaseEventAutomata ctA;
    	if (scope.contains("Globally")){
    	CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
    	    new CounterTrace.DCPhase(),
    	    new CounterTrace.DCPhase(P.and(S.negate())),
    	    //new CounterTrace.DCPhase(S.negate(), CounterTrace.BOUND_GREATEREQUAL, timebound),
    	    new CounterTrace.DCPhase(S.negate(), CounterTrace.BOUND_GREATER, timebound),
    	    new CounterTrace.DCPhase()
    	 });
    	 ctA = compiler.compile("bndResG"+nameindex, ct); //ctA.dump();
    	 nameindex++;
    	 return ctA;
	}
    	else 
    	if (scope.contains("Before")){
    	CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
    			new CounterTrace.DCPhase(R.negate()),
    			new CounterTrace.DCPhase(R.negate().and(P).and(S.negate())),
     		    new CounterTrace.DCPhase(S.negate().and(R.negate()), CounterTrace.BOUND_GREATEREQUAL, timebound),
    		    new CounterTrace.DCPhase()
    		 });    	
    		ctA = compiler.compile("bndResB"+nameindex, ct); //ctA.dump();
    		nameindex++;
    		return ctA;
    		}
    	else
    	if (scope.contains("until")){
    		CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
					new CounterTrace.DCPhase(),
         		    new CounterTrace.DCPhase(Q.and(R.negate())),
        		    new CounterTrace.DCPhase(R.negate()),
        		    new CounterTrace.DCPhase(P.and(R.negate()).and(S.negate())),
        		    new CounterTrace.DCPhase(S.negate().and(R.negate()), CounterTrace.BOUND_GREATEREQUAL, timebound),
        		    new CounterTrace.DCPhase()
    	    	});    	
        		ctA = compiler.compile("bndResU"+nameindex, ct); //ctA.dump();
        		nameindex++;
        		return ctA;
        	}
    	else	
    	if (scope.contains("After")){
    		CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
        			new CounterTrace.DCPhase(),
         		    new CounterTrace.DCPhase(Q),
         		    new CounterTrace.DCPhase(),
         		    new CounterTrace.DCPhase(P.and(S.negate())),
         		    new CounterTrace.DCPhase(S.negate(), CounterTrace.BOUND_GREATEREQUAL, timebound),
        		    new CounterTrace.DCPhase()
        		 });          		 
        		ctA = compiler.compile("bndResA"+nameindex, ct); 
        		nameindex++;
        		//ctA.dump();
        		return ctA;
    	}
    	else
    		if(scope.contains("Between")){
    			CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
    					new CounterTrace.DCPhase(),
             		    new CounterTrace.DCPhase(Q.and(R.negate())),
            		    new CounterTrace.DCPhase(R.negate()),
            		    new CounterTrace.DCPhase(P.and(R.negate()).and(S.negate())),
            		    new CounterTrace.DCPhase(S.negate().and(R.negate()), CounterTrace.BOUND_GREATEREQUAL, timebound),
            		    new CounterTrace.DCPhase(R.negate()),
            		    new CounterTrace.DCPhase(R),
            		    new CounterTrace.DCPhase()
        	    	});    	
        	    	ctA = compiler.compile("bndResBet"+nameindex, ct); //ctA.dump();
        	    	nameindex++;
        	    	return ctA;
    		
    		}
    	CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
	    	    new CounterTrace.DCPhase()});
    	ctA = compiler.compile("NoKnownScope", ct);
    	return ctA;
    }
    
  //%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
    //periodic Pattern
    //komplett und validiert
    public PhaseEventAutomata periodicPattern(CDD P, CDD Q, CDD R, int timebound, String scope) {
    	PhaseEventAutomata ctA;
    	if (scope.contains("Globally")){
    		CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
    	    	    new CounterTrace.DCPhase(),
    	    	    new CounterTrace.DCPhase(P.negate(), CounterTrace.BOUND_GREATER, 10),
    	    	    new CounterTrace.DCPhase()
    		});
    	 ctA = compiler.compile("periG"+nameindex, ct); //ctA.dump();
    	 nameindex++;
    	 return ctA;
	}
    	else 
    	if (scope.contains("Before")){
    	CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
    			new CounterTrace.DCPhase(R.negate()),
     		    new CounterTrace.DCPhase(P.negate().and(R.negate()), CounterTrace.BOUND_GREATER, timebound),
    		    //new CounterTrace.DCPhase(R.negate()),
    		    //new CounterTrace.DCPhase(R),
    		    new CounterTrace.DCPhase()
    		 });    	
    		ctA = compiler.compile("periB"+nameindex, ct); //ctA.dump();
    		nameindex++;
    		return ctA;
    		}
    	else
    	if (scope.contains("until")){
    		CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
					new CounterTrace.DCPhase(),
         		    new CounterTrace.DCPhase(Q.and(R.negate())),
        		    new CounterTrace.DCPhase(R.negate()),
        		    new CounterTrace.DCPhase(P.negate().and(R.negate()), CounterTrace.BOUND_GREATER, timebound),
        		    new CounterTrace.DCPhase()
    	    	});    	
        		ctA = compiler.compile("periU"+nameindex, ct); //ctA.dump();
        		nameindex++;
        		return ctA;
        	}
    	else	
    	if (scope.contains("After")){
    		CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
        			new CounterTrace.DCPhase(),
         		    new CounterTrace.DCPhase(Q),
         		    new CounterTrace.DCPhase(),
         		    new CounterTrace.DCPhase(P.negate(), CounterTrace.BOUND_GREATER, timebound),
        		    new CounterTrace.DCPhase()
        		 });          		 
        		ctA = compiler.compile("periA"+nameindex, ct); 
        		nameindex++;
        		//ctA.dump();
        		return ctA;
    	}
    	else
    		if(scope.contains("Between")){
    			CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
    					new CounterTrace.DCPhase(),
             		    new CounterTrace.DCPhase(Q.and(R.negate())),
    					new CounterTrace.DCPhase(R.negate()),
    	     		    new CounterTrace.DCPhase(P.negate().and(R.negate()), CounterTrace.BOUND_GREATER, timebound),
    	    		    new CounterTrace.DCPhase(R.negate()),
    	    		    new CounterTrace.DCPhase(R),
    	    		    new CounterTrace.DCPhase()
        	    	});    	
        	    	ctA = compiler.compile("periBet"+nameindex, ct); //ctA.dump();
        	    	nameindex++;
        	    	return ctA;
    		
    		}
    	CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
	    	    new CounterTrace.DCPhase()});
    	ctA = compiler.compile("NoKnownScope", ct);
    	return ctA;
    }
    
  //%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
    //response Pattern
    //NICHT komplett und validiert
    public PhaseEventAutomata responsePattern(CDD P, CDD Q, CDD R, CDD S, String scope) {
    	PhaseEventAutomata ctA;
    	if (scope.contains("Globally")){
    		//hier brauchen wir einen anderen Mechanismus denn S.negate m��te bis zum ende des intervalls gelten
    		CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
     	    	   new CounterTrace.DCPhase()
     	    	});
    		System.out.println("Not yet provided");
    	 ctA = compiler.compile("respG", ct); //ctA.dump();
    	 return ctA;    	    	
	}
    	else 
    	if (scope.contains("Before")){
    	CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
    			new CounterTrace.DCPhase(R.negate()),
     		    new CounterTrace.DCPhase(P.and(R.negate()).and(S.negate())),
    		    new CounterTrace.DCPhase(S.negate().and(R.negate())),
    		    new CounterTrace.DCPhase(R),
    		    new CounterTrace.DCPhase()
    		 });    	
    		ctA = compiler.compile("respB", ct); //ctA.dump();
    		return ctA;
    		}
    	else
    	if (scope.contains("until")){
    		//hier brauchen wir einen anderen Mechanismus denn S.negate m��te bis zum ende des intervalls gelten
    		CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
					new CounterTrace.DCPhase(),
    	    	});    	
    		System.out.println("Not yet provided");
        		ctA = compiler.compile("respU", ct); //ctA.dump();
        		return ctA;
        	}
    	else	
    	if (scope.contains("After")){
    		//hier brauchen wir einen anderen Mechanismus denn S.negate m��te bis zum ende des intervalls gelten
    		CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
        			new CounterTrace.DCPhase()
        		 });          		 
    		System.out.println("Not yet provided");
        		ctA = compiler.compile("respA", ct); 
        		//ctA.dump();
        		return ctA;
    	}
    	else
    		if(scope.contains("Between")){
    			CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
    					new CounterTrace.DCPhase(),
             		    new CounterTrace.DCPhase(Q.and(R.negate())),
    					new CounterTrace.DCPhase(R.negate()),
    	     		    new CounterTrace.DCPhase(P.and(R.negate()).and(S.negate())),
    	    		    new CounterTrace.DCPhase(R.negate().and(S.negate())),
    	    		    new CounterTrace.DCPhase(R),
    	    		    new CounterTrace.DCPhase()
        	    	});    	
        	    	ctA = compiler.compile("respBet", ct); //ctA.dump();
        	    	return ctA;
    		
    		}
    	CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
	    	    new CounterTrace.DCPhase()});
    	ctA = compiler.compile("NoKnownScope", ct);
    	return ctA;
    }
    
  //%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
    //bounded Invariance Pattern
    //(au�er between) validiert
    public PhaseEventAutomata bndInvariancePattern(CDD P, CDD Q, CDD R, CDD S, int timebound, String scope) {
    	PhaseEventAutomata ctA, ctA2;
    	if (scope.contains("Globally")){
    		CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
    	    	    new CounterTrace.DCPhase(),
    	    	    new CounterTrace.DCPhase(P),
    	    	    new CounterTrace.DCPhase(CDD.TRUE, CounterTrace.BOUND_LESS, timebound),
    	    	    new CounterTrace.DCPhase(S.negate()),
    	    	    new CounterTrace.DCPhase()
    	    	});
    	    	ctA = compiler.compile("inv1"+nameindex, ct); //ctA.dump();
    	    	nameindex++;
    	    
    	    	return ctA;
    	    	//return mctA;
	}
    	else 
    	if (scope.contains("Before")){
    		CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
    				new CounterTrace.DCPhase(R.negate()),
    	    	    new CounterTrace.DCPhase(P.and(R.negate())),
    	    	    new CounterTrace.DCPhase(R.negate(), CounterTrace.BOUND_LESS, timebound),
    	    	    new CounterTrace.DCPhase(S.negate().and(R.negate())),
    	    	    new CounterTrace.DCPhase()
    	    	});
    	    	ctA = compiler.compile("inv"+nameindex, ct); //ctA.dump();
    	    	nameindex++;
    	    	return ctA;
    		}
    	else
    	if (scope.contains("until")){
    		CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
    		           	    
            	    new CounterTrace.DCPhase(),
            	    new CounterTrace.DCPhase(Q.and(R.negate())),
            	    new CounterTrace.DCPhase(R.negate()),
            	    new CounterTrace.DCPhase(P.and(R.negate())),
            	    new CounterTrace.DCPhase(R.negate(), CounterTrace.BOUND_LESS, timebound),
            	    new CounterTrace.DCPhase(S.negate().and(R.negate())),
            	    new CounterTrace.DCPhase()
            	});
            	ctA = compiler.compile("inv"+nameindex, ct); //ctA.dump();
            	nameindex++;
            	
            	return ctA;
        	}
    	else	
    	if (scope.contains("After")){
        	CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
        	    new CounterTrace.DCPhase(),
        	    new CounterTrace.DCPhase(Q),
        	    new CounterTrace.DCPhase(),
	    	    new CounterTrace.DCPhase(P),
	    	    new CounterTrace.DCPhase(CDD.TRUE, CounterTrace.BOUND_LESS, timebound),
	    	    new CounterTrace.DCPhase(S.negate()),
        	    new CounterTrace.DCPhase()
        	});
        	ctA = compiler.compile("inv"+nameindex, ct); //ctA.dump();
        	nameindex++;
        	return ctA;
    	}
    	else
    		if(scope.contains("Between")){
    			CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
    					 new CounterTrace.DCPhase(),
    	            	    new CounterTrace.DCPhase(Q.and(R.negate())),
    	            	    new CounterTrace.DCPhase(R.negate()),
    	            	    new CounterTrace.DCPhase(P.and(R.negate())),
    	            	    new CounterTrace.DCPhase(R.negate(), CounterTrace.BOUND_LESS, timebound),
    	            	    new CounterTrace.DCPhase(S.negate().and(R.negate())),
    	            	    new CounterTrace.DCPhase(R.negate()),
    	            	    new CounterTrace.DCPhase(R),
    	            	    new CounterTrace.DCPhase()
                	});
                	ctA = compiler.compile("inv"+nameindex, ct); //ctA.dump();              
                   	nameindex++;
        	    	return ctA;
    		
    		}
    	CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
	    	    new CounterTrace.DCPhase()});
    	ctA = compiler.compile("NoKnownScope", ct);
    	return ctA;
    }
    
  //%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
    //invariant Pattern
    //validiert
    public PhaseEventAutomata invariantPattern(CDD P, CDD Q, CDD R, CDD S, String scope) {
    	PhaseEventAutomata ctA;
    	 ctA = absencePattern(P.and(S.negate()),Q,R, scope);
    	 return ctA;
	
    }
    
//    //Scope Before R
//    public PhaseEventAutomata absencePattern_Before(CDD P, CDD R, String scope) {
//    	PhaseEventAutomata ctA;
//    	CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
//    	    new CounterTrace.DCPhase(R.negate()),
//    	    new CounterTrace.DCPhase(P.and(R.negate())),
//    	    new CounterTrace.DCPhase()
//    	});    	
//    	ctA = compiler.compile("TAbsenceBefore", ct); ctA.dump();
//    	return ctA;
//        }
       


    
    public PhaseEventAutomata bndResp_Glob(CDD P, CDD S, int bound) {
    	PhaseEventAutomata ctA, mctA;
    	CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
    	    new CounterTrace.DCPhase(),
    	    new CounterTrace.DCPhase(P.and(S.negate())),
    	    new CounterTrace.DCPhase(S.negate(), CounterTrace.BOUND_GREATER, bound),
    	    new CounterTrace.DCPhase()
    	});
    	//MCTrace mct = new MCTrace(ct, entry, exit, missing, true);
    	//mctA = compiler.compile("TBndResp", mct); //mctA.dump();
    	ctA = compiler.compile("TBndResp"+nameindex, ct); //ctA.dump();
    	nameindex++;
    	return ctA;
    	//return mctA;
        }
    
     
    public PhaseEventAutomata testPrecedenceVac(CDD P, CDD S, CDD R) {
    	PhaseEventAutomata ctA;
    	CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
    	    new CounterTrace.DCPhase(S.negate().and(R.negate())),
    	    new CounterTrace.DCPhase((P.and(R.negate())).or(S.negate().and(R.negate()))),
    	    new CounterTrace.DCPhase()
    	});
    	ctA = compiler.compile("Test", ct); //ctA.dump();
    	return ctA;
        }

    public void runTest3() {
	CDD A = BooleanDecision.create("A");
	CDD B = BooleanDecision.create("B");
	CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
	    new CounterTrace.DCPhase(A, CounterTrace.BOUND_LESS, 1),
	    new CounterTrace.DCPhase(B, CounterTrace.BOUND_LESSEQUAL, 2)
	});
	MCTrace mct = new MCTrace(ct, entry, exit, missing, true);
	compiler.compile("T3", mct).dump();
	mct = new MCTrace(ct, null, exit, missing, true);
	compiler.compile("T4", mct).dump();
    }
    
    public PhaseEventAutomata deadlockCounterexample(CDD P, CDD S, int bound, int bound2) {
    	PhaseEventAutomata ctA, ctA2;
    	CounterTrace ct = new CounterTrace(new CounterTrace.DCPhase[] {
    		new CounterTrace.DCPhase(),
    	   // new CounterTrace.DCPhase(P.negate()),
    	    new CounterTrace.DCPhase(P, CounterTrace.BOUND_GREATEREQUAL, bound),
    	    //new CounterTrace.DCPhase(P.negate()),
    	    new CounterTrace.DCPhase()
    	});
    	
    	CounterTrace ct2 = new CounterTrace(new CounterTrace.DCPhase[] {
    			new CounterTrace.DCPhase(),
    		    new CounterTrace.DCPhase(P),
        	    new CounterTrace.DCPhase(P.negate(), CounterTrace.BOUND_LESSEQUAL, bound2),
        	    new CounterTrace.DCPhase(P),
        	    new CounterTrace.DCPhase()
        	});
    	
//    	CounterTrace ct2 = new CounterTrace(new CounterTrace.DCPhase[] {
//        	    new CounterTrace.DCPhase(),
//        	    new CounterTrace.DCPhase(P, CDD.TRUE, CounterTrace.BOUND_GREATER, bound),
//        	    new CounterTrace.DCPhase(P),
//        	    new CounterTrace.DCPhase(P.negate(), CounterTrace.BOUND_LESS, bound2),
//        	    new CounterTrace.DCPhase(P),
//        	    new CounterTrace.DCPhase()
//        	});
    	ctA = compiler.compile("TCounterEx", ct); ctA.dump();
    	ctA2 = compiler.compile("TCounterEx2", ct2); ctA2.dump();
    	ctA = ctA.parallel(ctA2);
    	ctA.dump();
    	return ctA;
        }
    
    
    

   

    public void run() {
    PhaseEventAutomata ctParallel, ct1A, ct2A, ct3A;
    CDD P = BooleanDecision.create("P");
	CDD S = BooleanDecision.create("S");
	CDD T = BooleanDecision.create("T");
	CDD R = BooleanDecision.create("R");
	CDD Q = BooleanDecision.create("Q");		
    
	//Zweimal sich wiedersprechende BoundedInvariance Anforderungen; Der resultierende Automat ist nur f�r den Fall 
	// G(not(P)) erf�llbar;
    //ct1A = bndInvariance(P, S,10);
    //ct2A = bndInvariance(P, S.negate(),10);
    //ctParallel = ct1A.parallel(ct2A);
    //ctParallel.dump();
    
	//Zwei sich widersprechende Anforderungen 
	//P--> neg(S) gilt f�r mindestens 11 time units
	//P--> S gilt in h�chstens 10 time units
	ct1A = bndInvariancePattern(P,Q,R,S.negate(),6, "Globally");   
    ct2A = bndResponsePattern(P,Q,R,S,10, "Globally");
    //ct3A = universalityPattern(P,Q,R,"Globally");
    ct3A = deadlockCounterexample(P,S,3,10);
    ctParallel = ct1A.parallel(ct2A);
    //ctParallel.dump();
    //ctParallel.dumpDot();

    ct1A = absencePattern(P,Q,R, "Before");
    ct1A.dump();
   

    
    //ct1A = responseChainPattern21(P,Q,R,S,T,"Between");
    //ct1A = precedenceChainPattern21(P,Q,R,S,T,"Globally");
    //ct1A.dump();
    
    DOTWriter dotwriter = new DOTWriter("C:/Test.dot", ct1A);//ctParallel
    dotwriter.write();
    ct1A = this.testPrecedenceVac(P, S, R);
    dotwriter.write("C:/vacuous/TestPrecVac.dot", ct1A);
    
    ct1A = precedencePattern(P,Q,R,S, "Between");
    ct1A.dump();
    
    DOTWriter dotwriter2 = new DOTWriter("C:/minDur.dot", ct1A);
    dotwriter2.write();
    
      
    J2UPPAALWriterV4 j2uppaalWriter = new J2UPPAALWriterV4();
    j2uppaalWriter.writePEA2UppaalFile("C:/UppaalWriterV4_neu.xml", ctParallel);   
    
    J2UPPAALConverter j2uppaalWr = new J2UPPAALConverter();
    j2uppaalWr.writePEA2UppaalFile("C:/UppaalConverter_neu.xml", ctParallel);   
    
    J2UPPAALConverterDOM j2uppaalDom = new J2UPPAALConverterDOM();
    j2uppaalDom.create("C:/UppaalConverterDOM.xml", ctParallel);
    
    
    try {
        PEAJ2XMLConverter conv = new PEAJ2XMLConverter();

        ArrayList<PhaseEventAutomata> peaList = new ArrayList<PhaseEventAutomata>();
        peaList.add(ctParallel);
        PEANet peanet = new PEANet();
        peanet.setPeas(peaList);
        conv.convert(peanet, "AmiTest2.xml");
    } catch (Exception e) {
        e.printStackTrace();
    }
    
    
//    CDD BatteryLow = BooleanDecision.create("BatteryLow");
//    CDD TankLow = BooleanDecision.create("TankLow");
//    CDD GeneratorOn = BooleanDecision.create("GeneratorOn");
//    ct1A = bndResp_Glob(BatteryLow, GeneratorOn, 1);
//    ct2A = bndResp_Glob(TankLow, GeneratorOn.negate(), 2);
//    ctParallel = ct1A.parallel(ct2A);
//    ctParallel.dump();
    
	//ct1A = minDuration_Glob();
	//ct2A = maxDuration_Glob();
	//ctParallel = ct1A.parallel(ct2A);
	//ctParallel.dump();
	
	//ct1A = periodic_Glob();
	//runTest2();
	//runTest3();
    }
    
    


    public static void main(String[] param) {
	org.apache.log4j.PropertyConfigurator.configure
	    (ClassLoader.getSystemResource("pea/test/TestLog.config"));
	new PatternToPEA().run();
    }
}


