package controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.MemberService;
import service.models.Member;
import util.InputUtil;

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
            String name = InputUtil.readString("Name: ");
            String emailInput = InputUtil.readString("Email (type NONE if not applicable): ");
            String phoneInput = InputUtil.readString("Phone (type NONE if not applicable): ");

            String email = parseOptionalString(emailInput);
            String phone = parseOptionalString(phoneInput);

            log.debug("Add Member input - name={}, email={}, phone={}", name, email, phone);

            Member member = new Member(name, email, phone);
            Long id = memberService.create(member);

            log.info("Member created successfully with id={}", id);
            System.out.println("Saved member with id=" + id);

        } catch (IllegalArgumentException ex) {
            log.warn("Validation error while adding member: {}", ex.getMessage());
            System.out.println("Could not add member: " + ex.getMessage());
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

            String nameInput = InputUtil.readString("New name: ");
            String emailInput = InputUtil.readString("New email: ");
            String phoneInput = InputUtil.readString("New phone: ");

            Member updated = new Member();
            updated.setName("-".equals(nameInput) ? existing.getName() : nameInput);

            if ("-".equals(emailInput)) updated.setEmail(existing.getEmail());
            else updated.setEmail(parseOptionalString(emailInput));

            if ("-".equals(phoneInput)) updated.setPhone(existing.getPhone());
            else updated.setPhone(parseOptionalString(phoneInput));

            Member result = memberService.update(id, updated);
            log.info("Member updated successfully for id={}", id);
            System.out.println("Updated: " + result);

        } catch (IllegalArgumentException ex) {
            log.warn("Validation error while updating member id={}: {}", id, ex.getMessage());
            System.out.println("Could not update member: " + ex.getMessage());
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

    /**
     * Converts sentinel strings into a nullable value.
     */
    private static String parseOptionalString(String input) {
        if (input == null) return null;
        String trimmed = input.trim();
        if (trimmed.equalsIgnoreCase("NONE")) return null;
        return trimmed;
    }
}
