package eval2;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import type.IndexItem;
import util.NumberUtils;

public class QueryDef {

	// name of QueryDef, equal to filename
	private String name;
	
	// project properties for variable resolution
	private Properties projectProperties;
	
	// Query template to be executed on Elasticsearch
	private String queryTemplate;

	// Query Properties (indexes, parameters, results ..)
	private Properties props;
	
	/**
	 *  A named QueryDef consists of a queryTemplate (textual query definition) and Properties
	 * @param name
	 * @param queryTemplate
	 * @param props
	 */
	public QueryDef( String name, Properties projectProperties, String queryTemplate, Properties props ) {
		
		this.name = name;
		this.projectProperties = projectProperties;
		this.queryTemplate = queryTemplate;
		this.props = props;
		
	}
	
	/**
	 * Get properties as Map<String,Object>
	 * @return
	 */
	public Map<String,Object> getPropertiesMap() {
		
		Map<String,Object> result = new HashMap<>();
		
		for ( String propName : props.stringPropertyNames() ) {
			
			String prop = props.getProperty(propName);
			
			Object o;
			// project variables start with "$$"
			if ( prop.startsWith("$$") ) {
				String projectPropertyKey = prop.substring(2);
				String projectPropertyValue = projectProperties.getProperty(projectPropertyKey);
				 o = NumberUtils.getNumberOrString( projectPropertyValue );
			} else {
				o = NumberUtils.getNumberOrString(prop);
			}
			
			result.put( propName, o );
		}
		
		return result;
	}
	

	public String getQueryTemplate() {
		return queryTemplate;
	}
	
	public void setQueryTemplate( String queryTemplate ) {
		this.queryTemplate = queryTemplate;
	}
	
	public void setProperties( Properties props ) {
		this.props = props;
	}
	
	public String getProperty( String key ) {
		
		String propValue = props.getProperty(key);
		
		if ( propValue == null)
			return null;
		
		if ( propValue.startsWith("$$") ) {
			return projectProperties.getProperty( propValue.substring(2) );
		} else {
			return propValue;
		}
		
	}
	
	public Map<String,String> getResults() {
		return getFilteredProperties("result.");
	}
	
	/**
	 * Get all key-value-pairs from the Properties that start with "param."
	 * In the returned Map, the keys are equal to the Properties key with the prefix removed:
	 * 
	 * "param.foo" becomes "foo" in the returned Map.
	 * 
	 * If the Property values represent numbers, the proper Number Object is returned.
	 * 
	 * @return
	 */
	public Map<String,Object> getQueryParameter() {
		
		String prefix = "param.";
		
		Map<String,Object> parameter = new HashMap<>();

		for ( String p : props.stringPropertyNames() ) {
			
			if ( p.startsWith( prefix ) ) {
				
				String resultName = p.substring(prefix.length());
				String stringValue = props.getProperty(p);
				
				Object o;
				if ( stringValue.startsWith("$$") ) {
					String projectPropertyKey = stringValue.substring(2);
					String projectPropertyValue = projectProperties.getProperty(projectPropertyKey);
					 o = NumberUtils.getNumberOrString( projectPropertyValue );
				} else {
					 o = NumberUtils.getNumberOrString(stringValue);
				}
				
				parameter.put( resultName, o );
				
			}
			
		}
		
		return parameter;
	}
	
	/**
	 * Get all map-entries starting with prefix.
	 * result keys  the prefix is removed from keys
	 * @param prefix
	 * @return
	 */
	private Map<String,String> getFilteredProperties(String prefix) {
		
		Map<String,String> results = new HashMap<>();

		for ( String p : props.stringPropertyNames() ) {
			if ( p.startsWith( prefix ) ) {
				String resultName = p.substring(prefix.length());
				results.put( resultName, props.getProperty(p) );
			}
		}
		
		return results;
	}
	
	/**
	 * Get enabled prop
	 * @return
	 */
	public Boolean isEnabled() {
		return Boolean.parseBoolean( props.getProperty("enabled") );
	}

	public String getName() {
		return name;
	}

	/**
	 * Return Property values with comma as a String array:
	 * "foo,bar" becomes ["foo","bar"]
	 * 
	 * @param key
	 * @return
	 */
	public String[] getPropertyAsStringArray(String key) {
		 
		String commaSeparated = props.getProperty(key);
		return commaSeparated.split(",");
		
	}
	
	/**
	 * Return Property values with comma as a Double array:
	 * "1.5,2.3" becomes [1.5,2.3]
	 * 
	 * @param key
	 * @return
	 */
	public Double[] getPropertyAsDoubleArray(String key) {
		
		String commaSeparated = props.getProperty(key);
		String[] parts = commaSeparated.split(",");
		Double[] doubleArray = new Double[parts.length];
		
		for ( int i=0; i<parts.length; i++ ) {
			doubleArray[i] = Double.parseDouble(parts[i]);
		}
		
		return doubleArray;
		
	}
	
	public String toString() {
		return name + "\n" + queryTemplate;
	}
	
	public String onError() {
		
		if ( props.containsKey("onError") ) {
			return props.getProperty("onError");
		} else {
			return projectProperties.getProperty("onError.default",IndexItem.ON_ERROR_DROP);
		}
		
	}
	
	public Boolean onErrorDrop() {
		return onError().equals(IndexItem.ON_ERROR_DROP);
	}
	
	public void setIndex( String index ) {
		props.setProperty("index", index);
	}

	public Double getErrorValue() {
		Double result = 1.0;
		if (onError().equals(IndexItem.ON_ERROR_SET0)) result = 0.0;
		return result;
	}
}
