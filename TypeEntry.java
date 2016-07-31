package jackDecompiler;

import java.util.LinkedList;
import java.util.List;

public class TypeEntry {
  int index;
  String type;
  List<String> parameterTypeList;
  boolean isMethod;

  TypeEntry(String t) {
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
    if (parameterTypeList != null) {
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