package controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.MemberService;
import service.models.Member;
import util.InputUtil;
import util.validators.MemberValidator;

import java.util.List;
import java.util.Optional;

public class MemberController {

    private static final Logger log = LoggerFactory.getLogger(MemberController.class);

    private final MemberService memberService;

    public MemberController() {
        this.memberService = new MemberService();
        log.debug("MemberController initialized with default MemberService.");
    }

    // Optional: for unit tests (inject a mocked service)
    public MemberController(MemberService memberService) {
        if (memberService == null) {
            log.error("Attempted to initialize MemberController with null MemberService.");
            throw new IllegalArgumentException("memberService cannot be null.");
        }
        this.memberService = memberService;
        log.debug("MemberController initialized with injected MemberService.");
    }

    public void handleInput() {
        log.info("Entered Member Services menu.");
        boolean running = true;

        while (running) {
            try {
                printMenu();
                int choice = InputUtil.readInt("Make a choice: ");
                log.debug("Member menu selection received: {}", choice);

                switch (choice) {
                    case 1 -> listAllMembers();
                    case 2 -> addMember();
                    case 3 -> findMemberById();
                    case 4 -> updateMember();
                    case 5 -> deleteMember();
                    case 0 -> {
                        log.info("Exiting Member Services menu.");
                        running = false;
                    }
                    default -> {
                        log.warn("Invalid Member menu option selected: {}", choice);
                        System.out.println("Invalid option. Please try again.");
                    }
                }
            } catch (Exception ex) {
                log.error("Unhandled exception in MemberController menu loop.", ex);
                System.out.println("An unexpected error occurred. Please try again.");
            }
        }
    }

    private void printMenu() {
        log.debug("Printing Member Services menu.");
        System.out.println();
        System.out.println("=== MEMBER SERVICES ===");
        System.out.println("1. List all members");
        System.out.println("2. Add a member");
        System.out.println("3. Find member by ID");
        System.out.println("4. Update a member");
        System.out.println("5. Delete a member");
        System.out.println("0. Back");
    }

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

    private void addMember() {
        log.info("Add Member operation started.");
        System.out.println();
        System.out.println("=== ADD MEMBER ===");

        try {
            // Inline validation per field (BookController style)
            String name = promptRequiredNameCreate();
            String email = promptOptionalEmailCreate();
            String phone = promptOptionalPhoneCreate();

            log.debug("Add Member validated input - name={}, email={}, phone={}", name, email, phone);

            Member member = new Member(name, email, phone);
            Long id = memberService.create(member);

            log.info("Member created successfully with id={}", id);
            System.out.println("Saved member with id=" + id);

        } catch (RuntimeException ex) {
            log.error("Unexpected error while adding member.", ex);
            System.out.println("Error adding member.");
        }
    }

    private void findMemberById() {
        System.out.println();
        System.out.println("=== FIND MEMBER ===");

        long id = InputUtil.readInt("Member ID: ");
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

    private void updateMember() {
        System.out.println();
        System.out.println("=== UPDATE MEMBER ===");

        long id = InputUtil.readInt("Member ID to update: ");
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

            // Inline validation per field (BookController style)
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

        } catch (RuntimeException ex) {
            log.error("Unexpected error while updating member id={}", id, ex);
            System.out.println("Error updating member.");
        }
    }

    private void deleteMember() {
        System.out.println();
        System.out.println("=== DELETE MEMBER ===");

        long id = InputUtil.readInt("Member ID to delete: ");
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
        } catch (RuntimeException ex) {
            log.error("Error deleting member with id={}", id, ex);
            System.out.println("Error deleting member.");
        }
    }

    // -------------------------------------------------------------------------
    // Inline prompt + validation helpers (CREATE)
    // -------------------------------------------------------------------------

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

    private String promptNameUpdate(String currentValue) {
        while (true) {
            String input = InputUtil.readString("New name: ");
            if ("-".equals(input)) return currentValue;

            try {
                return MemberValidator.requireValidName(input);
            } catch (IllegalArgumentException ex) {
                log.warn("Invalid name input (update): {}", ex.getMessage());
                System.out.println("Invalid name: " + ex.getMessage());
            }
        }
    }

    private String promptEmailUpdate(String currentValue) {
        while (true) {
            String input = InputUtil.readString("New email (or NONE to clear): ");
            if ("-".equals(input)) return currentValue;

            try {
                String normalized = MemberValidator.normalizeOptionalEmail(input); // NONE/blank -> null
                return MemberValidator.validateOptionalEmail(normalized);
            } catch (IllegalArgumentException ex) {
                log.warn("Invalid email input (update): {}", ex.getMessage());
                System.out.println("Invalid email: " + ex.getMessage());
            }
        }
    }

    private String promptPhoneUpdate(String currentValue) {
        while (true) {
            String input = InputUtil.readString("New phone (or NONE to clear): ");
            if ("-".equals(input)) return currentValue;

            try {
                String normalized = MemberValidator.normalizeOptionalPhone(input); // NONE/blank -> null
                return MemberValidator.validateOptionalPhone(normalized);
            } catch (IllegalArgumentException ex) {
                log.warn("Invalid phone input (update): {}", ex.getMessage());
                System.out.println("Invalid phone: " + ex.getMessage());
            }
        }
    }
}
