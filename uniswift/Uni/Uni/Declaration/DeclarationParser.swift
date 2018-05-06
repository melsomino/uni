//
//  Parser.swift
//  Uni
//
//  Created by Michael Vlasov on 28.04.2018.
//  Copyright © 2018 Unified. All rights reserved.
//

import Foundation

public class DeclarationParser {
	
	public static func parse(string: String) throws -> [DeclarationElement] {
		return try parse(chars: CharReader(string: string))
	}
	

	public static func parse(chars: CharReader) throws -> [DeclarationElement] {
		let tokens = try TokenReader(chars: chars, tokenizer: tokenize)
		var elements = [DeclarationElement]()
		if try tokens.pass(token: indentation) && tokens.passed.valueInt == 0 {
			try parse(elements: &elements, indentation: 0, tokens: tokens)
		}
		if tokens.hasCurrent {
			throw ParsingError.unexpected(token: tokens.current, context: tokens.currentContext)
		}
		return elements
	}

	// Internals
	
	private static func isNotCrOrLf(c: UnicodeScalar) -> Bool {
		return c != "\r" && c != "\n"
	}
	
	
	private static func isTab(c: UnicodeScalar) -> Bool {
		return c == "\t"
	}
	
	
	private static let reservedUnicode = " \t\r\n()='`~#".unicodeScalars
	private static func isNotReserved(c: UnicodeScalar) -> Bool {
		return !reservedUnicode.contains(c)
	}
	
	
	private static func isWhitespace(c: UnicodeScalar) -> Bool {
		return c == " " || c == "\t"
	}
	
	
	private static func parseBackQuotedString(chars: CharReader) throws -> LexicalToken {
		var string = ""
		let startPos = chars.currentPos
		while chars.hasCurrent {
			chars.passWhile { $0 != "`" }
			string += chars.passed
			if !chars.passChar("`") {
				throw ParsingError.error(message: "Unterminated backquoted string", context: chars.contextAt(pos: startPos))
			}
			if !chars.passChar("`") {
				break
			}
			string += "`"
		}
		return nameOrValue.with(value: .string(string))
	}
	
	
	private static let escapedByChar: [UnicodeScalar: UnicodeScalar] = [
		"0": "\0",
		"\'": "\'",
		"\\": "\\",
		"n": "\n",
		"r": "\r",
		"t": "\t"]
	
