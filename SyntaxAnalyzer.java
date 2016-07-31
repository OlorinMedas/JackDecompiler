package jackDecompiler;

import java.util.LinkedList;
import java.util.ListIterator;

class SyntaxAnalyzer {
  private LinkedList<VMCommand> list;
  private SyntaxNodeWithCounts syntaxTree;

  SyntaxAnalyzer(LinkedList<VMCommand> l) {
    list = l;
  }

  SyntaxNodeWithCounts getSyntaxTree() {
    return syntaxTree;
  }

  void analyzeClass(String className) {
    syntaxTree = new SyntaxNodeWithCounts(
            "class", className, 0, list.size(), false);
    int start, end;
    start = 0;
    syntaxTree.numOfVariables = -1;
    syntaxTree.numOfParameters = -1;
    ListIterator<VMCommand> iter = list.listIterator();
    iter.next();
    while (iter.hasNext()) {
      if (iter.next().type == VMCommandType.C_FUNCTION) {
        end = iter.previousIndex();
        analyzeSubroutine(start, end);
        start = end;
      }
    }
    end = iter.nextIndex();
    analyzeSubroutine(start, end);
    syntaxTree.numOfVariables += 1;
    syntaxTree.numOfParameters += 1;
  }

  private void analyzeSubroutine(int start, int end) {
    boolean isMethod = false;
    String subroutineGenre;
    VMCommand selfParam = list.get(start + 2);
    if (selfParam.command.equals("pop") &&
            selfParam.param.equals("pointer") &&
            selfParam.index == 0) {
      isMethod = true;
      subroutineGenre = "method";
    } else {
      subroutineGenre = "function";
    }
    SyntaxNodeWithCounts sNode = new SyntaxNodeWithCounts(
            subroutineGenre, list.get(start).param.split("\\.")[1], start, end,
            isMethod);
    syntaxTree.addLastChild(sNode);
    sNode.numOfVariables = list.get(start).index;
    ListIterator<VMCommand> iter = list.listIterator(start);
    VMCommand c = list.get(end - 1);
    while (iter.nextIndex() < end) {
      if ((c.type == VMCommandType.C_PUSH || c.type == VMCommandType.C_POP) &&
              c.param.equals("argument")) {
        if (c.index > sNode.numOfParameters) {
          sNode.numOfParameters = c.index;
        }
      }
      // static
      if ((c.type == VMCommandType.C_PUSH || c.type == VMCommandType.C_POP) &&
              c.param.equals("static")) {
        if (c.index > syntaxTree.numOfVariables) {
          syntaxTree.numOfVariables = c.index;
        }
      }
      // field
      if ((c.type == VMCommandType.C_PUSH || c.type == VMCommandType.C_POP) &&
              c.param.equals("this")) {
        if (c.index > syntaxTree.numOfParameters) {
          syntaxTree.numOfParameters = c.index;
        }
      }
      c = iter.next();
    }
    sNode.numOfParameters++;
    SyntaxNode statementsNode;
    if (isMethod) {
      sNode.numOfParameters--;
      statementsNode = new SyntaxNode("statements", null,
      		start + 3, end);
      sNode.addFirstChild(statementsNode);
      analyzeStatements(statementsNode, end);
    } else {
    	statementsNode = new SyntaxNode("statements", null,
      		start + 1, end);
    	sNode.addFirstChild(statementsNode);
      analyzeStatements(statementsNode, end);
    }
  }

//  private int skimStatements(ListIterator<VMCommand> iter, int start) {
//    if (iter.nextIndex() == start) {
//      return 0;
//    }
//    VMCommand lastCommand = iter.previous();
//    VMCommand prevCommand;
//    switch (lastCommand.type) {
//      case C_RETURN:
//        skimExpression(iter);
//        return skimStatements(iter, start);
//      case C_LABEL:
//        prevCommand = iter.previous();
//        if (lastCommand.param.startsWith("WHILE")) {
//          String expectedWhileExprLabel =
//                  lastCommand.param.replace("ND", "XPR");
//          while (prevCommand.type != VMCommandType.C_LABEL ||
//                  !prevCommand.param.equals(expectedWhileExprLabel)) {
//            prevCommand = iter.previous();
//          }
//          return skimStatements(iter, start);
//        } else {
//          // if statement
//          String expectedWhileExprLabel = "IF_TRUE" +
//                  lastCommand.param.charAt(lastCommand.param.length() - 1);
//          while (prevCommand.type != VMCommandType.C_LABEL ||
//                  prevCommand.param.equals(expectedWhileExprLabel)) {
//            prevCommand = iter.previous();
//          }
//          skimExpression(iter);
//          return skimStatements(iter, start);
//        }
//      case C_POP:
//        prevCommand = iter.previous();
//        if (lastCommand.param.equals("temp") &&
//                prevCommand.type == VMCommandType.C_CALL) {
//          // do statement
//          for (int i = 0; i < prevCommand.index; i++) {
//            skimExpression(iter);
//          }
//          return skimStatements(iter, start);
//        } else {
//          // let statement
//        	if (lastCommand.param.equals("that")) {
//        		iter.previous();
//        		iter.previous();
//        		skimExpression(iter);
//        		iter.previous();
//        		iter.previous();
//        	} else {
//        		iter.next();
//        	}
//        	skimExpression(iter);
//          return skimStatements(iter, start);
//        }
//      default:
//        return iter.nextIndex() - start;
//    }
//  }

