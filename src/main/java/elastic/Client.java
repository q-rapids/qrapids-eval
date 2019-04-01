package elastic;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.script.mustache.SearchTemplateRequestBuilder;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

public class Client {
	
	private static final Client instance = new Client(); 
	
	private static TransportClient client;
	
	private Client() {
		
		String serverIP = Indexes.getInstance().getElasticsearchServerIp();
		try {
			InetAddress address = InetAddress.getByName(serverIP);
			client = new PreBuiltTransportClient(Settings.EMPTY).addTransportAddress( new InetSocketTransportAddress( address , 9300 ) );
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	
	public static TransportClient getClient() {
		return client;
	}
	
}
