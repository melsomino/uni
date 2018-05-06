package org.unified.uni.declaration;

import org.unified.uni.lexer.CharReader;
import org.unified.uni.lexer.LexicalToken;
import org.unified.uni.lexer.TokenReader;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Uni Declaration Parser
 * <p>
 * Created by Michael Vlasov on 19.04.2018.
 * <p>
 * Copyright (c) 2018 Michael Vlasov
 */

public class DeclarationParser {

	public static DeclarationElement[] parse(String source) throws TokenReader.Error {
		return parse(new CharReader(source));
	}


	public static DeclarationElement[] parse(CharReader chars) throws TokenReader.Error {
		TokenReader tokens = new TokenReader(chars, DeclarationParser::tokenize);
		ArrayList<DeclarationElement> elements = new ArrayList<>();
		if (tokens.pass(indentation) && tokens.getPassed().valueInt() == 0) {
			parseElements(tokens, elements, 0);
		}
		if (tokens.isHasCurrent()) {
			throw new TokenReader.Error("Unexpected \"" + tokens.getCurrent().toString() + "\" at " + tokens.currentContext());
		}
		return elements.toArray(DeclarationElement.emptyArray);
	}


	// Internals


	private static boolean isNotCrOrLf(char c) {
		return c != '\r' && c != '\n';
	}


	private static boolean isTab(char c) {
		return c == '\t';
	}


	private static boolean isNotReserved(char c) {
		return " \t\r\n()='`~#".indexOf(c) < 0;
	}


	private static boolean isWhitespace(char c) {
		return (c == ' ') || (c == '\t');
	}


	private static LexicalToken parseBackquotedString(CharReader chars) throws TokenReader.Error {
		StringBuilder string = new StringBuilder();
		int startPos = chars.getCurrentPos();
		while (chars.isHasCurrent()) {
			chars.passWhile((c) -> c != '`');
			string.append(chars.passed());
			if (!chars.passChar('`')) {
				throw new TokenReader.Error("Unterminated backquoted string at " + chars.contextAt(startPos));
			}
			if (!chars.passChar('`')) {
				break;
			}
			string.append('`');
		}
		return nameOrValue.with(string.toString());
	}


	private static HashMap<Character, Character> escapedByChar = new HashMap<Character, Character>() {{
		put('0', '\0');
		put('\'', '\'');
		put('\\', '\\');
		put('n', '\n');
		put('r', '\r');
		put('t', '\t');
		put('b', '\b');
		put('f', '\f');
	}};


	private static char expectCharFromCodePoint(CharReader chars, int digits) throws TokenReader.Error {
		StringBuilder hex = new StringBuilder();
		while (digits > 0) {
			String hexChars = "0123456789ABCDEFabcdef";
			if (!chars.isHasCurrent() || hexChars.indexOf(chars.getCurrent()) < 0) {
				throw new TokenReader.Error("Invalid hex code string at " + chars.contextAt(chars.getCurrentPos()));
			}
			hex.append(chars.getCurrent());
			chars.next();
			digits -= 1;
		}
		return Character.toChars(Integer.parseInt(hex.toString(), 16))[0];
	}


	private static LexicalToken parseSinglequotedString(CharReader chars) throws TokenReader.Error {
		StringBuilder string = new StringBuilder();
		int startPos = chars.getCurrentPos();
		while (chars.isHasCurrent()) {
			if (chars.passChar('\'')) {
				return nameOrValue.with(string.toString());
			}
			if (chars.passChar('\\')) {
				if (!chars.isHasCurrent()) {
					break;
				}
				Character escaped = escapedByChar.get(chars.getCurrent());
				if (escaped != null) {
					string.append(escaped);
					chars.next();
				} else if (chars.passChar('x')) {
					string.append(expectCharFromCodePoint(chars, 2));
				} else if (chars.passChar('u')) {
					string.append(expectCharFromCodePoint(chars, 4));
				} else {
					throw new TokenReader.Error("Invalid escape sequence at " + chars.currentContext());
				}
			} else {
				string.append(chars.getCurrent());
				chars.next();
			}
		}
		throw new TokenReader.Error("Unterminated backquoted string at " + chars.contextAt(startPos));
	}


