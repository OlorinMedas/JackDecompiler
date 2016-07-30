package jackDecompiler;

import java.util.*;

class TypeChecker {
  private LinkedList<SyntaxNodeWithCounts> syntaxForest;
  private SyntaxNodeWithCounts syntaxTree;
  private HashMap<String, Type> symbolTable;
  private QuickUnionUF typeGroup;
  private LinkedList<Integer> subroutineIndexList;
  private LinkedList<SyntaxNode> recheckNodeList;
  private ArrayList<String> typeList;
  private boolean isVoidReturned;
  private int staticBaseIndex, fieldBaseIndex,
          parameterBaseIndex, variableIndex;
  private String className;

  private class Type {
    int index;
    String type;
    List<String> parameterTypeList;
    boolean isMethod;

    Type(String t) {
      type = t;
      parameterTypeList = null;
      isMethod = false;
    }

    void addParameter(String parameterType) {
      if (parameterTypeList == null) {
        parameterTypeList = new LinkedList<>();
      }
      parameterTypeList.add(parameterType);
    }

    public String toString() {
      if (isMethod) {
        String s = type + " (";
        for (String parameterType : parameterTypeList) {
          s += parameterType + ", ";
        }
        s = s.substring(0, s.length() - 2) + ")";
        return s;
      } else {
        return type;
      }
    }
  }

  /**
   *  @author Robert Sedgewick
   *  @author Kevin Wayne
   */
  private class QuickUnionUF {
    private int[] parent;   // parent[i] = parent of i
//    private int[] size;     // size[i] = number of sites in subtree rooted at i

    /**
     * Initializes an empty union-find data structure with <tt>n</tt> sites
     * <tt>0</tt> through <tt>n-1</tt>. Each site is initially in its own
     * component.
     *
     * @param  n the number of sites
     * @throws IllegalArgumentException if <tt>n &lt; 0</tt>
     */
    QuickUnionUF(int n) {
      parent = new int[n];
 //     size = new int[n];
      for (int i = 0; i < n; i++) {
        parent[i] = i;
 //       size[i] = 1;
      }
    }

    /**
     * Returns the component identifier for the component containing site <tt>p</tt>.
     *
     * @param  p the integer representing one object
     * @return the component identifier for the component containing site <tt>p</tt>
     * @throws IndexOutOfBoundsException unless <tt>0 &le; p &lt; n</tt>
     */
    int find(int p) {
      validate(p);
      while (p != parent[p]) {
        parent[p] = parent[parent[p]];
        p = parent[p];
      }
      return p;
    }

    // validate that p is a valid index
    private void validate(int p) {
      int n = parent.length;
      if (p < 0 || p >= n) {
        throw new IndexOutOfBoundsException(
                "index " + p + " is not between 0 and " + (n-1));
      }
    }

    /**
     * Returns true if the the two sites are in the same component.
     *
     * @param  p the integer representing one site
     * @param  q the integer representing the other site
     * @return <tt>true</tt> if the two sites <tt>p</tt> and <tt>q</tt> are in the same component;
     *         <tt>false</tt> otherwise
     * @throws IndexOutOfBoundsException unless
     *         both <tt>0 &le; p &lt; n</tt> and <tt>0 &le; q &lt; n</tt>
     */
    boolean connected(int p, int q) {
      return find(p) == find(q);
    }

    /**
     * Merges the component containing site <tt>p</tt> with the
     * the component containing site <tt>q</tt>.
     *
     * @param  p the integer representing one site
     * @param  q the integer representing the other site
     * @throws IndexOutOfBoundsException unless
     *         both <tt>0 &le; p &lt; n</tt> and <tt>0 &le; q &lt; n</tt>
     */
    void union(int p, int q) {
      int rootP = find(p);
      int rootQ = find(q);
      if (rootP == rootQ) return;

      // make smaller root point to larger one
//
        parent[rootP] = rootQ;
/*        size[rootQ] += size[rootP];
      }
      else {
        parent[rootQ] = rootP;
        size[rootP] += size[rootQ];
      }
      */
    }
  }

