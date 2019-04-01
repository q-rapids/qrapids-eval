package eval;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.script.mustache.SearchTemplateRequestBuilder;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import elastic.Client;
import util.JsonPath;

public class WriteTest {
	
	private static ObjectMapper mapper = new ObjectMapper();

	public static void main(String[] args) throws JsonParseException, JsonMappingException, IOException {

		String template = getTemplate("template/lastSnapshotDate");

		Map<String,Object> params = new HashMap<>();
		params.put("bcKey", "de.fhg.iese.dd.platform.parent:platform-parent");
		
	    SearchResponse sr = search(template, "sonarqube.measures",params);
	    
	    String response = sr.toString();
	    System.out.println(response);
		
		// JsonNode
		JsonNode node = mapper.readTree(response);
		System.out.println( JsonPath.getNode( node, "hits.hits[0]._source.snapshotDate") );
		String snapshotDate = JsonPath.getNode( node, "hits.hits[0]._source.snapshotDate").textValue();
		
		params.put("snapshotDate", snapshotDate);
		params.put("avgcplx.threshold",10);
		
		template = getTemplate("template/complexity");
		sr = search(template, "sonarqube.measures",params);
		System.out.println(sr);
		
		response = sr.toString();
		node = mapper.readTree(response);
		
		long good = JsonPath.getNode( node, "aggregations.goodBad.buckets[0].doc_count").longValue();
		long bad = JsonPath.getNode( node, "aggregations.goodBad.buckets[1].doc_count").longValue();
		
		System.out.println(good);
		System.out.println(bad);
		
		template = getTemplate("template/comments");
		params.put("comments.threshold.lower", 10);
		params.put("comments.threshold.upper", 30);
		sr = search(template,"sonarqube.measures",params);
		System.out.println(sr);
		
		response = sr.toString();
		node = mapper.readTree(response);
		
		good = JsonPath.getNode( node, "aggregations.good.buckets[0].doc_count").longValue();
		long all = JsonPath.getNode( node, "hits.total").longValue();
		
		System.out.println(good);
		System.out.println(all);
		
		
		template = getTemplate("template/duplication");
		params.put("avgdupdensity.threshold", 5);
		sr = search(template,"sonarqube.measures",params);
		System.out.println(sr);
		
		response = sr.toString();
		node = mapper.readTree(response);
		
		good = JsonPath.getNode( node, "aggregations.goodBad.buckets[0].doc_count").longValue();
		bad = JsonPath.getNode( node, "aggregations.goodBad.buckets[1].doc_count").longValue();
		
		System.out.println(good);
		System.out.println(bad);

		// CD_Backend_dd-dev_master
		template = getTemplate("template/testsuccess");
		params.put("jobName", "CD_Backend_dd-dev_master");
		sr = search(template,"jenkins",params);
		System.out.println(sr);
		
		response = sr.toString();
		node = mapper.readTree(response);
		
		long testsPass = JsonPath.getNode( node, "hits.hits[0]._source.testsPass").longValue();
		long testsSkip = JsonPath.getNode( node, "hits.hits[0]._source.testsSkip").longValue();
		long testsFail = JsonPath.getNode( node, "hits.hits[0]._source.testsFail").longValue();
		
		System.out.println(testsPass);
		System.out.println(testsSkip);
		System.out.println(testsFail);
	}
	

	private static SearchResponse search(String template, String index, Map<String,Object> params) {
			
		SearchResponse sr = new SearchTemplateRequestBuilder(Client.getClient())
	    		.setScript(template)
	    		.setScriptType(ScriptType.INLINE)
	    		.setRequest(new SearchRequest(index))
	    		.setScriptParams(params)
	    		.get()
	    		.getResponse();
		return sr;
		
	}

	private static String getTemplate(String resourcePath) {
		
		ClassLoader classloader = Thread.currentThread().getContextClassLoader();
		InputStream is = classloader.getResourceAsStream(resourcePath);
		java.util.Scanner s = new java.util.Scanner(is);
		s.useDelimiter("\\A");
		String template = s.next();
		s.close();
		return template;
		
	}
	
	private static void writeObjectExample() {
		Map<String, Object> o = new HashMap<String, Object>();
		
		o.put("i", 1);
		o.put("f", 1.5);
		o.put("s", "a string");
		o.put("d", new Date().getTime());
		
		
		IndexWriter.writeObject("qm", "test", "1", o);
	}




}
