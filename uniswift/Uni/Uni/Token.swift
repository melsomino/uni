//
//  Token.swift
//  Uni
//
//  Created by Michael Vlasov on 28.04.2018.
//  Copyright © 2018 Unified. All rights reserved.
//

import Foundation

public struct Token: CustomStringConvertible {
	public let type: Int
	public let value: Any?
	
	private init(type: Int, value: Any?) {
		self.type = type
		self.value = value
	}
	
	
	public var name: String {
		return Token.names[type]
	}
	
	public var stringValue: String {
		return value as? String ?? ""
	}
	
	public var intValue: Int {
		return value as? Int ?? 0
	}
	
	public static func register(name: String) -> Token {
		let type = Token.names.count
		Token.names.append(name)
		return Token(type: type, value: "")
	}
	
	
	public func with(value: Any) -> Token {
		return Token(type: type, value: value)
	}
	
	
	public var description: String {
		let name = Token.names[type]
		if let value = value {
			return "\(name): [\(value)]"
		}
		return name
	}
	
	// MARK: - Static
	
	public static let empty = Token(type: 0, value: nil)
	
	// MARK: - Internals
	
	private static var names = [""]
}


public class TokenError: Swift.Error {
	public let message: String
	
	init(message: String) {
		self.message = message
	}
	
	public var localizedDescription: String {
		return message
	}
	
}

public class TokenReader {
	
	public private(set) var hasCurrent: Bool
	public private(set) var passed = Token.empty
	public private(set) var current = Token.empty
	
	public init(string: String, tokenizer: @escaping (CharReader) throws -> Token?) throws {
		self.tokenizer = tokenizer
		chars = CharReader(string: string)
		currentPos = chars.currentPos
		hasCurrent = true
		passed = .empty
		current = .empty
		try next()
	}
	
	
	public var currentContext: String {
		return chars.contextAt(pos: currentPos)
	}
	
	
	public func pass(token: Token) throws -> Bool {
		if !hasCurrent || current.type != token.type {
			return false
		}
		passed = current
		try next()
		return true
	}
	
	
	public func pass(required: Token) throws {
		if !(try pass(token: required)) {
			throw TokenError(message: "Expected \(required.name) at: \(currentContext)")
		}
	}
	
	
	public func next() throws {
		if !hasCurrent {
			return
		}
		if !chars.hasCurrent {
			current = .empty
			hasCurrent = false
			return
		}
		currentPos = chars.currentPos
		guard let token = try tokenizer(chars) else {
			current = .empty
			hasCurrent = false
			if chars.hasCurrent {
				throw TokenError(message: "Error at \(currentContext)")
			}
			return
		}
		current = token
	}
	
	// MARK: - Internals
	
	private let chars: CharReader
	private var currentPos: String.UnicodeScalarIndex
	private let tokenizer: (CharReader) throws -> Token?
}

