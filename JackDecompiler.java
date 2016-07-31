package jackDecompiler;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.ListIterator;

public class JackDecompiler {
	static private boolean debug = true;

	private static String removeVMSuffix(String fileName) {
		return fileName.substring(0, fileName.length() - 3);
	}
	
	public static void main(String[] args) {
		String inputName = args[0];
		LinkedList<String> sourceList = new LinkedList<>();
		if (inputName.endsWith(".vm")) {
			sourceList.addLast(inputName);
		} else {
			File folder = new File(inputName);
			File[] fileList = folder.listFiles();
			assert fileList != null;
			for (File file : fileList) {
				if (file.isFile() && file.getName().endsWith(".vm")) {
					sourceList.addLast(inputName + "\\" + file.getName());
				}
			}
		}

    LinkedList<SyntaxNodeWithCounts> syntaxForest = new LinkedList<>();
    BufferedReader reader = null;
		for (String sourceName : sourceList) {
      try {
        reader = new BufferedReader(new FileReader(sourceName));
        VMParser parser = new VMParser(reader);
        parser.parse();
        String[] path = removeVMSuffix(sourceName).split("\\\\");
        String fileName = path[path.length - 1];
        SyntaxAnalyzer syntaxAnalyzer =
                new SyntaxAnalyzer(parser.getCommandList());
        syntaxAnalyzer.analyzeClass(fileName);
        syntaxForest.add(syntaxAnalyzer.getSyntaxTree());
      } catch (IOException e) {
        e.printStackTrace();
        return;
      } finally {
        try {
          assert reader != null;
          reader.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

    HashMap<String, TypeEntry> symbolTable;
    ListIterator<String> sourceNameIter = sourceList.listIterator();
    BufferedWriter writer = null;
    BufferedWriter xWriter = null;
    TypeChecker typeChecker = new TypeChecker(syntaxForest);
    for (SyntaxNodeWithCounts syntaxTree : syntaxForest) {
      try {
        String sourceName = sourceNameIter.next();
        typeChecker.checkType(syntaxTree);
        if (debug) {
          String outputXmlName = removeVMSuffix(sourceName) + ".xml";
          xWriter = new BufferedWriter(new FileWriter(outputXmlName));
          JackXmlWriter xmlWriter = new JackXmlWriter(xWriter, syntaxTree);
          xmlWriter.writeClass();
        }
        String outputJackName = removeVMSuffix(sourceName) + "_.jack";
        writer = new BufferedWriter(new FileWriter(outputJackName));
        JackWriter jackWriter = new JackWriter(writer, syntaxTree);
        jackWriter.writeClass();
      } catch (IOException e) {
        e.printStackTrace();
        return;
      } finally {
        try {
          assert reader != null;
          reader.close();
          assert writer != null;
          writer.close();
          assert xWriter != null;
          xWriter.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
	}
}