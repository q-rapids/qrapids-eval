package elastic.model;

import java.util.HashMap;
import java.util.Map;

public class Factor {
	
	private Boolean enabled;
	private String factor;
	private String name;
	private String description;
	private String[] strategic_indicators;
	private String evaluationDate;
	private Float value;


	private String datasource;
	
	public Factor( 
			Boolean enabled,
			String factor, 
			String name, 
			String description, 
			String[] strategic_indicators, 
			String evaluationDate, 
			Float value, 
			String datasource) { 

		this.enabled = enabled;
		this.factor = factor;
		this.name = name;
		this.description = description;
		this.strategic_indicators = strategic_indicators;
		this.evaluationDate = evaluationDate;
		this.value = value;
		this.datasource = datasource;

	}
	
	public String getId() {
		return this.factor + "-" + evaluationDate;
	}
	
	public Map<String, Object> getMap() {
		Map<String, Object> result = new HashMap<String, Object>();
		
		result.put("factor", factor);
		result.put("name", name);
		result.put("description", description);
		result.put("strategic_indicators", strategic_indicators);
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

	public String getFactor() {
		return factor;
	}

	public Boolean getEnabled() {
		return enabled;
	}
	
	public Float getValue() {
		return value;
	}

}
