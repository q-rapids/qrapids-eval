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
import elastic.model.Factor;
import eval.props.FactorsProperties;

public class EvalFactors {
	
	private static String metricsIndex = Indexes.getInstance().getMetricsIndex();

	private static FactorsProperties factorsProperties =  FactorsProperties.getInstance();
	
	private EvalFactors(){}
	
	public static void eval(String evaluationDate) {
		

		List<String> factorList = factorsProperties.getFactors();
		
		for ( String factor : factorList ) {
			Factor f = factorsProperties.prepareFactor(factor);
			
			if ( f.getEnabled() ) {
				evalFactor(f, evaluationDate);
				if ( f.getValue() != null && !f.getValue().isNaN() ) {
					f.setEvaluationDate(evaluationDate);
					IndexWriter.writeFactor(f);
					System.out.println("Factor " + f.getFactor() + " written, value=" + f.getValue());
				} else {
					System.out.println("Factor " + f.getFactor() + " not computed, value=" + f.getValue());
				}
			}
		}
			
	}

	private static void evalFactor(Factor f, String evaluationDate) {

		TransportClient tc = Client.getClient();

		try {
			SearchResponse sr = tc
			        .prepareSearch(metricsIndex)
			        .setSize(0)
			        .setQuery(QueryBuilders.boolQuery()
			        		.must(termQuery("factors", f.getFactor()))
			        		.must(termQuery("evaluationDate", evaluationDate))
			        		)
			        .addAggregation( AggregationBuilders.avg("average").field("value") )
			        .execute()
			        .actionGet();
			
			Avg avg = sr.getAggregations().get("average");
			
			Float average = (float) avg.getValue();
			
			f.setValue(average);
		} catch ( Throwable th ) {
			th.printStackTrace();
			f.setValue(null);
		}


	}
	
	public static void main(String[] args) {
		eval("2017-10-27");
	}
	
	

}
