// Lab 4 – CommandParser
package util;

import model.Command;

/**
 * Parses a raw input line into a {@link Command} record.
 * Lab 4: demonstrates String splitting and record construction.
 * <p>
 * Format: {@code verb [arg1] [arg2] ...}
 * Example: {@code deposit 2001 5000}
 */
public final class CommandParser {

    private CommandParser() {}

    /**
     * Parse a raw line into a Command.
     * @throws IllegalArgumentException if line is blank
     */
    public static Command parse(String line) {
        if (line == null || line.isBlank())
            throw new IllegalArgumentException("Command line must not be blank");

        String[] parts = line.strip().split("\\s+");
        String verb = parts[0];
        String[] args = new String[parts.length - 1];
        System.arraycopy(parts, 1, args, 0, args.length);

        return new Command(verb, args);
    }

    /**
     * Parse and require at least {@code minArgs} arguments.
     * @throws IllegalArgumentException if argument count is insufficient
     */
    public static Command parseWithMinArgs(String line, int minArgs) {
        Command cmd = parse(line);
        if (cmd.argCount() < minArgs)
            throw new IllegalArgumentException(
                    "Command '" + cmd.verb() + "' requires at least " +
                    minArgs + " argument(s), got " + cmd.argCount());
        return cmd;
    }
}
