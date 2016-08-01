package jackDecompiler;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;
import java.util.HashMap;
import java.util.LinkedList;

class JackWriter {
	private BufferedWriter writer;
	private SyntaxNodeWithCounts syntaxTree, subroutineNode;
  private HashMap<String, TypeEntry> symbolTable;
	private String indent;

	JackWriter(BufferedWriter w, SyntaxNodeWithCounts syntaxTree,
             HashMap<String, TypeEntry> symbolTable) {
		writer = w;
		this.syntaxTree = syntaxTree;
    this.symbolTable = symbolTable;
		indent = "";
    subroutineNode = null;
	}

	private void indent() {
		indent += "\t";
	}

	private void unindent() {
		indent = indent.substring(1);
	}

	private void writeStartWith(String value) throws IOException {
		writer.write(indent + value);
		if (Debug.STDOUT_JACK) {
			System.out.print(indent + value);
		}
	}

	private void writeVariable(String genre, int index) throws IOException {
		writer.write(genre + "_" + index);
    if (Debug.STDOUT_JACK) {
      System.out.print(genre + "_" + index);
    }
	}

  private void writeVariable(String variableName) throws IOException {
    String[] var = variableName.split(":");
    writer.write(var[0] + "_" + var[1]);
    if (Debug.STDOUT_JACK) {
      System.out.print(var[0] + "_" + var[1]);
    }
  }

	private void write(String s) throws IOException {
		writer.write(s);
    if (Debug.STDOUT_JACK) {
      System.out.print(s);
    }
	}

	private void writeLineEndWith(String s) throws IOException {
    writer.write(s);
    writer.newLine();
    if (Debug.STDOUT_JACK) {
      System.out.println(s);
    }
  }

  private void writeLine(String s) throws IOException {
    writeStartWith(s);
    writer.newLine();
    if (Debug.STDOUT_JACK) {
      System.out.println();
    }
  }

	void writeClass() throws IOException {
		writeStartWith("class " + syntaxTree.name);
		writeLineEndWith(" {");
		indent();
    if (syntaxTree.numOfVariables > 0) {
      String previousVarType = getClassVariableType("static", 0);
      writeStartWith("static ");
      write(previousVarType);
      write(" static_0");
      String currentVarType;
      for (int i = 1; i < syntaxTree.numOfVariables; i++) {
        currentVarType = getClassVariableType("static", i);
        if (currentVarType.equals(previousVarType)) {
          write(", ");
        } else {
          writeLineEndWith(";");
          writeStartWith("static ");
          write(currentVarType);
          write(" ");
        }
        writeVariable("static", i);
      }
      writeLineEndWith(";");
    }
    if (syntaxTree.numOfParameters > 0) {
      String previousVarType = getClassVariableType("field", 0);
      writeStartWith("field ");
      write(previousVarType);
      write(" field_0");
      String currentVarType;
      for (int i = 1; i < syntaxTree.numOfParameters; i++) {
        currentVarType = getClassVariableType("field", i);
        if (currentVarType.equals(previousVarType)) {
          write(", ");
        } else {
          writeLineEndWith(";");
          writeStartWith("field ");
          write(currentVarType);
          write(" ");
        }
        writeVariable("field", i);
      }
      writeLineEndWith(";");
    }
    writeLine("");
		for (SyntaxNode sn : syntaxTree.children) {
			SyntaxNodeWithCounts subNode = (SyntaxNodeWithCounts) sn;
			writeSubroutine(subNode);
      writeLine("");
		}
		unindent();
    writeLine("}");
	}

	private void writeSubroutine(SyntaxNodeWithCounts node) throws IOException {
	  subroutineNode = node;
		writeStartWith(node.genre + " " + node.type + " " + node.name);
		write(" (");
    if (node.numOfParameters > 0) {
      List<String> parameterTypeList = symbolTable.get(
              syntaxTree.name + "." + node.name).parameterTypeList;
      write(parameterTypeList.get(0) + " argument_0");
      int argumentIndex = 1;
      for (String parameterType :
              parameterTypeList.subList(1, parameterTypeList.size())) {
        write(", ");
        write(parameterType + " argument_" + argumentIndex++);
      }
    }
		writeLineEndWith(") {");
		indent();
    if (node.numOfVariables > 0) {
      String previousVarType = getVariableType("local", 0);
      writeStartWith("local ");
      write(previousVarType);
      write(" local_0");
      String currentVarType;
      for (int i = 1; i < node.numOfVariables; i++) {
        currentVarType = getVariableType("local", i);
        if (currentVarType.equals(previousVarType)) {
          write(", ");
        } else {
          writeLineEndWith(";");
          writeStartWith("local ");
          write(currentVarType);
          write(" ");
        }
        writeVariable("local", i);
      }
      writeLineEndWith(";");
      writeLine("");
    }
		writeStatements(node.getFirstChild());
    unindent();
    writeLine("}");
	}

