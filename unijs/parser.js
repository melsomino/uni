

function find_string_index_context(string, index) {
	let line = 0
	let line_start = 0
	let i = 0
	while (i < string.length) {
		const c = string[i]
		if (c === '\r' || c === '\n') {
			const line_end = i
			i += 1
			if (i < string.length && c === '\r' || string[i] === '\n') {
				i += 1
			}
			if (i >= index) {
				return {
					line: line,
					line_start: line_start,
					line_end: line_end,
					index: Math.min(index, line_end)
				}
			}
			line_start = i
			line += 1
		} else {
			i += 1
		}
	}
	return {
		line: line,
		line_start: line_start,
		line_end: string.length,
		index: Math.min(index, string.length)
	}
}


class Char_reader {
	constructor(source) {
		this.source = source
		this.next_pos = 0
		this.current_pos = 0
		this.length = source.length
		this.has_current = true
		this.tag = false
		this.current = ''
		this.passed_start = 0
		this.passed_end = 0
		this.next()
	}

	context_at(pos) {
		const context = find_string_index_context(this.source, pos)
		const line = this.source.substring(context.line_start, context.index) +
			'👉' + this.source.substring(context.index, context.line_end)
		return `\r\nline: ${context.line + 1}, col:  ${context.index - context.line_start + 1}\r\nhere: "${line}"`
	}

	get current_context() {
		return this.context_at(this.current_pos)
	}

	next() {
		if (!this.has_current) {
			return
		}
		if (this.next_pos < this.length) {
			this.current_pos = this.next_pos
			this.current = this.source[this.next_pos]
			this.next_pos += 1
		} else {
			this.has_current = false
			this.current = ''
			this.current_pos = this.length
		}
	}

	pass_char(test) {
		if (this.has_current && this.current === test) {
			this.next()
			return true
		}
		return false
	}

	pass_while(test) {
		const start_pos = this.current_pos
		while (this.has_current && test(this.current)) {
			this.next()
		}
		const end_pos = this.current_pos
		if (end_pos === start_pos) {
			return false
		}
		this.passed_start = start_pos
		this.passed_end = end_pos
		return true
	}

	get passed() {
		return this.source.substring(this.passed_start, this.passed_end)
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

	with(value) {
		return new Token(this.type, value)
	}

	toString() {
		const name = Token.names[this.type]
		return this.value !== null  ? `${name}: [${this.value}]` : name
	}
}


class Token_reader {
	constructor(source, tokenizer) {
		this.input = new Char_reader(source)
		this.has_current = true
		this.passed = null
		this.current_pos = this.input.current_pos
		this.current = null
		this.tokenizer = tokenizer
		this.next()
	}


	get current_context() {
		return this.input.context_at(this.current_pos)
	}


	pass(token) {
		if (!this.has_current || this.current.type !== token.type) {
			return false
		}
		this.passed = this.current
		this.next()
		return true
	}


	pass_required(token) {
		if (!this.pass(token)) {
			throw `Expected ${Token.names[token.type]} at: ${this.current_context}`
		}
	}


	next() {
		if (!this.has_current) {
			return
		}
		if (!this.input.has_current) {
			this.current = null
			this.has_current = false
			return
		}
		this.current_pos = this.input.current_pos
		this.current = this.tokenizer(this.input)
		if (this.current === null) {
			this.has_current = false
			if (this.input.has_current) {
				throw `Error at ${this.current_context}`
			}
		}
	}
}

Token.names = ['']

module.exports.Token = Token
module.exports.Token_reader = Token_reader