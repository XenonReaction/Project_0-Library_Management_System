package util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Scanner;

public class InputUtil {

    private static final Logger log = LoggerFactory.getLogger(InputUtil.class);
    private static final Scanner scanner = new Scanner(System.in);

    // Read integer with validation
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

    // Read non-empty string
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

    // Cleanup method called only when app exits
    public static void close() {
        log.info("Closing input scanner.");
        scanner.close();
    }
}
