//
//  UniTests.swift
//  UniTests
//
//  Created by Michael Vlasov on 26.04.2018.
//  Copyright © 2018 Unified. All rights reserved.
//

import XCTest
@testable import Uni

class UniTests: XCTestCase {

	private func test(source: String, expected: String? = nil) {
		do {
			let elements = try DeclarationParser.parse(string: source)
			let actual = String(declaration: elements).trimmingCharacters(in: .newlines)
			XCTAssertEqual(expected ?? source, actual)
		} catch {
			XCTFail(String(describing: error))
		}
	}
	
	
	func testDeclarationParser() throws {
		test(source: "element a=A  b=B\tc=C", expected: "element a=A b=B c=C")
		test(source: "a")
		test(source: "a=A b c=(1 2 3)")
		test(source: "a=A\r\n\tb=B")
		test(source: "a=A\r\n\t~ b=B\r\n\tc=C\r\nd=D", expected: "a=A b=B\r\n\tc=C\r\nd=D")
		test(source: "a='A\\r\\n\\t\\\\B'", expected: "a=`A\r\n\t\\B`")
		test(source: "'\\x09\\u0009'", expected: "`\t\t`")
		test(source: "`\t\r\n`")
	}
}
