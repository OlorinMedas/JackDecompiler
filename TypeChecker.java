package jackDecompiler;

import java.util.*;

class TypeChecker {
  private LinkedList<SyntaxNodeWithCounts> syntaxForest;
  private SyntaxNodeWithCounts syntaxTree;
  private HashMap<String, TypeEntry> symbolTable;
  private QuickUnionUF typeGroup;
  private LinkedList<Integer> subroutineIndexList;
  private LinkedList<RecheckEntry> recheckNodeList;
  private ArrayList<String> typeList;
  private boolean isVoidReturned;
  private int staticBaseIndex, fieldBaseIndex,
          parameterBaseIndex, variableIndex;
  private String className;

  private class RecheckEntry {
    String name;
    SyntaxNode node;

    RecheckEntry(String entryName, SyntaxNode entryNode) {
      name = entryName;
      node = entryNode;
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

    void union(int p, int q) {
      int rootP = find(p);
      int rootQ = find(q);
      if (rootP == rootQ) return;

      // make root of higher index point to one of lower index
      if (rootP > rootQ) {
        parent[rootP] = rootQ;
      }
      else {
        parent[rootQ] = rootP;
      }
    }
  }

  TypeChecker(LinkedList<SyntaxNodeWithCounts> sf) {
    syntaxForest = sf;
    symbolTable = new HashMap<>();
    typeList = new ArrayList<>(6);
    TypeEntry intType = new TypeEntry("int");
    intType.index = 0;
    typeList.add("int");
    symbolTable.put("int", intType);
    TypeEntry boolType = new TypeEntry("boolean");
    boolType.index = 1;
    typeList.add("boolean");
    symbolTable.put("boolean", boolType);
    TypeEntry charType = new TypeEntry("char");
    charType.index = 2;
    typeList.add("char");
    symbolTable.put("char", charType);
    TypeEntry stringType = new TypeEntry("String");
    stringType.index = 3;
    typeList.add("String");
    symbolTable.put("String", stringType);
    TypeEntry arrayType = new TypeEntry("Array");
    arrayType.index = 4;
    typeList.add("Array");
    symbolTable.put("Array", arrayType);
    int classIndex = 5;
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
    staticBaseIndex = classIndex;
    subroutineIndexList = new LinkedList<>();
    recheckNodeList = new LinkedList<>();
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
    symbolTable.put("Keyboard.keypressed", keyboard_keypressed);
    TypeEntry keyboard_readChar = new TypeEntry("char");
    keyboard_readChar.addParameter("void");
    symbolTable.put("Keyboard.readChar", keyboard_readChar);
    TypeEntry keyboard_readLine = new TypeEntry("String");
    keyboard_readLine.addParameter("void");
    symbolTable.put("Keyboard.readLine", keyboard_readLine);
    TypeEntry keyboard_readInt = new TypeEntry("int");
    keyboard_readInt.addParameter("void");
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
    memory_poke.addParameter("void");
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
    TypeEntry output_printString = new TypeEntry("String");
    output_printString.addParameter("void");
    symbolTable.put("Output.printString", output_printString);
    TypeEntry output_printInt = new TypeEntry("void");
    output_printInt.addParameter("void");
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
    TypeEntry string_backSpace = new TypeEntry("void");
    string_backSpace.addParameter("void");
    array_dispose.isMethod = true;
    symbolTable.put("String.backSpace", string_backSpace);
    TypeEntry string_doubleQuote = new TypeEntry("void");
    string_doubleQuote.addParameter("void");
    symbolTable.put("String.doubleQuote", string_doubleQuote);
    TypeEntry string_newLine = new TypeEntry("void");
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
    ListIterator<Integer> indexListIter = subroutineIndexList.listIterator(2);
    int nextSubroutineIndex = indexListIter.next();
    className = node.name;
    for (SyntaxNode child : node.children) {
      parameterBaseIndex = nextSubroutineIndex;
      variableIndex = indexListIter.next();
      nextSubroutineIndex = indexListIter.next();
      syntaxTree = (SyntaxNodeWithCounts) child;
      checkNodeType(syntaxTree.getFirstChild());
      for (RecheckEntry entry : recheckNodeList) {
        recheckNodeType(entry);
      }
      TypeEntry subroutineType = new TypeEntry(syntaxTree.type);
      subroutineType.isMethod = syntaxTree.isMethod;
      for (int i = 0; i < syntaxTree.numOfParameters; i++) {
        String parameterType = typeList.get(
                typeGroup.find(parameterBaseIndex + i));
        subroutineType.addParameter(parameterType);
      }
      if (subroutineType.parameterTypeList == null) {
        subroutineType.addParameter("void");
      }
      symbolTable.put(className + "." + syntaxTree.name, subroutineType);
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
            recheckNodeList.add(new RecheckEntry(
                    exprVarName, expressionNode.getFirstChild()));
            recheckNodeList.add(new RecheckEntry(
                    variableNode.name, variableNode));
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
    int variableUFIndex = variableIndex(node.name);
    int variableTypeIndex = typeGroup.find(variableUFIndex);
    TypeEntry nodeType = symbolTable.get(type);
    if (variableTypeIndex == variableUFIndex) {
      node.type = type;
      typeGroup.union(variableUFIndex, nodeType.index);
    } else {
      assert variableTypeIndex == nodeType.index;
      node.type = type;
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

    if (node == null) {
      return null;
    } else {
      return node.type;
    }
  }

  private void checkSubroutineCallType(SyntaxNode node) {
    TypeEntry nodeType = symbolTable.get(node.name);
    if (nodeType == null) {
      RecheckEntry entry = new RecheckEntry(node.name, node);
      recheckNodeList.add(entry);
    } else {
      node.type = nodeType.type;
      ListIterator<SyntaxNode> parameterNodeIter =
              node.children.listIterator();
      if (nodeType.isMethod) {
        String callerType = node.name.split("\\.")[0];
        node.getFirstChild().type = callerType;
        SyntaxNode objectNode = parameterNodeIter.next();
        assertVariableType(objectNode, callerType);
      }
      if (nodeType.parameterTypeList.size() == 1 &&
              nodeType.parameterTypeList.get(0).equals("void")) {
        return;
      }
      for (String parameterType : nodeType.parameterTypeList) {
        assertExpressionType(parameterNodeIter.next(), parameterType);
      }
    }
  }

  private void recheckNodeType(RecheckEntry entry) {
    entry.node.type = typeList.get(typeGroup.find(variableIndex(entry.name)));
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