  TypeChecker(LinkedList<SyntaxNodeWithCounts> sf) {
    syntaxForest = sf;
    symbolTable = new HashMap<>();
    Type intType = new Type("int");
    intType.index = 0;
    symbolTable.put("int", intType);
    Type boolType = new Type("boolean");
    boolType.index = 1;
    symbolTable.put("boolean", boolType);
    Type charType = new Type("char");
    charType.index = 2;
    symbolTable.put("char", charType);
    Type stringType = new Type("String");
    stringType.index = 3;
    symbolTable.put("String", stringType);
    Type arrayType = new Type("Array");
    arrayType.index = 4;
    symbolTable.put("Array", arrayType);
    int classIndex = 5;
    for (SyntaxNodeWithCounts classNode : syntaxForest) {
      className = classNode.name;
      switch (className) {
        case "Array": case "Keyboard": case "Math": case "Memory":
        case "Output": case "Screen": case "String": case "Sys":
          break;
        default:
          Type classType = new Type(className);
          classType.index = classIndex++;
          symbolTable.put(className, classType);
      }
    }
    staticBaseIndex = classIndex;
    subroutineIndexList = new LinkedList<>();
    recheckNodeList = new LinkedList<>();
    // Array
    Type array_new = new Type("Array");
    array_new.addParameter("int");
    symbolTable.put("Array.new", array_new);
    Type array_dispose = new Type("void");
    array_dispose.addParameter("void");
    array_dispose.isMethod = true;
    symbolTable.put("Array.dispose", array_dispose);
    // Keyboard
    Type keyboard_init = new Type("void");
    keyboard_init.addParameter("void");
    symbolTable.put("Keyboard.init", keyboard_init);
    Type keyboard_keypressed = new Type("char");
    keyboard_keypressed.addParameter("void");
    symbolTable.put("Keyboard.keypressed", keyboard_keypressed);
    Type keyboard_readChar = new Type("char");
    keyboard_readChar.addParameter("void");
    symbolTable.put("Keyboard.readChar", keyboard_readChar);
    Type keyboard_readLine = new Type("String");
    keyboard_readLine.addParameter("void");
    symbolTable.put("Keyboard.readLine", keyboard_readLine);
    Type keyboard_readInt = new Type("int");
    keyboard_readInt.addParameter("void");
    symbolTable.put("Keyboard.readInt", keyboard_readInt);
    // Math
    Type math_init = new Type("void");
    math_init.addParameter("void");
    symbolTable.put("Math.init", math_init);
    Type math_abs = new Type("int");
    math_abs.addParameter("int");
    symbolTable.put("Math.abs", math_abs);
    Type math_multiply = new Type("int");
    math_multiply.addParameter("int");
    math_multiply.addParameter("int");
    symbolTable.put("Math.multiply", math_multiply);
    Type math_divide = new Type("int");
    math_divide.addParameter("int");
    math_divide.addParameter("int");
    symbolTable.put("Math.divide", math_divide);
    Type math_sqrt = new Type("int");
    math_sqrt.addParameter("int");
    symbolTable.put("Math.sqrt", math_sqrt);
    Type math_max = new Type("int");
    math_max.addParameter("int");
    math_max.addParameter("int");
    symbolTable.put("Math.max", math_max);
    Type math_min = new Type("int");
    math_min.addParameter("int");
    math_min.addParameter("int");
    symbolTable.put("Math.min", math_min);
    // Memory
    Type memory_init = new Type("void");
    memory_init.addParameter("void");
    symbolTable.put("Memory.init", memory_init);
    Type memory_peek = new Type("int");
    memory_peek.addParameter("int");
    symbolTable.put("Memory.peek", memory_peek);
    Type memory_poke = new Type("void");
    memory_poke.addParameter("void");
    symbolTable.put("Memory.poke", memory_poke);
    Type memory_alloc = new Type("Array");
    memory_alloc.addParameter("int");
    symbolTable.put("Memory.alloc", memory_alloc);
    Type memory_deAlloc = new Type("void");
    memory_deAlloc.addParameter("Array");
    symbolTable.put("Memory.deAlloc", memory_deAlloc);
    // Output
    Type output_init = new Type("void");
    output_init.addParameter("void");
    symbolTable.put("Output.init", output_init);
    Type output_moveCursor = new Type("void");
    output_moveCursor.addParameter("int");
    output_moveCursor.addParameter("int");
    symbolTable.put("Output.moveCursor", output_moveCursor);
    Type output_printChar = new Type("void");
    output_printChar.addParameter("char");
    symbolTable.put("Output.printChar", output_printChar);
    Type output_printString = new Type("String");
    output_printString.addParameter("void");
    symbolTable.put("Output.printString", output_printString);
    Type output_printInt = new Type("void");
    output_printInt.addParameter("void");
    symbolTable.put("Output.printInt", output_printInt);
    Type output_println = new Type("void");
    output_println.addParameter("void");
    symbolTable.put("Output.println", output_println);
    Type output_backSpace = new Type("void");
    output_backSpace.addParameter("void");
    symbolTable.put("Output.backSpace", output_backSpace);
    // Screen
    Type screen_init = new Type("void");
    screen_init.addParameter("void");
    symbolTable.put("Screen.init", screen_init);
    Type screen_clearScreen = new Type("void");
    screen_clearScreen.addParameter("void");
    symbolTable.put("Screen.clearScreen", screen_clearScreen);
    Type screen_setColor = new Type("void");
    screen_setColor.addParameter("boolean");
    symbolTable.put("Screen.setColor", screen_setColor);
    Type screen_drawPixel = new Type("void");
    screen_drawPixel.addParameter("int");
    screen_drawPixel.addParameter("int");
    symbolTable.put("Screen.drawPixel", screen_drawPixel);
    Type screen_drawLine = new Type("void");
    screen_drawLine.addParameter("int");
    screen_drawLine.addParameter("int");
    screen_drawLine.addParameter("int");
    screen_drawLine.addParameter("int");
    symbolTable.put("Screen.drawLine", screen_drawLine);
    Type screen_drawRectangle = new Type("void");
    screen_drawRectangle.addParameter("int");
    screen_drawRectangle.addParameter("int");
    screen_drawRectangle.addParameter("int");
    screen_drawRectangle.addParameter("int");
    symbolTable.put("Screen.drawRectangle", screen_drawRectangle);
    Type screen_drawCircle = new Type("void");
    screen_drawCircle.addParameter("int");
    screen_drawCircle.addParameter("int");
    screen_drawCircle.addParameter("int");
    symbolTable.put("Screen.drawCircle", screen_drawCircle);
    // String
    Type string_new = new Type("String");
    string_new.addParameter("int");
    symbolTable.put("String.new", string_new);
    Type string_dispose = new Type("void");
    string_dispose.addParameter("void");
    string_dispose.isMethod = true;
    symbolTable.put("String.dispose", string_dispose);
    Type string_length = new Type("int");
    string_length.addParameter("void");
    string_length.isMethod = true;
    symbolTable.put("String.length", string_length);
    Type string_charAt = new Type("char");
    string_charAt.addParameter("int");
    string_charAt.isMethod = true;
    symbolTable.put("String.charAt", string_charAt);
    Type string_setCharAt = new Type("void");
    string_setCharAt.addParameter("int");
    string_setCharAt.isMethod = true;
    symbolTable.put("String.setCharAt", string_setCharAt);
    Type string_appendChar = new Type("String");
    string_appendChar.addParameter("char");
    string_appendChar.isMethod = true;
    symbolTable.put("String.appendChar", string_appendChar);
    Type string_eraseLastChar = new Type("void");
    string_eraseLastChar.addParameter("void");
    string_eraseLastChar.isMethod = true;
    symbolTable.put("String.eraseLastChar", string_eraseLastChar);
    Type string_intValue = new Type("int");
    string_intValue.addParameter("void");
    string_intValue.isMethod = true;
    symbolTable.put("String.intValue", string_intValue);
    Type string_setInt = new Type("void");
    string_setInt.addParameter("int");
    string_setInt.isMethod = true;
    symbolTable.put("String.setInt", string_setInt);
    Type string_backSpace = new Type("void");
    string_backSpace.addParameter("void");
    array_dispose.isMethod = true;
    symbolTable.put("String.backSpace", string_backSpace);
    Type string_doubleQuote = new Type("void");
    string_doubleQuote.addParameter("void");
    symbolTable.put("String.doubleQuote", string_doubleQuote);
    Type string_newLine = new Type("void");
    string_newLine.addParameter("void");
    symbolTable.put("String.newLine", string_newLine);
    // Sys
    Type sys_init = new Type("void");
    sys_init.addParameter("void");
    symbolTable.put("Sys.init", sys_init);
    Type sys_halt = new Type("void");
    sys_halt.addParameter("void");
    symbolTable.put("Sys.halt", sys_halt);
    Type sys_wait = new Type("void");
    sys_wait.addParameter("int");
    symbolTable.put("Sys.wait", sys_wait);
    Type sys_error = new Type("void");
    sys_error.addParameter("int");
    symbolTable.put("Sys.error", sys_error);
  }

