
//  UnElement.swift
//  Uni
//
//  Created by Michael Vlasov on 27.04.2018.
//  Copyright © 2018 Unified. All rights reserved.
//

import Foundation

public struct DeclarationAttribute {
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
}
