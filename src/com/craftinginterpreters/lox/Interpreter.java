package com.craftinginterpreters.lox;

import java.util.*;

import static com.craftinginterpreters.lox.TokenType.*;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
  final Environment globals = new Environment();
  private Environment environment = globals;
  private final Map<Expr, Integer> locals = new HashMap<>();

  Interpreter() {
    globals.define("clock", new LoxCallable() {
      @Override
      public int arity() {
        return 0;
      }

      @Override
      public Object call(Interpreter interpreter,
                         List<Object> arguments) {
        return (double) System.currentTimeMillis() / 1000.0;
      }

      @Override
      public String toString() {
        return "<native fn>";
      }
    });
  }

  void interpret(List<Stmt> statements) {
    try {
      for (Stmt statement : statements) {
        execute(statement);
      }
    } catch (RuntimeError error) {
      Lox.runtimeError(error);
    }
  }

  private Object evaluate(Expr expr) {
    return expr.accept(this);
  }

  private void execute(Stmt stmt) {
    stmt.accept(this);
  }

  void resolve(Expr expr, int depth) {
    locals.put(expr, depth);
  }

  void executeBlock(List<Stmt> statements, Environment environment) {
    Environment previous = this.environment;
    try {
      this.environment = environment;

      for (Stmt statement : statements) {
        execute(statement);
      }
    } finally {
      this.environment = previous;
    }
  }

  @Override
  public Void visitBlockStmt(Stmt.Block stmt) {
    executeBlock(stmt.statements, new Environment(environment));
    return null;
  }

  @Override
  public Void visitClassStmt(Stmt.Class stmt) {
    environment.define(stmt.name.lexeme, null);

    Map<String, LoxFunction> methods = new HashMap<>();
    for (Stmt.Function method : stmt.methods) {
      LoxFunction function = new LoxFunction(method, environment,
        method.name.lexeme.equals("init"));
      methods.put(method.name.lexeme, function);
    }

    LoxClass klass = new LoxClass(stmt.name.lexeme, methods);
    environment.assign(stmt.name, klass);
    return null;
  }

  @Override
  public Void visitExpressionStmt(Stmt.Expression stmt) {
    evaluate(stmt.expression);
    return null; // [void]
  }

  @Override
  public Void visitFunctionStmt(Stmt.Function stmt) {
    LoxFunction function = new LoxFunction(stmt, environment, false);
    environment.define(stmt.name.lexeme, function);
    return null;
  }

   public void evalFunction(Stmt.Function stmt) {
     LoxFunction function = new LoxFunction(stmt, globals, false);
     function.call(this, Collections.emptyList());
  }

  @Override
  public Void visitIfStmt(Stmt.If stmt) {
    if (isTruthy(evaluate(stmt.condition))) {
      execute(stmt.thenBranch);
    } else if (stmt.elseBranch != null) {
      execute(stmt.elseBranch);
    }
    return null;
  }

  @Override
  public Void visitPrintStmt(Stmt.Print stmt) {
    Object value = evaluate(stmt.expression);
    System.out.println(stringify(value));
    return null;
  }

  @Override
  public Void visitReturnStmt(Stmt.Return stmt) {
    Object value = null;
    if (stmt.value != null) value = evaluate(stmt.value);

    throw new Return(value);
  }

  @Override
  public Void visitVarStmt(Stmt.Var stmt) {
    Object value = null;
    if (stmt.initializer != null) {
      value = evaluate(stmt.initializer);
    }

    environment.define(stmt.name.lexeme, value);
    return null;
  }

  @Override
  public Void visitWhileStmt(Stmt.While stmt) {
    while (isTruthy(evaluate(stmt.condition))) {
      execute(stmt.body);
    }
    return null;
  }

  @Override
  public Object visitAssignExpr(Expr.Assign expr) {
    Object value = null;
    Object lvalue = null;
    TokenType op = expr.operator;


    if (op == TOK_DOUBLE_QUESTION_MARK_ASSIGN) {
      lvalue = evaluate(expr.left);
    } else if (op == TOK_ASSIGN) {
      value = evaluate(expr.value);
    } else  {
      value = evaluate(expr.value);
      lvalue = evaluate(expr.left);
    }

    switch (op) {
      case TOK_MUL_ASSIGN:
        checkNumberOperands(expr.name, value, lvalue);
        value = (double) lvalue * (double) value;
        break;
      case TOK_DIV_ASSIGN:
        if (value instanceof Number && lvalue instanceof Number) {
          value = (double) lvalue / (double) value;
        }
        break;
      case TOK_MOD_ASSIGN:
        if (value instanceof Number && lvalue instanceof Number) {
          value = (double) lvalue % (double) value;
        }
        break;
      case TOK_PLUS_ASSIGN:
        if (value instanceof Number && lvalue instanceof Number) {
          value = (double) lvalue + (double) value;
        }
        break;
      case TOK_MINUS_ASSIGN:
        if (value instanceof Number && lvalue instanceof Number) {
          value = (double) lvalue - (double) value;
        }
        break;
      case TOK_SHL_ASSIGN:
        if (value instanceof Number && lvalue instanceof Number) {
          value = (int) lvalue << (int) value;
        }
        break;
      case TOK_SAR_ASSIGN:
        if (value instanceof Number && lvalue instanceof Number) {
          value = (int) lvalue >> (int) value;
        }
        break;
      case TOK_SHR_ASSIGN:
        if (value instanceof Number && lvalue instanceof Number) {
          value = (int) lvalue >>> (int) value;
        }
        break;

      case TOK_AND_ASSIGN:
        if (value instanceof Boolean && lvalue instanceof Boolean) {
          value = (boolean) lvalue & (boolean) value;
        }
        break;

      case TOK_XOR_ASSIGN:
        if (value instanceof Boolean && lvalue instanceof Boolean) {
          value = (boolean) lvalue ^ (boolean) value;
        }
        break;

      case TOK_OR_ASSIGN:
        if (value instanceof Boolean && lvalue instanceof Boolean) {
          value = (boolean) lvalue | (boolean) value;
        }
        break;

      case TOK_POW_ASSIGN:
        if (value instanceof Number && lvalue instanceof Number) {
          value = Math.pow((double) lvalue, (double) value);
        }
        break;

      case TOK_LAND_ASSIGN:
        if (value instanceof Boolean && lvalue instanceof Boolean) {
          value = (boolean) lvalue && (boolean) value;
        }
        break;

      case TOK_LOR_ASSIGN:
        if (value instanceof Boolean && lvalue instanceof Boolean) {
          value = (boolean) lvalue || (boolean) value;
        }
        break;

      case TOK_DOUBLE_QUESTION_MARK_ASSIGN:
        if (lvalue == null) {
          value = evaluate(expr.value);
        }
        break;
      default:
        break;
    }

    Integer distance = locals.get(expr);
    environment.assignAt(distance, expr.name, value);

    return value;
  }

  @Override
  public Object visitConditionExpr(Expr.Condition expr) {
    Object first = evaluate(expr.first);
    return isTruthy(first) ? evaluate(expr.middle) : evaluate(expr.last);
  }

  @Override
  public Object visitCoalesceExpr(Expr.Coalesce expr) {
    Object left = evaluate(expr.left);
    return left != null ? left : evaluate(expr.right);
  }

  @Override
  public Object visitBinaryExpr(Expr.Binary expr) {
    Object left = evaluate(expr.left);
    Object right = evaluate(expr.right); // [left]

    switch (expr.operator.type) {
      case TOK_NEQ:
        return !isEqual(left, right);
      case TOK_EQ:
        return isEqual(left, right);
      case TOK_STRICT_EQ:
        return isStrictEqual(left, right);
      case TOK_STRICT_NEQ:
        return !isStrictEqual(left, right);
      case TOK_GT:
        checkNumberOperands(expr.operator, left, right);
        return (double) left > (double) right;
      case TOK_GTE:
        checkNumberOperands(expr.operator, left, right);
        return (double) left >= (double) right;
      case TOK_LT:
        checkNumberOperands(expr.operator, left, right);
        return (double) left < (double) right;
      case TOK_LTE:
        checkNumberOperands(expr.operator, left, right);
        return (double) left <= (double) right;
      case TOK_SHL:
        checkNumberOperands(expr.operator, left, right);
        return Utils.toInt(left) << Utils.toInt(right);
      case TOK_SAR:
        checkNumberOperands(expr.operator, left, right);
        return Utils.toInt(left) >> Utils.toInt(right);
      case TOK_SHR:
        checkNumberOperands(expr.operator, left, right);
        return Utils.toInt(left) >>> Utils.toInt(right);
      case MINUS:
        checkNumberOperands(expr.operator, left, right);
        return (double) left - (double) right;
      case PLUS:
        if (left instanceof Double && right instanceof Double) {
          return (double) left + (double) right;
        } // [plus]

        if (left instanceof String && right instanceof String) {
          return (String) left + (String) right;
        }

        throw new RuntimeError(expr.operator,
          "Operands must be two numbers or two strings.");
      case SLASH:
        checkNumberOperands(expr.operator, left, right);
        return (double) left / (double) right;
      case TOK_STAR:
        checkNumberOperands(expr.operator, left, right);
        return (double) left * (double) right;
      case TOK_MOD:
        checkNumberOperands(expr.operator, left, right);
        return (double) left % (double) right;
      case TOK_POW:
        checkNumberOperands(expr.operator, left, right);
        return Math.pow((double) left, (double) right);
    }

    // Unreachable.
    return null;
  }

  @Override
  public Object visitCallExpr(Expr.Call expr) {
    Object callee = evaluate(expr.callee);

    List<Object> arguments = new ArrayList<>();
    for (Expr argument : expr.arguments) { // [in-order]
      arguments.add(evaluate(argument));
    }

    if (!(callee instanceof LoxCallable)) {
      throw new RuntimeError(expr.paren,
        "Can only call functions and classes.");
    }

    LoxCallable function = (LoxCallable) callee;
    if (arguments.size() != function.arity()) {
      throw new RuntimeError(expr.paren, "Expected " +
        function.arity() + " arguments but got " +
        arguments.size() + ".");
    }

    return function.call(this, arguments);
  }

  @Override
  public Object visitGetExpr(Expr.Get expr) {
    Object object = evaluate(expr.object);
    if (object instanceof LoxInstance) {
      return ((LoxInstance) object).get(expr.name);
    }

    if (object instanceof JSObject) {
      return ((JSObject) object).getValue(expr.name.lexeme);
    }

    throw new RuntimeError(expr.name,
      "Only instances have properties.");
  }

  @Override
  public Object visitGroupingExpr(Expr.Grouping expr) {
    return evaluate(expr.expression);
  }

  @Override
  public Object visitLiteralExpr(Expr.Literal expr) {
    return expr.value;
  }

  @Override
  public Object visitObjectLiteralExpr(Expr.ObjectLiteral expr) {
    JSObject jsObject = new JSObject();
    for(Map.Entry<String, Expr> entry: expr.prop.entrySet()) {
     Object value = entry.getValue().accept(this);
     jsObject.defineProperty(entry.getKey(), value);
    }
    return jsObject;
  }

  @Override
  public Object visitLogicalExpr(Expr.Logical expr) {
    Object left = evaluate(expr.left);

    if (expr.operator.type == TokenType.TOK_LOR) {
      if (isTruthy(left)) return left;
    } else {
      if (!isTruthy(left)) return left;
    }

    return evaluate(expr.right);
  }

  @Override
  public Object visitBitwiseExpr(Expr.Bitwise expr) {
    Object left = evaluate(expr.left);
    Object right = evaluate(expr.right);
    checkNumberOperands(expr.operator, left, right);
    int leftInt = Utils.toInt(left);
    int rightInt = Utils.toInt(right);
    TokenType type = expr.operator.type;
    if (type == TOK_BIT_OR) {
      return leftInt | rightInt;
    } else if (type == TOK_XOR) {
      return leftInt ^ rightInt;
    } else {
      return leftInt & rightInt;
    }
  }

  @Override
  public Object visitSetExpr(Expr.Set expr) {
    Object object = evaluate(expr.object);

    if (object instanceof LoxInstance) {
      Object value = evaluate(expr.value);
      ((LoxInstance) object).set(expr.name, value);
      return value;
    } else if (object instanceof JSObject) {
      Object value = evaluate(expr.value);
      ((JSObject) object).setProp(expr.name.lexeme, value);
      return value;
    }
    throw new RuntimeError(expr.name, "Only instances have fields.");
  }

  @Override
  public Object visitThisExpr(Expr.This expr) {
    return lookUpVariable(expr.keyword, expr);
  }

  @Override
  public Object visitPostfixExpr(Expr.Postfix expr) {
    Object left = evaluate(expr.left);
    switch (expr.operator.type) {
      case TOK_DEC:
        //todo
        return null;
      case TOK_INC:
        //todo
        return null;
    }
    return null;
  }

  @Override
  public Object visitUnaryExpr(Expr.Unary expr) {
    Object right = evaluate(expr.right);

    switch (expr.operator.type) {
      case BANG:
        return !isTruthy(right);
      case BITWISE_BANG:
        return ~Utils.toInt(right);
      case MINUS:
        checkNumberOperand(expr.operator, right);
        return -(double) right;
      case PLUS:
        checkNumberOperand(expr.operator, right);
        return right;
      case TOK_DEC:
        //todo
        return right;
      case TOK_INC:
        //todo
        return right;
      case TOK_VOID:
        //todo
        return right;
      case TOK_TYPEOF:
        //todo
        return right;
      case TOK_DELETE:
        //todo
        return right;
      case TOK_AWAIT:
        //todo
        return right;
    }

    // Unreachable.
    return null;
  }

  @Override
  public Object visitVariableExpr(Expr.Variable expr) {
    return lookUpVariable(expr.name, expr);
  }

  private Object lookUpVariable(Token name, Expr expr) {
    Integer distance = locals.get(expr);
    return environment.getAt(distance, name.lexeme);
  }

  private void checkNumberOperand(Token operator, Object operand) {
    if (operand instanceof Double) return;
    throw new RuntimeError(operator, "Operand must be a number.");
  }

  private void checkNumberOperands(Token operator,
                                   Object left, Object right) {
    if (left instanceof Double && right instanceof Double) return;
    // [operand]
    throw new RuntimeError(operator, "Operands must be numbers.");
  }

  private boolean isTruthy(Object object) {
    if (object == null) return false;
    if (object instanceof Boolean) return (boolean) object;
    return true;
  }

  // https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Operators/Equality
  private boolean isEqual(Object a, Object b) {
    // todo
    if (a == null && b == null) return true;
    if (a == null) return false;

    return a.equals(b);
  }

  private boolean isStrictEqual(Object a, Object b) {
    // nil is only equal to nil.
    if (a == null && b == null) return true;
    if (a == null) return false;

    return a.equals(b);
  }

  private String stringify(Object object) {
    if (object == null) return "nil";

    // Hack. Work around Java adding ".0" to integer-valued doubles.
    if (object instanceof Double) {
      String text = object.toString();
      if (text.endsWith(".0")) {
        text = text.substring(0, text.length() - 2);
      }
      return text;
    }

    return object.toString();
  }
}