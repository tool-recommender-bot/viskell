package nl.utwente.group10.ghcj;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * A conversation with an instance of ghci.
 */
public class GhciSession implements Closeable {
    /** The process this GhciSession will communicate with. */
    private final Process ghci;

    /** Raw input stream for result data from ghci to the application. */
    private final InputStream in;

    /** Raw output stream from the application to ghci. */
    private final OutputStream out;

    /** Responses from ghci are terminated by a null byte. */
    private static final char SENTINEL = 0;

    /** All communication is done over UTF_8. */
    private static final Charset UTF_8 = StandardCharsets.UTF_8;

    /** The path to GHCI. */
    private static final String GHCIPATH = "ghci";

    /**
     * Builds a new communication session with ghci.
     *
     * @throws GhciException when ghci can not be found, can not be executed,
     *         or does not understand our setup sequence.
     */
    public GhciSession() throws GhciException {
        try {
            this.ghci = new ProcessBuilder(GHCIPATH)
                            .redirectErrorStream(true)
                            .start();

            this.in = this.ghci.getInputStream();
            this.out = this.ghci.getOutputStream();
        } catch (IOException e) {
            throw new GhciException(e);
        }

        /* Make it so that GHCi prints a null byte to its standard output when
           it expects input. By setting the prompt to a zero byte, GHCi will
           print a zero byte whenever it expects the user (that's us) to enter
           the next expression. In UTF-8, zero bytes are not part of any
           character except for NUL, the zero character, which makes them a
           useful sentinel. */
        this.eval(":set prompt " + SENTINEL);

        /* Make it so that GHCi resets bindings after every command. This makes
           it slightly less likely that GHCi state will affect our results. */
        this.eval(":set +r");
    }

    /**
     * @return a GhciEnvironment that is connected to this GhciSession.
     */
    public final GhciEnvironment getEnvironment() {
        return new GhciEnvironment(this);
    }

    /**
     * Evaluates a Haskell expression and wait for it to compute.
     *
     * @param cmd The (complete) Haskell
     * @return the result, including newline, as a string.
     * @throws GhciException when ghci is not ready to evaluate the expression.
     * @throws HaskellException when the expression can not be computed.
     */
    public final String eval(final String cmd) throws GhciException {
        StringBuilder responseBuilder = new StringBuilder();

        try {
            // Send the expression to ghci.
            this.out.write(cmd.getBytes(UTF_8));
            this.out.write('\n');
            this.out.flush();

            // Wait for the sentinel.
            int input;
            while ((input = this.in.read()) != 0) {
                responseBuilder.append((char) input);
            }
        } catch (IOException e) {
            throw new GhciException(e);
        }

        String response = responseBuilder.toString();

        // Check for hints that something went wrong
        // To do: Make this better

        String exceptionHeader = "*** Exception: ";
        String parseErrorHeader = "<interactive>";

        List<String> lines = Splitter.on("\n").splitToList(response);
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);

            if (line.startsWith(exceptionHeader)) {
                String msg = line.substring(exceptionHeader.length());
                throw new HaskellException(msg);
            }

            if (line.startsWith(parseErrorHeader)) {
                List<String> sublines = lines.subList(i, lines.size());
                String msg = Joiner.on("\n").join(sublines);
                throw new HaskellException(msg);
            }
        }

        return response;
    }

    /**
     * @return a String representation of this GhciSession.
     */
    public final String toString() {
        return "GhciSession{" + this.ghci + "}";
    }

    /**
     * Destroys the ghci instance and closes communications channels.
     * @throws IOException when closing the channels fails.
     */
    @Override
    public final void close() throws IOException {
        this.in.close();
        this.out.close();
        this.ghci.destroy();
    }
}
