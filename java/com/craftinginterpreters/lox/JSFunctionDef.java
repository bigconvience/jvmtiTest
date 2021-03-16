package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.craftinginterpreters.lox.JSAtomEnum.JS_ATOM__default_;
import static com.craftinginterpreters.lox.JSContext.DEFINE_GLOBAL_LEX_VAR;
import static com.craftinginterpreters.lox.JS_PROP.JS_PROP_CONFIGURABLE;
import static com.craftinginterpreters.lox.JS_PROP.JS_PROP_WRITABLE;
import static com.craftinginterpreters.lox.LoxJS.JS_EVAL_TYPE_GLOBAL;
import static com.craftinginterpreters.lox.OPCodeEnum.*;
import static com.craftinginterpreters.lox.Parser.ARGUMENT_VAR_OFFSET;

/**
 * @author benpeng.jiang
 * @title: FunctionDef
 * @projectName LoxScript
 * @description: TODO
 * @date 2021/2/228:17 PM
 */ //< stmt-expression
//> stmt-function
public class JSFunctionDef extends Stmt {
  JSContext ctx;
  JSFunctionDef parent;
  int parent_cpool_idx;

  int parent_scope_level;
  final List<JSFunctionDef> child_list;

  final List<JSValue> cpool;
  final List<LabelSlot> label_slots;
  int last_opcode_line_num = -1;
  int last_opcode_pos = -1;

  final Token name = null;
  final List<Token> params;

  final List<JSVarScope> scopes;
  int scope_level;
  int scopeFirst;

  final Map<String, JSVarDef> varDefMap;
  final List<JSVarDef> vars;
  final List<JSVarDef> args;
  final List<JSHoistedDef> hoisted_def;
  final List<JSClosureVar> closure_var;
  final Map<String, JSHoistedDef> hoistDef;
  Stmt.Block body;
  int eval_type;
  boolean isEval;
  boolean is_global_var;
  JSVarScope curScope;
  DynBuf byte_code;

  JSAtom func_name;

  List<JumpSlot> jump_slots;
  int jump_size;
  int jump_count;

  List<LineNumberSlot> line_number_slots;
  int line_number_size;
  int line_number_count;
  int line_number_last;
  int line_number_last_pc;

  JSFunctionDef(JSFunctionDef parent,
                boolean isEval, boolean isFuncExpr, String filename, int lineNum) {
    this.parent = parent;
    this.isEval = isEval;

    params = new ArrayList<>();
    varDefMap = new HashMap<>();
    hoistDef = new HashMap<>();
    scopes = new ArrayList<>();
    vars = new ArrayList<>();
    hoisted_def = new ArrayList<>();
    args = new ArrayList<>();
    closure_var = new ArrayList<>();
    child_list = new ArrayList<>();
    cpool = new ArrayList<>();
    label_slots = new ArrayList<>();
    line_number_slots = new ArrayList<>();
    jump_slots = new ArrayList<>();
  }

  @Override
  <R> R accept(Visitor<R> visitor) {
    return visitor.visitFunctionStmt(this);
  }


  void addVarDef(String name, JSVarDef varDef) {
    varDefMap.put(name, varDef);
  }

  JSVarDef getVarDef(String name) {
    return varDefMap.get(name);
  }


  JSVarDef getArgDef(String name) {
    return varDefMap.get(name);
  }

  JSHoistedDef findHoistedDef(Token name) {
    return hoistDef.get(name.lexeme);
  }

  JSHoistedDef addHoistedDef(Token name) {
    JSHoistedDef hoistedDef = new JSHoistedDef();
    hoistedDef.name = name;
    hoistDef.put(name.lexeme, hoistedDef);
    return hoistedDef;
  }

  int getScopeCount() {
    return scopes.size();
  }

  int addScope() {
    int scope = getScopeCount();
    JSVarScope varScope = new JSVarScope();
    scopes.add(varScope);
    scope_level = scope;
    return scope;
  }

