package eval2;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import type.Factor;
import type.IndexItem;
import type.Indicator;
import type.Metric;
import type.Relation;

import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

import util.Evaluator;
import util.FileUtils;

public class EvalProject {
	
	private Logger log = Logger.getLogger(this.getClass().getName());
	
	private String evaluationDate;

	// project folder containing queries, properties etc.
	private File projectFolder;
	
	// contents of projectFolder/propect.properties
	private Properties projectProperties;
	
	// Elasticsearch source
	private Elasticsearch elasticSource;
	
	// Elasticsearch target
	private Elasticsearch elasticTarget;

	// param query set of this project
	private Map<String,QueryDef> paramQuerySet;
	
	// metric query set of this project
	private Map<String,QueryDef> metricQuerySet;
	
	public String projectErrorStrategy;

	
	public EvalProject(File projectFolder, String evaluationDate ) {
		
		this.projectFolder = projectFolder;
		
		String projectPropertyFilename = projectFolder.getAbsolutePath() + File.separatorChar + "project.properties";
		this.projectProperties = FileUtils.loadProperties( new File(projectPropertyFilename) );
		
		projectErrorStrategy = projectProperties.getProperty("onError", IndexItem.ON_ERROR_DROP);

		this.evaluationDate = evaluationDate;
		
	}
	
	public void validateModel() {
		
		File metricQueryFolder = new File( projectFolder.getAbsolutePath() + File.separatorChar + "metrics" );
		metricQuerySet = getQuerySet( metricQueryFolder ); 
		
		ModelChecker.check( metricQuerySet, readFactorMap(), readIndicatorMap() );
		
	}
	
	public void run() {
		
		validateModel();
		
		File metricQueryFolder = new File( projectFolder.getAbsolutePath() + File.separatorChar + "metrics" );
		metricQuerySet = getQuerySet( metricQueryFolder ); 

		log.info("Connecting to Elasticsearch Source (" + projectProperties.getProperty("elasticsearch.source.ip") + ")\n");
		elasticSource = new Elasticsearch( projectProperties.getProperty("elasticsearch.source.ip") );
		
		log.info("Connecting to Elasticsearch Target (" + projectProperties.getProperty("elasticsearch.target.ip") + ")\n");
		elasticTarget = new Elasticsearch( projectProperties.getProperty("elasticsearch.target.ip") );
		
		File paramQueryFolder = new File( projectFolder.getAbsolutePath() + File.separatorChar + "params" );
		paramQuerySet = getQuerySet( paramQueryFolder ); 
		
		log.info("Executing param queries (" + paramQuerySet.size() + " found)\n");
		Map<String,Object> queryParameter = executeParamQueryset( paramQuerySet, evaluationDate );
		log.info("Param query result: " + queryParameter + "\n"); 

		log.info("Executing metric queries (" + metricQuerySet.size() + " found)\n");
		List<Metric> metrics = executeMetricQueries(queryParameter, metricQuerySet);
		
		log.info("Storing metrics (" + metrics.size() + " computed)\n");
		elasticTarget.storeMetrics( projectProperties, evaluationDate, metrics );

		/*
		List<Relation> metricrelations = computeMetricRelations(metrics);
		log.info("Storing metric relations (" + metricrelations.size() + " computed)\n");
		elasticTarget.storeRelations( projectProperties, evaluationDate, metricrelations );
		
		log.info("Computing Factors ...\n"); 
		Collection<Factor> factors = computeFactors();
		
		log.info("Storing factors (" + factors.size() + " computed)\n");
		elasticTarget.storeFactors( projectProperties, evaluationDate, factors );
		
		log.info("Storing factor relations ... \n");
		List<Relation> factorrelations = computeFactorRelations(factors);
		elasticTarget.storeRelations( projectProperties, evaluationDate, factorrelations );

		// try { Thread.sleep(1000); } catch (InterruptedException e) { e.printStackTrace(); }
		
		log.info("Computing Indicators  ...\n");
		Collection<Indicator> indicators = computeIndicators();
		
		elasticTarget.storeIndicators( projectProperties, evaluationDate, indicators );
		*/
	}
	

