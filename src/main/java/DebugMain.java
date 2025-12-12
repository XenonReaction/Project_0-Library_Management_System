import util.DbConnectionUtil;
import util.InputUtil;

public class DebugMain {
    public static void main(String[] args) {
        System.out.println("Program started!");

        // Test DB Connection and Close
        try {
            DbConnectionUtil.getConnection();
            System.out.println("DB connection established!");
        } catch (Exception e) {
            System.out.println("DB connection failed!");
            e.printStackTrace();
        }

        try {
            DbConnectionUtil.closeConnection();
            System.out.println("DB connection closed!");
        } catch (Exception e) {
            System.out.println("DB connection close failed!");
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
    }
}
