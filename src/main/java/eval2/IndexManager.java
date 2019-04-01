package eval2;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.logging.Logger;

import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.xcontent.XContentType;

public class IndexManager {
	
	private Logger log = Logger.getLogger(this.getClass().getName());
	
	private Elasticsearch es;
	private IndicesAdminClient indicesAdminClient;
	
	/**
	 * Create a IndexManager for an Elasticsearch instance
	 * @param es
	 */
	public IndexManager( Elasticsearch es ) {
		this.es = es;
		indicesAdminClient = es.getClient().admin().indices();
	}
	
	

	/**
	 * Create an Index using a Schema stored in the resource folder
	 * @param indexName name of index to be created
	 * @param schemaPath Pathname of schema file
	 * @param mappingType Mapping type
	 */
	public void createIndex( String indexName, String schemaPath, String mappingType ) {
		
		String schema = loadSchema(schemaPath);
		
		indicesAdminClient.prepareCreate(indexName)
			.addMapping( mappingType , schema, XContentType.JSON )
			.get();
		
	}


	/**
	 * Load a schema file form the resource folder
	 * @param name
	 * @return
	 */
	private static String loadSchema( String name ) {
		// java.net.URL
		ClassLoader classloader = Thread.currentThread().getContextClassLoader();
		InputStream is = classloader.getResourceAsStream(name);
		
		java.util.Scanner scanner = new java.util.Scanner(is);
		scanner.useDelimiter("\\A");
	    String result =  scanner.hasNext() ? scanner.next() : "";
	    
	    try {
	    	
	    	scanner.close();
			is.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	    
	    return result;
	}
	
	/**
	 * 
	 * @param index The index name to write to
	 * @param type The mapping type to use
	 * @param id Id of the document to be written
	 * @param o Object key-value Map
	 * @return
	 */
	public IndexResponse writeObject( String index, String type, String id, Map<String, Object> o) {
		
		TransportClient client = es.getClient();

		IndexResponse response = client.prepareIndex(index, type, id)
		        .setSource(o)
		        .get();
		
		return response;

	}
	


}

