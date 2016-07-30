package jackDecompiler;

import java.util.LinkedList;

class SyntaxNode {
	String genre, name, type;
	int startIndex;
  private int endIndex;
	LinkedList<SyntaxNode> children;

	SyntaxNode(String nG, String n, int sI, int eI) {
		genre = nG;
		name = n;
    type = null;
		startIndex = sI;
		endIndex = eI;
		children = new LinkedList<>();
	}

	void addFirstChild(SyntaxNode n) {
		children.addFirst(n);
	}

	void addLastChild(SyntaxNode n) {
		children.addLast(n);
	}

	SyntaxNode getFirstChild() {
	  if (children == null) {
	    return null;
    }
		return children.getFirst();
	}

  SyntaxNode getLastChild() {
    if (children == null) {
      return null;
    }
    return children.getLast();
  }

  SyntaxNode getMiddleChild() {
    if (children == null) {
      return null;
    }
    return children.get(1);
  }

  void removeFirstChild() {
    children.removeFirst();
  }
	
	public String toString() {
		return genre + " " + name + "(" + startIndex + ", " + endIndex + ")";
	}
}