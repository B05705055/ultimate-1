package tw.ntu.svvrl.ultimate.scantu.lib;

import java.util.ArrayList;

public class CACSLCodeBeautifier {
	
	private static boolean IN_STRING = false;
	private static boolean IN_COMMENT = false;
	private static boolean IN_FOR = false;
	private static int CONDITION_LEVEL = 0;
	private static int BLOCK_LEVEL = 0;
	
	/*private static int isKeywordStatement (String inputString) {
		int NOT_KEYWORD = 0;
		int CONDITION = 1;
		
		if (inputString.indexOf("if(") == 0 || inputString.indexOf("while(") == 0 ||
				inputString.indexOf("for(") == 0) {
			return CONDITION;
		}
		return NOT_KEYWORD;
	}*/
	
	private static String addTab (String inputString, int level) {
		String outputString = "";
		for (int i = 0; i < level; i++) {
			outputString = outputString + "        ";
		}
		outputString = outputString + inputString;
		return outputString;
	}
	
	/** codeBeautify()
	 * Input:   original code   - array of String
	 * Output: beautified code  - array of String
	 */
	public static ArrayList<String> codeBeautify(ArrayList<String> inputCode) {
		
		ArrayList<String> beautifiedCode = new ArrayList<String>();
		
		for (int codeLine = 0; codeLine < inputCode.size(); codeLine++) {
			String nowCodeLine = inputCode.get(codeLine);
			String uncheckedCodeLine = nowCodeLine;
			
			/* remove the space, tab in front of the code line. */
			while (uncheckedCodeLine.length() > 0 &&
					(uncheckedCodeLine.substring(0, 1).equals("\s") || uncheckedCodeLine.substring(0, 1).equals("\t"))) {
				if (uncheckedCodeLine.substring(0, 1).equals("\s")) {
					uncheckedCodeLine = uncheckedCodeLine.replaceFirst("\\s+", "");
				}
				else {
					uncheckedCodeLine = uncheckedCodeLine.replaceFirst("\\t+", "");
				}
			}
			if (uncheckedCodeLine.length() == 0) {
				continue;
			}
			
			if (uncheckedCodeLine.length() >= 3 && uncheckedCodeLine.substring(0, 3).equals("for") && IN_FOR == false) {
				IN_FOR = true;
			}
			for (int charIndex = 0; charIndex < uncheckedCodeLine.length(); charIndex++) {
				if (charIndex < uncheckedCodeLine.length()-1 && uncheckedCodeLine.substring(charIndex, charIndex+2).equals("//") &&
						IN_COMMENT == false && IN_STRING == false) {
					if (charIndex != 0) {
						beautifiedCode.add(addTab(uncheckedCodeLine.substring(0, charIndex), BLOCK_LEVEL));
					}
					break;
				}
				else if (charIndex < uncheckedCodeLine.length()-1 && uncheckedCodeLine.substring(charIndex, charIndex+2).equals("/*") &&
						IN_COMMENT == false && IN_STRING == false) {
					if (charIndex != 0) {
						beautifiedCode.add(addTab(uncheckedCodeLine.substring(0, charIndex), BLOCK_LEVEL));
					}
					IN_COMMENT = true;
					continue;
				}
				else if (charIndex < uncheckedCodeLine.length()-1 && uncheckedCodeLine.substring(charIndex, charIndex+2).equals("*/") &&
						IN_COMMENT == true && IN_STRING == false) {
					IN_COMMENT = false;
					ArrayList<String> newCodeLines = new ArrayList<String>();
					newCodeLines.add(uncheckedCodeLine.substring(charIndex+2));
					beautifiedCode.addAll(codeBeautify(newCodeLines));
					break;
				}
				else if (uncheckedCodeLine.substring(charIndex, charIndex+1).equals("\"") && IN_COMMENT == false) {
					if (IN_STRING == false || charIndex == 0 || !uncheckedCodeLine.substring(charIndex-1, charIndex).equals("\\")) {
						IN_STRING = !IN_STRING;
					}
				}
				else if (uncheckedCodeLine.substring(charIndex, charIndex+1).equals("(") && IN_COMMENT == false && IN_STRING == false) {
					CONDITION_LEVEL++;
				}
				else if (uncheckedCodeLine.substring(charIndex, charIndex+1).equals(")") && IN_COMMENT == false && IN_STRING == false) {
					CONDITION_LEVEL--;
					if (CONDITION_LEVEL == 0) {
						IN_FOR = false;
					}
				}
				
				if (uncheckedCodeLine.substring(charIndex, charIndex+1).equals(";") &&
						IN_COMMENT == false && IN_STRING == false && IN_FOR == false) {
					beautifiedCode.add(addTab(uncheckedCodeLine.substring(0, charIndex+1), BLOCK_LEVEL));
					ArrayList<String> newCodeLines = new ArrayList<String>();
					newCodeLines.add(uncheckedCodeLine.substring(charIndex+1));
					beautifiedCode.addAll(codeBeautify(newCodeLines));
					break;
				}
				else if (uncheckedCodeLine.substring(charIndex, charIndex+1).equals("{") &&
						IN_COMMENT == false && IN_STRING == false && IN_FOR == false) {
					beautifiedCode.add(addTab(uncheckedCodeLine.substring(0, charIndex+1), BLOCK_LEVEL));
					BLOCK_LEVEL++;
					ArrayList<String> newCodeLines = new ArrayList<String>();
					newCodeLines.add(uncheckedCodeLine.substring(charIndex+1));
					beautifiedCode.addAll(codeBeautify(newCodeLines));
					break;
				}
				else if (uncheckedCodeLine.substring(charIndex, charIndex+1).equals("}") &&
						IN_COMMENT == false && IN_STRING == false && IN_FOR == false) {
					if (charIndex == 0) {
						BLOCK_LEVEL--;
						beautifiedCode.add(addTab(uncheckedCodeLine.substring(0, 1), BLOCK_LEVEL));
						if (uncheckedCodeLine.length() > 1) {
							ArrayList<String> newCodeLines = new ArrayList<String>();
							newCodeLines.add(uncheckedCodeLine.substring(charIndex+1));
							beautifiedCode.addAll(codeBeautify(newCodeLines));
						}
						break;
					}
					else {
						beautifiedCode.add(addTab(uncheckedCodeLine.substring(0, charIndex), BLOCK_LEVEL));
						ArrayList<String> newCodeLines = new ArrayList<String>();
						newCodeLines.add(uncheckedCodeLine.substring(charIndex+1));
						beautifiedCode.addAll(codeBeautify(newCodeLines));
						break;
					}
				}
				
				if (charIndex == uncheckedCodeLine.length()-1 && IN_COMMENT == false) {
					beautifiedCode.add(addTab(nowCodeLine, BLOCK_LEVEL));
					break;
				}
			}
			
			/*
			if (isKeywordStatement(uncheckedCodeLine) == 1) {
				
				for (int j = 0; j < nowCodeLine.length(); j++) {
					if (nowCodeLine.substring(j, j+1).equals("\\") &&
							nowCodeLine.substring(j+1, j+2).equals("\"")) {
						j++;
					}
					else if (nowCodeLine.substring(j, j+1).equals("\"")) {
						IN_STRING = !IN_STRING;
					}
					else if (nowCodeLine.substring(j, j+1).equals("{") && IN_STRING == false && j != nowCodeLine.length()) {
						
						beautifiedCode.add(nowCodeLine.substring(0, j+1));
						
						ArrayList<String> newCodeLines = new ArrayList<String>();
						newCodeLines.add(nowCodeLine.substring(j+1));
						//System.out.println(nowCodeLine.substring(0, j+1));
						beautifiedCode.addAll(codeBeautify(newCodeLines));
						break;
					}
					else if (j == nowCodeLine.length()) {
						beautifiedCode.add(nowCodeLine);
					}
				}
			}
			else {
				beautifiedCode.add(nowCodeLine);
			}*/
		}
		
		return beautifiedCode;
	}
}