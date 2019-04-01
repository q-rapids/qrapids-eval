package eval.props;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import elastic.model.Metric;

public class MetricsProperties {
	
	private static final MetricsProperties instance = new MetricsProperties();
	private static final String metricsPropertyFile = "metrics.properties";
	private static Properties metricsProperties;
	
	
	private MetricsProperties() {
		
		FileInputStream input;
		try {
			input = new FileInputStream(metricsPropertyFile);
			metricsProperties = new Properties();
			metricsProperties.load(input);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public static MetricsProperties getInstance() {
		return instance;
	}
	
	public String get(String property) {
		return metricsProperties.getProperty(property);
	}
	
	
	public String[] getArray(String array) {
		 
		String commaSeparated = metricsProperties.getProperty(array);
		return commaSeparated.split(",");
		
	}
	
	/**
	 * Return Metric object based on properties
	 * @param prefix Name of the Metric
	 * @return
	 */
	public Metric prepareMetric( String prefix ) {
		
		Boolean enabled = Boolean.parseBoolean( get(prefix + ".enabled") );
		String metric = prefix;
		String name = get(prefix + ".name");
		String description = get(prefix + ".description");
		String[] factors = getArray(prefix + ".factors");
		String source = get(prefix + ".source");

		return new Metric(enabled, metric, name, description, factors, null, null, null, source);
	}

}
