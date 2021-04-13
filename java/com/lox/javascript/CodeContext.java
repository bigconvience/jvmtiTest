package com.lox.javascript;

/**
 * @author benpeng.jiang
 * @title: CodeContext
 * @projectName LoxScript
 * @description: TODO
 * @date 2021/3/12:00 PM
 */
public class CodeContext {
  byte[] bc_buf; /* code buffer */
  int bc_len;   /* length of the code buffer */
  int pos;      /* position past the matched code pattern */
  int line_num; /* last visited OP_line_num parameter or -1 */
  int op;
  int idx;
  int label;
  int val;
  JSAtom atom;


  static boolean code_match(CodeContext s, int pos, Object... args) {
    return false;
  }
}
