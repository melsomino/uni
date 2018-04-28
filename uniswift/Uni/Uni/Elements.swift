//
//  UnElement.swift
//  Uni
//
//  Created by Michael Vlasov on 27.04.2018.
//  Copyright © 2018 Unified. All rights reserved.
//

import Foundation

public struct UnAttribute {
	public enum Value {
		case missing
		case string(String)
		case array([String])
	}
	
	public let name: String
	public let value: Value
	
	init(name: String, value: Value) {
		self.name = name
		self.value = value
	}
	
	public func append(to string: inout String) {
		switch value {
		case .missing:
			string += UnAttribute.quotedIfRequired(string: name)
		case .string(let value):
			string += UnAttribute.quotedIfRequired(string: name) + "=" + UnAttribute.quotedIfRequired(string: value)
		case .array(let value):
			string += "(" + value.map {UnAttribute.quotedIfRequired(string: $0) }.joined(separator: " ") + ")"
			
		}
	}
	
	
	public static func append(attributes: [UnAttribute], to string: inout String) {
		var isFirst = true
		for attribute in attributes {
			if isFirst {
				isFirst = false
			} else {
				string += " "
			}
			attribute.append(to: &string)
		}
	}
	
	// MARK: - Internals
	
	private static let reservedChars = CharacterSet(charactersIn: " \\t\\r\\n()='`~#")
	fileprivate static func quotedIfRequired(string: String) -> String {
		return string.rangeOfCharacter(from: reservedChars) != nil
			? "`" + string.replacingOccurrences(of: "`", with: "``") + "`"
			: string
	}
	
}


public struct UnElement {
	public let attributes: [UnAttribute]
	public let children: [UnElement]
	
	public init(attributes: [UnAttribute], children: [UnElement]) {
		self.attributes = attributes
		self.children = children
	}
	
	public func append(to string: inout String, indentation: String) {
		string += indentation
		UnAttribute.append(attributes: attributes, to: &string)
		string += "\r\n"
		UnElement.append(elements: children, indentation: indentation + "\t", to: &string)
	}
	
	public static func append(elements: [UnElement], indentation: String, to string: inout String) {
		for element in elements {
			element.append(to: &string, indentation: indentation)
		}
	}
}
