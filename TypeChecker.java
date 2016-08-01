package jackDecompiler;

import java.util.*;

class TypeChecker {
  private SyntaxNodeWithCounts syntaxTree;
  private HashMap<String, TypeEntry> symbolTable;
  private QuickUnionUF typeGroup;
  private LinkedList<Integer> subroutineIndexList;
  private ArrayList<String> typeList;
  private boolean isVoidReturned;
  private int staticBaseIndex, fieldBaseIndex,
          argumentBaseIndex, localBaseIndex;
  private String className;

  /**
   *  @author Robert Sedgewick
   *  @author Kevin Wayne
   */
  private class QuickUnionUF {
    private int[] parent;   // parent[i] = parent of i
    private int[] type;
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
      type = new int[n];
      for (int i = 0; i < n; i++) {
        parent[i] = i;
        type[i] = i;
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
      while (p != parent[p]) {
        parent[p] = parent[parent[p]];
        p = parent[p];
      }
      return p;
    }

    int findType(int p) {
      int rootP = find(p);
      return -type[rootP] - 1;
    }

    void union(int p, int q) {
      int rootP = find(p);
      int rootQ = find(q);
      if (rootP == rootQ) return;

      // make root of higher index point to one of lower index
      if (rootP > rootQ) {
        parent[rootP] = rootQ;
        type[rootP] = -1;
      }
      else {
        parent[rootQ] = rootP;
        type[rootQ] = -1;
      }
    }

    // set the type of connected component of p to t
    void setType(int p, int t) {
      int rootP = find(p);
      type[rootP] = -(t + 1);
    }
  }

  private void outputCheck(String s) {
    if (Debug.STDOUT_CHECK) {
      System.out.println(s + "...");
    }
  }