  void checkType(SyntaxNodeWithCounts node) {
    int numOfTypes = staticBaseIndex;
    subroutineIndexList.add(numOfTypes);
    numOfTypes += node.numOfVariables;
    fieldBaseIndex = numOfTypes;
    subroutineIndexList.add(numOfTypes);
    numOfTypes += node.numOfParameters;
    subroutineIndexList.add(numOfTypes);
    for (SyntaxNode child : node.children) {
      isVoidReturned = true;
      checkReturnVoid(child);
      if (isVoidReturned) {
        ((SyntaxNodeWithCounts) child).type = "void";
      } else {
        if (child.genre.equals("function")) {
          checkConstructor(child);
        }
      }
      numOfTypes += ((SyntaxNodeWithCounts) child).numOfParameters;
      subroutineIndexList.add(numOfTypes);
      numOfTypes += ((SyntaxNodeWithCounts) child).numOfVariables;
      subroutineIndexList.add(numOfTypes);
    }
    typeGroup = new QuickUnionUF(numOfTypes);
    ListIterator<Integer> indexListIter = subroutineIndexList.listIterator(3);
    int nextSubroutineIndex = indexListIter.next();
    className = node.name;
    for (SyntaxNode child : node.children) {
      parameterBaseIndex = nextSubroutineIndex;
      variableIndex = indexListIter.next();
      nextSubroutineIndex = indexListIter.next();
      syntaxTree = (SyntaxNodeWithCounts) child;
      checkNodeType(syntaxTree.getFirstChild());
    }
    // symbolTable.put(child.name, new Type("void"));
    for (SyntaxNode recheckNode : recheckNodeList) {
        checkNodeType(recheckNode);
    }
  }

