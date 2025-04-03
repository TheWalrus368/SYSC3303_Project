public class Print {
    public static final String ANSI_BLUE = "\u001B[34m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_RED = "\u001B[31m";

    public static final String ANSI_RESET = "\u001B[0m\n";

    public static void blue(String message) {
        System.out.print(ANSI_BLUE + message + ANSI_RESET);
    }

    public static void green(String message) {
        System.out.print(ANSI_GREEN + message + ANSI_RESET);
    }
    public static void yellow(String message) {
        System.out.print(ANSI_YELLOW + message + ANSI_RESET);
    }

    public static void red(String message) {
        System.out.print(ANSI_RED + message + ANSI_RESET);
    }
}
