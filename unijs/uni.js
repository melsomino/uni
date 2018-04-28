const parser = require('./parser')
const Token = parser.Token

nameOrValueToken = Token.register('name or value')
nameValueSeparatorToken = Token.register('=')
attributeContinuationToken = Token.register('~')
indentationToken = Token.register('⇥')
openListToken = Token.register('(')
closeListToken = Token.register(')')


function isNotCrOrLf(c) {
	return c !== '\r' && c !== '\n'
}


function isTab(c) {
	return c === '\t'
}


function isNotReserved(c) {
	return !" \t\r\n()='`~#".includes(c)
}


function isWhitespace(c) {
	return (c === ' ') || (c === '\t')
}


function parseBackQuotedString(input) {
	let string = ''
	const startPos = input.currentPos
	while (input.hasCurrent) {
		input.passWhile((c) => c !== '`')
		string += input.passed
		if (!input.passChar('`')) {
			throw `Unterminated backquoted string at ${input.contextAt(startPos)}`
		}
		if (!input.passChar('`')) {
			break
		}
		string += '`'
	}
	return nameOrValueToken.withValue(string)
}

const escapedByChar = { '0': '\0', '\'': '\'', '\\': '\\', 'n': '\n', 'r': '\r', 'v': '\v', 't': '\t', 'b': '\b', 'f': '\f' }
const hexChars = '0123456789ABCDEFabcdef'

function expectCharFromCodePoint(input, digits) {
	let hex = ''
	while (digits > 0) {
		if (!input.hasCurrent || !hexChars.includes(input.current)) {
			throw `Invalid hex code string at ${input.contextAt(input.currentPos)}`
		}
		hex += input.current
		input.next()
		digits -= 1
	}
	return String.fromCodePoint(Number.parseInt(hex, 16))
}

function parseSingleQuotedString(input) {
	let string = ''
	const startPos = input.currentPos;
	while (input.hasCurrent) {
		if (input.passChar('\'')) {
			return nameOrValueToken.withValue(string)
		}
		if (input.passChar('\\')) {
			if(!input.hasCurrent) {
				break
			}
			if (input.current in escapedByChar) {
				string += escapedByChar[input.current]
				input.next()
			} else if (input.passChar('x')) {
				string += expectCharFromCodePoint(input, 2)
			} else if (input.passChar('u')) {
				string += expectCharFromCodePoint(input, 4)
			} else {
				throw `Invalid escape sequence at ${input.currentContext}`
			}
		} else {
			string += input.current
			input.next()
		}
	}
	throw `Unterminated backquoted string at ${input.contextAt(startPos)}`
}


function passLineEnd(input) {
	if (input.current === '#') {
		input.passWhile(isNotCrOrLf)
	}
	if (input.passChar('\r')) {
		input.passChar('\n')
		return true
	}
	if (input.passChar('\n')) {
		return true
	}
	return false
}


function parseIndentation(input) {
	while (!input.tag || passLineEnd(input)) {
		input.tag = true
		const start = input.nextPos
		input.passWhile(isTab)
		if (!input.hasCurrent) {
			return null
		}
		input.passWhile(isWhitespace)
		if (input.current !== '\r' && input.current !== '\n' && input.current !== '#') {
			return indentationToken.withValue(input.nextPos - start)
		}
	}
	return null
}


function tokenize(input) {
	let token = parseIndentation(input)
	if (token != null) {
		return token
	}
	if (!input.hasCurrent) {
		return null
	}
	if (input.passChar('=')) {
		token = nameValueSeparatorToken
	} else if (input.passChar('~')) {
		token = attributeContinuationToken
	} else if (input.passChar('(')) {
		token = openListToken
	} else if (input.passChar(')')) {
		token = closeListToken
	} else if (input.passChar("'")) {
		token = parseSingleQuotedString(input)
	} else if (input.passChar("`")) {
		token = parseBackQuotedString(input)
	} else if (input.passWhile(isNotReserved)) {
		token = nameOrValueToken.withValue(input.passed)
	} else {
		return null
	}
	input.passWhile(isWhitespace)
	return token
}


function parseAttributes(reader, attributes) {
	while (reader.pass(nameOrValueToken)) {
		const name = reader.passed.value
		let value = null
		if (reader.pass(nameValueSeparatorToken)) {
			if (reader.pass(openListToken)) {
				value = []
				while (reader.pass(nameOrValueToken)) {
					value.push(reader.passed.value)
				}
				reader.passRequired(closeListToken)
			} else {
				reader.passRequired(nameOrValueToken)
				value = reader.passed.value
			}
		}
		attributes.push([name, value])
	}
}


function passIndentation(reader, expectedIndentation) {
	if (reader.hasCurrent && reader.current.type === indentationToken.type &&
		reader.current.value === expectedIndentation) {
		reader.next()
		return true
	}
	return false
}


function parseElement(reader, elements, indentation) {
	const attributes = []
	const children = []
	parseAttributes(reader, attributes)
	while (passIndentation(reader, indentation + 1)) {
		if (reader.pass(attributeContinuationToken)) {
			parseAttributes(reader, attributes)
		} else {
			parseElements(reader, children, indentation + 1)
			break
		}
	}
	elements.push([attributes, children])
}


function parseElements(reader, elements, indentation) {
	do {
		parseElement(reader, elements, indentation)
	} while (passIndentation(reader, indentation))
}


function parse(source) {
	const reader = new parser.TokenReader(source, tokenize)
	const elements = []
	if (reader.pass(indentationToken) && reader.passed.value === 0) {
		parseElements(reader, elements, 0)
	}
	if (reader.hasCurrent) {
		throw `Unexpected "${reader.current.toString()}" at ${reader.currentContext}`
	}
	return elements
}


module.exports.parse = parse
