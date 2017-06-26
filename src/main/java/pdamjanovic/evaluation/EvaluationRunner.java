package pdamjanovic.evaluation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.stereotype.Component;

import pdamjanovic.stemmer.SerbianAnalyzer;
import pdamjanovic.util.ParseUtil;

/**
 * 
 * @author p.damjanovic
 *
 */
@Component
public class EvaluationRunner implements CommandLineRunner {

	@Autowired
	SerbianAnalyzer serbianAnalyzer;

	String docsPath = "docs_verification";
	Resource[] documents;

	// collection of calculated document metrics
	List<DocumentMetrics> collection = new ArrayList<>();

	// Map<Document, Map<Stem, List<Token>>
	Map<String, Map<String, List<String>>> stemToken = new HashMap<>();

	// Map<Document, Map<Lemma, List<Token>>
	Map<String, Map<String, List<String>>> lemmaToken = new HashMap<>();

	// Map<Document, Map<Lemma, Map<Stem, List<Token>>>
	Map<String, Map<String, Map<String, List<String>>>> accuracy1Map = new HashMap<>();

	// Map<Document, Map<Stem, Map<Lemma, List<Token>>>
	Map<String, Map<String, Map<String, List<String>>>> accuracy2Map = new HashMap<>();

	@Autowired
	private ResourceLoader resourceLoader;