  public JSVarDef findLexicalDef(JSAtom varName) {
    JSVarScope scope = curScope;
    while (scope != null) {
      JSVarDef varDef = scope.get(varName);
      if (varDef != null && varDef.is_lexical) {
        return varDef;
      }
      scope = scope.prev;
    }

    if (isEval && eval_type == LoxJS.JS_EVAL_TYPE_GLOBAL) {
      return findLexicalHoistedDef(varName);
    }
    return null;
  }

  public JSHoistedDef findLexicalHoistedDef(JSAtom varName) {
    JSHoistedDef hoistedDef = findHoistedDef(varName);
    if (hoistedDef != null && hoistedDef.is_lexical) {
      return hoistedDef;
    }
    return null;
  }

  JSHoistedDef findHoistedDef(JSAtom varName) {
    for (JSHoistedDef hf : hoisted_def) {
      if (hf.var_name.equals(varName)) {
        return hf;
      }
    }
    return null;
  }

  public JSVarDef findVarInChildScope(JSAtom name) {
    for (JSVarDef vd : vars) {
      if (vd != null && vd.var_name.equals(name) && vd.scope_level == 0) {
        if (isChildScope(vd.func_pool_or_scope_idx, scope_level)) {
          return vd;
        }
        return vd;
      }
    }

    return null;
  }

  public boolean isChildScope(int scope, int parentScope) {
    while (scope > 0) {
      if (scope == parentScope) {
        return true;
      }
      scope = scopes.get(scope).parent;
    }
    return false;
  }

  public JSHoistedDef addHoistedDef(int cpoolIdx, JSAtom varName,
                                    int varIdx,
                                    boolean isLexical) {
    JSHoistedDef hf = new JSHoistedDef();
    hoisted_def.add(hf);
    hf.var_name = varName;
    hf.cpool_idx = cpoolIdx;
    hf.is_lexical = isLexical;
    hf.forceInit = false;
    hf.varIdx = varIdx;
    hf.scope_level = scope_level;
    return hf;
  }

  public int addScopeVar(JSAtom varName, JSVarKindEnum varKind) {
    int idx = addVar(varName);
    if (idx >= 0) {
      JSVarDef vd = vars.get(idx);
      vd.var_kind = varKind;
      vd.scope_level = scope_level;
      vd.scope_next = scopeFirst;
      curScope.first = idx;
      scopeFirst = idx;
    }

    return idx;
  }

  public int addVar(JSAtom varName) {
    JSVarDef vd = new JSVarDef();
    vars.add(vd);
    vd.var_name = varName;
    return vars.size() - 1;
  }

  public int findVar(JSAtom varName) {
    for (int i = 0; i < vars.size(); i++) {
      JSVarDef vd = vars.get(i);
      if (vd.var_name.equals(varName) && vd.scope_level == 0) {
        return i;
      }
    }

    return findArg(varName);
  }

  public int findArg(JSAtom varName) {
    for (int i = 0; i < args.size(); i++) {
      JSVarDef vd = args.get(i);
      if (vd.var_name.equals(varName)) {
        return i | Parser.ARGUMENT_VAR_OFFSET;
      }
    }
    return -1;
  }

  void enter_scope(int scope, DynBuf bcOut) {
    JSFunctionDef s = this;
    if (scope == 1) {
      instantiate_hoisted_definitions(bcOut);
    }

    for (int scopeIdx = s.scopes.get(scope).first; scopeIdx >= 0; ) {
      JSVarDef vd = s.vars.get(scopeIdx);
      if (vd.scope_level == scopeIdx) {
        if (isFuncDecl(vd.var_kind)) {
          bcOut.dbuf_putc(OP_fclosure);
          bcOut.dbuf_put_u32(vd.func_pool_or_scope_idx);
          bcOut.dbuf_putc(OP_put_loc);
        } else {
          bcOut.dbuf_putc(OP_set_loc_uninitialized);
        }
        bcOut.dbuf_put_u16((short) scopeIdx);
        scopeIdx = vd.scope_next;
      } else {
        break;
      }
    }
  }


