//
//  Parser.swift
//  Uni
//
//  Created by Michael Vlasov on 28.04.2018.
//  Copyright © 2018 Unified. All rights reserved.
//

import Foundation

public class Uni {
	
	public static func  parse(string: String) throws -> [UnElement] {
		let tokens = try TokenReader(string: string, tokenizer: tokenize)
		var elements = [UnElement]()
		if try tokens.pass(token: indentation_token) && tokens.passed.intValue == 0 {
			try parse(elements: &elements, indentation: 0, tokens: tokens)
		}
		if tokens.hasCurrent {
			throw TokenError(message: "Unexpected \"\(tokens.current)\" at \(tokens.currentContext)")
		}
		return elements
	}
	
	// Internals
	
	
	static func is_not_cr_or_lf(c: UnicodeScalar) -> Bool {
		return c != "\r" && c != "\n"
	}
	
	
	static func is_tab(c: UnicodeScalar) -> Bool {
		return c == "\t"
	}
	
	
	static let reservedUnicode = " \t\r\n()='`~#".unicodeScalars
	static func is_not_reserved(c: UnicodeScalar) -> Bool {
		return reservedUnicode.contains(c)
	}
	
	
	static func is_whitespace(c: UnicodeScalar) -> Bool {
		return c == " " || c == "\t"
	}
	
	
	static func parseBackQuotedString(chars: CharReader) throws -> Token {
		var string = ""
		let startPos = chars.currentPos
		while chars.hasCurrent {
			chars.passWhile { $0 != "`" }
			string += chars.passed
			if !chars.passChar("`") {
				throw TokenError(message: "Unterminated backquoted string at \(chars.contextAt(pos: startPos))")
			}
			if !chars.passChar("`") {
				break
			}
			string += "`"
		}
		return name_or_value_token.with(value: string)
	}
	
	
	static let escapedByChar: [UnicodeScalar: UnicodeScalar] = [
		"0": "\0",
		"\'": "\'",
		"\\": "\\",
		"n": "\n",
		"r": "\r",
		"t": "\t"]
	