	@Override
	public void run(String... args) {
		try {
			System.out.println("Starting evaluation");

			// load lexical resource
			documents = ResourcePatternUtils.getResourcePatternResolver(resourceLoader)
					.getResources("classpath*:/" + docsPath + "/*.txt");

			firstPass();
			secondPass();

			double precisionSum = 0D, recallSum = 0D, accuracy1Sum = 0D, accuracy2Sum = 0D;
			double numberOfDocuments = collection.size();

			for (DocumentMetrics documentMetrics : collection) {
				precisionSum += documentMetrics.getPrecision();
				recallSum += documentMetrics.getRecall();
				accuracy1Sum += documentMetrics.getAccuracy1();
				accuracy2Sum += documentMetrics.getAccuracy2();
			}

			double precisionCollection = precisionSum / numberOfDocuments;
			double recallCollection = recallSum / numberOfDocuments;

			// Formula for F measure is f = 2PR/(P+R)
			// where P is the precision and R is the recall of collection
			double fMeasure = (2 * precisionCollection * recallCollection) / (precisionCollection + recallCollection);

			System.out.println("******Collection statistics******");
			System.out.println("Precision : " + precisionCollection);
			System.out.println("Recall : " + recallCollection);
			System.out.println("Accuracy1 : " + accuracy1Sum / numberOfDocuments);
			System.out.println("Accuracy2 : " + accuracy2Sum / numberOfDocuments);
			System.out.println("F measure : " + fMeasure);

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void firstPass() throws IOException {

		for (Resource resource : documents) {

			String documentName = resource.getFilename();
			BufferedReader bufRead = getReaderFromResource(documentName);
			String myLine = null;

			while ((myLine = bufRead.readLine()) != null) {

				String[] array1 = myLine.split("\t");
				String tokenTxt = array1[0];
				String partOfSpeech = array1[6];
				String lemmaTxt = array1[7];
				String stemTxt = stem(tokenTxt, partOfSpeech, lemmaTxt);

				if ("".equals(stemTxt))
					continue;

				Map<String, List<String>> stemTokenDokument = stemToken.get(documentName);
				if (stemTokenDokument == null) {
					stemTokenDokument = new HashMap<>();
					stemToken.put(documentName, stemTokenDokument);
				}

				List<String> tokensForStem = stemTokenDokument.get(stemTxt);
				if (tokensForStem == null) {
					tokensForStem = new ArrayList<>();
					stemTokenDokument.put(stemTxt, tokensForStem);
				}

				tokensForStem.add(tokenTxt);

				Map<String, List<String>> lemmaTokenDokument = lemmaToken.get(documentName);
				if (lemmaTokenDokument == null) {
					lemmaTokenDokument = new HashMap<>();
					lemmaToken.put(documentName, lemmaTokenDokument);
				}

				List<String> tokensForLemma = lemmaTokenDokument.get(lemmaTxt);
				if (tokensForLemma == null) {
					tokensForLemma = new ArrayList<>();
					lemmaTokenDokument.put(lemmaTxt, tokensForLemma);
				}

				tokensForLemma.add(tokenTxt);

				Map<String, Map<String, List<String>>> acc1MapDocument = accuracy1Map.get(documentName);
				if (acc1MapDocument == null) {
					acc1MapDocument = new HashMap<>();
					accuracy1Map.put(documentName, acc1MapDocument);
				}

				Map<String, List<String>> acc1List = acc1MapDocument.get(lemmaTxt);
				if (acc1List == null) {
					acc1List = new HashMap<>();
					acc1MapDocument.put(lemmaTxt, acc1List);
				}

				List<String> tokensStem = acc1List.get(stemTxt);
				if (tokensStem == null) {
					tokensStem = new ArrayList<>();
					acc1List.put(stemTxt, tokensStem);
				}

				tokensStem.add(tokenTxt);
				acc1List.put(stemTxt, tokensStem);

				// Map<Stem, Map<Lemma, List<Token>>
				Map<String, Map<String, List<String>>> acc2MapDocument = accuracy2Map.get(documentName);
				if (acc2MapDocument == null) {
					acc2MapDocument = new HashMap<>();
					accuracy2Map.put(documentName, acc2MapDocument);
				}

				Map<String, List<String>> acc2List = acc2MapDocument.get(stemTxt);
				if (acc2List == null) {
					acc2List = new HashMap<>();
					acc2MapDocument.put(stemTxt, acc2List);
				}

				List<String> tokensLemma = acc2List.get(lemmaTxt);
				if (tokensLemma == null) {
					tokensLemma = new ArrayList<>();
					acc2List.put(lemmaTxt, tokensLemma);
				}

				tokensLemma.add(tokenTxt);
			}

			bufRead.close();
		}
	}

	public void secondPass() throws IOException {

		for (Resource resource : documents) {

			String documentName = resource.getFilename();
			BufferedReader bufRead = getReaderFromResource(documentName);
			String myLine = null;

			int numberOfTokens = 0;
			double precisionSum = 0D, recallSum = 0D;

			while ((myLine = bufRead.readLine()) != null) {

				String[] array1 = myLine.split("\t");
				// File columns:
				// 1. Token,
				// 2. Text ID,
				// 3. Author ID,
				// 4. Year,
				// 5. Domain ID,
				// 6. Source Description ID,
				// 7. Part of Speech (PoS) and
				// 8. Lemma.
				String tokenTxt = array1[0];
				String partOfSpeech = array1[6];
				String lemmaTxt = array1[7];
				String stemTxt = stem(tokenTxt, partOfSpeech, lemmaTxt);

				if ("".equals(stemTxt))
					continue;

				double numberOfTokensWithSameLemmaAndStem = accuracy1Map.get(documentName).get(lemmaTxt).get(stemTxt)
						.size();

				double numberofTokensWithSameStem = stemToken.get(documentName).get(stemTxt).size();

				double precisionWord = numberOfTokensWithSameLemmaAndStem / numberofTokensWithSameStem;

				double numberofTokensWithSameLemma = lemmaToken.get(documentName).get(lemmaTxt).size();

				double recallWord = numberOfTokensWithSameLemmaAndStem / numberofTokensWithSameLemma;

				precisionSum += precisionWord;
				recallSum += recallWord;
				numberOfTokens++;
			}

			double precisionDocument = precisionSum / numberOfTokens;
			double recallDocument = recallSum / numberOfTokens;

			int countAcc1Document = countAccDocument(documentName, accuracy1Map);
			float accuracy1Document = (float) countAcc1Document / accuracy1Map.get(documentName).size();

			int countAcc2Document = countAccDocument(documentName, accuracy2Map);
			float accuracy2Document = (float) countAcc2Document / accuracy2Map.get(documentName).size();

			DocumentMetrics documentMetrics = new DocumentMetrics(documentName, precisionDocument, recallDocument,
					accuracy1Document, accuracy2Document);
			collection.add(documentMetrics);

			bufRead.close();
		}
	}

	public BufferedReader getReaderFromResource(String resourceName) {
		InputStream inStream = this.getClass().getClassLoader().getResourceAsStream(docsPath + "/" + resourceName);
		return new BufferedReader(new InputStreamReader(inStream));
	}

	public String stem(String tokenTxt, String partOfSpeech, String lemmaTxt) throws IOException {
		String retVal = "";

		tokenTxt = tokenTxt.toLowerCase();
		lemmaTxt = lemmaTxt.toLowerCase();

		if ("<unknown>".equals(lemmaTxt))
			return retVal;

		boolean usefulWordType = !partOfSpeech.equals(ParseUtil.WordType.Punctuation.toString())
				&& !partOfSpeech.equals(ParseUtil.WordType.SentenceEndMarker.toString())
				&& !partOfSpeech.equals(ParseUtil.WordType.NonSerbian.toString())
				&& !partOfSpeech.equals(ParseUtil.WordType.Number.toString())
				&& !partOfSpeech.equals(ParseUtil.WordType.RomanNumerical.toString());

		if (usefulWordType && tokenTxt.length() > 2) {

			tokenTxt = ParseUtil.convertAccentWord(tokenTxt);
			lemmaTxt = ParseUtil.convertAccentWord(lemmaTxt);

			TokenStream ts = serbianAnalyzer.tokenStream("fieldName", new StringReader(tokenTxt));
			CharTermAttribute cattr = ts.addAttribute(CharTermAttribute.class);
			ts.reset();
			while (ts.incrementToken()) {
				retVal = cattr.toString();
			}
			ts.end();
			ts.close();
		}

		return retVal;
	}

	public int countAccDocument(String documentName, Map<String, Map<String, Map<String, List<String>>>> accuracyMap) {

		int retVal = 0;
		Map<String, Map<String, List<String>>> values = accuracyMap.get(documentName);
		for (String key : values.keySet()) {
			if (values.get(key).size() == 1) {
				retVal++;
			}
		}
		return retVal;
	}

}