  private void analyzeStatements(SyntaxNode sNode, int end) {
    if (sNode.startIndex == end) {
      return;
    }
    ListIterator<VMCommand> iter = list.listIterator(end);
    VMCommand lastCommand = iter.previous();
    VMCommand prevCommand;
    int splitIndex;
    switch (lastCommand.type) {
      case C_RETURN:
        splitIndex = skimExpression(iter);
        analyzeReturn(sNode, splitIndex, end);
        analyzeStatements(sNode, splitIndex);
        break;
      case C_LABEL:
        prevCommand = iter.previous();
        if (lastCommand.param.startsWith("WHILE")) {
          String expectedWhileExprLabel =
                  lastCommand.param.replace("ND", "XP");
          while (prevCommand.type != VMCommandType.C_LABEL ||
                  !prevCommand.param.equals(expectedWhileExprLabel)) {
            prevCommand = iter.previous();
          }
          splitIndex = iter.nextIndex();
          analyzeWhile(sNode, splitIndex, end);
          analyzeStatements(sNode, splitIndex);
        } else {
          // if statement
          String expectedIFExprLabel = lastCommand.param;
          if (lastCommand.param.contains("END")) {
            expectedIFExprLabel = lastCommand.param.replace("END", "FALSE");
          }
          while (prevCommand.type != VMCommandType.C_GOTO ||
                  !prevCommand.param.equals(expectedIFExprLabel)) {
            prevCommand = iter.previous();
          }
          iter.previous();
          splitIndex = skimExpression(iter);
          analyzeIf(sNode, splitIndex, end);
          analyzeStatements(sNode, splitIndex);
        }
        break;
      case C_POP:
        prevCommand = iter.previous();
        if (lastCommand.param.equals("temp") &&
                prevCommand.type == VMCommandType.C_CALL) {
          // do statement
          splitIndex = iter.nextIndex();
          for (int i = 0; i < prevCommand.index; i++) {
            splitIndex = skimExpression(iter);
          }
          analyzeDo(sNode, splitIndex, end);
          analyzeStatements(sNode, splitIndex);
        } else {
          iter.next();
          // let statement
          if (lastCommand.param.equals("that")) {
            iter.previous();
            iter.previous();
            iter.previous();
            skimExpression(iter);
            iter.previous();
            iter.previous();
          }
          splitIndex = skimExpression(iter);
          analyzeLet(sNode, splitIndex, end);
          analyzeStatements(sNode, splitIndex);
        }
        break;
      default:
    }
  }

  private void analyzeLet(SyntaxNode sNode, int start, int end) {
    SyntaxNode letNode = new SyntaxNode("let", null, start, end);
    sNode.addFirstChild(letNode);
    ListIterator<VMCommand> iter = list.listIterator(end);
    VMCommand lvalCommand = iter.previous();
    if (lvalCommand.param.equals("that")) {
      iter = list.listIterator(end - 4);
      int split = skimExpression(iter);
      analyzeExpression(letNode, split, end - 4);
      analyzeExpression(letNode, start, split - 2);
      analyzeVariable(letNode, split - 2, split - 1);
    } else {
      analyzeExpression(letNode, start, end - 1);
      analyzeVariable(letNode, end - 1, end);
    }
  }

