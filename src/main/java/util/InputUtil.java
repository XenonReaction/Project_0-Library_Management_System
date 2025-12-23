package util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Scanner;

/**
 * Utility class for handling console-based user input.
 *
 * <p>This class centralizes all {@link Scanner}-based input logic to ensure:</p>
 * <ul>
 *   <li>Consistent prompting and validation behavior</li>
 *   <li>Clear separation between input handling and business logic</li>
 *   <li>Centralized logging of user input events</li>
 * </ul>
 *
 * <p><strong>Design notes:</strong></p>
 * <ul>
 *   <li>Uses a single static {@link Scanner} instance bound to {@code System.in}</li>
 *   <li>Methods block until valid input is received (where applicable)</li>
 *   <li>This is appropriate for a CLI-based application</li>
 * </ul>
 *
 * <p><strong>Lifecycle:</strong></p>
 * <ul>
 *   <li>The scanner is created once when the class is loaded</li>
 *   <li>{@link #close()} should be called exactly once when the application exits</li>
 * </ul>
 */
public class InputUtil {

    /**
     * Logger for input-related events.
     */
    private static final Logger log = LoggerFactory.getLogger(InputUtil.class);

    /**
     * Shared scanner instance for reading from standard input.
     */
    private static final Scanner scanner = new Scanner(System.in);

    /**
     * Prompts the user for an integer value and blocks until a valid integer is entered.
     *
     * <p>This method:</p>
     * <ul>
     *   <li>Displays the provided prompt</li>
     *   <li>Reads a full line of input</li>
     *   <li>Attempts to parse it as an {@code int}</li>
     *   <li>Repeats until parsing succeeds</li>
     * </ul>
     *
     * @param prompt text displayed to the user before reading input
     * @return the parsed integer value
     */
    public static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine();

            log.debug("User entered raw integer input: '{}'", input);

            try {
                int value = Integer.parseInt(input);
                log.debug("Parsed integer successfully: {}", value);
                return value;
            } catch (NumberFormatException ex) {
                log.warn("Invalid integer input received: '{}'", input);
                System.out.println("Invalid number. Please try again.");
            }
        }
    }

    /**
     * Prompts the user for a non-empty string.
     *
     * <p>This method trims whitespace and rejects empty input.</p>
     *
     * @param prompt text displayed to the user before reading input
     * @return a non-empty, trimmed string
     */
    public static String readString(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();

            log.debug("User entered raw string input: '{}'", input);

            if (!input.isEmpty()) {
                return input;
            }

            log.warn("Empty string input rejected.");
            System.out.println("Input cannot be empty.");
        }
    }

    /**
     * Prompts the user for a line of input and allows empty input.
     *
     * <p>This method performs no validation and returns the input exactly
     * as entered (including empty strings).</p>
     *
     * <p>Common use cases:</p>
     * <ul>
     *   <li>"Press Enter to continue" pauses</li>
     *   <li>Optional update fields</li>
     * </ul>
     *
     * @param prompt text displayed to the user before reading input
     * @return the raw input line (may be empty)
     */
    public static String readLineAllowEmpty(String prompt) {
        System.out.print(prompt);
        String input = scanner.nextLine();

        log.debug("User entered raw line input (allow empty): '{}'", input);

        return input;
    }

    /**
     * Closes the shared {@link Scanner}.
     *
     * <p>This method should be called exactly once when the application
     * is shutting down to release {@code System.in} resources.</p>
     *
     * <p>After calling this method, further input attempts will fail.</p>
     */
    public static void close() {
        log.info("Closing input scanner.");
        scanner.close();
    }
}
