package eval;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Eval {
	
	private Eval() {};
	
	private static SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
	
	public static void eval() {
		
		
		String evaluationDate = format.format(new Date());
		System.out.println("Evaluation Date: " + evaluationDate);
		
		System.out.println("Eval.eval(): Start Metric Evaluation");
		EvalMetrics.eval(evaluationDate);
		
		// Wait a little for storage
		sleep(1000);
		
		System.out.println("Eval.eval(): Start Factor Evaluation");
		EvalFactors.eval(evaluationDate);
		
		// Wait a little for storage
		sleep(1000);
		
		System.out.println("Eval.eval(): Start Indicator Evaluation");
		EvalIndicators.eval(evaluationDate);
		

	}
	
	
	private static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		eval();
	}

}
