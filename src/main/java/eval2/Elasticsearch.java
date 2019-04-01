package eval2;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.Properties;

import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryAction;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.script.mustache.SearchTemplateRequestBuilder;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import type.Factor;
import type.IndexItem;
import type.Indicator;
import type.Metric;
import type.Relation;
import util.Evaluator;
import util.JsonPath;

public class Elasticsearch {
	
	private Logger log = Logger.getLogger(this.getClass().getName());
	
	private TransportClient client;

	private static ObjectMapper mapper = new ObjectMapper();
	
	private String elasticsearchIP;
	
	public String getElasticsearchIP() {
		return elasticsearchIP;
	}

	/**
	 * Create on address of an Elasticsearch Server
	 * @param elasticsearchIP
	 */
	public Elasticsearch( String elasticsearchIP ) {
		
		this.elasticsearchIP = elasticsearchIP;

		try {
			InetAddress address = InetAddress.getByName(elasticsearchIP);
			client = new PreBuiltTransportClient(Settings.EMPTY).addTransportAddress( new InetSocketTransportAddress( address , 9300 ) );
			if ( client.connectedNodes().size() == 0 ) {
				log.severe( "Could not connect ot Elasticsearch on " + elasticsearchIP + ", exiting." );
				System.exit(0);
			}
		} catch (UnknownHostException e) {
			System.err.println("Could not connect ot Elasticsearch on " + elasticsearchIP);
			e.printStackTrace();
		}
		
	}
	
	/**
	 * Get the TransportClient
	 * @return
	 */
	public TransportClient getClient() {
		return client;
	}

	/**
	 * Execute QueryDef with additional parameters
	 * @param externalParameters Additional parameters derived by i.e. param-queries
	 * @param queryDef the queryDef to execute
	 * @return
	 */
	public Map<String,Object> execute( Map<String,Object> externalParameters, QueryDef queryDef ) {
		
		log.info("Executing QueryDef " + queryDef.getName() + "\nIndex: " + queryDef.getProperty("index") + "\nExternal parameters: " + externalParameters + "\n" + "query parameters: " + queryDef.getQueryParameter() + "\n");
		
		Map<String,Object> execParams = new HashMap<>();
		
		execParams.putAll(externalParameters);
		execParams.putAll( queryDef.getQueryParameter() ); 

	    SearchResponse sr = search(queryDef.getQueryTemplate(), (String) queryDef.getProperty("index"), execParams );
	    
	    Map<String,Object> executionResult = new HashMap<>();
	    
	    
	    if ( sr == null ) {
	    	log.warning("QueryDef " + queryDef.getName() + " failed.\n");
	    	return executionResult;
	    }
	    
	    Map<String,String> queryResults = queryDef.getResults();
	    
	    String response = sr.toString();
	    log.info("Elasticsearch Response: " + response.trim() + "\n");
	    
	    try {
	    	
	    	JsonNode node = mapper.readTree(response); 
		
		    for ( Entry<String,String> e : queryResults.entrySet() ) { 
		    	
		    	JsonNode value = JsonPath.getNode( node, e.getValue() );
		    	Object o = convert( value );
		    	executionResult.put(e.getKey(), o);
		    }

	    } catch ( Exception e ) {
	    	e.printStackTrace();
	    }
	    

		return executionResult;
	}
	
	/**
	 * Try to convert a JsonNode into a Number Object.
	 * Return at least String value
	 * @param node
	 * @return
	 */
	private static Object convert( JsonNode node ) {
		
		if ( node.isLong() ) {
			return node.asLong();
		}
		
		if ( node.isInt() ) {
			return node.asInt();
		}
		
		if ( node.isDouble() ) {
			return node.asDouble();
		}
		
		if ( node.isTextual() ) {
			return node.textValue();
		}
		
		return node.asText();
	}
	
	/**
	 * Perform an Elasticsearch search
	 * @param template The queryTemplate
	 * @param index The index to run the query on
	 * @param params Parameters for the templateQuery
	 * @return
	 */
	public SearchResponse search(String template, String index, Map<String,Object> params) {
		
		try {
		
			SearchResponse sr = new SearchTemplateRequestBuilder(client)
		    		.setScript(template)
		    		.setScriptType(ScriptType.INLINE)
		    		.setRequest(new SearchRequest(index))
		    		.setScriptParams(params)
		    		.get()
		    		.getResponse();
			
			return sr;
		} catch (RuntimeException rte) {
			log.severe(rte.getMessage() + "\n" + rte.toString() + "\n");
			return null;
		}
		
	}