  TypeChecker(LinkedList<SyntaxNodeWithCounts> syntaxForest,
              HashMap<String, TypeEntry> st) {
    symbolTable = st;
    typeList = new ArrayList<>(7);
    TypeEntry defaultType = new TypeEntry("type");
    defaultType.index = 0;
    typeList.add("type");
    symbolTable.put("type", defaultType);
    TypeEntry intType = new TypeEntry("int");
    intType.index = 1;
    typeList.add("int");
    symbolTable.put("int", intType);
    TypeEntry boolType = new TypeEntry("boolean");
    boolType.index = 2;
    typeList.add("boolean");
    symbolTable.put("boolean", boolType);
    TypeEntry charType = new TypeEntry("char");
    charType.index = 3;
    typeList.add("char");
    symbolTable.put("char", charType);
    TypeEntry stringType = new TypeEntry("String");
    stringType.index = 4;
    typeList.add("String");
    symbolTable.put("String", stringType);
    TypeEntry arrayType = new TypeEntry("Array");
    arrayType.index = 5;
    typeList.add("Array");
    symbolTable.put("Array", arrayType);
    int classIndex = 6;
    for (SyntaxNodeWithCounts classNode : syntaxForest) {
      className = classNode.name;
      switch (className) {
        case "Array": case "Keyboard": case "Math": case "Memory":
        case "Output": case "Screen": case "String": case "Sys":
          break;
        default:
          TypeEntry classType = new TypeEntry(className);
          classType.index = classIndex++;
          typeList.add(className);
          symbolTable.put(className, classType);
      }
    }
    subroutineIndexList = new LinkedList<>();
    // Array
    TypeEntry array_new = new TypeEntry("Array");
    array_new.addParameter("int");
    symbolTable.put("Array.new", array_new);
    TypeEntry array_dispose = new TypeEntry("void");
    array_dispose.addParameter("void");
    array_dispose.isMethod = true;
    symbolTable.put("Array.dispose", array_dispose);
    // Keyboard
    TypeEntry keyboard_init = new TypeEntry("void");
    keyboard_init.addParameter("void");
    symbolTable.put("Keyboard.init", keyboard_init);
    TypeEntry keyboard_keypressed = new TypeEntry("char");
    keyboard_keypressed.addParameter("void");
    symbolTable.put("Keyboard.keyPressed", keyboard_keypressed);
    TypeEntry keyboard_readChar = new TypeEntry("char");
    keyboard_readChar.addParameter("void");
    symbolTable.put("Keyboard.readChar", keyboard_readChar);
    TypeEntry keyboard_readLine = new TypeEntry("String");
    keyboard_readLine.addParameter("String");
    symbolTable.put("Keyboard.readLine", keyboard_readLine);
    TypeEntry keyboard_readInt = new TypeEntry("int");
    keyboard_readInt.addParameter("String");
    symbolTable.put("Keyboard.readInt", keyboard_readInt);
    // Math
    TypeEntry math_init = new TypeEntry("void");
    math_init.addParameter("void");
    symbolTable.put("Math.init", math_init);
    TypeEntry math_abs = new TypeEntry("int");
    math_abs.addParameter("int");
    symbolTable.put("Math.abs", math_abs);
    TypeEntry math_multiply = new TypeEntry("int");
    math_multiply.addParameter("int");
    math_multiply.addParameter("int");
    symbolTable.put("Math.multiply", math_multiply);
    TypeEntry math_divide = new TypeEntry("int");
    math_divide.addParameter("int");
    math_divide.addParameter("int");
    symbolTable.put("Math.divide", math_divide);
    TypeEntry math_sqrt = new TypeEntry("int");
    math_sqrt.addParameter("int");
    symbolTable.put("Math.sqrt", math_sqrt);
    TypeEntry math_max = new TypeEntry("int");
    math_max.addParameter("int");
    math_max.addParameter("int");
    symbolTable.put("Math.max", math_max);
    TypeEntry math_min = new TypeEntry("int");
    math_min.addParameter("int");
    math_min.addParameter("int");
    symbolTable.put("Math.min", math_min);
    // Memory
    TypeEntry memory_init = new TypeEntry("void");
    memory_init.addParameter("void");
    symbolTable.put("Memory.init", memory_init);
    TypeEntry memory_peek = new TypeEntry("int");
    memory_peek.addParameter("int");
    symbolTable.put("Memory.peek", memory_peek);
    TypeEntry memory_poke = new TypeEntry("void");
    memory_poke.addParameter("int");
    memory_poke.addParameter("int");
    symbolTable.put("Memory.poke", memory_poke);
    TypeEntry memory_alloc = new TypeEntry("Array");
    memory_alloc.addParameter("int");
    symbolTable.put("Memory.alloc", memory_alloc);
    TypeEntry memory_deAlloc = new TypeEntry("void");
    memory_deAlloc.addParameter("Array");
    symbolTable.put("Memory.deAlloc", memory_deAlloc);
    // Output
    TypeEntry output_init = new TypeEntry("void");
    output_init.addParameter("void");
    symbolTable.put("Output.init", output_init);
    TypeEntry output_moveCursor = new TypeEntry("void");
    output_moveCursor.addParameter("int");
    output_moveCursor.addParameter("int");
    symbolTable.put("Output.moveCursor", output_moveCursor);
    TypeEntry output_printChar = new TypeEntry("void");
    output_printChar.addParameter("char");
    symbolTable.put("Output.printChar", output_printChar);
    TypeEntry output_printString = new TypeEntry("void");
    output_printString.addParameter("String");
    symbolTable.put("Output.printString", output_printString);
    TypeEntry output_printInt = new TypeEntry("void");
    output_printInt.addParameter("int");
    symbolTable.put("Output.printInt", output_printInt);
    TypeEntry output_println = new TypeEntry("void");
    output_println.addParameter("void");
    symbolTable.put("Output.println", output_println);
    TypeEntry output_backSpace = new TypeEntry("void");
    output_backSpace.addParameter("void");
    symbolTable.put("Output.backSpace", output_backSpace);
    // Screen
    TypeEntry screen_init = new TypeEntry("void");
    screen_init.addParameter("void");
    symbolTable.put("Screen.init", screen_init);
    TypeEntry screen_clearScreen = new TypeEntry("void");
    screen_clearScreen.addParameter("void");
    symbolTable.put("Screen.clearScreen", screen_clearScreen);
    TypeEntry screen_setColor = new TypeEntry("void");
    screen_setColor.addParameter("boolean");
    symbolTable.put("Screen.setColor", screen_setColor);
    TypeEntry screen_drawPixel = new TypeEntry("void");
    screen_drawPixel.addParameter("int");
    screen_drawPixel.addParameter("int");
    symbolTable.put("Screen.drawPixel", screen_drawPixel);
    TypeEntry screen_drawLine = new TypeEntry("void");
    screen_drawLine.addParameter("int");
    screen_drawLine.addParameter("int");
    screen_drawLine.addParameter("int");
    screen_drawLine.addParameter("int");
    symbolTable.put("Screen.drawLine", screen_drawLine);
    TypeEntry screen_drawRectangle = new TypeEntry("void");
    screen_drawRectangle.addParameter("int");
    screen_drawRectangle.addParameter("int");
    screen_drawRectangle.addParameter("int");
    screen_drawRectangle.addParameter("int");
    symbolTable.put("Screen.drawRectangle", screen_drawRectangle);
    TypeEntry screen_drawCircle = new TypeEntry("void");
    screen_drawCircle.addParameter("int");
    screen_drawCircle.addParameter("int");
    screen_drawCircle.addParameter("int");
    symbolTable.put("Screen.drawCircle", screen_drawCircle);
    // String
    TypeEntry string_new = new TypeEntry("String");
    string_new.addParameter("int");
    symbolTable.put("String.new", string_new);
    TypeEntry string_dispose = new TypeEntry("void");
    string_dispose.addParameter("void");
    string_dispose.isMethod = true;
    symbolTable.put("String.dispose", string_dispose);
    TypeEntry string_length = new TypeEntry("int");
    string_length.addParameter("void");
    string_length.isMethod = true;
    symbolTable.put("String.length", string_length);
    TypeEntry string_charAt = new TypeEntry("char");
    string_charAt.addParameter("int");
    string_charAt.isMethod = true;
    symbolTable.put("String.charAt", string_charAt);
    TypeEntry string_setCharAt = new TypeEntry("void");
    string_setCharAt.addParameter("int");
    string_setCharAt.addParameter("char");
    string_setCharAt.isMethod = true;
    symbolTable.put("String.setCharAt", string_setCharAt);
    TypeEntry string_appendChar = new TypeEntry("String");
    string_appendChar.addParameter("char");
    string_appendChar.isMethod = true;
    symbolTable.put("String.appendChar", string_appendChar);
    TypeEntry string_eraseLastChar = new TypeEntry("void");
    string_eraseLastChar.addParameter("void");
    string_eraseLastChar.isMethod = true;
    symbolTable.put("String.eraseLastChar", string_eraseLastChar);
    TypeEntry string_intValue = new TypeEntry("int");
    string_intValue.addParameter("void");
    string_intValue.isMethod = true;
    symbolTable.put("String.intValue", string_intValue);
    TypeEntry string_setInt = new TypeEntry("void");
    string_setInt.addParameter("int");
    string_setInt.isMethod = true;
    symbolTable.put("String.setInt", string_setInt);
    TypeEntry string_backSpace = new TypeEntry("char");
    string_backSpace.addParameter("void");
    array_dispose.isMethod = true;
    symbolTable.put("String.backSpace", string_backSpace);
    TypeEntry string_doubleQuote = new TypeEntry("char");
    string_doubleQuote.addParameter("void");
    symbolTable.put("String.doubleQuote", string_doubleQuote);
    TypeEntry string_newLine = new TypeEntry("char");
    string_newLine.addParameter("void");
    symbolTable.put("String.newLine", string_newLine);
    // Sys
    TypeEntry sys_init = new TypeEntry("void");
    sys_init.addParameter("void");
    symbolTable.put("Sys.init", sys_init);
    TypeEntry sys_halt = new TypeEntry("void");
    sys_halt.addParameter("void");
    symbolTable.put("Sys.halt", sys_halt);
    TypeEntry sys_wait = new TypeEntry("void");
    sys_wait.addParameter("int");
    symbolTable.put("Sys.wait", sys_wait);
    TypeEntry sys_error = new TypeEntry("void");
    sys_error.addParameter("int");
    symbolTable.put("Sys.error", sys_error);
  }

