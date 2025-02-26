package org.example.moduley;

import org.example.modulex.FormattingUtils;

public class PrintUtils {

  private PrintUtils() {}

  public static void print() {
      System.out.println(FormattingUtils.format("Hello"));
  }
}