  private void checkNodeType(SyntaxNode node) {
    switch (node.genre) {
      case "statements": case "whileStatements":
      case "thenStatements": case "elseStatments":
        for (SyntaxNode child : node.children) {
          checkNodeType(child);
        }
        break;
      case "if":
        if (node.children.size() == 3) {
          checkNodeType(node.getMiddleChild());
        }
      case "while":
        checkNodeType(node.getLastChild());
        assertExpressionType(node.getFirstChild(), "boolean");
        break;
      case "let":
        if (node.children.size() == 3) {
          assertExpressionType(node.getMiddleChild(), "int");
        }
        SyntaxNode variableNode = node.getFirstChild();
        SyntaxNode expressionNode = node.getLastChild();
        String variableType = checkVariableType(variableNode);
        if (variableType != null) {
          assertExpressionType(expressionNode, variableType);
        } else {
          String expressionType = checkExpressionType(expressionNode);
          if (expressionType == null) {
            String exprVarName = expressionNode.getFirstChild().name;
            typeGroup.union(variableIndex(variableNode.name),
                    variableIndex(exprVarName));
            recheckNodeList.add(expressionNode);
            recheckNodeList.add(variableNode);
          } else {
            assertVariableType(variableNode, expressionType);
          }
        }
        break;
      case "return":
        if (syntaxTree.type == null) {
          syntaxTree.type = checkExpressionType(node.getFirstChild());
        }
        break;
      case "variable":
        checkVariableType(node);
        break;
      case "StringConst":
        node.type = "String";
        break;
      case "IntConst":
        node.type = "int";
        break;
      case "KeywordConst":
        if (node.name.equals("this")) {
          node.type = syntaxTree.type;
        }
        break;
      case "array":
        assertVariableType(node, "Array");
        assertExpressionType(node.getFirstChild(), "int");
        break;
      case "do":
        node = node.getFirstChild();
      case "subroutineCall":
        checkSubroutineCallType(node);
        break;
      case "term":
        switch (node.name) {
          case "add": case "sub": case "gt": case "lt": case "eq":
            assertExpressionType(node.getFirstChild(), "int");
          case "neg":
            assertExpressionType(node.getLastChild(), "int");
            break;
          case "and": case "or":
            assertExpressionType(node.getFirstChild(), "boolean");
          case "not":
            assertExpressionType(node.getLastChild(), "boolean");
            break;
        }
      default:
    }
  }