	/**
	 * Computes Factor values based on relation items
	 * @return List of computed Factors
	 */
	private Collection<Factor> computeFactors() {
		
		List<Factor> result = new ArrayList<>();
		
		String factorQueryDir = projectFolder.getAbsolutePath() + File.separatorChar + "factors";
		QueryDef factorQuery = loadQueryDef(factorQueryDir, "factor");
		factorQuery.setIndex( factorQuery.getProperty("index") + "." + projectProperties.getProperty("project.name"));
		
		Map<String,Factor> factorMap = readFactorMap();
		
		for ( Entry<String,Factor> e : factorMap.entrySet() ) {
			
			Factor fact = e.getValue();
			
			if ( !fact.isEnabled() ) {
				log.info("Factor " + fact.getFactor() + " is disabled.\n");
				continue;
			} else {
				log.info("Computing factor  " + fact.getFactor() + ".\n") ; 
			}

			Map<String,Object> parameters = new HashMap<>();
			parameters.put( "evaluationDate", evaluationDate);
			parameters.put( "project", projectProperties.getProperty("project.name") );
			parameters.put( "targetType", e.getValue().getType() );
			parameters.put( "targetId", e.getValue().getElasticId() ); 
			
			Map<String,Object> results = elasticTarget.execute(parameters, factorQuery);
			String metricDef = factorQuery.getProperty("metric");


			Double factorValue=null;
			try {
				factorValue = evaluate( metricDef, results );
			} catch( RuntimeException rte ) {
				
				log.warning("Evaluation of formula " + metricDef + " failed. \nFactor: " + fact.getName() + "\n");

				if ( fact.onErrorSet0() ) {
					log.warning("Factor " + fact.getFactor() + " set to 0.\n");
					factorValue = 0.0;
				} else {
					log.warning("Factor " + fact.getFactor() + " is dropped.\n");
					continue;
				}

			}
				
			// factorValue not numeric?
			if ( factorValue.isNaN() || factorValue.isInfinite() ) {
				
				log.warning("Evaluation of Factor " + fact.getFactor() + " resulted in non-numeric value.\n" );
				
				if ( fact.onErrorSet0() ) {
					log.warning("Factor " + fact.getFactor() + " set to 0.\n");
					factorValue = 0.0;
				} else {
					log.warning("Factor " + fact.getFactor() + " is dropped.\n");
					continue;
				}
			} else {
				log.info("Value of factor " + fact.getFactor() + " = " + factorValue + "\n");
			}
			
			fact.setValue(factorValue);
			fact.setEvaluationDate(evaluationDate);
			
			String info;
			info = "parameters: " + parameters.toString() + "\n";
			info += "query-properties: " + factorQuery.getQueryParameter().toString() + "\n";
			info += "executionResults: " + results.toString() + "\n";
			info += "formula: " + metricDef + "\n";
			info += "value: " + factorValue;

			fact.setInfo(info);
			
			result.add(fact);
		}
		
		return result;
		
		
	}
	
	/**
	 * Compute Relations between Factors and Indicators
	 * @param factors 
	 * @return List of Relation
	 */
	private List<Relation> computeFactorRelations( Collection<Factor> factors ) {
		
		List<Relation> result = new ArrayList<>();
		
		Map<String,Indicator> indicatorMap = readIndicatorMap();
		
		for ( Factor factor : factors ) {
			for ( int i = 0; i < factor.getIndicators().length; i++ ) {
				
				String indicatorid = factor.getIndicators()[i];
				Double weight = factor.getWeights()[i];

				Indicator indicator = indicatorMap.get(indicatorid);
				
				if ( indicator == null ) {
					log.info( "Warning: Impact of Factor " + factor.getName() + " on undefined Indicator " + indicatorid + "is not stored."  );
				} else {
					if ( !indicator.isEnabled() ) {
						log.info("Indicator " + indicator.getName() + " is disabled. No relation created.\n");
						continue;
					}
					
					Relation imp = new Relation(factor.getProject(), factor, indicator, evaluationDate, factor.getValue() * weight, weight);
					result.add(imp);
				}
				
			}
		}
		
		
		return result;
		
	}
	