	private void writeStatements(SyntaxNode node) throws IOException {
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
	}

	private void writeDo(SyntaxNode node) throws IOException {
    writeStartWith("do ");
		writeSubroutineCall(node.getFirstChild());
		writeLineEndWith(";");
	}

	private void writeIf(SyntaxNode node) throws IOException {
    writeStartWith("if (");
		writeExpression(node.getFirstChild(), false);
		writeLineEndWith(") {");
    indent();
		writeStatements(node.getMiddleChild());
    unindent();
		if (node.children.size() == 3) {
      writeLine("} else {");
      indent();
      writeStatements(node.getLastChild());
      unindent();
    }
    writeLine("}");
	}

	private void writeLet(SyntaxNode node) throws IOException {
    writeStartWith("let ");
		SyntaxNode variable = node.getFirstChild();
		writeVariable(variable.name);
		if (node.children.size() > 2) {
			write("[");
			writeExpression(node.getMiddleChild(), false);
			write("]");
		}
		write(" = ");
		writeExpression(node.getLastChild(), false);
		writeLineEndWith(";");
	}

	private void writeReturn(SyntaxNode node) throws IOException {
    writeStartWith("return");
		if (!subroutineNode.type.equals("void")) {
		  write(" ");
			writeExpression(node.getFirstChild(), false);
		}
		writeLineEndWith(";");
	}

	private void writeWhile(SyntaxNode node) throws IOException {
		writeStartWith("while (");
		writeExpression(node.getFirstChild(), false);
		writeLineEndWith(") {");
    indent();
		writeStatements(node.getLastChild());
    unindent();
    writeLine("}");
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
				write("(");
			}
			if (node.name == null) {
				node = node.getFirstChild();
			}
			writeExpression(node.getFirstChild(), true);
			switch (node.name) {
				case "add":
					write(" + ");
					break;
				case "sub":
					write(" - ");
					break;
				case "and":
					write(" & ");
					break;
				case "or":
					write(" | ");
					break;
				case "gt":
					write(" > ");
					break;
				case "lt":
					write(" < ");
					break;
				case "eq":
					write(" = ");
					break;
				default:
					if (node.name.equals("Math.multiply")) {
						write(" * ");
					} else {
						write(" / ");
					}
			}
			writeExpression(node.getLastChild(), true);
			if (isNested) {
				write(")");
			}
		} else {
			writeTerm(node);
		}
	}

	private void writeTerm(SyntaxNode node) throws IOException {
		String name = node.name;
		if (node.children.size() > 0) {
			node = node.getFirstChild();
		}
		switch (node.genre) {
			case "subroutineCall":
				writeSubroutineCall(node);
				break;
			case "IntConst": case "KeywordConst":
        write(node.name);
        break;
      case "variable":
				writeVariable(node.name);
        break;
			case "StringConst":
				write("\"" + node.name + "\"");
				break;
			case "array":
				writeVariable(node.getFirstChild().name);
				write("[");
				writeExpression(node.getLastChild(), false);
				write("]");
				break;
			case "term":
				if (name.equals("neg")) {
					write("-");
					writeExpression(node, true);
				} else {
					String notOprerand = node.getFirstChild().name;
					if (notOprerand != null && notOprerand.equals("0")) {
						write("true");
					} else {
						write("~");
						writeExpression(node, true);
					}
				}
			default:
		}
	}

	private void writeSubroutineCall(SyntaxNode node) throws IOException {
	  if (node.name.equals("Main.main")) {
	    write("Main.main()");
    } else {
      TypeEntry subroutineType = symbolTable.get(node.name);
      int parameterIndex = 0;
      if (subroutineType.isMethod) {
        writeExpression(node.getFirstChild(), false);
        parameterIndex++;
      } else {
        write(node.name.split("\\.")[0]);
      }
      write(".");
      write(node.name.split("\\.")[1]);
      write("(");
      if (node.children.size() > parameterIndex) {
        writeExpression(node.children.get(parameterIndex), false);
        List<SyntaxNode> parameterList = node.children;
        for (SyntaxNode parameterNode :
                parameterList.subList(parameterIndex + 1, parameterList.size())) {
          write(", ");
          writeExpression(parameterNode, false);
        }
      } else {
        write("");
      }
      write(")");
    }
	}

  private String getClassVariableType(String genre, int index) {
    return symbolTable.get(syntaxTree.name + "." + genre + ":" + index).type;
  }

  private String getVariableType(String genre, int index) {
    return symbolTable.get(syntaxTree.name + "." + subroutineNode.name + "." +
            genre + ":" + index).type;
  }
}