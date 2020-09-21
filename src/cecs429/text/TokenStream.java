package cecs429.text;

import java.io.IOException;

/**
 * Creates a sequence of String tokens from the contents of another stream, breaking the bytes of the stream into tokens
 * in some way.
 */
public interface TokenStream {
    // Returns an iterable sequence of strings representing the tokens wrapped by the stream.
    Iterable<String> getTokens();
    // Closes the TokenStream and any resources it has open.
    void close() throws IOException;
}
