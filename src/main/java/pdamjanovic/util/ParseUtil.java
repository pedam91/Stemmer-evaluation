package pdamjanovic.util;

import org.springframework.stereotype.Component;

@Component
public class ParseUtil {

	public static enum WordType {
	    Noun("N"), Verb("V"), Adjective("A"), Pronoun("PRO"), Number("NUM"), Preposition("PREP"), Conjuction("CONJ"), 
	    Interjection("INT"), Particle("PAR"), Adverb("ADV"), Prefix("PREF"), Abbreviation("ABB"), RomanNumerical("RN"), 
	    Punctuation("PUNCT"), SentenceEndMarker("SENT"), NonSerbian("?");

	    private final String text;

	    private WordType(final String text) {
	        this.text = text;
	    }

	    @Override
	    public String toString() {
	        return text;
	    }
	}

	public static String convertAccentWord(String word) {

		String retVal = word;

		retVal = word.replace("cy", "\u010D"); // č
		retVal = retVal.replace("cx", "\u0107"); // ć
		retVal = retVal.replace("zx", "\u017E"); // ž
		retVal = retVal.replace("sx", "\u0161"); // š
		retVal = retVal.replace("dx", "\u0111"); // đ
		retVal = retVal.replace("lx", "lj");
		retVal = retVal.replace("nx", "nj");

		return retVal;
	}

}
