// Lab 4 – Command record
// An immutable snapshot of a parsed user command (verb + arguments).
package model;

/**
 * Immutable record representing a parsed user command.
 * Lab 4: demonstrates Java records as data carriers.
 *
 * @param verb the command keyword (e.g. "deposit", "withdraw")
 * @param args the positional arguments that follow the verb
 */
public record Command(String verb, String[] args) {

    /** Compact constructor – normalises verb to lowercase. */
    public Command {
        if (verb == null || verb.isBlank())
            throw new IllegalArgumentException("Command verb must not be blank");
        verb = verb.strip().toLowerCase();
        args = (args == null) ? new String[0] : args.clone();
    }

    /** Convenience: number of arguments provided. */
    public int argCount() { return args.length; }

    /** Convenience: get arg by index, or null if out of range. */
    public String arg(int index) {
        return (index >= 0 && index < args.length) ? args[index] : null;
    }

    @Override
    public String toString() {
        return "Command{verb='" + verb + "', args=" + java.util.Arrays.toString(args) + "}";
    }
}
