import util.DbConnectionUtil;
import util.InputUtil;
import repository.DAO.BookDAO;
import repository.entities.BookEntity;

public class DebugMain {
    public static void main(String[] args) {
        System.out.println("Program started!");

        // Test DB Connection
        try {
            DbConnectionUtil.getConnection();
            System.out.println("DB connection established!");
        } catch (Exception e) {
            System.out.println("DB connection failed!");
            e.printStackTrace();
        }


        // Test Scanner functions
        int num = InputUtil.readInt("Please input an integer:");
        System.out.println("User input: " + num);

        String input = InputUtil.readString("Please input a string:");
        System.out.println("User input: " + input);

        try {
            InputUtil.close();
            System.out.println("Scanner closed!");
        } catch (Exception e) {
            System.out.println("Scanner close failed!");
            e.printStackTrace();
        }


        // Test BookDAO and BookEntity
        BookDAO dao = new BookDAO();

        // CREATE
        BookEntity book = new BookEntity(
                "The Pragmatic Programmer",
                "Andrew Hunt",
                "978-0201616224",
                1999
        );

        dao.save(book);
        System.out.println("Saved: " + book);

        // READ
        System.out.println("\nAll books after insert:");
        dao.findAll().forEach(System.out::println);

        // DELETE
        dao.deleteById((int) book.getId());
        System.out.println("\nDeleted book with id = " + book.getId());

        // VERIFY DELETE
        System.out.println("\nAll books after delete:");
        dao.findAll().forEach(System.out::println);

        // Test Database Connection Close
        try {
            DbConnectionUtil.closeConnection();
            System.out.println("DB connection closed!");
        } catch (Exception e) {
            System.out.println("DB connection close failed!");
            e.printStackTrace();
        }

    }
}
