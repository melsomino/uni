//
//  String+Declaration.swift
//  Uni
//
//  Created by Michael Vlasov on 06.05.2018.
//  Copyright © 2018 Unified. All rights reserved.
//

import Foundation

public extension String {
	public init(declaration: [DeclarationElement]) {
		self.init()
		append(elements: declaration, indentation: "")
	}

	
	public mutating func append(elements: [DeclarationElement], indentation: String) {
		for element in elements {
			append(element: element, indentation: indentation)
		}
	}


	public mutating func append(element: DeclarationElement, indentation: String) {
		append(indentation)
		append(attributes: element.attributes)
		append("\r\n")
		append(elements: element.children, indentation: indentation + "\t")
	}


	public mutating func append(attributes: [DeclarationAttribute]) {
		var isFirst = true
		for attribute in attributes {
			if isFirst {
				isFirst = false
			} else {
				append(" ")
			}
			append(attribute: attribute)
		}
	}

	
	public mutating func append(attribute: DeclarationAttribute) {
		append(attribute.name.quotedIfRequired)
		switch attribute.value {
		case .missing:
			break
		case .string(let value):
			append("=")
			append(value.quotedIfRequired)
		case .array(let value):
			append("=(")
			append(value.map { $0.quotedIfRequired }.joined(separator: " "))
			append(")")
		}
	}

	// MARK: - Internals
	
	private static let reservedChars = CharacterSet(charactersIn: " \t\r\n()='`\"~#")
	private var quotedIfRequired: String {
		let range = rangeOfCharacter(from: String.reservedChars)
		return range != nil
			? "`" + replacingOccurrences(of: "`", with: "``") + "`"
			: self
	}
}
