package eval.props;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import elastic.model.Factor;

public class FactorsProperties {
	
	private static final FactorsProperties instance = new FactorsProperties();
	private static final String factorsPropertyFile = "factors.properties";
	private static Properties factorsProperties;
	
	
	private FactorsProperties() {
		
		FileInputStream input;
		try {
			input = new FileInputStream(factorsPropertyFile);
			factorsProperties = new Properties();
			factorsProperties.load(input);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public static FactorsProperties getInstance() {
		return instance;
	}
	
	public String get(String property) {
		return factorsProperties.getProperty(property);
	}
	
	
	public String[] getArray(String array) {
		
		String commaSeparated = factorsProperties.getProperty(array);
		
		return commaSeparated.split(",");
	}
	
	/**
	 * Return Metric object based on properties
	 * @param prefix Name of the Metric
	 * @return
	 */
	public Factor prepareFactor( String prefix ) {
		int a;
		Boolean enabled = Boolean.parseBoolean( get(prefix + ".enabled") );
		String factor = prefix; ;
		String name = get(prefix + ".name");
		String description = get(prefix + ".description");
		String[] strategic_indicators = getArray(prefix + ".strategic_indicators");
		
		return new Factor(enabled, factor, name, description, strategic_indicators, null, null, null);
	}
	
	public List<String> getFactors() {
		
		List<String> result = new ArrayList<String>();
				
		Set<Object> keys = factorsProperties.keySet();
		
		for ( Object k : keys ) {
			String ks = (String) k;
			if ( ks.endsWith(".name") ) {
				result.add(ks.substring(0, ks.indexOf(".name")));	
			}
		}

		return result;
		
	}

}
