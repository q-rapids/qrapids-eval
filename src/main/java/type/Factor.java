package type;

import java.util.HashMap;
import java.util.Map;

public class Factor extends IndexItem {
	
	public Factor( 
			
			Boolean enabled,
			String project,
			String factor,
			String evaluationDate,
			
			String [] indicators,
			Double [] weights,
			
			String name, 
			String description, 
			String datasource,
			Double value,
			String info,
			String onError
			
		) { 

		this.enabled = enabled;
		
		this.project  = project;
		this.id = factor;
		this.evaluationDate = evaluationDate;
		
		this.parents = indicators;
		this.weights = weights;
		
		this.name = name;
		this.description = description;
		this.datasource = datasource;
		
		this.value = value;
		this.info = info;
		
		this.onError = onError;

	}
	
	@Override
	public String getType() {
		return "factors";
	}
	
	public Boolean getEnabled() {
		return enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	public String getFactor() {
		return id;
	}

	public void setFactor(String factor) {
		this.id = factor;
	}

	public String[] getIndicators() {
		return parents;
	}

	public void setIndicators(String[] indicators) {
		this.parents = indicators;
	}

	public Map<String, Object> getMap() {
		
		Map<String, Object> result = new HashMap<String, Object>();
		
		result.put("project", project);
		result.put("factor", id);
		result.put("evaluationDate", evaluationDate);
		
		result.put("indicators", parents);
		result.put("weights", weights);
		
		result.put("name", name);
		result.put("description", description);
		result.put("datasource", datasource);
		
		result.put("value", value);
		result.put("info", info);
		
		return result;
		
	}




}
