//
//  TokenReader.swift
//  Uni
//
//  Created by Michael Vlasov on 06.05.2018.
//  Copyright © 2018 Unified. All rights reserved.
//

import Foundation

public enum ParsingError: Error, CustomStringConvertible, CustomDebugStringConvertible {
	case expected(required: LexicalToken, context: String)
	case unexpected(token: LexicalToken, context: String)
	case error(message: String, context: String)
	
	public var localizedDescription: String {
		switch self {
		case .expected(let required, let context):
			return "Expected \(required.nameString) at: \(context)"
		case .unexpected(let token, let context):
			return "Unexpected \(token.nameString) at: \(context)"
		case .error(let message, let context):
			return "\(message) at \(context)"
		}
	}
	
	public var description: String {
		return localizedDescription
	}
	
	public var debugDescription: String {
		return localizedDescription
	}
}


public class TokenReader {
	
	public private(set) var hasCurrent: Bool
	public private(set) var passed = LexicalToken.empty
	public private(set) var current = LexicalToken.empty

	
	public init(chars: CharReader, tokenizer: @escaping (CharReader) throws -> LexicalToken?) throws {
		self.tokenizer = tokenizer
		self.chars = chars
		currentPos = chars.currentPos
		hasCurrent = true
		passed = .empty
		current = .empty
		try next()
	}
	
	
	public var currentContext: String {
		return chars.contextAt(pos: currentPos)
	}
	
	
	public func pass(token: LexicalToken) throws -> Bool {
		guard hasCurrent && current.name == token.name else {
			return false
		}
		passed = current
		try next()
		return true
	}
	
	
	public func pass(required: LexicalToken) throws {
		guard try pass(token: required) else {
			throw ParsingError.expected(required: required, context: currentContext)
		}
	}
	
	
	public func next() throws {
		guard hasCurrent else {
			return
		}
		guard chars.hasCurrent else {
			current = .empty
			hasCurrent = false
			return
		}
		currentPos = chars.currentPos
		guard let token = try tokenizer(chars) else {
			hasCurrent = false
			if chars.hasCurrent {
				throw ParsingError.unexpected(token: current, context: currentContext)
			}
			return
		}
		current = token
	}
	
	// MARK: - Internals
	
	private let chars: CharReader
	private var currentPos: String.UnicodeScalarIndex
	private let tokenizer: (CharReader) throws -> LexicalToken?
}

