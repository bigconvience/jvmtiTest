package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class JSAtom {
  static final int JS_ATOM_TYPE_STRING = 0;
  static final int JS_ATOM_TYPE_GLOBAL_SYMBOL = 1;
  static final int JS_ATOM_TYPE_SYMBOL = 2;
  static final int JS_ATOM_TYPE_PRIVATE = 3;

  static final int JS_ATOM_TAG_INT = (1 << 31);
  static final int JS_ATOM_MAX_INT = (JS_ATOM_TAG_INT - 1);
  static final int JS_ATOM_MAX = ((1 << 30) - 1);

  boolean __JS_AtomIsTaggedInt() {
    return (getVal() & JS_ATOM_TAG_INT) != 0;
  }

  int __JS_AtomToUInt32() {
    return getVal() & ~JS_ATOM_TAG_INT;
  }

  static final JSAtom JS_ATOM_NULL = new JSAtom(0);
  static final JSAtom JS_ATOM_empty_string = new JSAtom(JSAtomEnum.JS_ATOM_empty_string.ordinal());

  private final int val;

  public JSAtom(int val) {
    this.val = val;
  }

  public int getVal() {
    return val;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    JSAtom atom = (JSAtom) o;
    return val == atom.val;
  }

  @Override
  public int hashCode() {
    return Objects.hash(val);
  }

  static List<String> js_atom_init;

  static {
    js_atom_init = new ArrayList<>();

    js_atom_init.add(null);
    js_atom_init.add("null");
    js_atom_init.add("false");
    js_atom_init.add("true");
    js_atom_init.add("if");
    js_atom_init.add("else");
    js_atom_init.add("return");
    js_atom_init.add("var");
    js_atom_init.add("this");
    js_atom_init.add("delete");
    js_atom_init.add("void");
    js_atom_init.add("typeof");
    js_atom_init.add("new");
    js_atom_init.add("in");
    js_atom_init.add("instanceof");
    js_atom_init.add("do");
    js_atom_init.add("while");
    js_atom_init.add("for");
    js_atom_init.add("break");
    js_atom_init.add("continue");
    js_atom_init.add("switch");
    js_atom_init.add("case");
    js_atom_init.add("default");
    js_atom_init.add("throw");
    js_atom_init.add("try");
    js_atom_init.add("catch");
    js_atom_init.add("finally");
    js_atom_init.add("function");
    js_atom_init.add("debugger");
    js_atom_init.add("with");
    js_atom_init.add("class");
    js_atom_init.add("const");
    js_atom_init.add("enum");
    js_atom_init.add("export");
    js_atom_init.add("extends");
    js_atom_init.add("import");
    js_atom_init.add("super");
    js_atom_init.add("implements");
    js_atom_init.add("interface");
    js_atom_init.add("let");
    js_atom_init.add("package");
    js_atom_init.add("private");
    js_atom_init.add("protected");
    js_atom_init.add("public");
    js_atom_init.add("static");
    js_atom_init.add("yield");
    js_atom_init.add("await");
    js_atom_init.add("");
    js_atom_init.add("length");
    js_atom_init.add("fileName");
    js_atom_init.add("lineNumber");
    js_atom_init.add("message");
    js_atom_init.add("errors");
    js_atom_init.add("stack");
    js_atom_init.add("name");
    js_atom_init.add("toString");
    js_atom_init.add("toLocaleString");
    js_atom_init.add("valueOf");
    js_atom_init.add("eval");
    js_atom_init.add("prototype");
    js_atom_init.add("constructor");
    js_atom_init.add("configurable");
    js_atom_init.add("writable");
    js_atom_init.add("enumerable");
    js_atom_init.add("value");
    js_atom_init.add("get");
    js_atom_init.add("set");
    js_atom_init.add("of");
    js_atom_init.add("__proto__");
    js_atom_init.add("undefined");
    js_atom_init.add("number");
    js_atom_init.add("boolean");
    js_atom_init.add("string");
    js_atom_init.add("object");
    js_atom_init.add("symbol");
    js_atom_init.add("integer");
    js_atom_init.add("unknown");
    js_atom_init.add("arguments");
    js_atom_init.add("callee");
    js_atom_init.add("caller");
    js_atom_init.add("<eval>");
    js_atom_init.add("<ret>");
    js_atom_init.add("<var>");
    js_atom_init.add("<with>");
    js_atom_init.add("lastIndex");
    js_atom_init.add("target");
    js_atom_init.add("index");
    js_atom_init.add("input");
    js_atom_init.add("defineProperties");
    js_atom_init.add("apply");
    js_atom_init.add("join");
    js_atom_init.add("concat");
    js_atom_init.add("split");
    js_atom_init.add("construct");
    js_atom_init.add("getPrototypeOf");
    js_atom_init.add("setPrototypeOf");
    js_atom_init.add("isExtensible");
    js_atom_init.add("preventExtensions");
    js_atom_init.add("has");
    js_atom_init.add("deleteProperty");
    js_atom_init.add("defineProperty");
    js_atom_init.add("getOwnPropertyDescriptor");
    js_atom_init.add("ownKeys");
    js_atom_init.add("add");
    js_atom_init.add("done");
    js_atom_init.add("next");
    js_atom_init.add("values");
    js_atom_init.add("source");
    js_atom_init.add("flags");
    js_atom_init.add("global");
    js_atom_init.add("unicode");
    js_atom_init.add("raw");
    js_atom_init.add("new.target");
    js_atom_init.add("this.active_func");
    js_atom_init.add("<home_object>");
    js_atom_init.add("<computed_field>");
    js_atom_init.add("<static_computed_field>");
    js_atom_init.add("<class_fields_init>");
    js_atom_init.add("<brand>");
    js_atom_init.add("#constructor");
    js_atom_init.add("as");
    js_atom_init.add("from");
    js_atom_init.add("meta");
    js_atom_init.add("*default*");
    js_atom_init.add("*");
    js_atom_init.add("Module");
    js_atom_init.add("then");
    js_atom_init.add("resolve");
    js_atom_init.add("reject");
    js_atom_init.add("promise");
    js_atom_init.add("proxy");
    js_atom_init.add("revoke");
    js_atom_init.add("async");
    js_atom_init.add("exec");
    js_atom_init.add("groups");
    js_atom_init.add("status");
    js_atom_init.add("reason");
    js_atom_init.add("globalThis");
    js_atom_init.add("bigint");
    js_atom_init.add("bigfloat");
    js_atom_init.add("bigdecimal");
    js_atom_init.add("roundingMode");
    js_atom_init.add("maximumSignificantDigits");
    js_atom_init.add("maximumFractionDigits");
    js_atom_init.add("not-equal");
    js_atom_init.add("timed-out");
    js_atom_init.add("ok");
    js_atom_init.add("toJSON");
    js_atom_init.add("Object");
    js_atom_init.add("Array");
    js_atom_init.add("Error");
    js_atom_init.add("Number");
    js_atom_init.add("String");
    js_atom_init.add("Boolean");
    js_atom_init.add("Symbol");
    js_atom_init.add("Arguments");
    js_atom_init.add("Math");
    js_atom_init.add("JSON");
    js_atom_init.add("Date");
    js_atom_init.add("Function");
    js_atom_init.add("GeneratorFunction");
    js_atom_init.add("ForInIterator");
    js_atom_init.add("RegExp");
    js_atom_init.add("ArrayBuffer");
    js_atom_init.add("SharedArrayBuffer");
    js_atom_init.add("Uint8ClampedArray");
    js_atom_init.add("Int8Array");
    js_atom_init.add("Uint8Array");
    js_atom_init.add("Int16Array");
    js_atom_init.add("Uint16Array");
    js_atom_init.add("Int32Array");
    js_atom_init.add("Uint32Array");
    js_atom_init.add("BigInt64Array");
    js_atom_init.add("BigUint64Array");
    js_atom_init.add("Float32Array");
    js_atom_init.add("Float64Array");
    js_atom_init.add("DataView");
    js_atom_init.add("BigInt");
    js_atom_init.add("BigFloat");
    js_atom_init.add("BigFloatEnv");
    js_atom_init.add("BigDecimal");
    js_atom_init.add("OperatorSet");
    js_atom_init.add("Operators");
    js_atom_init.add("Map");
    js_atom_init.add("Set");
    js_atom_init.add("WeakMap");
    js_atom_init.add("WeakSet");
    js_atom_init.add("Map Iterator");
    js_atom_init.add("Set Iterator");
    js_atom_init.add("Array Iterator");
    js_atom_init.add("String Iterator");
    js_atom_init.add("RegExp String Iterator");
    js_atom_init.add("Generator");
    js_atom_init.add("Proxy");
    js_atom_init.add("Promise");
    js_atom_init.add("PromiseResolveFunction");
    js_atom_init.add("PromiseRejectFunction");
    js_atom_init.add("AsyncFunction");
    js_atom_init.add("AsyncFunctionResolve");
    js_atom_init.add("AsyncFunctionReject");
    js_atom_init.add("AsyncGeneratorFunction");
    js_atom_init.add("AsyncGenerator");
    js_atom_init.add("EvalError");
    js_atom_init.add("RangeError");
    js_atom_init.add("ReferenceError");
    js_atom_init.add("SyntaxError");
    js_atom_init.add("TypeError");
    js_atom_init.add("URIError");
    js_atom_init.add("InternalError");
    js_atom_init.add("<brand>");
    js_atom_init.add("Symbol.toPrimitive");
    js_atom_init.add("Symbol.iterator");
    js_atom_init.add("Symbol.match");
    js_atom_init.add("Symbol.matchAll");
    js_atom_init.add("Symbol.replace");
    js_atom_init.add("Symbol.search");
    js_atom_init.add("Symbol.split");
    js_atom_init.add("Symbol.toStringTag");
    js_atom_init.add("Symbol.isConcatSpreadable");
    js_atom_init.add("Symbol.hasInstance");
    js_atom_init.add("Symbol.species");
    js_atom_init.add("Symbol.unscopables");
    js_atom_init.add("Symbol.asyncIterator");
    js_atom_init.add("Symbol.operatorSet");
  }
}