  private void analyzeDo(SyntaxNode sNode, int start, int end) {
    SyntaxNode doNode = new SyntaxNode("do", null, start, end);
    sNode.addFirstChild(doNode);
    analyzeSubroutineCall(doNode, start, end - 1);
  }

  private void analyzeIf(SyntaxNode sNode, int start, int end) {
    SyntaxNode ifNode = new SyntaxNode("if", null, start, end);
    sNode.addFirstChild(ifNode);
    ListIterator<VMCommand> iter = list.listIterator(end);
    String endLabel = iter.previous().param, trueLabel;
    boolean hasElse = endLabel.contains("END");
    int split = iter.nextIndex();
    if (hasElse) {
      String falseLabel = endLabel.replace("END", "FALSE");
      VMCommand prevCommand = iter.previous();
      while (prevCommand.type != VMCommandType.C_LABEL ||
              !prevCommand.param.equals(falseLabel)) {
        prevCommand = iter.previous();
      }
      SyntaxNode elseStatementsNode = new SyntaxNode("elseStatements", null,
      		iter.nextIndex() + 1, end - 1);
      ifNode.addFirstChild(elseStatementsNode);
      analyzeStatements(elseStatementsNode, end - 1);
      split = iter.previousIndex();
      trueLabel = endLabel.replace("END", "TRUE");
    } else {
      trueLabel = endLabel.replace("FALSE", "TRUE");
    }
    VMCommand prevCommand = iter.previous();
    while (prevCommand.type != VMCommandType.C_LABEL ||
            !prevCommand.param.equals(trueLabel)) {
      prevCommand = iter.previous();
    }
    SyntaxNode trueStatementsNode = new SyntaxNode("thenStatements", null,
    		iter.nextIndex() + 1, split);
    ifNode.addFirstChild(trueStatementsNode);
    analyzeStatements(trueStatementsNode, split);
    iter.previous();
    analyzeExpression(ifNode, start, iter.previousIndex());
  }

  private void analyzeWhile(SyntaxNode sNode, int start, int end) {
    SyntaxNode whileNode = new SyntaxNode("while", null, start, end);
    sNode.addFirstChild(whileNode);
    ListIterator<VMCommand> iter = list.listIterator(start);
    String endLabel = iter.next().param.replace("XP", "ND");
    VMCommand whileExpr = iter.next();
    while (whileExpr.type != VMCommandType.C_IF ||
            !whileExpr.param.equals(endLabel)) {
      whileExpr = iter.next();
    }
    int split = iter.previousIndex() - 1;
    SyntaxNode whileStatementsNode = new SyntaxNode("whileStatements", null,
    		iter.nextIndex(), end - 2);
    whileNode.addFirstChild(whileStatementsNode);
    analyzeStatements(whileStatementsNode, end - 2);
    analyzeExpression(whileNode, start + 1, split);
  }

  private void analyzeReturn(SyntaxNode sNode, int start, int end) {
    SyntaxNode returnNode = new SyntaxNode("return", null, start, end);
    sNode.addFirstChild(returnNode);
    analyzeExpression(returnNode, start, end - 1);
  }

  private void analyzeSubroutineCall(SyntaxNode sNode, int start, int end) {
    SyntaxNode callNode = new SyntaxNode(
            "subroutineCall", null, start, end);
    sNode.addFirstChild(callNode);
    VMCommand callCommand = list.get(end - 1);
    callNode.name = callCommand.param;
    ListIterator<VMCommand> iter = list.listIterator(end - 1);
    int numOfParams = callCommand.index, exprStart, exprEnd = end - 1;
    for (int i = 0; i < numOfParams; i++) {
      exprStart = skimExpression(iter);
      analyzeExpression(callNode, exprStart, exprEnd);
      exprEnd = exprStart;
    }
  }

