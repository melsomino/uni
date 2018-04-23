package org.unified;

import org.unified.uni.Element;
import org.unified.uni.Token_reader;
import org.unified.uni.Uni;


public class Main {

	public static void main(String[] args) {
		try {
			String source = "element '\\t' a=b\r\n\tchild c=(a b '\\t\\'\')";
			Element[] elements = Uni.parse(source);
			String lines = Element.to_string(elements);
			System.out.println(lines);
		}
		catch (Token_reader.Error error) {
			error.printStackTrace();
		}
	}
}
