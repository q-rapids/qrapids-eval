package eval2;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import type.Factor;
import type.Indicator;

public class ModelChecker {
	public static Logger log = Logger.getLogger("eval2.ModelChecker");
	
	public static void check(Map<String,QueryDef> metricQueries, Map<String, Factor> factorMap, Map<String, type.Indicator> indicatorMap) {
		
		Set<String> allInfluencedFactors = new HashSet<>();
		
		// Factors referenced in <metric>.properties are defined in factor.properties
		for ( QueryDef qd : metricQueries.values() ) {
			
			if ( !qd.isEnabled() ) continue;
			
			String[] influencedFactors = qd.getPropertyAsStringArray("factors");
			for ( String f : influencedFactors ) {
				allInfluencedFactors.add(f);
				if ( !factorMap.containsKey(f)) {
					log.warning( "Factor " + f + " is influenced by Metric " + qd.getName() + " but not defined in factor.properties.\n" );
				} else {
					Factor fact = factorMap.get(f);
					if ( !fact.isEnabled() ) {
						log.warning( "Factor " + f + " is influenced by Metric " + qd.getName() + " but is not enabled in factor.properties.\n" );
					}
				}
			}
		}
		
		// for each factor defined in factor.properties
		// check, that it is influenced by a metric (factor is listed under 'factors' in metric.properties)
		for ( String f : factorMap.keySet() ) {
			
			if ( !factorMap.get(f).isEnabled() ) continue;
			
			if ( !allInfluencedFactors.contains(f) ) {
				log.warning("Factor " + f + " is defined in factor.properties but not influenced by any Metric.\n");
			}
		}
		
		// indicators referenced in factor.properties are defined in indicator.properties
		Set<String> allinfluencedIndicators = new HashSet<>();
		for ( Factor f : factorMap.values() ) {
			if ( !f.isEnabled() ) continue;
			for ( String i : f.getIndicators() ) {
				allinfluencedIndicators.add(i);
				if ( !indicatorMap.containsKey(i) ) {
					log.warning("Indicator " + i + " is influenced by Factor " + f.getFactor() + " but not defined in indicator.properties.\n");
				} else {
					if ( !indicatorMap.get(i).isEnabled() ) {
						log.warning( "Indicator " + i + " is influenced by Factor " + f.getFactor() + " but is not enabled in indicator.properties.\n" );
					}
				}
			}
		}
		
		// for each indicator defined in indicator.properties
		// check, that it is influenced by a factor (indicator is listed under 'indicators' in factor.properties)
		for ( String i : indicatorMap.keySet() ) {
			if ( !indicatorMap.get(i).isEnabled() ) continue;
			
			if ( !allinfluencedIndicators.contains(i) ) {
				log.warning("Indicator " + i + " is defined in indicator.properties but not influenced by any Factor defined in factor.properties.\n");
			}
		}
		
		
	}

}