  private void analyzeExpression(SyntaxNode sNode, int start, int end) {
    SyntaxNode exprNode = new SyntaxNode("term", null, start, end);
    sNode.addFirstChild(exprNode);
    ListIterator<VMCommand> iter = list.listIterator(end);
    VMCommand currentCommand = iter.previous();
    switch (currentCommand.type) {
      case C_PUSH:
        iter.next();
        analyzeVariable(exprNode, start, end);
        return;
      case C_CALL:
        // String constant
      	boolean notEmpty = false;
      	while (currentCommand.param.equals("String.appendChar")) {
      		iter.previous();
      		currentCommand = iter.previous();
      		notEmpty = true;
      	}
      	if (currentCommand.param.equals("String.new") && notEmpty) {
      		iter.next();
      		analyzeStringConst(exprNode, start, end);
      		return;
      	}
        // subroutineCall
        int expressionListLength = currentCommand.index;
        for (int i = 0; i < expressionListLength; i++) {
          skimExpression(iter);
        }
        analyzeSubroutineCall(exprNode, iter.nextIndex(), end);
        return;
      case C_ARITHMETIC:
        exprNode.name = currentCommand.command;
        switch (currentCommand.command) {
          case "add": case "sub": case "and": case "or":
          case "gt": case "lt": case "eq":
            analyzeExpression(exprNode, skimExpression(iter), end - 1);
            end = iter.nextIndex() + 1;
          case "neg": case "not":
            analyzeExpression(exprNode, skimExpression(iter), end - 1);
        }
      default:
    }
  }

  private void analyzeStringConst(SyntaxNode sNode, int start, int end) {
    ListIterator<VMCommand> iter = list.listIterator(end);
    VMCommand currentCommand = iter.previous();
    String s = "";
    char c;
    while (currentCommand.param.equals("String.appendChar")) {
      currentCommand = iter.previous();
      c = (char) currentCommand.index;
      s = c + s;
      currentCommand = iter.previous();
    }
    SyntaxNode node = new SyntaxNode("StringConst", s, start, end);
    sNode.addFirstChild(node);
  }

  private void analyzeVariable(SyntaxNode sNode, int start, int end) {
    SyntaxNode varNode = new SyntaxNode("variable", null, start, end);
    sNode.addFirstChild(varNode);
    VMCommand variableCommand = list.get(end - 1);
    switch (variableCommand.param) {
      case "constant":
        varNode.genre = "IntConst";
        varNode.name = String.valueOf(variableCommand.index);
        return;
      case "this":
        varNode.name = "field:" + variableCommand.index;
        return;
      case "that":
        varNode.genre = "array";
        analyzeExpression(varNode, start, end - 4);
        analyzeVariable(varNode, end - 4, end - 3);
        return;
      case "pointer":
        varNode.genre = "KeywordConst";
        varNode.name = "this";
        return;
      default:
        varNode.name = variableCommand.param + ":" + variableCommand.index;
    }
  }

  private int skimExpression(ListIterator<VMCommand> iter) {
    VMCommand currentCommand = iter.previous();
    switch (currentCommand.type) {
      case C_PUSH:
      		iter.next();
      		return skimVariable(iter);
      case C_CALL:
        // String constant
        while (currentCommand.param.equals("String.appendChar")) {
          iter.previous();
          currentCommand = iter.previous();
        }
        if (currentCommand.param.equals("String.new")) {
          iter.previous();
          return iter.nextIndex();
        }
        // subroutineCall
        int expressionListLength = currentCommand.index;
        for (int i = 0; i < expressionListLength; i++) {
          skimExpression(iter);
        }
        return iter.nextIndex();
      case C_ARITHMETIC:
        switch (currentCommand.command) {
          case "add": case "sub": case "and": case "or":
          case "gt": case "lt": case "eq":
            skimExpression(iter);
          case "neg": case "not":
            return skimExpression(iter);
        }
      default:
        return -1;
    }
  }

  private int skimVariable(ListIterator<VMCommand> iter) {
    VMCommand variableCommand = iter.previous();
    switch (variableCommand.param) {
      case "that":
        iter.previous();
        return skimExpression(iter);
      default:
        return iter.nextIndex();
    }
  }
}
