//
//  Token.swift
//  Uni
//
//  Created by Michael Vlasov on 28.04.2018.
//  Copyright © 2018 Unified. All rights reserved.
//

import Foundation

public struct LexicalToken: CustomStringConvertible {
	public enum Value {
		case missing
		case string(String)
		case int(Int)
	}
	public let name: Int
	public let value: Value
	
	private init(name: Int, value: Value) {
		self.name = name
		self.value = value
	}
	
	public var nameString: String {
		return LexicalToken.names[name]
	}
	
	public var valueString: String {
		switch value {
		case .missing:
			return ""
		case .string(let string):
			return string
		case .int(let int):
			return String(describing: int)
		}
	}
	
	public var valueInt: Int {
		switch value {
		case .missing:
			return 0
		case .string(let string):
			return Int(string) ?? 0
		case .int(let int):
			return int
		}
	}
	
	public func with(value: Value) -> LexicalToken {
		return LexicalToken(name: name, value: value)
	}
	
	
	public var description: String {
		if case .missing = value {
			return nameString
		}
		return "(\(nameString), \(valueString))"
	}
	
	// MARK: - Static

	public static func register(_ nameString: String) -> LexicalToken {
		let name = LexicalToken.names.count
		LexicalToken.names.append(nameString)
		return LexicalToken(name: name, value: .missing)
	}
	
	
	public static let empty = LexicalToken(name: 0, value: .missing)
	
	// MARK: - Internals
	
	private static var names = [""]
}


