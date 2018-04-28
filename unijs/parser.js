

function findStringIndexContext(string, index) {
	let line = 0
	let lineStart = 0
	let i = 0
	while (i < string.length) {
		const c = string[i]
		if (c === '\r' || c === '\n') {
			const lineEnd = i
			i += 1
			if (i < string.length && c === '\r' || string[i] === '\n') {
				i += 1
			}
			if (i >= index) {
				return {
					line: line,
					lineStart: lineStart,
					lineEnd: lineEnd,
					index: Math.min(index, lineEnd)
				}
			}
			lineStart = i
			line += 1
		} else {
			i += 1
		}
	}
	return {
		line: line,
		lineStart: lineStart,
		lineEnd: string.length,
		index: Math.min(index, string.length)
	}
}


class CharReader {
	constructor(source) {
		this.source = source
		this.nextPos = 0
		this.currentPos = 0
		this.length = source.length
		this.hasCurrent = true
		this.tag = false
		this.current = ''
		this.passedStart = 0
		this.passedEnd = 0
		this.next()
	}

	contextAt(pos) {
		const context = findStringIndexContext(this.source, pos)
		const line = this.source.substring(context.lineStart, context.index) +
			'👉' + this.source.substring(context.index, context.lineEnd)
		return `\r\nline: ${context.line + 1}, col:  ${context.index - context.lineStart + 1}\r\nhere: "${line}"`
	}

	get currentContext() {
		return this.contextAt(this.currentPos)
	}

	next() {
		if (!this.hasCurrent) {
			return
		}
		if (this.nextPos < this.length) {
			this.currentPos = this.nextPos
			this.current = this.source[this.nextPos]
			this.nextPos += 1
		} else {
			this.hasCurrent = false
			this.current = ''
			this.currentPos = this.length
		}
	}

	passChar(test) {
		if (this.hasCurrent && this.current === test) {
			this.next()
			return true
		}
		return false
	}

	passWhile(test) {
		const startPos = this.currentPos
		while (this.hasCurrent && test(this.current)) {
			this.next()
		}
		const endPos = this.currentPos
		if (endPos === startPos) {
			return false
		}
		this.passedStart = startPos
		this.passedEnd = endPos
		return true
	}

	get passed() {
		return this.source.substring(this.passedStart, this.passedEnd)
	}
}


class Token {
	constructor(type, value) {
		this.type = type
		this.value = value
	}

	static register(name) {
		const type = Token.names.length
		Token.names.push(name)
		return new Token(type, null)
	}

	withValue(value) {
		return new Token(this.type, value)
	}

	toString() {
		const name = Token.names[this.type]
		return this.value !== null  ? `${name}: [${this.value}]` : name
	}
}


class TokenReader {
	constructor(source, tokenizer) {
		this.input = new CharReader(source)
		this.hasCurrent = true
		this.passed = null
		this.currentPos = this.input.currentPos
		this.current = null
		this.tokenizer = tokenizer
		this.next()
	}


	get currentContext() {
		return this.input.contextAt(this.currentPos)
	}


	passToken(token) {
		if (!this.hasCurrent || this.current.type !== token.type) {
			return false
		}
		this.passed = this.current
		this.next()
		return true
	}


	passRequired(token) {
		if (!this.passToken(token)) {
			throw `Expected ${Token.names[token.type]} at: ${this.currentContext}`
		}
	}


	next() {
		if (!this.hasCurrent) {
			return
		}
		if (!this.input.hasCurrent) {
			this.current = null
			this.hasCurrent = false
			return
		}
		this.currentPos = this.input.currentPos
		this.current = this.tokenizer(this.input)
		if (this.current === null) {
			this.hasCurrent = false
			if (this.input.hasCurrent) {
				throw `Error at ${this.currentContext}`
			}
		}
	}
}

Token.names = ['']

module.exports.Token = Token
module.exports.TokenReader = TokenReader