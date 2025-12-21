package controller;

import util.InputUtil;

// TODO (later): import service.MemberService;
// TODO (later): import service.Member;

public class MemberController {

    // TODO (later): private final MemberService memberService;

    public MemberController() {
        // TODO (later): this.memberService = new MemberService();
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

        // TODO (later):
        // memberService.findAll().forEach(System.out::println);

        System.out.println("(TODO) Service layer not implemented yet.");
    }

    private void addMember() {
        System.out.println();
        System.out.println("=== ADD MEMBER ===");

        String name = InputUtil.readString("Name: ");

        String email = InputUtil.readString("Email (blank for none): ");
        if (email != null && email.isBlank()) email = null;

        String phone = InputUtil.readString("Phone (blank for none): ");
        if (phone != null && phone.isBlank()) phone = null;

        // TODO (later):
        // Member member = new Member(name, email, phone);
        // Member saved = memberService.create(member);
        // System.out.println("Saved: " + saved);

        System.out.println("(TODO) Would create member: " + name);
    }

    private void findMemberById() {
        System.out.println();
        System.out.println("=== FIND MEMBER ===");

        int id = InputUtil.readInt("Member ID: ");

        // TODO (later):
        // memberService.findById(id)
        //     .ifPresentOrElse(
        //         System.out::println,
        //         () -> System.out.println("No member found with id=" + id)
        //     );

        System.out.println("(TODO) Would look up member with id=" + id);
    }

    private void updateMember() {
        System.out.println();
        System.out.println("=== UPDATE MEMBER ===");

        int id = InputUtil.readInt("Member ID to update: ");

        String newName = InputUtil.readString("New name (blank to keep): ");
        String newEmail = InputUtil.readString("New email (blank to keep, type NULL to clear): ");
        String newPhone = InputUtil.readString("New phone (blank to keep, type NULL to clear): ");

        // TODO (later):
        // var maybeMember = memberService.findById(id);
        // if (maybeMember.isEmpty()) { ... }
        // Member member = maybeMember.get();
        // if (!newName.isBlank()) member.setName(newName);
        // if (!newEmail.isBlank()) { if ("NULL".equalsIgnoreCase(newEmail)) member.setEmail(null); else member.setEmail(newEmail); }
        // if (!newPhone.isBlank()) { if ("NULL".equalsIgnoreCase(newPhone)) member.setPhone(null); else member.setPhone(newPhone); }
        // memberService.update(member);

        System.out.println("(TODO) Would update member id=" + id);
    }

    private void deleteMember() {
        System.out.println();
        System.out.println("=== DELETE MEMBER ===");

        int id = InputUtil.readInt("Member ID to delete: ");

        // TODO (later):
        // memberService.deleteById(id);

        System.out.println("(TODO) Would delete member id=" + id);
    }
}
