package app;

import controller.MainMenuController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {

        // Visual separator between application runs (sessions)
        log.info("\n\n============================================================");
        log.info("               NEW APPLICATION SESSION STARTED               ");
        log.info("============================================================\n");

        new MainMenuController().start();
    }
}
