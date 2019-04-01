package eval;

import static org.elasticsearch.index.query.QueryBuilders.termQuery;

import java.util.List;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.NoNodeAvailableException;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.avg.Avg;

import elastic.Client;
import elastic.Indexes;
import elastic.model.Indicator;
import eval.props.IndicatorsProperties;

public class EvalIndicators {
	
	private static String factorsIndex = Indexes.getInstance().getFactorsIndex();
	private static IndicatorsProperties indicatorProperties = IndicatorsProperties.getInstance();
	
	private EvalIndicators(){}
	
	public static void eval(String evaluationDate) {
		

		List<String> indicatorList = indicatorProperties.getIndicators();
		
		for ( String indicator : indicatorList ) {
			Indicator i = indicatorProperties.prepareIndicator(indicator);
			
			if ( i.getEnabled() ) {
				evalIndicator(i, evaluationDate);
				if ( i.getValue() != null && !i.getValue().isNaN()) {
					i.setEvaluationDate(evaluationDate);
					IndexWriter.writeIndicator(i);
					System.out.println("Factor " + i.getIndicator() + " written, value=" + i.getValue());
				} else {
					System.out.println("Indicator " + i.getIndicator() + " not computed");
				}
			}
		}
			
	}
	
	private static void evalIndicator(Indicator i, String evaluationDate) {

		TransportClient tc = Client.getClient();

		try {
			SearchResponse sr = tc
			        .prepareSearch(factorsIndex)
			        .setSize(0)
			        .setQuery(QueryBuilders.boolQuery()
			        		.must(termQuery("strategic_indicators", i.getIndicator()))
			        		.must(termQuery("evaluationDate", evaluationDate))
			        		)
			        .addAggregation( AggregationBuilders.avg("average").field("value") )
			        .execute()
			        .actionGet();
			
			Avg avg = sr.getAggregations().get("average");		
			Float average = (float) avg.getValue();
			i.setValue(average);
		} catch (Throwable th) {
			th.printStackTrace();
			i.setValue(null);
		}

	}
	
	public static void main(String[] args) {
		eval("2017-11-27");
	}
	
	

}
