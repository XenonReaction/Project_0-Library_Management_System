package controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.MemberService;
import service.models.Member;
import util.InputUtil;
import util.validators.MemberValidator;

import java.util.List;
import java.util.Optional;

/**
 * Controller for "Member Services" menu operations.
 *
 * <p>This controller is responsible for:
 * <ul>
 *   <li>Displaying the Member menu and routing user choices to actions</li>
 *   <li>Collecting and validating user input (via {@link InputUtil} and {@link MemberValidator})</li>
 *   <li>Calling the service layer ({@link MemberService}) to perform business operations</li>
 *   <li>Handling exceptions gracefully so the application does not crash</li>
 * </ul>
 *
 * <p><strong>Layering:</strong> This class should not directly access the database or DAOs.
 * All persistence work should be delegated to the service layer.
 *
 * <p><strong>Logging:</strong> Avoid logging PII (email/phone). This controller intentionally logs
 * only non-sensitive details (e.g., IDs, counts, and names when appropriate).
 */
public class MemberController {

    private static final Logger log = LoggerFactory.getLogger(MemberController.class);

    /**
     * Service layer dependency used to perform member operations.
     */
    private final MemberService memberService;

    /**
     * Constructs a controller with a default {@link MemberService}.
     *
     * <p>Used in production runtime wiring.
     */
    public MemberController() {
        this.memberService = new MemberService();
        log.debug("MemberController initialized with default MemberService.");
    }

    /**
     * Constructs a controller with an injected {@link MemberService}.
     *
     * <p>This constructor is primarily intended for unit tests where a mock service
     * can be provided.
     *
     * @param memberService service instance to use (must not be null)
     * @throws IllegalArgumentException if {@code memberService} is null
     */
    public MemberController(MemberService memberService) {
        if (memberService == null) {
            log.error("Attempted to initialize MemberController with null MemberService.");
            throw new IllegalArgumentException("memberService cannot be null.");
        }
        this.memberService = memberService;
        log.debug("MemberController initialized with injected MemberService.");
    }

    /**
     * Runs the interactive Member Services menu loop until the user exits.
     *
     * <p>All errors are caught so the user can continue interacting with the menu.
     */
    public void handleInput() {
        log.info("Entered Member Services menu.");
        boolean running = true;

        while (running) {
            try {
                printMenu();
                int choice = InputUtil.readInt("Make a choice: ");
                log.debug("Member menu selection received: {}", choice);

                switch (choice) {
                    case 1 -> {
                        listAllMembers();
                        pressEnterToContinue();
                    }
                    case 2 -> {
                        addMember();
                        pressEnterToContinue();
                    }
                    case 3 -> {
                        findMemberById();
                        pressEnterToContinue();
                    }
                    case 4 -> {
                        updateMember();
                        pressEnterToContinue();
                    }
                    case 5 -> {
                        deleteMember();
                        pressEnterToContinue();
                    }
                    case 0 -> {
                        log.info("Exiting Member Services menu.");
                        running = false; // no pause here
                    }
                    default -> {
                        log.warn("Invalid Member menu option selected: {}", choice);
                        System.out.println("Invalid option. Please try again.");
                        pressEnterToContinue();
                    }
                }
            } catch (Exception ex) {
                // Catch-all so the menu does not crash the entire application.
                log.error("Unhandled exception in MemberController menu loop.", ex);
                System.out.println("An unexpected error occurred. Please try again.");
                pressEnterToContinue();
            }
        }
    }

    /**
     * Prints the Member Services menu options to the console.
     */
    private void printMenu() {
        log.debug("Printing Member Services menu.");
        System.out.println();
        System.out.println("=== MEMBER SERVICES ===");
        System.out.println("1. List all members");
        System.out.println("2. Add a member");
        System.out.println("3. Find member by ID");
        System.out.println("4. Update a member");
        System.out.println("5. Delete a member");
        System.out.println("0. Back to Main Menu");
    }

    /**
     * Pauses execution until the user presses Enter.
     *
     * <p>This prevents the menu from immediately re-printing after an operation.
     */
    private void pressEnterToContinue() {
        InputUtil.readLineAllowEmpty("Press Enter to continue...");
    }

    /**
     * Retrieves and prints all members from the system.
     *
     * <p>If no members exist, prints a friendly message instead of an empty list.
     */
    private void listAllMembers() {
        log.info("Listing all members.");
        System.out.println();
        System.out.println("=== ALL MEMBERS ===");

        try {
            List<Member> members = memberService.getAll();
            log.debug("Retrieved {} members.", members.size());

            if (members.isEmpty()) {
                System.out.println("No members found.");
                return;
            }

            members.forEach(System.out::println);

        } catch (RuntimeException ex) {
            log.error("Failed to retrieve members.", ex);
            System.out.println("Error retrieving members.");
        }
    }