	private static boolean passLineEnd(CharReader chars) {
		if (chars.getCurrent() == '#') {
			chars.passWhile(DeclarationParser::isNotCrOrLf);
		}
		if (chars.passChar('\r')) {
			chars.passChar('\n');
			return true;
		}
		return chars.passChar('\n');
	}


	private static LexicalToken parseIndentation(CharReader chars) {
		while (chars.getTag() == null || passLineEnd(chars)) {
			chars.setTag(true);
			int start = chars.getNextPos();
			chars.passWhile(DeclarationParser::isTab);
			if (!chars.isHasCurrent()) {
				return null;
			}
			chars.passWhile(DeclarationParser::isWhitespace);
			char c = chars.getCurrent();
			if (c != '\r' && c != '\n' && c != '#') {
				return indentation.with(chars.getNextPos() - start);
			}
		}
		return null;
	}


	private static LexicalToken tokenize(CharReader chars) throws TokenReader.Error {
		LexicalToken token = parseIndentation(chars);
		if (token != null) {
			return token;
		}
		if (!chars.isHasCurrent()) {
			return null;
		}
		if (chars.passChar('=')) {
			token = nameValueSeparator;
		} else if (chars.passChar('~')) {
			token = attributeContinuation;
		} else if (chars.passChar('(')) {
			token = openList;
		} else if (chars.passChar(')')) {
			token = closeList;
		} else if (chars.passChar('\'')) {
			token = parseSinglequotedString(chars);
		} else if (chars.passChar('`')) {
			token = parseBackquotedString(chars);
		} else if (chars.passWhile(DeclarationParser::isNotReserved)) {
			token = nameOrValue.with(chars.passed());
		} else {
			return null;
		}
		chars.passWhile(DeclarationParser::isWhitespace);
		return token;
	}


	private static void parseAttributes(TokenReader tokens, ArrayList<DeclarationAttribute> attributes) throws TokenReader.Error {
		while (tokens.pass(nameOrValue)) {
			String name = tokens.getPassed().valueString();
			Object value = null;
			if (tokens.pass(nameValueSeparator)) {
				if (tokens.pass(openList)) {
					ArrayList<String> arrayBuilder = new ArrayList<>();
					while (tokens.pass(nameOrValue)) {
						arrayBuilder.add(tokens.getPassed().valueString());
					}
					tokens.passRequired(closeList);
					value = arrayBuilder.toArray();
				} else {
					tokens.passRequired(nameOrValue);
					value = tokens.getPassed().valueString();
				}
			}
			attributes.add(new DeclarationAttribute(name, value));
		}
	}


	private static boolean passIndentation(TokenReader tokens, int expectedIndentation) throws TokenReader.Error {
		if (tokens.isHasCurrent() && tokens.getCurrent().name == indentation.name &&
			tokens.getCurrent().valueInt() == expectedIndentation) {
			tokens.next();
			return true;
		}
		return false;
	}


	private static void parseElement(TokenReader tokens, ArrayList<DeclarationElement> elements, int indentation) throws TokenReader.Error {
		ArrayList<DeclarationAttribute> attributes = new ArrayList<>();
		ArrayList<DeclarationElement> children = new ArrayList<>();
		parseAttributes(tokens, attributes);
		while (passIndentation(tokens, indentation + 1)) {
			if (tokens.pass(attributeContinuation)) {
				parseAttributes(tokens, attributes);
			} else {
				parseElements(tokens, children, indentation + 1);
				break;
			}
		}
		elements.add(new DeclarationElement(attributes.toArray(DeclarationAttribute.emptyArray), children.toArray(DeclarationElement.emptyArray)));
	}


	private static void parseElements(TokenReader tokens, ArrayList<DeclarationElement> elements, int indentation) throws TokenReader.Error {
		do {
			parseElement(tokens, elements, indentation);
		} while (passIndentation(tokens, indentation));
	}


	private static LexicalToken nameOrValue = LexicalToken.register("name or value");
	private static LexicalToken nameValueSeparator = LexicalToken.register("=");
	private static LexicalToken attributeContinuation = LexicalToken.register("~");
	private static LexicalToken indentation = LexicalToken.register("⇥");
	private static LexicalToken openList = LexicalToken.register("(");
	private static LexicalToken closeList = LexicalToken.register(")");

}
