package jackDecompiler;

				import java.io.BufferedWriter;
				import java.io.IOException;
				import java.util.List;
				import java.util.LinkedList;

class JackWriter {
	private BufferedWriter writer;
	private SyntaxNodeWithCounts syntaxTree;
	private String indent;
	private boolean isConstructor, debug;

	JackWriter(BufferedWriter w, SyntaxNodeWithCounts st) {
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

	private void writeStartWith(String value) throws IOException {
		writer.write(indent + value);
		if (debug) {
			System.out.print(indent + value);
		}
	}

	private void writeVariable(String genre, int index) throws IOException {
		writer.write(genre + "_" + index);
    if (debug) {
      System.out.print(genre + "_" + index);
    }
	}

	private void write(String s) throws IOException {
		writer.write(s);
    if (debug) {
      System.out.print(s);
    }
	}

	private void writeLineEndWith(String s) throws IOException {
    writer.write(s);
    writer.newLine();
    if (debug) {
      System.out.println(s);
    }
  }

  private void writeLine(String s) throws IOException {
    writeStartWith(s);
    writer.newLine();
    if (debug) {
      System.out.println();
    }
  }

	void writeClass() throws IOException {
		writeStartWith("class " + syntaxTree.name);
		writeLineEndWith(" {");
		indent();
		if ((syntaxTree.numOfVariables + syntaxTree.numOfParameters) > 0) {
			if (syntaxTree.numOfVariables > 0) {
        writeStartWith("static type static_0");
				for (int i = 1; i < syntaxTree.numOfVariables; i++) {
					write(", ");
					writeVariable("static", i);
				}
				writeLineEndWith(";");
			}
			if (syntaxTree.numOfParameters > 0) {
        writeStartWith("field type field_0");
				for (int i = 1; i < syntaxTree.numOfVariables; i++) {
					write(", ");
					writeVariable("field", i);
				}
				writeLineEndWith(";");
			}
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
		isConstructor = false;
		if (node.genre.equals("function")) {
			isConstructor = true;
			checkConstructor(node.getFirstChild());
		}
		if (isConstructor) {
      writeStartWith("constructor " + syntaxTree.name + " " + node.name);
		} else {
      writeStartWith(node.genre);
			if (node.type.equals("void")) {
				write(" void ");
			} else {
				write(" type ");
			}
			write(node.name);
			// write()
		}
		write(" (");
		if (node.numOfParameters > 0) {
			write("type argument_0");
			for (int i = 1; i < node.numOfParameters; i++) {
				write(", type ");
				writeVariable("argument", i);
			}
		}
		writeLineEndWith(") {");
		indent();
		if (node.numOfVariables > 0) {
			writeStartWith("var type local_0");
			for (int i = 1; i < node.numOfVariables; i++) {
				write(", ");
				writeVariable("local", i);
			}
			writeLineEndWith(";");
      writeLine("");
		}
		writeStatements(node.getFirstChild());
    unindent();
    writeLine("}");
	}

	private void checkConstructor(SyntaxNode sn) {
		for (SyntaxNode node : sn.children) {
			switch (node.genre) {
				case "return":
					SyntaxNode returnVar = node.getFirstChild();
					if (returnVar != null) {
						returnVar = returnVar.children.getFirst();
					}
					assert returnVar != null;
					isConstructor &= returnVar.genre.equals("KeywordConst") &&
									returnVar.name.equals("this");
					break;
				case "while":
					checkConstructor(node.getLastChild());
					break;
				case "if":
					checkConstructor(node.getLastChild());
					if (node.children.size() > 2) {
						checkConstructor(node.getMiddleChild());
					}
					break;
				default:
			}
		}
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
		write(variable.name);
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
		if (!node.type.equals("void")) {
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
			case "IntConst": case "KeywordConst": case "variable":
				write(node.name);
				break;
			case "StringConst":
				write("\" + node.name + \"");
				break;
			case "array":
				write(node.getFirstChild().name);
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
		write(node.name.split("\\.")[0]);
		write(".");
		write(node.name.split("\\.")[1]);
		write("(");
		if (node.children.size() > 0) {
			writeExpression(node.getFirstChild(), false);
			List<SyntaxNode> parameterList = node.children;
			for (SyntaxNode parameterNode :
							parameterList.subList(1, parameterList.size())) {
				write(", ");
				writeExpression(parameterNode, false);
			}
		} else {
			write("");
		}
		write(")");
	}
}