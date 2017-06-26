package pdamjanovic.evaluation;

/**
 * Container of document metrics used to represent stemmer evaluation
 * 
 * @author p.damjanovic
 *
 */
public class DocumentMetrics {

	String documentName;
	double precision;
	double recall;
	double accuracy1;
	double accuracy2;

	public DocumentMetrics(String documentName, double precision, double recall, double accuracy1, double accuracy2) {
		super();
		this.documentName = documentName;
		this.precision = precision;
		this.recall = recall;
		this.accuracy1 = accuracy1;
		this.accuracy2 = accuracy2;
	}

	public double getPrecision() {
		return precision;
	}

	public double getRecall() {
		return recall;
	}

	public double getAccuracy1() {
		return accuracy1;
	}

	public double getAccuracy2() {
		return accuracy2;
	}
}