	/**
	 * Compute Indicator values based on Factor-indicator relations
	 * @return List of Indicator
	 */
	private Collection<Indicator> computeIndicators() {
		
		List<Indicator> result = new ArrayList<>();
		
		String indicatorQueryDir = projectFolder.getAbsolutePath() + File.separatorChar + "indicators";
		QueryDef indicatorQuery = loadQueryDef(indicatorQueryDir, "indicator");
		indicatorQuery.setIndex(indicatorQuery.getProperty("index") + "." + projectProperties.getProperty("project.name"));
		 
		Map<String,Indicator> indicatorMap = readIndicatorMap();
		
		for ( Entry<String,Indicator> e : indicatorMap.entrySet() ) {
			
			Indicator ind = e.getValue();
			
			if ( !ind.isEnabled() ) {
				log.info("Indicator " + ind.getIndicator() + " is disabled.\n");
				continue;
			} else {
				log.info("Computing indicator " + ind.getIndicator() + ".\n") ; 
			}
			
			Map<String,Object> parameters = new HashMap<>();
			parameters.put( "evaluationDate", evaluationDate);
			parameters.put( "project", projectProperties.getProperty("project.name") );
			parameters.put( "targetType", e.getValue().getType() );
			parameters.put( "targetId", e.getValue().getElasticId() ); 
			
			Map<String,Object> results = elasticTarget.execute(parameters, indicatorQuery);
			String metricDef = indicatorQuery.getProperty( "metric" );

			Double indicatorValue;
			try {
				indicatorValue = evaluate( metricDef, results );
			} catch (RuntimeException rte) {

				log.warning("Evaluation of formula " + metricDef + " failed.\nIndicator: " + ind.getName());

				if ( ind.onErrorSet0() ) {
					log.warning("Indicator " + ind.getName() + " set to 0.\n");
					indicatorValue = 0.0;
				} else {
					log.warning("Indicator " + ind.getName() + " is dropped.\n");
					continue;
				}
				
			}

			if ( indicatorValue.isNaN() || indicatorValue.isInfinite() ) {
				if ( ind.onErrorSet0() ) {
					log.warning("Indicator " + ind.getName() + " set to 0.\n");
					indicatorValue = 0.0;
				} else {
					log.warning("Indicator " + ind.getName() + " is dropped.\n");
					continue;
				}
			} else {
				log.info("Value of indicator " + ind.getIndicator() + " = " + indicatorValue + "\n");
			}
			
			ind.setValue(indicatorValue);
			ind.setEvaluationDate(evaluationDate);
			
			String info;
			info = "parameters: " + parameters.toString() + "\n";
			info += "query-properties: " + indicatorQuery.getQueryParameter().toString() + "\n";
			info += "executionResults: " + results.toString() + "\n";
			info += "formula: " + metricDef + "\n";
			info += "value: " + indicatorValue;

			ind.setInfo(info);
			
			result.add(ind);
		}
		
		return result;
		
	}

	/**
	 * Execute a Set of Queries
	 * @param querySets Map of QueryDef
	 * @return Map of execution results Name->Value
	 */
	private Map<String, Object> executeParamQueryset( Map<String, QueryDef> querySets, String evaluationDate ) {
		
		Map<String,Object> allExecutionResults = new HashMap<>();
		allExecutionResults.put("evaluationDate", evaluationDate);

		for ( String key : querySets.keySet() ) {

			Map<String,Object> executionResult = elasticSource.execute( allExecutionResults, querySets.get(key) );
			allExecutionResults.putAll(executionResult);

		}
		
		return allExecutionResults;
		
	}
	
