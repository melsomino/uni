package org.unified;

import org.unified.uni.declaration.DeclarationElement;
import org.unified.uni.declaration.DeclarationString;
import org.unified.uni.lexer.TokenReader;
import org.unified.uni.declaration.DeclarationParser;


public class Main {

	private static void test(int name, String source, String expected) throws TokenReader.Error {
		if (expected == null) {
			expected = source;
		}
		DeclarationElement[] elements = DeclarationParser.parse(source);
		String actual = DeclarationString.toString(elements).trim();
		if (!actual.equals(expected)) {
			System.err.println(name + ": \"" + actual + "\" != \"" + expected + "\"");
		}
	}

	private static void test(int name, String source) throws TokenReader.Error {
		test(name, source, null);
	}

	public static void main(String[] args) {
		try {
			test(1, "element a=A  b=B\tc=C", "element a=A b=B c=C");
			test(2, "a");
			test(3, "a=A b c=(1 2 3)");
			test(4, "a=A\r\n\tb=B");
			test(5, "a=A\r\n\t~ b=B\r\n\tc=C\r\nd=D", "a=A b=B\r\n\tc=C\r\nd=D");
			test(6, "a='A\\r\\n\\t\\\\B'", "a=`A\r\n\t\\B`");
			test(7, "'\\x09\\u0009'", "`\t\t`");
			test(8, "`\t\r\n`");
		}
		catch (TokenReader.Error error) {
			error.printStackTrace();
		}
	}
}