	private static let hexChars = CharacterSet(charactersIn: "0123456789ABCDEFabcdef")
	
	
	private static func expectCharFromCodePoint(chars: CharReader, digits: Int) throws -> UnicodeScalar{
		var hex = [UnicodeScalar]()
		var digits = digits
		while digits > 0 {
			if !chars.hasCurrent || !hexChars.contains(chars.current) {
				throw ParsingError.error(message: "Invalid hex code string", context: chars.contextAt(pos: chars.currentPos))
			}
			hex.append(chars.current)
			chars.next()
			digits -= 1
		}
		let hexString = String(String.UnicodeScalarView(hex))
		return UnicodeScalar(Int(hexString, radix: 16) ?? 0) ?? "\0"
	}
	
	
	private static func parseSingleQuotedString(chars: CharReader) throws -> LexicalToken {
		var unicode = [UnicodeScalar]()
		let startPos = chars.currentPos
		while chars.hasCurrent {
			if chars.passChar("\'") {
				return nameOrValue.with(value: .string(String(String.UnicodeScalarView(unicode))))
			}
			if chars.passChar("\\") {
				if !chars.hasCurrent {
					break
				}
				if let escaped = escapedByChar[chars.current] {
					unicode.append(escaped)
					chars.next()
				} else if chars.passChar("x") {
					unicode.append(try expectCharFromCodePoint(chars: chars, digits: 2))
				} else if chars.passChar("u") {
					unicode.append(try expectCharFromCodePoint(chars: chars, digits: 4))
				} else {
					throw ParsingError.error(message: "Invalid escape sequence", context: chars.currentContext)
				}
			} else {
				unicode.append(chars.current)
				chars.next()
			}
		}
		throw ParsingError.error(message: "Unterminated backquoted string", context: chars.contextAt(pos: startPos))
	}
	
	
	private static func passLineEnd(chars: CharReader) -> Bool {
		if chars.current == "#" {
			chars.passWhile(isNotCrOrLf)
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
	
	
	private static func parseIndentation(chars: CharReader) -> LexicalToken? {
		while chars.tag == nil || passLineEnd(chars: chars) {
			chars.tag = true
			let start = chars.nextPos
			chars.passWhile(isTab)
			if !chars.hasCurrent {
				return nil
			}
			chars.passWhile(isWhitespace)
			if chars.current != "\r" && chars.current != "\n" && chars.current != "#" {
				let level = chars.unicode.distance(from: start, to: chars.nextPos)
				return indentation.with(value: .int(level))
			}
		}
		return nil
	}
	
	
	private static func tokenize(chars: CharReader) throws -> LexicalToken? {
		var token = parseIndentation(chars: chars)
		if let token = token {
			return token
		}
		guard chars.hasCurrent else {
			return nil
		}
		if chars.passChar("=") {
			token = nameValueSeparator
		} else if chars.passChar("~") {
			token = attributeContinuation
		} else if chars.passChar("(") {
			token = openList
		} else if chars.passChar(")") {
			token = closeList
		} else if chars.passChar("\'") {
			token = try parseSingleQuotedString(chars: chars)
		} else if chars.passChar("`") {
			token = try parseBackQuotedString(chars: chars)
		} else if chars.passWhile(isNotReserved) {
			token = nameOrValue.with(value: .string(String(chars.passed)))
		} else {
			return nil
		}
		chars.passWhile(isWhitespace)
		return token
	}
	
	
	private static func parse(attributes: inout [DeclarationAttribute], tokens: TokenReader) throws {
		while try tokens.pass(token: nameOrValue) {
			let name = tokens.passed.valueString
			var value = DeclarationAttribute.Value.missing
			if try tokens.pass(token: nameValueSeparator) {
				if try tokens.pass(token: openList) {
					var items = [String]()
					while try tokens.pass(token: nameOrValue) {
						items.append(tokens.passed.valueString)
					}
					try tokens.pass(required: closeList)
					value = .array(items)
				} else {
					try tokens.pass(required: nameOrValue)
					value = .string(tokens.passed.valueString)
				}
			}
			attributes.append(DeclarationAttribute(name: name, value: value))
		}
	}
	
	
	private static func pass(expectedIndentation: Int, tokens: TokenReader) throws -> Bool {
		if tokens.hasCurrent && tokens.current.name == indentation.name &&
			tokens.current.valueInt == expectedIndentation {
			try tokens.next()
			return true
		}
		return false
	}
	
	
	private static func parseElement(to elements: inout [DeclarationElement], indentation: Int, tokens: TokenReader) throws  {
		var attributes = [DeclarationAttribute]()
		var children = [DeclarationElement]()
		try parse(attributes: &attributes, tokens: tokens)
		while try pass(expectedIndentation: indentation + 1, tokens: tokens) {
			if try tokens.pass(token: attributeContinuation) {
				try parse(attributes: &attributes, tokens: tokens)
			} else {
				try parse(elements: &children, indentation: indentation + 1, tokens: tokens)
				break
			}
		}
		elements.append(DeclarationElement(attributes: attributes, children: children))
	}
	
	
	private static func parse(elements: inout [DeclarationElement], indentation: Int, tokens: TokenReader) throws {
		repeat {
			try parseElement(to: &elements, indentation: indentation, tokens: tokens)
		} while try pass(expectedIndentation: indentation, tokens: tokens)
	}
	

	static let nameOrValue = LexicalToken.register("name or value")
	static let nameValueSeparator = LexicalToken.register("=")
	static let attributeContinuation = LexicalToken.register("~")
	static let indentation = LexicalToken.register("⇥")
	static let openList = LexicalToken.register("(")
	static let closeList = LexicalToken.register(")")
}
