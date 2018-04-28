//
//  CharReader.swift
//  Uni
//
//  Created by Michael Vlasov on 26.04.2018.
//  Copyright © 2018 Unified. All rights reserved.
//

import Foundation

public final class CharReader {
	public let string: String
	public let unicode: String.UnicodeScalarView

	public private(set) var hasCurrent: Bool
	public private(set) var current: UnicodeScalar
	public private(set) var currentPos: String.UnicodeScalarView.Index
	public private(set) var nextPos: String.UnicodeScalarView.Index
	public var tag: Any?

	public init(string: String) {
		self.string = string
		unicode = string.unicodeScalars
		nextPos = unicode.startIndex
		currentPos = nextPos
		endPos = unicode.endIndex
		hasCurrent = true
		current = "\0"
		passedStart = currentPos
		passedEnd = currentPos
		next()
	}
	
	
	public func next() {
		if !hasCurrent {
			return
		}
		if nextPos < endPos {
			currentPos = nextPos
			current = unicode[nextPos]
			nextPos = unicode.index(after: nextPos)
		} else {
			hasCurrent = false
			current = "\0"
			currentPos = endPos
		}
	}


	public func contextAt(pos: String.UnicodeScalarIndex) -> String {
		let context = Context(source: unicode, index: pos)
		return "\r\n" +
			"line: \(context.line + 1), col: \(unicode.distance(from: context.index, to: context.lineStart) + 1)\r\n" +
			"here: \(string[context.lineStart ..< context.index])👉\(string[context.index ..< context.lineEnd])"
	}

	
	@discardableResult
	public func passChar(_ test: UnicodeScalar) -> Bool {
		if hasCurrent && current == test {
			next()
			return true
		}
		return false
	}

	
	@discardableResult
	public func passWhile(_ test: (UnicodeScalar) -> Bool) -> Bool {
		let startPos = currentPos
		while hasCurrent && test(current) {
			next()
		}
		let endPos = currentPos
		if endPos == startPos {
			return false
		}
		passedStart = startPos
		passedEnd = endPos
		return true
	}
	
	
	public var passed: Substring {
		return string[passedStart ..< passedEnd]
	}

	
	public var currentContext: String  {
		return contextAt(pos: currentPos)
	}

	// MARK: - Internals
	
	
	private var endPos: String.UnicodeScalarView.Index
	private var passedStart: String.UnicodeScalarView.Index
	private var passedEnd: String.UnicodeScalarView.Index

	private struct Context {
		let line: Int
		let lineStart: String.UnicodeScalarView.Index
		let lineEnd: String.UnicodeScalarView.Index
		let index: String.UnicodeScalarView.Index


		init(source: String.UnicodeScalarView, index: String.UnicodeScalarView.Index) {
			var line = 0
			var i = source.startIndex
			let end = source.endIndex
			var lineStart = i
			while i < end {
				let c = source[i]
				if c == "\r" || c == "\n" {
					let lineEnd = i
					i = source.index(after: i)
					if i < end && c == "\r" || source[i] == "\n" {
						i = source.index(after: i)
					}
					if i >= index {
						self.line = line
						self.lineStart = lineStart
						self.lineEnd = lineEnd
						self.index = min(index, lineEnd)
						return
					}
					lineStart = i
					line += 1
				} else {
					i = source.index(after: i)
				}
			}
			self.line = line
			self.lineStart = lineStart
			self.lineEnd = end
			self.index = min(index, end)
		}

	}

}
