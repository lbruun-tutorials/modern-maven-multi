package org.example.printing;

import org.example.core.FormattingUtils;

public class PrintUtils {

    private PrintUtils() {}

    public static void print() {
        System.out.println(FormattingUtils.format("Hello"));
    }
}
