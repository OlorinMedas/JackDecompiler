package jackDecompiler;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;
import java.util.LinkedList;

class JackXmlWriter {
  private BufferedWriter writer;
  private SyntaxNodeWithCounts syntaxTree;
  private String indent, returnType;
  private boolean debug;

  JackXmlWriter(BufferedWriter w, SyntaxNodeWithCounts st) {
    writer = w;
    syntaxTree = st;
    indent = "";
    debug = true;
  }

  private void indent() {
    indent += "\t";
  }

  private void unindent() {
    indent = indent.substring(1);
  }

  private void writeTag(String tag, boolean isClosing) throws IOException {
    writer.write(indent + "<");
    if (isClosing) {
      writer.write("/");
    }
    writer.write(tag + ">");
    writer.newLine();
    if (debug) {
      System.out.print(indent + "<");
      if (isClosing) {
        System.out.print("/");
      }
      System.out.println(tag + ">");
    }
  }

  private void writeLine(String tag, String value) throws IOException {
    writer.write(indent + "<" + tag + "> " + value + " </" + tag + ">");
    writer.newLine();
    if (debug) {
      System.out.println(indent + "<" + tag + "> " + value + " </" + tag + ">");
    }
  }

  private void writeVariable(String genre, int index, boolean isDefine)
          throws IOException {
    String tag;
    if (isDefine) {
      tag = "define_";
    } else {
      tag = "refer_";
    }
    tag += genre + "_variable_" + index;
    writeLine(tag, genre + "_" + index);
  }

  private void writeVariable(String variable, boolean isDefine)
          throws IOException {
    if (variable.equals("this")) {
      writeLine("keyword", "this");
    } else {
      String tag;
      if (isDefine) {
        tag = "define_";
      } else {
        tag = "refer_";
      }
      String[] var = variable.split("_");
      tag += var[0] + "_variable_" + var[1];
      writeLine(tag, variable);
    }
  }

  void writeClass() throws IOException {
    writeTag("class", false);
    indent();
    writeLine("keyword", "class");
    writeLine("define_class", syntaxTree.name);
    writeLine("symbol", "{");
    indent();
    if ((syntaxTree.numOfVariables + syntaxTree.numOfParameters) > 0) {
      writeTag("classVarDec", false);
      indent();
      if (syntaxTree.numOfVariables > 0) {
        writeLine("keyword", "static");
        writeLine("keyword", "type");
        writeVariable("static", 0, true);
        for (int i = 1; i < syntaxTree.numOfVariables; i++) {
          writeLine("symbol", ",");
          writeVariable("static", i, true);
        }
        writeLine("symbol", ";");
      }
      if (syntaxTree.numOfParameters > 0) {
        writeLine("keyword", "field");
        writeLine("keyword", "type");
        writeVariable("field", 0, true);
        for (int i = 1; i < syntaxTree.numOfVariables; i++) {
          writeLine("symbol", ",");
          writeVariable("field", i, true);
        }
        writeLine("symbol", ";");
      }
      unindent();
      writeTag("classVarDec", true);
    }
    for (SyntaxNode sn : syntaxTree.children) {
      SyntaxNodeWithCounts subNode = (SyntaxNodeWithCounts) sn;
      writeSubroutine(subNode);
    }
    unindent();
    writeTag("class", true);
  }

  private void writeSubroutine(SyntaxNodeWithCounts node) throws IOException {
    writeTag("subroutineDec", false);
    indent();
    writeLine("keyword", node.genre);
    returnType = node.type;
    if (node.genre.equals("constructor")) {
      writeLine("refer_class", syntaxTree.name);
    } else {
      writeLine("keyword", returnType);
    }
    writeLine("define_subroutine", node.name);
    writeLine("symbol", "(");
    writeTag("parameterList", false);
    if (node.numOfParameters > 0) {
      indent();
      writeLine("keyword", "type");
      writeVariable("argument", 0, true);
      for (int i = 1; i < node.numOfParameters; i++) {
        writeLine("symbol", ",");
        writeLine("keyword", "type");
        writeVariable("argument", i, true);
      }
      unindent();
    }
    writeTag("parameterList", true);
    writeLine("symbol", ")");
    writeTag("subroutineBody", false);
    indent();
    writeLine("symbol", "{");
    if (node.numOfVariables > 0) {
      writeTag("varDec", false);
      indent();
      writeLine("keyword", "var");
      writeLine("keyword", "type");
      writeVariable("local", 0, true);
      for (int i = 1; i < node.numOfVariables; i++) {
        writeLine("symbol", ",");
        writeVariable("local", i, true);
      }
      writeLine("symbol", ";");
      unindent();
      writeTag("varDec", true);
    }
    writeStatements(node.getFirstChild());
    writeLine("symbol", "}");
    unindent();
    writeTag("subroutineBody", true);
    unindent();
    writeTag("subroutineDec", true);
  }

  private void writeStatements(SyntaxNode node) throws IOException {
    writeTag("statements", false);
    indent();
    LinkedList<SyntaxNode> statements = node.children;
    for (SyntaxNode sn : statements) {
      switch (sn.genre) {
        case "do":
          writeDo(sn);
          break;
        case "if":
          writeIf(sn);
          break;
        case "let":
          writeLet(sn);
          break;
        case "return":
          writeReturn(sn);
          break;
        case "while":
          writeWhile(sn);
          break;
        default:
      }
    }
    unindent();
    writeTag("statements", true);
  }

  private void writeDo(SyntaxNode node) throws IOException {
    writeTag("doStatement", false);
    indent();
    writeLine("keyword", "do");
    writeSubroutineCall(node.getFirstChild());
    writeLine("symbol", ";");
    unindent();
    writeTag("doStatement", true);
  }

