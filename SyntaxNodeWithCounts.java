package jackDecompiler;

class SyntaxNodeWithCounts extends SyntaxNode {
	String type;
	int numOfParameters, numOfVariables;
  boolean isMethod;

	SyntaxNodeWithCounts(String ng, String n, int sI, int eI,
                       boolean iM) {
		super(ng, n, sI, eI);
		numOfVariables = 0;
		numOfParameters = -1;
    isMethod = iM;
	}
}