	public void storeMetrics(Properties projectProperties, String evaluationDate, Collection<Metric> metrics) {
		
		String metricIndex = projectProperties.getProperty("metrics.index") + "." + projectProperties.getProperty("project.name");
		
		checkCreateIndex(  metricIndex, "schema/metric.schema", "metrics" );
		
		long deleted = deleteCurrentEvaluation(
				metricIndex,
				projectProperties.getProperty("project.name"),
				evaluationDate
		);
		
		log.info("deleted " + deleted + " metrics (evaluationDate=" + evaluationDate + ")\n");
		
		long deleted2 = deleteCurrentEvaluation(
				projectProperties.getProperty("relations.index") + "." + projectProperties.getProperty("project.name"),  
				projectProperties.getProperty("project.name"),
				evaluationDate
		);
		
		log.info("deleted " + deleted2 + " relations (evaluationDate=" + evaluationDate + ")\n");
		
		BulkResponse br = writeBulk(evaluationDate, metricIndex, "metrics", metrics);
		
		log.info( bulkResponseCheck(br) + "\n" );
		
		
	}
	
	public void storeRelations(Properties projectProperties, String evaluationDate, Collection<Relation> relations) {
		
		String indexName = projectProperties.getProperty("relations.index") + "." + projectProperties.getProperty("project.name");;
		
		checkCreateIndex(  indexName, "schema/relation.schema", "relations" ); 
		
		BulkResponse br = writeBulk(evaluationDate, indexName, "relations", relations);
		
		log.info( bulkResponseCheck(br) );
		
	}
	

	public void storeFactors(Properties projectProperties, String evaluationDate, Collection<Factor> factors ) {
		
		String indexName = projectProperties.getProperty("factors.index") + "." + projectProperties.getProperty("project.name");;
		
		checkCreateIndex(  indexName, "schema/factor.schema", "factors" );
		
		long deleted = deleteCurrentEvaluation(
				indexName, 
				projectProperties.getProperty("project.name"),
				evaluationDate
		);
		
		log.info("deleted " + deleted + " factors (evaluationDate=" + evaluationDate + ")");
		
		BulkResponse br = writeBulk(evaluationDate, indexName, "factors", factors);
		
		log.info( bulkResponseCheck(br) );
		
	}
	
	public void storeIndicators(Properties projectProperties, String evaluationDate, Collection<Indicator> indicators) {
		String indexName = projectProperties.getProperty("indicators.index") + "." + projectProperties.getProperty("project.name");;
		
		checkCreateIndex(  indexName, "schema/indicator.schema", "indicators" );
		
		long deleted = deleteCurrentEvaluation(
				indexName, 
				projectProperties.getProperty("project.name"),
				evaluationDate
		);
		
		log.info("deleted " + deleted + " indicators (evaluationDate=" + evaluationDate + ")");
		
		BulkResponse br = writeBulk(evaluationDate, indexName, "indicators", indicators);
		
		log.info( bulkResponseCheck(br) );
		
	}
	
	private void checkCreateIndex( String indexName, String schemaPathname, String mappingType ) {

		try {
			IndexManager mgr = new IndexManager(this);
			mgr.createIndex(indexName, schemaPathname, mappingType);
		} catch ( Exception e) {
			log.info( e.getMessage() + "\n"); 
		}
		
	}
	
	private BulkResponse writeBulk( String evaluationDate, String index, String mappingType, Collection<? extends IndexItem> items) {
		
		if ( items.size() == 0 ) {
			log.warning("No items stored");
			return null;
		}
		
		BulkRequestBuilder brb = client.prepareBulk().setRefreshPolicy(RefreshPolicy.IMMEDIATE);
		
		for (IndexItem i : items ) {
			brb.add( client.prepareIndex(index, mappingType, i.getElasticId()  ).setSource( i.getMap() ) );
		}

		
		BulkResponse bulkResponse = brb.get();
		
		return bulkResponse;

	}
	
	private String bulkResponseCheck( BulkResponse br ) {
		
		if ( br == null ) {
			log.warning("Response is null");
			return "";
		}
		
		String result = "";
		
		if ( br.hasFailures() ) {
			for ( BulkItemResponse bir : br.getItems() ) {
				if (bir.getFailure() != null) {
					result += bir.getFailure().getId() + ":" + bir.getFailureMessage() + " \n";
				}
			}
		} else {
			result = "BulkUpdate success! " + br.getItems().length + " items written!" ;
		}
		
		return result;
	}
	
	private long deleteCurrentEvaluation( String indexName, String project, String evaluationDate ) {
		try {
			BulkByScrollResponse response =
					  DeleteByQueryAction.INSTANCE.newRequestBuilder(client)
					    .filter(QueryBuilders.matchQuery("evaluationDate", evaluationDate))
					    .filter(QueryBuilders.matchQuery("project", project))
					    .source(indexName)                                  
					    .get();  
			
			return response.getDeleted();
		} catch ( RuntimeException rte ) {
			return 0;
		}
	}


}
