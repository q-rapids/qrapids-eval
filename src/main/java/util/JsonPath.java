package util;

import com.fasterxml.jackson.databind.JsonNode;

public class JsonPath {
	
	public static JsonNode getNode(JsonNode node, String path) {
		
		for ( String pe : path.split("\\.") ) {
			
			if ( pe.contains("[") ) {
				String part[] = pe.split("\\[|\\]");
				node = node.path(part[0]);
				node = node.path(Integer.parseInt(part[1]));
			} else {
				node = node.path(pe);
			}
			
		}

		return node;
	}

}
