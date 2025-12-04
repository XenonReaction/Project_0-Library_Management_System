import util.InputUtil;

public class DebugMain {
    public static void main(String[] args) {
        System.out.println("Testing scanner...");
        int value = InputUtil.readInt("Enter a number: ");
        System.out.println("You typed: " + value);
    }
}
