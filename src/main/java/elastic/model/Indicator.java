package elastic.model;

import java.util.HashMap;
import java.util.Map;

public class Indicator {
	
	private Boolean enabled;
	private String indicator;
	private String name;
	private String description;
	private String evaluationDate;
	private Float value;
	private String datasource;
	
	public Indicator( 
			Boolean enabled,
			String indicator, 
			String name, 
			String description, 
			String evaluationDate, 
			Float value, 
			String datasource) { 

		this.enabled = enabled;
		this.indicator = indicator;
		this.name = name;
		this.description = description;
		this.evaluationDate = evaluationDate;
		this.value = value;
		this.datasource = datasource;

	}
	
	public String getId() {
		return this.indicator + "-" + evaluationDate;
	}
	
	public Map<String, Object> getMap() {
		Map<String, Object> result = new HashMap<String, Object>();
		
		result.put("strategic_indicator", indicator);
		result.put("name", name);
		result.put("description", description);
		result.put("evaluationDate", evaluationDate);
		result.put("value", value);
		result.put("datasource", datasource);
		
		return result;
	}
	
	public void setValue( Float value ) {
		this.value = value;
	}
	
	public void setEvaluationDate( String evaluationDate ) {
		this.evaluationDate = evaluationDate;
	}

	public String getIndicator() {
		return indicator;
	}

	public Boolean getEnabled() {
		return enabled;
	}

	public Float getValue() {
		return value;
	}

	public String getName() {
		return name;
	}


}
