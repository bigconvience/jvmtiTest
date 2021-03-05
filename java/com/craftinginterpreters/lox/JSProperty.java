package com.craftinginterpreters.lox;

/**
 * @author benpeng.jiang
 * @title: JSProperty
 * @projectName LoxScript
 * @description: TODO
 * @date 2021/3/212:11 AM
 */
public class JSProperty {
  /* flags for object properties */
  static final int JS_PROP_CONFIGURABLE = (1 << 0);
  static final int JS_PROP_WRITABLE = (1 << 1);
  static final int JS_PROP_ENUMERABLE = (1 << 2);
  static final int JS_PROP_C_W_E = (JS_PROP_CONFIGURABLE | JS_PROP_WRITABLE | JS_PROP_ENUMERABLE);
  static final int JS_PROP_LENGTH = (1 << 3); /* used internally in Arrays */
  static final int JS_PROP_TMASK = (3 << 4); /* mask for NORMAL, GETSET, VARREF, AUTOINIT */
  static final int JS_PROP_NORMAL = (0 << 4);
  static final int JS_PROP_GETSET = (1 << 4);
  static final int JS_PROP_VARREF = (2 << 4); /* used internally */
  static final int JS_PROP_AUTOINIT = (3 << 4); /* used internally */

  /* flags for JS_DefineProperty */
  static final int JS_PROP_HAS_SHIFT = 8;
  static final int JS_PROP_HAS_CONFIGURABLE = (1 << 8);
  static final int JS_PROP_HAS_WRITABLE = (1 << 9);
  static final int JS_PROP_HAS_ENUMERABLE = (1 << 10);
  static final int JS_PROP_HAS_GET = (1 << 11);
  static final int JS_PROP_HAS_SET = (1 << 12);
  static final int JS_PROP_HAS_VALUE = (1 << 13);
}