  void checkType(SyntaxNodeWithCounts node) {
    outputCheck("Checking class " + node.name);
    staticBaseIndex = 0;
    int numOfTypes = staticBaseIndex;
    subroutineIndexList.add(numOfTypes);
    numOfTypes += node.numOfVariables;
    fieldBaseIndex = numOfTypes;
    subroutineIndexList.add(numOfTypes);
    numOfTypes += node.numOfParameters;
    subroutineIndexList.add(numOfTypes);
    for (SyntaxNode child : node.children) {
      isVoidReturned = true;
      SyntaxNodeWithCounts subroutineNode = (SyntaxNodeWithCounts) child;
      checkReturnVoid(subroutineNode);
      if (isVoidReturned) {
        subroutineNode.type = "void";
      } else {
        if (child.genre.equals("function")) {
          checkConstructor(subroutineNode);
        }
      }
      numOfTypes += subroutineNode.numOfParameters;
      subroutineIndexList.add(numOfTypes);
      numOfTypes += subroutineNode.numOfVariables;
      subroutineIndexList.add(numOfTypes);
    }
    typeGroup = new QuickUnionUF(numOfTypes);
    ListIterator<Integer> indexListIter = subroutineIndexList.listIterator(2);
    int nextSubroutineIndex = indexListIter.next();
    className = node.name;
    for (SyntaxNode child : node.children) {
      argumentBaseIndex = nextSubroutineIndex;
      localBaseIndex = indexListIter.next();
      nextSubroutineIndex = indexListIter.next();
      syntaxTree = (SyntaxNodeWithCounts) child;
      outputCheck("Checking " + child.genre + " " + child.name);
      checkNodeType(syntaxTree.getFirstChild());
      TypeEntry subroutineType = new TypeEntry(syntaxTree.type);
      subroutineType.isMethod = syntaxTree.isMethod;
      for (int i = 0; i < syntaxTree.numOfParameters; i++) {
        int parameterTypeIndex = typeGroup.findType(argumentBaseIndex + i);
        String parameterType = "type";
        if (parameterTypeIndex > 0) {
          parameterType = typeList.get(parameterTypeIndex);
        }
        subroutineType.addParameter(parameterType);
      }
      if (subroutineType.parameterTypeList == null) {
        subroutineType.addParameter("void");
      }
      TypeEntry entry = symbolTable.get(className + "." + syntaxTree.name);
      if (entry == null) {
        symbolTable.put(className + "." + syntaxTree.name, subroutineType);
      }
      for (int i = 0; i < syntaxTree.numOfVariables; i++) {
        int localTypeIndex = typeGroup.findType(localBaseIndex + i);
        String localType = "type";
        if (localTypeIndex > 0) {
          typeList.get(localTypeIndex);
        }
        symbolTable.put(className + "." + syntaxTree.name + ".local:" + i,
                symbolTable.get(localType));
      }
      outputCheck("");
    }
    for (int i = 0; i < node.numOfVariables; i++) {
      int staticTypeIndex = typeGroup.findType(staticBaseIndex + i);
      String staticType = "type";
      if (staticTypeIndex > 0) {
        typeList.get(staticTypeIndex);
      }
      symbolTable.put(className + ".static:" + i,
              symbolTable.get(staticType));
    }
    for (int i = 0; i < node.numOfParameters; i++) {
      int fieldTypeIndex = typeGroup.findType(fieldBaseIndex + i);
      String fieldType = "type";
      if (fieldTypeIndex > 0) {
        typeList.get(fieldTypeIndex);
      }
      symbolTable.put(className + ".field:" + i,
              symbolTable.get(fieldType));
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
        outputCheck("Checking if statement");
        outputCheck("Checking condition expression");
        assertExpressionType(node.getFirstChild(), "boolean");
        outputCheck("Checking the true branch");
        if (node.children.size() == 3) {
          checkNodeType(node.getMiddleChild());
          outputCheck("Checking the false branch");
        }
        checkNodeType(node.getLastChild());
        break;
      case "while":
        outputCheck("Checking while statement");
        outputCheck("Checking condition expression");
        assertExpressionType(node.getFirstChild(), "boolean");
        outputCheck("Checking the loop statements");
        checkNodeType(node.getLastChild());
        break;
      case "let":
        outputCheck("Checking let statement");
        outputCheck("Checking lhs variable");
        SyntaxNode variableNode = node.getFirstChild();
        String variableType = checkVariableType(variableNode);
        if (node.children.size() == 3) {
          outputCheck("Checking array index");
          assertExpressionType(node.getMiddleChild(), "int");
        }
        SyntaxNode expressionNode = node.getLastChild();
        if (variableType != null) {
          outputCheck("Inferring assignment expression type");
          assertExpressionType(expressionNode, variableType);
        } else {
          outputCheck("Checking assignment expression");
          String expressionType = checkExpressionType(expressionNode);
          String exprVarName = expressionNode.getFirstChild().name;
          if (expressionType == null) {
            outputCheck("Establish equality");
            if (exprVarName != null && exprVarName.contains(":")) {
              typeGroup.union(variableIndex(variableNode.name),
                      variableIndex(exprVarName));
            }
          } else {
            outputCheck("Inferring lhs variable");
            assertVariableType(variableNode, expressionType);
          }
        }
        break;
      case "return":
        outputCheck("Checking return statement");
        if (syntaxTree.type == null) {
          outputCheck("Checking return expression");
          syntaxTree.type = checkExpressionType(node.getFirstChild());
        }
        break;
      case "variable":
        checkVariableType(node);
        break;
//      case "StringConst":
//        break;
//      case "IntConst":
//        break;
//      case "KeywordConst":
//        break;
      case "array":
        outputCheck("Checking array");
        assertVariableType(node, "Array");
        outputCheck("Checking array index");
        assertExpressionType(node.getFirstChild(), "int");
        break;
      case "do":
        outputCheck("Checking do statement");
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
    outputCheck("Verifying variable of type " + type);
    int variableUFIndex = variableIndex(node.name);
    int variableTypeIndex = typeGroup.findType(variableUFIndex);
    TypeEntry nodeType = symbolTable.get(type);
    if (variableTypeIndex < 0) {
      // no previous guess of type
      typeGroup.setType(variableUFIndex, nodeType.index);
    } else {
      if (variableTypeIndex != nodeType.index) {
        String variableType = typeList.get(variableTypeIndex);
        switch (variableType) {
          case "int":
            if (!type.equals("boolean")) {
              typeGroup.setType(variableUFIndex, nodeType.index);
            }
            break;
          case "char":
            break;
          case "boolean": case "Array":
            typeGroup.setType(variableUFIndex, nodeType.index);
            break;
          default:
        }
      }
    }
  }

  private int variableIndex(String name) {
    String[] variable = name.split(":");
    int index = Integer.parseInt(variable[1]);
    switch (variable[0]) {
      case "static":
        return index + staticBaseIndex;
      case "field":
        return index + fieldBaseIndex;
      case "argument":
        return index + argumentBaseIndex;
      case "local":
        return index + localBaseIndex;
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
          outputCheck("Checking " + node.name + " expression");
          switch (node.name) {
            case "add": case "sub": case "neg":
              return "int";
            case "gt": case "lt": case "eq": case "not": case "and": case "or":
              return "boolean";
            default:
              return null;
          }
        }
      case "array":
        assertVariableType(node.getFirstChild(), "Array");
        assertExpressionType(node.getLastChild(), "int");
        return "int";
      case "variable":
        return checkVariableType(node);
      case "subroutineCall":
        return checkSubroutineCallType(node);
      case "IntConst":
        outputCheck("Checking int constant");
        return "int";
      case "StringConst":
        outputCheck("Checking String constant");
        return "String";
      case "KeywordConst":
        outputCheck("Checking keyword constant");
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
    outputCheck("Checking variable");
    int variableUFIndex = variableIndex(node.name);
    int variableTypeIndex = typeGroup.findType(variableUFIndex);
    if (variableTypeIndex < 0) {
      return null;
    } else {
      return typeList.get(variableTypeIndex);
    }
  }

  private String checkSubroutineCallType(SyntaxNode node) {
    outputCheck("Checking subroutine call");
    TypeEntry nodeType = symbolTable.get(node.name);
    if (nodeType == null) {
      return null;
    } else {
      ListIterator<SyntaxNode> parameterNodeIter =
              node.children.listIterator();
      if (nodeType.isMethod) {
        String callerType = node.name.split("\\.")[0];
        assertExpressionType(node.getFirstChild(), callerType);
        SyntaxNode objectNode = parameterNodeIter.next();
        assertExpressionType(objectNode, callerType);
      }
      if (nodeType.parameterTypeList.size() > 1 ||
              !nodeType.parameterTypeList.get(0).equals("void")) {
        for (String parameterType : nodeType.parameterTypeList) {
          assertExpressionType(parameterNodeIter.next(), parameterType);
        }
      }
      return nodeType.type;
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

  private void checkConstructor(SyntaxNodeWithCounts sn) {
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