  private void assertVariableType(SyntaxNode node, String type) {
    node.type = type;
    Type nodeType = symbolTable.get(type);
    typeGroup.union(variableIndex(node.name), nodeType.index);
    symbolTable.put(className + "." + syntaxTree.name + "." + node.name,
            nodeType);
  }

  private int variableIndex(String name) {
    String[] variable = name.split("_");
    int index = Integer.parseInt(variable[1]);
    switch (variable[0]) {
      case "static":
        return index + staticBaseIndex;
      case "field":
        return index + fieldBaseIndex;
      case "argument":
        return index + parameterBaseIndex;
      case "local":
        return index + variableIndex;
      default:
        return 0;
    }
  }

  private void assertExpressionType(SyntaxNode node, String assertedType) {
    switch (node.genre) {
      case "term":
        if (node.name == null) {
          assertExpressionType(node.getFirstChild(), assertedType);
        } else {
          checkNodeType(node);
        }
        break;
      case "variable":
        assertVariableType(node, assertedType);
        break;
      default:
    }
  }

  private String checkExpressionType(SyntaxNode node) {
    switch (node.genre) {
      case "term":
        if (node.name == null) {
          return checkExpressionType(node.getFirstChild());
        } else {
          switch (node.name) {
            case "add": case "sub": case "neg":
              return "int";
            case "gt": case "lt": case "eq": case "not":
              return "boolean";
            default:
              return "";
          }
        }
      case "variable":
        return checkVariableType(node);
      case "subroutineCall":
        checkSubroutineCallType(node);
        return node.type;
      case "IntConst":
        return "int";
      case "StringConst":
        return "String";
      case "KeywordConst":
        switch (node.name) {
          case "false": case "true":
            return "boolean";
          case "this":
            return className;
        }
      default:
        return "";
    }
  }

  private String checkVariableType(SyntaxNode node) {
    Type nodeType = symbolTable.get(node.type);
    if (nodeType == null) {
      return null;
    } else {
      return nodeType.type;
    }
  }

  private void checkSubroutineCallType(SyntaxNode node) {
    Type nodeType = symbolTable.get(node.name);
    if (nodeType == null) {
      recheckNodeList.add(node);
    } else {
      node.type = nodeType.type;
      ListIterator<SyntaxNode> parameterNodeIter =
              node.children.listIterator();
      if (nodeType.isMethod) {
        node.getFirstChild().type = node.name.split("\\.")[0];
        parameterNodeIter.next();
      }
      for (String parameterType : nodeType.parameterTypeList) {
        assertExpressionType(parameterNodeIter.next(), parameterType);
      }
    }
  }

  private void checkReturnVoid(SyntaxNode sn) {
    if (isVoidReturned) {
      for (SyntaxNode node : sn.children) {
        switch (node.genre) {
          case "return":
            SyntaxNode returnVar = node.getFirstChild();
            if (returnVar != null) {
              returnVar = returnVar.children.getFirst();
            }
            assert returnVar != null;
            isVoidReturned = returnVar.genre.equals("IntConst") &&
                    returnVar.name.equals("0");
            break;
          case "while":
            checkReturnVoid(node.getLastChild());
            break;
          case "if":
            checkReturnVoid(node.getLastChild());
            if (node.children.size() > 2) {
              checkReturnVoid(node.getMiddleChild());
            }
            break;
          case "statements":
            checkReturnVoid(node);
            break;
          default:
        }
      }
    }
  }

  private void checkConstructor(SyntaxNode sn) {
    SyntaxNode statements = sn.getFirstChild();
    SyntaxNode firstStatement = statements.getFirstChild();
    if (firstStatement.genre.equals("let")) {
      SyntaxNode variable = firstStatement.getFirstChild();
      if (variable.name == null) {
        return;
      }
      if (variable.name.equals("this")) {
        SyntaxNode expression = firstStatement.getLastChild();
        if (expression.genre.equals("term") && expression.name == null) {
          expression = expression.getFirstChild();
          if (expression.name.equals("Memory.alloc")) {
            SyntaxNode parameter = expression.getFirstChild();
            if (parameter.genre.equals("term") && parameter.name == null) {
              parameter = parameter.getFirstChild();
              if(parameter.genre.equals("IntConst")) {
                sn.genre = "constructor";
                sn.type = className;
                statements.removeFirstChild();
              }
            }
          }
        }
      }
    }
  }
}
