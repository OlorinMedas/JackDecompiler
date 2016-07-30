package jackDecompiler;

class VMCommand {
	VMCommandType type;
	String command;
	String param;
	int index;

	VMCommand(VMCommandType t, String c) {
		type = t;
		command = c;
		param = null;
		index = -1;
	}
	
	VMCommand(VMCommandType t, String c, String s) {
		type = t;
		command = c;
		param = s;
		index = -1;
	}
	
	VMCommand(VMCommandType t, String c, String s, int i) {
		type = t;
		command = c;
		param = s;
		index = i;
	}

	public String toString() {
		return command + " " + param + " " + index;
	}
}