    /**
     * Prompts the user for member details and creates a new member.
     *
     * <p>Input is validated using {@link MemberValidator}. Email/phone are optional.
     */
    private void addMember() {
        log.info("Add Member operation started.");
        System.out.println();
        System.out.println("=== ADD MEMBER ===");

        try {
            String name = promptRequiredNameCreate();
            String email = promptOptionalEmailCreate();
            String phone = promptOptionalPhoneCreate();

            // PII-safe: don't log email/phone values
            log.debug("Add Member validated input - name={}", name);

            Member member = new Member(name, email, phone);
            Long id = memberService.create(member);

            log.info("Member created successfully with id={}", id);
            System.out.println("Saved member with id=" + id);

        } catch (IllegalArgumentException ex) {
            log.warn("Add Member rejected: {}", ex.getMessage());
            System.out.println(ex.getMessage());
        } catch (RuntimeException ex) {
            log.error("Unexpected error while adding member.", ex);
            System.out.println("Error adding member.");
        }
    }

    /**
     * Prompts the user for a member ID and prints the member if found.
     */
    private void findMemberById() {
        System.out.println();
        System.out.println("=== FIND MEMBER ===");

        long id = promptValidMemberId("Member ID: ");
        log.debug("Find Member requested for id={}", id);

        try {
            Optional<Member> maybeMember = memberService.getById(id);
            if (maybeMember.isEmpty()) {
                log.info("No member found with id={}", id);
                System.out.println("No member found with id=" + id);
                return;
            }

            log.info("Member found with id={}", id);
            System.out.println(maybeMember.get());

        } catch (RuntimeException ex) {
            log.error("Error finding member with id={}", id, ex);
            System.out.println("Error finding member.");
        }
    }

    /**
     * Prompts the user for a member ID, loads the current member, and applies updates.
     *
     * <p>Update conventions:
     * <ul>
     *   <li>{@code "-"} keeps the current value</li>
     *   <li>{@code "NONE"} clears an optional value (email/phone becomes {@code null})</li>
     * </ul>
     */
    private void updateMember() {
        System.out.println();
        System.out.println("=== UPDATE MEMBER ===");

        long id = promptValidMemberId("Member ID to update: ");
        log.debug("Update Member requested for id={}", id);

        try {
            Optional<Member> maybeExisting = memberService.getById(id);
            if (maybeExisting.isEmpty()) {
                log.info("No member found to update with id={}", id);
                System.out.println("No member found with id=" + id);
                return;
            }

            Member existing = maybeExisting.get();
            log.debug("Existing member before update: {}", existing);

            System.out.println("Enter new values.");
            System.out.println("Use '-' to keep the current value.");
            System.out.println("For Email/Phone: '-' keeps current, 'NONE' clears it (NULL), otherwise enter a value.");

            String name = promptNameUpdate(existing.getName());
            String email = promptEmailUpdate(existing.getEmail());
            String phone = promptPhoneUpdate(existing.getPhone());

            Member updated = new Member();
            updated.setName(name);
            updated.setEmail(email);
            updated.setPhone(phone);

            Member result = memberService.update(id, updated);
            log.info("Member updated successfully for id={}", id);
            System.out.println("Updated: " + result);

        } catch (IllegalArgumentException ex) {
            log.warn("Update Member rejected for id={}: {}", id, ex.getMessage());
            System.out.println(ex.getMessage());
        } catch (RuntimeException ex) {
            log.error("Unexpected error while updating member id={}", id, ex);
            System.out.println("Error updating member.");
        }
    }

    /**
     * Prompts the user for a member ID and deletes the member if allowed.
     *
     * <p>Any deletion policy (e.g., blocked when loans exist) should be enforced by the
     * service layer, not the controller.
     */
    private void deleteMember() {
        System.out.println();
        System.out.println("=== DELETE MEMBER ===");

        long id = promptValidMemberId("Member ID to delete: ");
        log.debug("Delete Member requested for id={}", id);

        try {
            boolean deleted = memberService.delete(id);
            if (deleted) {
                log.info("Member deleted successfully with id={}", id);
                System.out.println("Deleted member id=" + id);
            } else {
                log.info("No member found to delete with id={}", id);
                System.out.println("No member found with id=" + id + " (nothing deleted).");
            }
        } catch (IllegalArgumentException ex) {
            log.warn("Delete Member rejected for id={}: {}", id, ex.getMessage());
            System.out.println(ex.getMessage());
        } catch (RuntimeException ex) {
            log.error("Error deleting member with id={}", id, ex);
            System.out.println("Error deleting member.");
        }
    }