	/**
	 * Execute Metric queries
	 * @param parameters Parameter Map
	 * @param metricQuerySet Query Map
	 * @return List of Metric
	 */
	private List<Metric> executeMetricQueries( Map<String,Object> parameters, Map<String, QueryDef> metricQuerySet) {
		
		List<Metric> result = new ArrayList<>();
		

		for ( String key : metricQuerySet.keySet() ) {
			
			QueryDef metricQueryDef = metricQuerySet.get(key);
			
			if ( !metricQueryDef.isEnabled() ) {
				log.info("Metric " + metricQueryDef.getName() + " is disabled.\n");
				continue;
			}
			
			String info;
			info = "parameters: " + parameters.toString() + "\n";
			
			info += "query-properties: " + metricQueryDef.getQueryParameter().toString() + "\n";
			
			log.info("Executing metric query: " + key + "\n");
			Map<String,Object> executionResult = elasticSource.execute( parameters, metricQueryDef );
			log.info("result: " + executionResult + "\n");
			
			info += "executionResults: " + executionResult.toString() + "\n";
			
			String metricDef = metricQueryDef.getProperty("metric");
			
			info += "formula: " + metricDef + "\n";
			
			Map<String,Object> evalParameters = new HashMap<>();
			evalParameters.putAll(parameters);
			evalParameters.putAll(executionResult);
			
			Double metricValue=null;
			try {
				
				metricValue = evaluate( metricDef, evalParameters );
				info += "value: " + metricValue;
				
			} catch (RuntimeException rte) {
				
				log.warning("Evaluation of formula " + metricDef + " failed. \nMetric: " + key);
				if ( metricQueryDef.onErrorDrop() ) {
					log.warning("Metric " + key + " is dropped.");
					continue;
				} else {
					metricValue = metricQueryDef.getErrorValue();
					log.warning("Metric " + key + " set to " + metricValue + ".");
				}
				
			}
			
			log.info("Metric " + metricQueryDef.getName() +" = " + metricValue + "\n");
			
			if( metricValue.isInfinite() || metricValue.isNaN() ) {
				log.warning("Formula evaluated as NaN or inifinite.");
				if ( metricQueryDef.onErrorDrop() ) {
					log.warning("Metric " + key + " is dropped.");
					continue;
				} else {
					metricValue = metricQueryDef.getErrorValue();
					log.warning("Metric " + key + " set to " + metricValue + ".");
				}
			}
			
			String project = projectProperties.getProperty("project.name");
			String metric = metricQueryDef.getName();
			String name = metricQueryDef.getProperty("name");
			String description = metricQueryDef.getProperty("description");
			String[] factors = metricQueryDef.getPropertyAsStringArray("factors");
			Double[] weights = metricQueryDef.getPropertyAsDoubleArray("weights");
			String datasource = elasticSource.getElasticsearchIP() + ":9200/" + metricQueryDef.getProperty("index");
		
			String onError = metricQueryDef.getProperty("onError");
			if ( onError == null ) {
				onError = projectErrorStrategy;
			}
		
			Metric m = new Metric(project, metric, evaluationDate, factors, weights, name, description, datasource, metricValue, info, onError );
			result.add(m);
			
		}
		
		return result;
		
	}
	
	/**
	 * Compute relations between (enabled) Metrics and Factors
	 * @param metrics 
	 * @return List of Relation
	 */
	private List<Relation> computeMetricRelations( List<Metric> metrics ) {
		
		List<Relation> result = new ArrayList<>();
		
		Map<String,Factor> factorMap = readFactorMap();
		
		for ( Metric metric : metrics ) {
			for ( int i = 0; i < metric.getFactors().length; i++ ) {
				
				String factorid = metric.getFactors()[i];
				Double weight = metric.getWeights()[i];

				Factor factor = factorMap.get(factorid);
				
				if ( factor == null ) {
					log.info( "Warning: Impact of Metric " + metric.getName() + " on undefined Factor " + factor + "is not stored."  );
				} else {
					if ( !factor.isEnabled() ) {
						log.info("Factor " + factor.getName() + " is disabled. No relation created.\n");
						continue;
					}
					
					Relation imp = new Relation(metric.getProject(), metric, factor, evaluationDate, metric.getValue() * weight, weight);
					result.add(imp);
				}
			}
		}

		return result;
		
	}

	/**
	 * Read Map of Factors (id->Factor) from factors.properties file
	 * @return Map of Factors
	 */
	private Map<String, Factor> readFactorMap() {
		
		
		Map<String,Factor> result = new HashMap<>();
		
		File factorPropFile = new File( projectFolder.getAbsolutePath() + File.separatorChar + "factors.properties" );
		
		Properties factorProperties = FileUtils.loadProperties(factorPropFile);
		List<String> factors = getFactors( factorProperties );
		
		for ( String f : factors ) {
			
			Boolean enabled = Boolean.parseBoolean(  factorProperties.getProperty(f + ".enabled") );
			String project = projectProperties.getProperty("project.name");
			String factor = f;
			
			String[] indicators = factorProperties.getProperty(f + ".indicators").split(",");
			Double[] weights = getAsDoubleArray( factorProperties.getProperty(f + ".weights") );
			
			String name = factorProperties.getProperty(f + ".name");
			String description = factorProperties.getProperty(f + ".description");
			String datasource = null;
			
			Double value = null;
			String info = null;
			
			String onError = factorProperties.getProperty(f + ".onError");
			
			if ( onError == null ) {
				onError = projectErrorStrategy;
			}

			Factor fact = new Factor(enabled, project, factor, evaluationDate, indicators, weights, name, description, datasource, value, info, onError );
			result.put(f, fact);
			
		}
		
		return result;
		
	}
	
