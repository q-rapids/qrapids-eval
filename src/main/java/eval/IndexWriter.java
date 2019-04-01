package eval;

import java.util.List;
import java.util.Map;

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.transport.TransportClient;

import elastic.Client;
import elastic.Indexes;
import elastic.model.Factor;
import elastic.model.Indicator;
import elastic.model.Metric;

public class IndexWriter {
	
	private static Indexes i = Indexes.getInstance();

	
	public static IndexResponse writeMetric( Metric metric ) {

		String index = i.getMetricsIndex();
		String type = i.getMetricsIndexType();
		
		return write( index, type, metric.getId(), metric.getMap() );
	}
	
	public static IndexResponse writeFactor( Factor factor ) {

		String index = i.getFactorsIndex();
		String type = i.getFactorsIndexType();
		
		return write( index, type, factor.getId(), factor.getMap() );
	}
	
	public static IndexResponse writeIndicator( Indicator indicator ) {

		String index = i.getIndicatorsIndex();
		String type = i.getIndicatorsIndexType();
		
		return write( index, type, indicator.getId(), indicator.getMap() );
	}
	
	private static IndexResponse write( String index, String type, String id, Map<String, Object> o) {
		
		TransportClient client = Client.getClient();

		IndexResponse response = client.prepareIndex(index, type, id)
		        .setSource(o)
		        .get();
		
		return response;

	}
	
	public static IndexResponse writeObject( String index, String type, String id, Map<String, Object> o) {
		
		TransportClient client = Client.getClient();

		IndexResponse response = client.prepareIndex(index, type, id)
		        .setSource(o)
		        .get();
		
		return response;

	}
	
	public static org.elasticsearch.action.bulk.BulkResponse writeBulk( String index, String type, List<Map<String, Object>> bulk) {
		
		TransportClient client = Client.getClient();
		BulkRequestBuilder brb = client.prepareBulk();
		
		for ( Map<String,Object> o : bulk ) {
			brb.add( client.prepareIndex(index, type, (String) o.get("hash") + o.get("filename")).setSource(o) );
		}

		BulkResponse bulkResponse = brb.get();
		
		return bulkResponse;

	}

}