    // -------------------------------------------------------------------------
    // Inline prompt + validation helpers
    // -------------------------------------------------------------------------

    /**
     * Prompts the user for a member ID and validates it using {@link MemberValidator}.
     *
     * @param prompt text displayed to the user
     * @return a valid, positive member ID
     */
    private long promptValidMemberId(String prompt) {
        while (true) {
            long input = InputUtil.readInt(prompt);
            try {
                return MemberValidator.requireValidMemberId(input);
            } catch (IllegalArgumentException ex) {
                log.warn("Invalid member id input: {}", ex.getMessage());
                System.out.println("Invalid Member ID: " + ex.getMessage());
            }
        }
    }

    // -------------------------------------------------------------------------
    // Inline prompt + validation helpers (CREATE)
    // -------------------------------------------------------------------------

    /**
     * Prompts for and validates a required member name.
     *
     * @return validated member name
     */
    private String promptRequiredNameCreate() {
        while (true) {
            String input = InputUtil.readString("Name: ");
            try {
                return MemberValidator.requireValidName(input);
            } catch (IllegalArgumentException ex) {
                log.warn("Invalid name input: {}", ex.getMessage());
                System.out.println("Invalid name: " + ex.getMessage());
            }
        }
    }

    /**
     * Prompts for and validates an optional email.
     *
     * <p>User may type {@code NONE} (or blank, depending on validator behavior) to store null.
     *
     * @return validated email or {@code null}
     */
    private String promptOptionalEmailCreate() {
        while (true) {
            String input = InputUtil.readString("Email (type NONE if not applicable): ");
            try {
                String normalized = MemberValidator.normalizeOptionalEmail(input);
                return MemberValidator.validateOptionalEmail(normalized);
            } catch (IllegalArgumentException ex) {
                log.warn("Invalid email input: {}", ex.getMessage());
                System.out.println("Invalid email: " + ex.getMessage());
            }
        }
    }

    /**
     * Prompts for and validates an optional phone number.
     *
     * <p>User may type {@code NONE} (or blank, depending on validator behavior) to store null.
     *
     * @return validated phone number or {@code null}
     */
    private String promptOptionalPhoneCreate() {
        while (true) {
            String input = InputUtil.readString("Phone (type NONE if not applicable): ");
            try {
                String normalized = MemberValidator.normalizeOptionalPhone(input);
                return MemberValidator.validateOptionalPhone(normalized);
            } catch (IllegalArgumentException ex) {
                log.warn("Invalid phone input: {}", ex.getMessage());
                System.out.println("Invalid phone: " + ex.getMessage());
            }
        }
    }

    // -------------------------------------------------------------------------
    // Inline prompt + validation helpers (UPDATE)
    // -------------------------------------------------------------------------

    /**
     * Prompts for an updated name value and resolves update rules using {@link MemberValidator}.
     *
     * @param currentValue current name
     * @return updated name (or current name if user chooses to keep it)
     */
    private String promptNameUpdate(String currentValue) {
        while (true) {
            String input = InputUtil.readString("New name: ");
            try {
                return MemberValidator.resolveUpdatedName(input, currentValue);
            } catch (IllegalArgumentException ex) {
                log.warn("Invalid name input (update): {}", ex.getMessage());
                System.out.println("Invalid name: " + ex.getMessage());
            }
        }
    }

    /**
     * Prompts for an updated email value and resolves update rules using {@link MemberValidator}.
     *
     * @param currentValue current email (may be null)
     * @return updated email (may be null)
     */
    private String promptEmailUpdate(String currentValue) {
        while (true) {
            String input = InputUtil.readString("New email (or NONE to clear): ");
            try {
                return MemberValidator.resolveUpdatedEmail(input, currentValue);
            } catch (IllegalArgumentException ex) {
                log.warn("Invalid email input (update): {}", ex.getMessage());
                System.out.println("Invalid email: " + ex.getMessage());
            }
        }
    }

    /**
     * Prompts for an updated phone value and resolves update rules using {@link MemberValidator}.
     *
     * @param currentValue current phone (may be null)
     * @return updated phone (may be null)
     */
    private String promptPhoneUpdate(String currentValue) {
        while (true) {
            String input = InputUtil.readString("New phone (or NONE to clear): ");
            try {
                return MemberValidator.resolveUpdatedPhone(input, currentValue);
            } catch (IllegalArgumentException ex) {
                log.warn("Invalid phone input (update): {}", ex.getMessage());
                System.out.println("Invalid phone: " + ex.getMessage());
            }
        }
    }
}