	/**
	 * Read Indicators from indicators.properties file
	 * @return
	 */
	private Map<String, Indicator> readIndicatorMap() {
		
		Map<String,Indicator> result = new HashMap<>();
		
		File indicatorPropFile = new File( projectFolder.getAbsolutePath() + File.separatorChar + "indicators.properties" );
		
		Properties indicatorProperties = FileUtils.loadProperties(indicatorPropFile);
		List<String> indicators = getFactors( indicatorProperties );
		
		for ( String i : indicators ) {
			
			Boolean enabled = Boolean.parseBoolean(  indicatorProperties.getProperty(i + ".enabled") );
			String project = projectProperties.getProperty("project.name");
			String indicator = i;
			
			String[] parents = indicatorProperties.getProperty(i + ".parents").split(",");
			Double[] weights = getAsDoubleArray( indicatorProperties.getProperty(i + ".weights") );
			
			String name = indicatorProperties.getProperty(i + ".name");
			String description = indicatorProperties.getProperty(i + ".description");
			String datasource = null;
			
			Double value = null;
			String info = null;
			
			String onError = indicatorProperties.getProperty(i + ".onError");
			
			if ( onError == null ) {
				onError = projectErrorStrategy;
			}

			Indicator ind = new Indicator(enabled, project, indicator, evaluationDate, parents, weights, name, description, datasource, value, info, onError );
			result.put(i, ind);
			
		}
		
		return result;
		
	}
	
	/**
	 * Read List of Factors ids from factors.properties file
	 * @param props
	 * @return
	 */
	private List<String> getFactors( Properties props ) {
		
		List<String> result = new ArrayList<String>();
				
		Set<Object> keys = props.keySet();
		
		for ( Object k : keys ) {
			String ks = (String) k;
			if ( ks.endsWith(".name") ) {
				result.add(ks.substring(0, ks.indexOf(".name")));	
			}
		}

		return result;
		
	}

	/**
	 * Evaluate metric formula for given named parameters
	 * @param metric
	 * @param evalParameters
	 * @return
	 */
	private Double evaluate(String metric, Map<String, Object> evalParameters) {
		
		for ( String key : evalParameters.keySet() ) {
			metric = metric.replaceAll( key, evalParameters.get(key).toString() );
		}
		
		return Evaluator.eval(metric);
	}

	/**
	 * Read Map of QueryDefs from directory
	 * @param queryDirectory
	 * @return Map of QueryDefs
	 */
	private Map<String,QueryDef> getQuerySet(File queryDirectory) {
		
		Map<String,QueryDef> querySets = new HashMap<>();
		
		String[] filenames = queryDirectory.list();
		Arrays.sort(filenames);
		
		
		for ( String fname : filenames ) {
			
			String pathName = queryDirectory.getAbsolutePath() + File.separatorChar + fname;
			
			File f = new File(pathName);
			if ( f.isFile() ) {
				String filename = f.getName();
				String[] parts = filename.split("\\.");
				String name = parts[0];
				String type = parts[1];
				
				if ( type.equals("query") ) {
					
					String queryTemplate = FileUtils.readFile(f);
					
					if ( querySets.containsKey(name) ) {
						querySets.get(name).setQueryTemplate( queryTemplate ); 
					} else {
						querySets.put(name,  new QueryDef(name, projectProperties, queryTemplate, null) );
					}
					
				}
				
				if ( type.equals("properties") ) {

					Properties props = FileUtils.loadProperties(f);
						
					if ( querySets.containsKey(name) ) {
						querySets.get(name).setProperties( props );
					} else {
						querySets.put(name,  new QueryDef(name, projectProperties, null, props) );
					}

				}
			}
		}
		
		return querySets;

	}
	
	public QueryDef loadQueryDef( String directory, String name ) {
		
		File templateFile = new File( directory + File.separatorChar + name + ".query");
		String queryTemplate = FileUtils.readFile(templateFile);
		
		File propertyFile = new File( directory + File.separatorChar + name + ".properties");
		Properties props = FileUtils.loadProperties(propertyFile);
		
		return new QueryDef(name, projectProperties, queryTemplate, props);

	}
	
	/**
	 * Return Property values with comma as a Double array:
	 * "1.5,2.3" becomes [1.5,2.3]
	 * 
	 * @param key
	 * @return
	 */
	public Double[] getAsDoubleArray(String commaSeparated) {
		
		String[] parts = commaSeparated.split(",");
		Double[] doubleArray = new Double[parts.length];
		
		for ( int i=0; i<parts.length; i++ ) {
			doubleArray[i] = Double.parseDouble(parts[i]);
		}
		
		return doubleArray;
		
	}
	
	
	
}
