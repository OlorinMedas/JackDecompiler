package jackDecompiler;

import java.io.BufferedReader;
import java.util.LinkedList;
import java.io.IOException;

class VMParser {
  private static BufferedReader reader;
  private static LinkedList<VMCommand> commandList;

  VMParser(BufferedReader r) {
    reader = r;
    commandList = new LinkedList<>();
  }

  void parse() throws IOException {
    String line;
    do {
      line = reader.readLine();
      if (line != null) {
        line = (line + "").split("//")[0].trim();
        if (!line.isEmpty())
          addCommand(line.split(" "));
      }
    } while (line != null);
  }

  private void addCommand(String[] command) {
    VMCommandType type;
    switch (command[0]) {
      case "add":
      case "sub":
      case "neg":
      case "eq":
      case "gt":
      case "lt":
      case "and":
      case "or":
      case "not":
        commandList.add(new VMCommand(VMCommandType.C_ARITHMETIC, command[0]));
        return;
      case "push":
        type = VMCommandType.C_PUSH;
        break;
      case "pop":
        type = VMCommandType.C_POP;
        break;
      case "label":
        commandList.add(new VMCommand(VMCommandType.C_LABEL, command[0],
                command[1]));
        return;
      case "goto":
        commandList.add(new VMCommand(VMCommandType.C_GOTO, command[0],
                command[1]));
        return;
      case "if-goto":
        commandList.add(new VMCommand(VMCommandType.C_IF, command[0],
                command[1]));
        return;
      case "function":
        type = VMCommandType.C_FUNCTION;
        break;
      case "call":
        type = VMCommandType.C_CALL;
        break;
      case "return":
        commandList.add(new VMCommand(VMCommandType.C_RETURN, "return"));
        return;
      default:
        throw new java.security.InvalidParameterException(
                "Error: Undefined VM Command! : " + command[0]);
    }
    commandList.add(new VMCommand(type, command[0], command[1],
            Integer.parseInt(command[2])));
  }

  LinkedList<VMCommand> getCommandList() {
    return commandList;
  }
}
