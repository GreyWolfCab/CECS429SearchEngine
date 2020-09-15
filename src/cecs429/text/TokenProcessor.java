package cecs429.text;

/**
 * A TokenProcessor applies some rules of normalization to a token from a document, and returns a term for that token.
 */
public interface TokenProcessor {
	/**
	 * Normalizes a token into a term.
	 */
	String processToken(String token);
}
