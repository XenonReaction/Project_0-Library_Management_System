import util.DbConnectionUtil;
import util.InputUtil;

public class DebugMain {
    public static void main(String[] args) {
        System.out.println("Program started!");

        // This call forces class loading → static block executes
        try {
            DbConnectionUtil.getConnection();
            System.out.println("DB connection established!");
        } catch (Exception e) {
            System.out.println("DB connection failed!");
            e.printStackTrace();
        }
    }
}
