package eval.props;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import elastic.model.Indicator;

public class IndicatorsProperties {
	
	private static final IndicatorsProperties instance = new IndicatorsProperties();
	private static final String indicatorsPropertyFile = "indicators.properties";
	private static Properties indicatorsProperties;
	
	
	private IndicatorsProperties() {
		
		FileInputStream input;
		try {
			input = new FileInputStream(indicatorsPropertyFile);
			indicatorsProperties = new Properties();
			indicatorsProperties.load(input);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public static IndicatorsProperties getInstance() {
		return instance;
	}
	
	public String get(String property) {
		return indicatorsProperties.getProperty(property);
	}
	
	
	public String[] getArray(String array) {
		
		String commaSeparated = indicatorsProperties.getProperty(array);
		
		return commaSeparated.split(",");
	}
	
	/**
	 * Return Indicator object based on properties
	 * @param prefix Name of the Metric
	 * @return
	 */
	public Indicator prepareIndicator( String prefix ) {
		
		Boolean enabled = Boolean.parseBoolean( get(prefix + ".enabled")  );
		String indicator = prefix;
		String name = get(prefix + ".name");
		String description = get(prefix + ".description");
		
		return new Indicator(enabled, indicator, name, description, null, null, null);
	}
	
	public List<String> getIndicators() {
		
		List<String> result = new ArrayList<String>();
				
		Set<Object> keys = indicatorsProperties.keySet();
		
		for ( Object k : keys ) {
			String ks = (String) k;
			if ( ks.endsWith(".name") ) {
				result.add(ks.substring(0, ks.indexOf(".name")));	
			}
		}

		return result;
		
	}
	
}
