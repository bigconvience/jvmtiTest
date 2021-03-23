package com.craftinginterpreters.lox;

/**
 * @author benpeng.jiang
 * @title: TextUtils
 * @projectName LoxScript
 * @description: TODO
 * @date 2021/3/228:22 PM
 */
public class TextUtils {
  /**
   * Returns true if the string is null or 0-length.
   * @param str the string to be examined
   * @return true if str is null or zero length
   */
  public static boolean isEmpty(CharSequence str) {
    return str == null || str.length() == 0;
  }
}