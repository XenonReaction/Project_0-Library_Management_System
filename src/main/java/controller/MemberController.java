package controller;

import service.MemberService;
import service.models.Member;
import util.InputUtil;

import java.util.List;
import java.util.Optional;

public class MemberController {

    private final MemberService memberService;

    public MemberController() {
        this.memberService = new MemberService();
    }

    // Optional: for unit tests (inject a mocked service)
    public MemberController(MemberService memberService) {
        if (memberService == null) throw new IllegalArgumentException("memberService cannot be null.");
        this.memberService = memberService;
    }

    public void handleInput() {
        boolean running = true;

        while (running) {
            printMenu();
            int choice = InputUtil.readInt("Make a choice: ");

            switch (choice) {
                case 1 -> listAllMembers();
                case 2 -> addMember();
                case 3 -> findMemberById();
                case 4 -> updateMember();
                case 5 -> deleteMember();
                case 0 -> running = false;
                default -> System.out.println("Invalid option. Please try again.");
            }
        }
    }

    private void printMenu() {
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
        System.out.println();
        System.out.println("=== ALL MEMBERS ===");

        try {
            List<Member> members = memberService.getAll();
            if (members.isEmpty()) {
                System.out.println("No members found.");
                return;
            }

            members.forEach(System.out::println);

        } catch (RuntimeException ex) {
            System.out.println("Error retrieving members: " + ex.getMessage());
        }
    }

    private void addMember() {
        System.out.println();
        System.out.println("=== ADD MEMBER ===");

        try {
            String name = InputUtil.readString("Name: ");

            // NOTE: InputUtil.readString() does NOT allow blank input.
            // Use sentinel values for optional fields.
            String emailInput = InputUtil.readString("Email (type NONE if not applicable): ");
            String phoneInput = InputUtil.readString("Phone (type NONE if not applicable): ");

            String email = parseOptionalString(emailInput);
            String phone = parseOptionalString(phoneInput);

            Member member = new Member(name, email, phone);
            Long id = memberService.create(member);

            System.out.println("Saved member with id=" + id);

        } catch (IllegalArgumentException ex) {
            System.out.println("Could not add member: " + ex.getMessage());
        } catch (RuntimeException ex) {
            System.out.println("Error adding member: " + ex.getMessage());
        }
    }

    private void findMemberById() {
        System.out.println();
        System.out.println("=== FIND MEMBER ===");

        long id = InputUtil.readInt("Member ID: ");

        try {
            Optional<Member> maybeMember = memberService.getById(id);
            if (maybeMember.isEmpty()) {
                System.out.println("No member found with id=" + id);
                return;
            }

            System.out.println(maybeMember.get());

        } catch (RuntimeException ex) {
            System.out.println("Error finding member: " + ex.getMessage());
        }
    }

    private void updateMember() {
        System.out.println();
        System.out.println("=== UPDATE MEMBER ===");

        long id = InputUtil.readInt("Member ID to update: ");

        try {
            Optional<Member> maybeExisting = memberService.getById(id);
            if (maybeExisting.isEmpty()) {
                System.out.println("No member found with id=" + id);
                return;
            }

            Member existing = maybeExisting.get();
            System.out.println("Current: " + existing);
            System.out.println();
            System.out.println("Update rules:");
            System.out.println("- For name: enter '-' to keep current.");
            System.out.println("- For email/phone: enter '-' to keep current, or 'NONE' to clear.");

            String nameInput = InputUtil.readString("New name: ");
            String emailInput = InputUtil.readString("New email: ");
            String phoneInput = InputUtil.readString("New phone: ");

            Member updated = new Member();
            updated.setName("-".equals(nameInput) ? existing.getName() : nameInput);

            if ("-".equals(emailInput)) updated.setEmail(existing.getEmail());
            else updated.setEmail(parseOptionalString(emailInput)); // "NONE" -> null

            if ("-".equals(phoneInput)) updated.setPhone(existing.getPhone());
            else updated.setPhone(parseOptionalString(phoneInput)); // "NONE" -> null

            Member result = memberService.update(id, updated);
            System.out.println("Updated: " + result);

        } catch (IllegalArgumentException ex) {
            System.out.println("Could not update member: " + ex.getMessage());
        } catch (RuntimeException ex) {
            System.out.println("Error updating member: " + ex.getMessage());
        }
    }

    private void deleteMember() {
        System.out.println();
        System.out.println("=== DELETE MEMBER ===");

        long id = InputUtil.readInt("Member ID to delete: ");

        try {
            boolean deleted = memberService.delete(id);
            if (deleted) {
                System.out.println("Deleted member id=" + id);
            } else {
                System.out.println("No member found with id=" + id + " (nothing deleted).");
            }
        } catch (RuntimeException ex) {
            System.out.println("Error deleting member: " + ex.getMessage());
        }
    }

    /**
     * Converts sentinel strings into a nullable value.
     * Because InputUtil.readString() disallows blank, we use "NONE" to mean null.
     */
    private static String parseOptionalString(String input) {
        if (input == null) return null;
        String trimmed = input.trim();
        if (trimmed.equalsIgnoreCase("NONE")) return null;
        return trimmed;
    }
}
