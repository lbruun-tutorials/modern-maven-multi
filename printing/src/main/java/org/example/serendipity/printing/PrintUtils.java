package org.example.serendipity.printing;

import org.example.serendipity.core.FormattingUtils;

public class PrintUtils {

    private PrintUtils() {}

    public static void print() {
        System.out.println(FormattingUtils.format("Hello"));
    }
}