  private void writeIf(SyntaxNode node) throws IOException {
    writeTag("ifStatement", false);
    indent();
    writeLine("keyword", "if");
    writeLine("symbol", "(");
    writeExpression(node.getFirstChild(), false);
    writeLine("symbol", ")");
    writeLine("symbol", "{");
    writeStatements(node.getMiddleChild());
    writeLine("symbol", "}");
    if (node.children.size() == 3) {
      writeLine("keyword", "else");
      writeLine("symbol", "{");
      writeStatements(node.getLastChild());
      writeLine("symbol", "}");
    }
    unindent();
    writeTag("ifStatement", true);
  }

  private void writeLet(SyntaxNode node) throws IOException {
    writeTag("letStatement", false);
    indent();
    writeLine("keyword", "let");
    SyntaxNode variable = node.getFirstChild();
    writeVariable(variable.name, false);
    if (node.children.size() > 2) {
      writeLine("symbol", "[");
      writeExpression(node.getMiddleChild(), false);
      writeLine("symbol", "]");
    }
    writeLine("symbol", "=");
    writeExpression(node.getLastChild(), false);
    writeLine("symbol", ";");
    unindent();
    writeTag("letStatement", true);
  }

  private void writeReturn(SyntaxNode node) throws IOException {
    writeTag("returnStatement", false);
    indent();
    writeLine("keyword", "return");
    if (!returnType.equals("void")) {
      writeExpression(node.getFirstChild(), false);
    }
    writeLine("symbol", ";");
    unindent();
    writeTag("returnStatement", true);
  }

  private void writeWhile(SyntaxNode node) throws IOException {
    writeTag("whileStatement", false);
    indent();
    writeLine("keyword", "while");
    writeLine("symbol", "(");
    writeExpression(node.getFirstChild(), false);
    writeLine("symbol", ")");
    writeLine("symbol", "{");
    writeStatements(node.getLastChild());
    writeLine("symbol", "}");
    unindent();
    writeTag("whileStatement", true);
  }

  private void writeExpression(SyntaxNode node, boolean isNested)
          throws IOException {
    if ((node.name == null &&
            (node.getFirstChild().genre.equals("subroutineCall") &&
                    (node.getFirstChild().name.equals("Math.multiply") ||
                            node.getFirstChild().name.equals("Math.divide"))))
            ||
            (node.name != null &&
                    (!node.name.equals("neg") && !node.name.equals("not")))) {
      if (isNested) {
        writeTag("term", false);
        indent();
        writeLine("symbol", "(");
      }
      writeTag("expression", false);
      indent();
      if (node.name == null) {
        node = node.getFirstChild();
      }
      writeExpression(node.getFirstChild(), true);
      switch (node.name) {
        case "add":
          writeLine("symbol", "+");
          break;
        case "sub":
          writeLine("symbol", "-");
          break;
        case "and":
          writeLine("symbol", "&amp;");
          break;
        case "or":
          writeLine("symbol", "|");
          break;
        case "gt":
          writeLine("symbol", "&gt;");
          break;
        case "lt":
          writeLine("symbol", "&lt;");
          break;
        case "eq":
          writeLine("symbol", "=");
          break;
        default:
          if (node.name.equals("Math.multiply")) {
            writeLine("symbol", "*");
          } else {
            writeLine("symbol", "/");
          }
      }
      writeExpression(node.getLastChild(), true);
      unindent();
      writeTag("expression", true);
      if (isNested) {
        writeLine("symbol", ")");
        unindent();
        writeTag("term", true);
      }
    } else {
      writeTerm(node, isNested);
    }
  }

  private void writeTerm(SyntaxNode node, boolean isNested) throws IOException {
    if (!isNested) {
      writeTag("expression", false);
      indent();
    }
    writeTag("term", false);
    indent();
    String name = node.name;
    if (node.children.size() > 0) {
      node = node.getFirstChild();
    }
    switch (node.genre) {
      case "subroutineCall":
        writeSubroutineCall(node);
        break;
      case "IntConst":
        writeLine("integerConstant", node.name);
        break;
      case "KeywordConst":
        writeLine("keywordConstant", node.name);
        break;
      case "StringConst":
        writeLine("stringConstant", "&quot;" + node.name + "&quot;");
        break;
      case "array":
        writeVariable(node.getFirstChild().name, false);
        writeLine("symbol", "[");
        writeExpression(node.getLastChild(), false);
        writeLine("symbol", "]");
        break;
      case "variable":
        writeVariable(node.name, false);
        break;
      case "term":
        if (name.equals("neg")) {
          writeLine("symbol", "-");
          writeExpression(node, true);
        } else {
          String notOprerand = node.getFirstChild().name;
          if (notOprerand != null && notOprerand.equals("0")) {
            writeLine("keywordConstant", "true");
          } else {
            writeLine("symbol", "~");
            writeExpression(node, true);
          }
        }
      default:
    }
    unindent();
    writeTag("term", true);
    if (!isNested) {
      unindent();
      writeTag("expression", true);
    }
  }

  private void writeSubroutineCall(SyntaxNode node) throws IOException {
    writeLine("refer_class", node.name.split("\\.")[0]);
    writeLine("symbol", ".");
    writeLine("refer_subroutine", node.name.split("\\.")[1]);
    writeLine("symbol", "(");
    if (node.children.size() > 0) {
      writeTag("expressionList", false);
      indent();
      writeExpression(node.getFirstChild(), false);
      List<SyntaxNode> parameterList = node.children;
      for (SyntaxNode parameterNode :
              parameterList.subList(1, parameterList.size())) {
        writeLine("symbol", ",");
        writeExpression(parameterNode, false);
      }
      unindent();
      writeTag("expressionList", true);
    } else {
      writeLine("expressionList", "");
    }
    writeLine("symbol", ")");
  }
}