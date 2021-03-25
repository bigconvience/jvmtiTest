package com.lox.javascript;

import com.lox.clibrary.stdlib_h;

import java.util.*;

import static com.lox.javascript.DynBuf.*;
import static com.lox.javascript.JSFunctionDef.get_first_lexical_var;
import static com.lox.javascript.JSVarDefEnum.*;
import static com.lox.javascript.LoxJS.JS_MODE_STRICT;
import static com.lox.javascript.OPCodeEnum.*;
import static com.lox.javascript.PutLValueEnum.*;
import static com.lox.clibrary.stdlib_h.abort;
import static com.lox.javascript.TokenType.*;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {
  
static final int DECL_MASK_FUNC = (1 << 0); /* allow normal function declaration */
  /* ored with DECL_MASK_FUNC if function declarations are allowed with a label */
static final int DECL_MASK_FUNC_WITH_LABEL = (1 << 1);
static final int DECL_MASK_OTHER  = (1 << 2); /* all other declarations */
static final int DECL_MASK_ALL =  (DECL_MASK_FUNC | DECL_MASK_FUNC_WITH_LABEL | DECL_MASK_OTHER);
    
  private final Stack<Map<String, Boolean>> scopes = new Stack<>();
  JSFunctionDef cur_func;
  private DynBuf bc;
  final JSContext ctx;
  private final JSRuntime rt;
  int last_line_num;
  boolean is_module;

  Resolver(JSContext jsContext, JSFunctionDef fd) {
    ctx = jsContext;
    cur_func = fd;
    bc = new DynBuf();
    cur_func.byte_code = bc;
    rt = ctx.rt;
    last_line_num = 0;
  }

  private enum FunctionType {
    NONE,
    FUNCTION,
    INITIALIZER,
    METHOD
  }

  private enum ClassType {
    NONE,
    CLASS
  }

  private ClassType currentClass = ClassType.NONE;

  void resolve(List<Stmt> statements) {
    for (Stmt statement : statements) {

      resolve(statement);
    }
  }

  @Override
  public Void visitBlockStmt(Stmt.Block stmt) {
    emit_op(OPCodeEnum.OP_enter_scope);
    emit_u16(stmt.scope);
    resolve(stmt.statements);
    return null;
  }

  @Override
  public Void visitClassStmt(Stmt.Class stmt) {
    ClassType enclosingClass = currentClass;
    currentClass = ClassType.CLASS;

    declare(stmt.name);
    define(stmt.name);

    beginScope();
    scopes.peek().put("this", true);

    for (JSFunctionDef method : stmt.methods) {
      FunctionType declaration = FunctionType.METHOD;
      if (method.name.lexeme.equals("init")) {
        declaration = FunctionType.INITIALIZER;
      }

      resolveFunction(method, declaration); // [local]
    }

    endScope();

    currentClass = enclosingClass;
    return null;
  }

  @Override
  public Void visitExpressionStmt(Stmt.Expression stmt) {
    resolve(stmt.expression);
    Resolver s = this;
    if (cur_func.eval_ret_idx >= 0) {
            /* store the expression value so that it can be returned
               by eval() */
      emit_op(s, OP_put_loc);
      emit_u16(s, cur_func.eval_ret_idx);
    } else {
      emit_op(s, OPCodeEnum.OP_drop); /* drop the result */
    }
    return null;
  }

  @Override
  public Void visitFunctionStmt(JSFunctionDef stmt) {

    return null;
  }

  @Override
  public Void visitIfStmt(Stmt.If stmt) {
    int label1, label2, mask;
    Resolver s = this;
    push_scope(s);
    set_eval_ret_undefined(s);
    resolve(stmt.condition);
    label1 = emit_goto(s, OP_if_false, -1);
    if ((cur_func.js_mode & JS_MODE_STRICT) != 0) {
      mask = 0;
    } else {
      mask = DECL_MASK_FUNC;
    }
    resolve(stmt.thenBranch);
    if (stmt.elseBranch != null) {
      label2 = emit_goto(s, OP_goto, -1);

      emit_label(s, label1);
      resolve(stmt.elseBranch);
      label1 = label2;
    }
    emit_label(s, label1);
    pop_scope(s);
    return null;
  }

  @Override
  public Void visitPrintStmt(Stmt.Print stmt) {
    Expr expr = stmt.expression;
    expr.accept(this);

    emit_op(OPCodeEnum.OP_print);
    return null;
  }

  @Override
  public Void visitReturnStmt(Stmt.Return stmt) {


    if (stmt.value != null) {

      resolve(stmt.value);
    }

    return null;
  }

  @Override
  public Void visitVarStmt(Stmt.Var stmt) {
    JSFunctionDef s = cur_func;


    DynBuf bc_buf = bc;
    JSVarDefEnum varDef = stmt.varDef;
    Expr initializer = stmt.initializer;

    JSAtom name = stmt.name;
    int scope = stmt.scope;
    if (initializer != null) {

      if (varDef == JS_VAR_DEF_VAR) {
        emit_op(OPCodeEnum.OP_scope_get_var);
        emit_u32(name);
        emit_u16(scope);
        LValue lValue = LValue.get_lvalue(this, bc_buf, false, TOK_ASSIGN);
        initializer.accept(this);
        LValue.put_lvalue(this, lValue, PUT_LVALUE_NOKEEP, false);
      } else {
        initializer.accept(this);
        emit_op((varDef == JS_VAR_DEF_LET || varDef == JS_VAR_DEF_CONST)
          ? OPCodeEnum.OP_scope_put_var_init : OPCodeEnum.OP_scope_put_var);
        emit_u32(name);
        emit_u16(scope);
      }
    } else {
      if (varDef == JS_VAR_DEF_LET) {
        emit_op(OP_undefined);
        emit_op(OPCodeEnum.OP_scope_put_var_init);
        emit_u32(name);
        emit_u16(scope);
      }
    }

    return null;
  }

  @Override
  public Void visitWhileStmt(Stmt.While stmt) {
    resolve(stmt.condition);
    resolve(stmt.body);
    return null;
  }

  @Override
  public Void visitAssignExpr(Expr.Assign expr) {
    JSFunctionDef s = cur_func;
    Expr value = expr.value;
    Expr left = expr.left;
    left.accept(this);

    int tok = expr.operator.ordinal();

    DynBuf bc_buf = bc;

    if (tok == TOK_ASSIGN.ordinal()
      || tok >= TOK_MUL_ASSIGN.ordinal() && tok <= TOK_POW_ASSIGN.ordinal()) {
      LValue lValue = LValue.get_lvalue(this, bc_buf, tok != TOK_ASSIGN.ordinal(), TokenType.values()[tok]);
      value.accept(this);
      LValue.put_lvalue(this, lValue, PUT_LVALUE_KEEP_TOP, false);
    }
    return null;
  }

  @Override
  public Void visitConditionExpr(Expr.Condition expr) {
    resolve(expr.first);
    resolve(expr.middle);
    resolve(expr.last);
    return null;
  }

  @Override
  public Void visitBinaryExpr(Expr.Binary expr) {
    resolve(expr.left);
    resolve(expr.right);
    OPCodeEnum opcode = null;
    Token tok = expr.operator;
    switch (tok.type) {
      case TOK_PLUS:
        opcode = OPCodeEnum.OP_add;
        break;
      case TOK_MINUS:
        opcode = OPCodeEnum.OP_sub;
        break;
      case TOK_STAR:
        opcode = OPCodeEnum.OP_mul;
        break;
      case TOK_SLASH:
        opcode = OPCodeEnum.OP_div;
        break;
      case TOK_LT:
        opcode = OPCodeEnum.OP_lt;
        break;
      case TOK_LTE:
        opcode = OPCodeEnum.OP_lte;
        break;
      case TOK_GT:
        opcode = OPCodeEnum.OP_gt;
        break;
      case TOK_GTE:
        opcode = OPCodeEnum.OP_gte;
        break;
      case TOK_EQ:
        opcode = OPCodeEnum.OP_eq;
        break;
      case TOK_NEQ:
        opcode = OPCodeEnum.OP_neq;
        break;
      case TOK_STRICT_EQ:
        opcode = OPCodeEnum.OP_strict_eq;
        break;
      case TOK_STRICT_NEQ:
        opcode = OPCodeEnum.OP_strict_neq;
        break;
      default:
        stdlib_h.abort();
    }
    emit_op(opcode);
    return null;
  }

  @Override
  public Void visitCallExpr(Expr.Call expr) {
    resolve(expr.callee);

    for (Expr argument : expr.arguments) {
      resolve(argument);
    }

    return null;
  }

  @Override
  public Void visitGetExpr(Expr.Get expr) {
    resolve(expr.object);
    return null;
  }

  @Override
  public Void visitGroupingExpr(Expr.Grouping expr) {
    resolve(expr.expression);
    return null;
  }

  @Override
  public Void visitLiteralExpr(Expr.Literal expr) {
    Object val = expr.value;
    if (val instanceof JSAtom) {
      emit_op(OPCodeEnum.OP_push_atom_value);
      put_value(bc, val);
    } else if (val instanceof Integer) {
      emit_op(OPCodeEnum.OP_push_i32);
      emit_u32((Integer) val);
    } else if (val instanceof Boolean) {
      emit_op((Boolean) val ? OPCodeEnum.OP_push_true : OPCodeEnum.OP_push_false);
    }
    return null;
  }

  @Override
  public Void visitObjectLiteralExpr(Expr.ObjectLiteral expr) {
    return null;
  }

  @Override
  public Void visitLogicalExpr(Expr.Logical expr) {
    resolve(expr.left);
    resolve(expr.right);
    return null;
  }

  @Override
  public Void visitBitwiseExpr(Expr.Bitwise expr) {
    resolve(expr.left);
    resolve(expr.right);
    return null;
  }

  @Override
  public Void visitSetExpr(Expr.Set expr) {
    resolve(expr.value);
    resolve(expr.object);
    return null;
  }

  @Override
  public Void visitThisExpr(Expr.This expr) {
    if (currentClass == ClassType.NONE) {
      Lox.error(expr.keyword,
        "Cannot use 'this' outside of a class.");
      return null;
    }

    resolveLocal(expr, expr.keyword);
    return null;
  }

  @Override
  public Void visitUnaryExpr(Expr.Unary expr) {
    Resolver s = this;
    TokenType op = expr.operator.type;
    switch (op) {
      case TOK_PLUS:
      case TOK_MINUS:
      case TOK_BANG:
      case TOK_BITWISE_BANG:
      case TOK_VOID:
        resolve(expr.right);
        switch (op) {
          case TOK_MINUS:
            emit_op(s, OPCodeEnum.OP_neg);
            break;
          case TOK_PLUS:
            emit_op(s, OPCodeEnum.OP_plus);
            break;
          case TOK_BANG:
            emit_op(s, OPCodeEnum.OP_lnot);
            break;
          case TOK_BITWISE_BANG:
            emit_op(s, OPCodeEnum.OP_not);
            break;
          case TOK_VOID:
            emit_op(s, OPCodeEnum.OP_drop);
            emit_op(s, OP_undefined);
            break;
          default:
            abort();
        }
        break;
    }
    return null;
  }

  @Override
  public Void visitPostfixExpr(Expr.Postfix expr) {
    resolve(expr.left);
    return null;
  }

  @Override
  public Void visitVariableExpr(Expr.Variable expr) {
    JSAtom name = expr.name.ident_atom;
    int scope = expr.scope_level;
    emit_op(OPCodeEnum.OP_scope_get_var);
    emit_u32(name);
    emit_u16(scope);

    return null;
  }


  @Override
  public Void visitCoalesceExpr(Expr.Coalesce expr) {
    resolve(expr.left);
    resolve(expr.right);
    return null;
  }

  public void resolve() {
    resolve(cur_func.body);
    Resolver s = this;
    JSFunctionDef fd = cur_func;
    if (!s.is_module) {
      /* return the value of the hidden variable eval_ret_idx  */
      emit_op(s, OPCodeEnum.OP_get_loc);
      emit_u16(s, fd.eval_ret_idx);

      emit_op(s, OPCodeEnum.OP_return);
    } else {
      emit_op(s, OPCodeEnum.OP_return_undef);
    }
  }

  private void resolve(Stmt stmt) {
    last_line_num = stmt.line_number;
    stmt.accept(this);
  }

  private void resolve(Expr expr) {
    expr.accept(this);
  }

  private void resolveFunction(
    JSFunctionDef function, FunctionType type) {

    JSFunctionDef enclosureFunc = cur_func;
    cur_func = function;

    beginScope();
    for (Token param : function.params) {
      declare(param);
      define(param);
    }

    resolve(function.body);
    enter_scope(function, 1, null);
    endScope();
    cur_func = enclosureFunc;
  }

  private void beginScope() {
    scopes.push(new HashMap<String, Boolean>());
  }

  private void endScope() {
    scopes.pop();
  }

  private void declare(Token name) {
    if (scopes.isEmpty()) return;

    Map<String, Boolean> scope = scopes.peek();
    if (scope.containsKey(name.lexeme)) {
      Lox.error(name,
        "Variable with this name already declared in this scope.");
    }

    scope.put(name.lexeme, false);
  }

  private void define(Token name) {
    if (scopes.isEmpty()) return;
    scopes.peek().put(name.lexeme, true);
  }

  private void resolveLocal(Expr expr, Token name) {
    for (int i = scopes.size() - 1; i >= 0; i--) {
      if (scopes.get(i).containsKey(name.lexeme)) {
        return;
      }
    }

    // Not found. Assume it is global.
  }

  private void enter_scope(JSFunctionDef s, int scope, DynBuf bcOut) {
    if (scope == 1) {

    }

    for (int scopeIdx = s.scopes.get(scope).first; scopeIdx >= 0; ) {
      JSVarDef vd = s.vars.get(scopeIdx);
      if (vd.scope_level == scopeIdx) {
        if (JSFunctionDef.isFuncDecl(vd.var_kind)) {
          bcOut.dbuf_putc(OPCodeEnum.OP_fclosure);
          bcOut.dbuf_put_u32(vd.func_pool_or_scope_idx);
          bcOut.dbuf_putc(OP_put_loc);
        } else {
          bcOut.dbuf_putc(OPCodeEnum.OP_set_loc_uninitialized);
        }
        bcOut.dbuf_put_u16((short) scopeIdx);
        scopeIdx = vd.scope_next;
      } else {
        break;
      }
    }
  }

  static int emit_op(Resolver s, OPCodeEnum opCodeEnum) {
    return s.emit_op(opCodeEnum);
  }

  static int emit_op(Resolver s, byte val) {
    return s.emit_op(val);
  }

  static int emit_op(Resolver s, int opcode) {
    return s.emit_op((byte)(0xFF & opcode));
  }
  int emit_op(OPCodeEnum opCodeEnum) {
    return emit_op((byte) opCodeEnum.ordinal());
  }

  int emit_op(byte val) {
    Resolver s = this;
    JSFunctionDef fd = s.cur_func;
    DynBuf bc = s.cur_func.byte_code;

    if (fd.last_opcode_line_num != s.last_line_num) {
      bc.dbuf_putc(OPCodeEnum.OP_line_num);
      bc.dbuf_put_u32(s.last_line_num);
      fd.last_opcode_line_num = s.last_line_num;
    }
    fd.last_opcode_pos = bc.size;
    return bc.dbuf_putc(val);
  }

  public static int emit_label(Resolver s, int label) {
    return s.emit_label(label);
  }
  int emit_label(int label) {
    if (label >= 0) {
      emit_op(OPCodeEnum.OP_label);
      emit_u32(label);
      cur_func.label_slots.get(label).pos = cur_func.byte_code.size;
      return cur_func.byte_code.size - 4;
    } else {
      return -1;
    }
  }

  int emit_u32(JSAtom atom) {
    return emit_u32(atom.getVal());
  }

  int emit_u32(int val) {
    return bc.dbuf_put_u32(val);
  }

  int emit_u16(int val) {
    return bc.dbuf_put_u16(val);
  }

  static int emit_u16(Resolver s, int val) {
    return s.emit_u16(val);
  }

  static int emit_u32(Resolver s, int val) {
    return s.emit_u32(val);
  }


  static int new_label(Resolver s) {
    return s.cur_func.new_label_fd();
  }

  static int emit_goto(Resolver s, OPCodeEnum opcode, int label) {
    if (js_is_live_code(s)) {
      if (label < 0)
        label = new_label(s);
      emit_op(s, opcode);
      emit_u32(s, label);
      s.cur_func.label_slots.get(label).ref_count++;
      return label;
    }
    return -1;
  }

  static OPCodeEnum get_prev_opcode(JSFunctionDef fd) {
      return DynBuf.getOPCode(fd.byte_code.buf, fd.last_opcode_pos);
  }

  static boolean js_is_live_code(Resolver s) {
    switch (get_prev_opcode(s.cur_func)) {
      case OP_tail_call:
      case OP_tail_call_method:
      case OP_return:
      case OP_return_undef:
      case OP_return_async:
      case OP_throw:
      case OP_throw_var:
      case OP_goto:
      case OP_goto8:
      case OP_goto16:
      case OP_ret:
        return FALSE;
      default:
        return TRUE;
    }
  }

  static void set_eval_ret_undefined(Resolver s)
  {
    if (s.cur_func.eval_ret_idx >= 0) {
      emit_op(s, OP_undefined);
      emit_op(s, OP_put_loc);
      emit_u16(s, s.cur_func.eval_ret_idx);
    }
  }

  static int push_scope(Resolver s) {
    if (s.cur_func != null) {
      JSFunctionDef fd = s.cur_func;
      int scope = fd.add_scope();
      return scope;
    }
    return 0;
  }

  static void pop_scope(Resolver s) {
    if (s.cur_func != null) {
      /* disable scoped variables */
      JSFunctionDef fd = s.cur_func;
      int scope = fd.scope_level;
      emit_op(s, OP_leave_scope);
      emit_u16(s, scope);
      fd.scope_level = fd.scopes.get(scope).parent;
      fd.scope_first = get_first_lexical_var(fd, fd.scope_level);
    }
  }

}
