package com.audit.menu;

public class MenuHelper {

    public static final String RESET  = "\u001B[0m";
    public static final String BOLD   = "\u001B[1m";
    public static final String CYAN   = "\u001B[36m";
    public static final String GREEN  = "\u001B[32m";
    public static final String RED    = "\u001B[31m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE   = "\u001B[34m";
    public static final String WHITE  = "\u001B[37m";

    public static void printBanner() {
        System.out.println(CYAN + BOLD);
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║        SECURE AUDIT LOGGING & MONITORING SYSTEM          ║");
        System.out.println("║         (OWASP A10 - Insufficient Logging)               ║");
        System.out.println("║          Symbiosis Institute of Technology, Pune         ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println(RESET);
    }

    public static void printHeader(String title) {
        System.out.println();
        System.out.println(BLUE + BOLD + "─".repeat(60));
        System.out.println("  " + title);
        System.out.println("─".repeat(60) + RESET);
    }

    public static void success(String msg) {
        System.out.println(GREEN + "[✔] " + msg + RESET);
    }

    public static void error(String msg) {
        System.out.println(RED + "[✘] " + msg + RESET);
    }

    public static void info(String msg) {
        System.out.println(YELLOW + "[i] " + msg + RESET);
    }

    public static void printResult(String result) {
        if (result.startsWith("SUCCESS"))  success(result.replace("SUCCESS: ", ""));
        else if (result.startsWith("ERROR") || result.startsWith("DB ERROR")) error(result);
        else System.out.println(result);
    }

    public static void tableHeader(String cols) {
        System.out.println(WHITE + BOLD + cols + RESET);
        System.out.println("-".repeat(cols.length()));
    }

    public static void noData() {
        info("No records found.");
    }
}
