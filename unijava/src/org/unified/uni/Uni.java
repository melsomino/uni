package org.unified.uni;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Uni parser
 * <p>
 * Created by Michael Vlasov on 19.04.2018.
 * <p>
 * Copyright (c) 2017 Michael Vlasov
 */

public class Uni {

	public static Element[] parse(String source) throws Token_reader.Error {
		Token_reader reader = new Token_reader(source, Uni::tokenize);
		ArrayList<Element> elements = new ArrayList<>();
		if (reader.pass(indentation_token) && (Integer) reader.passed.value == 0) {
			parse_elements(reader, elements, 0);
		}
		if (reader.has_current) {
			throw new Token_reader.Error("Unexpected \"" + reader.current.toString() + "\" at " + reader.current_context());
		}
		return elements.toArray(Element.emptyArray);
	}

	// Internals


	static boolean is_not_cr_or_lf(char c) {
		return c != '\r' && c != '\n';
	}


	static boolean is_tab(char c) {
		return c == '\t';
	}


	static boolean is_not_reserved(char c) {
		return " \t\r\n()='`~#".indexOf(c) < 0;
	}


	static boolean is_whitespace(char c) {
		return (c == ' ') || (c == '\t');
	}


	static Token parse_backquoted_string(Char_reader input) throws Token_reader.Error {
		StringBuilder string = new StringBuilder();
		int start_pos = input.current_pos;
		while (input.has_current) {
			input.pass_while((c) -> c != '`');
			string.append(input.passed());
			if (!input.pass_char('`')) {
				throw new Token_reader.Error("Unterminated backquoted string at " + input.context_at(start_pos));
			}
			if (!input.pass_char('`')) {
				break;
			}
			string.append('`');
		}
		return name_or_value_token.with(string.toString());
	}


	static HashMap<Character, Character> escaped_by_char = new HashMap<Character, Character>() {{
		put('0', '\0');
		put('\'', '\'');
		put('\\', '\\');
		put('n', '\n');
		put('r', '\r');
		put('t', '\t');
		put('b', '\b');
		put('f', '\f');
	}};
	static String hex_chars = "0123456789ABCDEFabcdef";


	static char expect_char_from_code_point(Char_reader input, int digits) throws Token_reader.Error {
		StringBuilder hex = new StringBuilder();
		while (digits > 0) {
			if (!input.has_current || hex_chars.indexOf(input.current) < 0) {
				throw new Token_reader.Error("Invalid hex code string at " + input.context_at(input.current_pos));
			}
			hex.append(input.current);
			input.next();
			digits -= 1;
		}
		return Character.toChars(Integer.parseInt(hex.toString(), 16))[0];
	}


	static Token parse_singlequoted_string(Char_reader input) throws Token_reader.Error {
		StringBuilder string = new StringBuilder();
		int start_pos = input.current_pos;
		while (input.has_current) {
			if (input.pass_char('\'')) {
				return name_or_value_token.with(string.toString());
			}
			if (input.pass_char('\\')) {
				if (!input.has_current) {
					break;
				}
				Character escaped = escaped_by_char.get(input.current);
				if (escaped != null) {
					string.append(escaped);
					input.next();
				} else if (input.pass_char('x')) {
					string.append(expect_char_from_code_point(input, 2));
				} else if (input.pass_char('u')) {
					string.append(expect_char_from_code_point(input, 4));
				} else {
					throw new Token_reader.Error("Invalid escape sequence at " + input.current_context());
				}
			} else {
				string.append(input.current);
				input.next();
			}
		}
		throw new Token_reader.Error("Unterminated backquoted string at " + input.context_at(start_pos));
	}


	static boolean pass_line_end(Char_reader input) {
		if (input.current == '#') {
			input.pass_while(Uni::is_not_cr_or_lf);
		}
		if (input.pass_char('\r')) {
			input.pass_char('\n');
			return true;
		}
		if (input.pass_char('\n')) {
			return true;
		}
		return false;
	}


	static Token parse_indentation(Char_reader input) {
		while (input.tag == null || pass_line_end(input)) {
			input.tag = true;
			int start = input.next_pos;
			input.pass_while(Uni::is_tab);
			if (!input.has_current) {
				return null;
			}
			input.pass_while(Uni::is_whitespace);
			if (input.current != '\r' && input.current != '\n' && input.current != '#') {
				return indentation_token.with(input.next_pos - start);
			}
		}
		return null;
	}


	static Token tokenize(Char_reader input) throws Token_reader.Error {
		Token token = parse_indentation(input);
		if (token != null) {
			return token;
		}
		if (!input.has_current) {
			return null;
		}
		if (input.pass_char('=')) {
			token = name_value_separator_token;
		} else if (input.pass_char('~')) {
			token = attribute_continuation_token;
		} else if (input.pass_char('(')) {
			token = open_list_token;
		} else if (input.pass_char(')')) {
			token = close_list_token;
		} else if (input.pass_char('\'')) {
			token = parse_singlequoted_string(input);
		} else if (input.pass_char('`')) {
			token = parse_backquoted_string(input);
		} else if (input.pass_while(Uni::is_not_reserved)) {
			token = name_or_value_token.with(input.passed());
		} else {
			return null;
		}
		input.pass_while(Uni::is_whitespace);
		return token;
	}


	static void parse_attributes(Token_reader reader, ArrayList<Attribute> attributes) throws Token_reader.Error {
		while (reader.pass(name_or_value_token)) {
			String name = reader.passed.value.toString();
			Object value = null;
			if (reader.pass(name_value_separator_token)) {
				if (reader.pass(open_list_token)) {
					ArrayList arrayBuilder = new ArrayList();
					while (reader.pass(name_or_value_token)) {
						arrayBuilder.add(reader.passed.value);
					}
					reader.pass_required(close_list_token);
					value = arrayBuilder.toArray();
				} else {
					reader.pass_required(name_or_value_token);
					value = reader.passed.value;
				}
			}
			attributes.add(new Attribute(name, value));
		}
	}


	static boolean pass_indentation(Token_reader reader, int expected_indentation) throws Token_reader.Error {
		if (reader.has_current && reader.current.type == indentation_token.type &&
			(Integer) reader.current.value == expected_indentation) {
			reader.next();
			return true;
		}
		return false;
	}


	static void parse_element(Token_reader reader, ArrayList<Element> elements, int indentation) throws Token_reader.Error {
		ArrayList<Attribute> attributes = new ArrayList<>();
		ArrayList<Element> children = new ArrayList<>();
		parse_attributes(reader, attributes);
		while (pass_indentation(reader, indentation + 1)) {
			if (reader.pass(attribute_continuation_token)) {
				parse_attributes(reader, attributes);
			} else {
				parse_elements(reader, children, indentation + 1);
				break;
			}
		}
		elements.add(new Element(attributes.toArray(Attribute.emptyArray), children.toArray(Element.emptyArray)));
	}


	static void parse_elements(Token_reader reader, ArrayList<Element> elements, int indentation) throws Token_reader.Error {
		do {
			parse_element(reader, elements, indentation);
		} while (pass_indentation(reader, indentation));
	}


	static Token name_or_value_token = Token.register("name or value");
	static Token name_value_separator_token = Token.register("=");
	static Token attribute_continuation_token = Token.register("~");
	static Token indentation_token = Token.register("⇥");
	static Token open_list_token = Token.register("(");
	static Token close_list_token = Token.register(")");

}