  void instantiate_hoisted_definitions(DynBuf bc) {
    JSFunctionDef s = this;
    int i, idx, var_idx;
    for (i = 0; i < s.hoisted_def.size(); i++) {
      JSHoistedDef hf = s.hoisted_def.get(i);
      int has_closure = 0;
      boolean force_init = hf.forceInit;
      if (s.is_global_var && hf.var_name != JSAtom.JS_ATOM_NULL) {
        for (idx = 0; idx < s.closure_var.size(); idx++) {
          JSClosureVar cv = s.closure_var.get(idx);
          if (hf.var_name.equals(cv.var_name)) {
            has_closure = 2;
            force_init = false;
            break;
          }
        }
        if (has_closure == 0) {
          int flags = 0;
          if (s.eval_type != JS_EVAL_TYPE_GLOBAL) {
            flags |= JS_PROP_CONFIGURABLE;
          }

          if (hf.cpool_idx >= 0 && !hf.is_lexical) {
            bc.dbuf_putc(OP_fclosure);
            bc.dbuf_put_u32(hf.cpool_idx);
            bc.dbuf_putc(OP_define_func);
            bc.put_atom(hf.var_name);
            bc.dbuf_putc(flags);
            continue;
          } else {
            if (hf.is_lexical) {
              flags |= DEFINE_GLOBAL_LEX_VAR;
              if (!hf.isConst) {
                flags |= JS_PROP_WRITABLE;
              }
            }
            bc.dbuf_putc(OP_define_var);
            bc.put_atom(hf.var_name);
            bc.dbuf_putc(flags);
          }
        }

        if (hf.cpool_idx >= 0 || force_init) {
          if (hf.cpool_idx >= 0) {
            bc.dbuf_putc(OP_fclosure);
            bc.dbuf_put_u32(hf.cpool_idx);
            if (hf.var_name.getVal() == JS_ATOM__default_.ordinal()) {
              /* set default export function name */
              bc.dbuf_putc(OP_set_name);
              bc.put_atom(hf.var_name);
            }
          } else {
            bc.dbuf_putc(OP_undefined);
          }
          if (s.is_global_var) {
            if (has_closure == 2) {
              bc.dbuf_putc(OP_put_var_ref);
              bc.dbuf_put_u16(idx);
            } else if (has_closure == 1) {
              bc.dbuf_putc(OP_define_field);
              bc.put_atom(hf.var_name);
              bc.dbuf_putc(OP_drop);
            } else {
              /* XXX: Check if variable is writable and enumerable */
              bc.dbuf_putc(OP_put_var);
              bc.put_atom(hf.var_name);
            }
          } else {
            var_idx = hf.varIdx;
            if ((var_idx & ARGUMENT_VAR_OFFSET) != 0) {
              bc.dbuf_putc(OP_put_arg);
              bc.dbuf_put_u16(var_idx - ARGUMENT_VAR_OFFSET);
            } else {
              bc.dbuf_putc(OP_put_loc);
              bc.dbuf_put_u16(var_idx);
            }
          }
        }
      }
    }
    s.hoisted_def.clear();
  }

  public static boolean isFuncDecl(JSVarKindEnum varKind) {
    return varKind == JSVarKindEnum.JS_VAR_FUNCTION_DECL ||
      varKind == JSVarKindEnum.JS_VAR_NEW_FUNCTION_DECL;
  }

  int new_label() {
    return new_label(-1);
  }

  int update_label(int label, int delta) {
    LabelSlot ls = label_slots.get(label);
    ls.ref_count += delta;
    return ls.ref_count;
  }

  int new_label(int label) {
    LabelSlot ls;
    if (label < 0) {
      label = label_slots.size();
      ls = new LabelSlot();
      label_slots.add(ls);
      ls.ref_count = 0;
      ls.pos = -1;
      ls.pos2 = -1;
      ls.addr = -1;
    }
    return label;
  }


  OPCodeEnum get_prev_code() {
    if (last_opcode_pos < 0) {
      return OPCodeEnum.OP_invalid;
    }
    return OPCodeEnum.values()[byte_code.get_byte(last_opcode_pos)];
  }
}