	static let hexChars = CharacterSet(charactersIn: "0123456789ABCDEFabcdef")
	
	
	static func expectCharFromCodePoint(chars: CharReader, digits: Int) throws -> UnicodeScalar{
		var hex = [UnicodeScalar]()
		var digits = digits
		while digits > 0 {
			if !chars.hasCurrent || hexChars.contains(chars.current) {
				throw TokenError(message: "Invalid hex code string at \(chars.contextAt(pos: chars.currentPos))")
			}
			hex.append(chars.current)
			chars.next()
			digits -= 1
		}
		let hexString = String(String.UnicodeScalarView(hex))
		return UnicodeScalar(Int(hexString, radix: 16) ?? 0) ?? "\0"
	}
	
	
	static func parseSingleQuotedString(chars: CharReader) throws -> Token {
		var string = [UnicodeScalar]()
		let startPos = chars.currentPos
		while chars.hasCurrent {
			if chars.passChar("\'") {
				return name_or_value_token.with(value: string)
			}
			if chars.passChar("\\") {
				if !chars.hasCurrent {
					break
				}
				if let escaped = escapedByChar[chars.current] {
					string.append(escaped)
					chars.next()
				} else if chars.passChar("x") {
					string.append(try expectCharFromCodePoint(chars: chars, digits: 2))
				} else if chars.passChar("u") {
					string.append(try expectCharFromCodePoint(chars: chars, digits: 4))
				} else {
					throw TokenError(message: "Invalid escape sequence at \(chars.currentContext)")
				}
			} else {
				string.append(chars.current)
				chars.next()
			}
		}
		throw TokenError(message: "Unterminated backquoted string at \(chars.contextAt(pos: startPos))")
	}
	
	
	static func passLineEnd(chars: CharReader) -> Bool {
		if chars.current == "#" {
			chars.passWhile(is_not_cr_or_lf)
		}
		if chars.passChar("\r") {
			chars.passChar("\n")
			return true
		}
		if chars.passChar("\n") {
			return true
		}
		return false
	}
	
	
	static func parseIndentation(chars: CharReader) -> Token? {
		while chars.tag == nil || passLineEnd(chars: chars) {
			chars.tag = true
			let start = chars.nextPos
			chars.passWhile(is_tab)
			if !chars.hasCurrent {
				return nil
			}
			chars.passWhile(is_whitespace)
			if chars.current != "\r" && chars.current != "\n" && chars.current != "#" {
				let indentation = chars.unicode.distance(from: start, to: chars.nextPos)
				return indentation_token.with(value: indentation)
			}
		}
		return nil
	}
	
	
	static func tokenize(chars: CharReader) throws -> Token? {
		var token = parseIndentation(chars: chars)
		if let token = token {
			return token
		}
		guard chars.hasCurrent else {
			return nil
		}
		if chars.passChar("=") {
			token = name_value_separator_token
		} else if chars.passChar("~") {
			token = attribute_continuation_token
		} else if chars.passChar("(") {
			token = open_list_token
		} else if chars.passChar(")") {
			token = close_list_token
		} else if chars.passChar("\'") {
			token = try parseSingleQuotedString(chars: chars)
		} else if chars.passChar("`") {
			token = try parseBackQuotedString(chars: chars)
		} else if chars.passWhile(is_not_reserved) {
			token = name_or_value_token.with(value: chars.passed)
		} else {
			return nil
		}
		chars.passWhile(is_whitespace)
		return token
	}
	
	
	static func parse(attributes: inout [UnAttribute], tokens: TokenReader) throws {
		while try tokens.pass(token: name_or_value_token) {
			let name = tokens.passed.stringValue
			var value = UnAttribute.Value.missing
			if try tokens.pass(token: name_value_separator_token) {
				if try tokens.pass(token: open_list_token) {
					var items = [String]()
					while try tokens.pass(token: name_or_value_token) {
						items.append(tokens.passed.stringValue)
					}
					try tokens.pass(required: close_list_token)
					value = .array(items)
				} else {
					try tokens.pass(required: name_or_value_token)
					value = .string(tokens.passed.stringValue)
				}
			}
			attributes.append(UnAttribute(name: name, value: value))
		}
	}
	
	
	static func pass(expectedIndentation: Int, tokens: TokenReader) throws -> Bool {
		if tokens.hasCurrent && tokens.current.type == indentation_token.type &&
			tokens.current.intValue == expectedIndentation {
			try tokens.next()
			return true
		}
		return false
	}
	
	
	static func parseElement(to elements: inout [UnElement], indentation: Int, tokens: TokenReader) throws  {
		var attributes = [UnAttribute]()
		var children = [UnElement]()
		try parse(attributes: &attributes, tokens: tokens)
		while try pass(expectedIndentation: indentation + 1, tokens: tokens) {
			if try tokens.pass(token: attribute_continuation_token) {
				try parse(attributes: &attributes, tokens: tokens)
			} else {
				try parse(elements: &children, indentation: indentation + 1, tokens: tokens)
				break
			}
		}
		elements.append(UnElement(attributes: attributes, children: children))
	}
	
	
	static func parse(elements: inout [UnElement], indentation: Int, tokens: TokenReader) throws {
		repeat {
			try parseElement(to: &elements, indentation: indentation, tokens: tokens)
		} while try pass(expectedIndentation: indentation, tokens: tokens)
	}
	
	
	static let name_or_value_token = Token.register(name: "name or value")
	static let name_value_separator_token = Token.register(name: "=")
	static let attribute_continuation_token = Token.register(name: "~")
	static let indentation_token = Token.register(name: "⇥")
	static let open_list_token = Token.register(name: "(")
	static let close_list_token = Token.register(name: ")")
}
