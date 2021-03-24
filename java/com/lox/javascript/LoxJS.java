package com.lox.javascript;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

import static com.lox.javascript.JSClassID.JS_CLASS_OBJECT;
import static com.lox.javascript.JSStdClassDef.js_std_class_def;

/**
 * @author benpeng.jiang
 * @title: JSEval
 * @projectName LoxScript
 * @description: TODO
 * @date 2021/1/512:41 AM
 */
public class LoxJS {
  public static final int JS_EVAL_TYPE_GLOBAL = 0;
  public static final int JS_EVAL_TYPE_MODULE = 1;
  public static final int JS_EVAL_TYPE_DIRECT = 2;
  public static final int JS_EVAL_TYPE_INDIRECT = 3;
  public static final int JS_EVAL_TYPE_MASK = 3;

  public static final int JS_MODE_STRICT = (1 << 0);
  public static final int JS_MODE_STRIP  = (1 << 1);
  public static final int JS_MODE_MATH   = (1 << 2);

  public static int evalFile(JSContext ctx, String filename, boolean module) throws IOException {
    int ret, evalFlags;
    byte[] bytes = Files.readAllBytes(Paths.get(filename));
    if (module) {
      evalFlags = JS_EVAL_TYPE_MODULE;
    } else {
      evalFlags = JS_EVAL_TYPE_GLOBAL;
    }
    ret = evalBuf(ctx, bytes, filename, evalFlags);
    return ret;
  }

  public static int evalBuf(JSContext ctx, byte[] bytes, String filename, int evalFlags) {
    JSValue val;
    int ret;
    String buf = new String(bytes, Charset.defaultCharset());
    val = JSEval(ctx, buf, filename, evalFlags);
    ret = 0;
    return ret;
  }

  public static JSValue JSEval(JSContext ctx, String input, String filename, int evalFlags) {
    JSValue ret;
    ret = JSEvalInternal(ctx, ctx.global_obj, input, filename, evalFlags, -1);
    return ret;
  }

  public static JSValue JSEvalInternal(JSContext ctx, JSValue thisObject, String input, String filename, int flags, int scope_idx) {
    return ctx.__JS_evalInternal(thisObject, input, filename, flags, scope_idx);
  }

  public JSRuntime JS_NewRuntime() {
    return JS_NewRuntime2();
  }

  JSRuntime JS_NewRuntime2() {
    JSRuntime rt = new JSRuntime();
    rt.init_class_range(js_std_class_def, JS_CLASS_OBJECT.ordinal(), js_std_class_def.length);

    return rt;
  }

  public static void JS_DumpMemoryUsage(PrintStream stdout, final JSMemoryUsage s, JSRuntime rt)
  {

  }

}