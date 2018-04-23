const parser = require('./parser')
const Token = parser.Token

name_or_value_token = Token.register('name or value')
name_value_separator_token = Token.register('=')
attribute_continuation_token = Token.register('~')
indentation_token = Token.register('⇥')
open_list_token = Token.register('(')
close_list_token = Token.register(')')


function is_not_cr_or_lf(c) {
	return c !== '\r' && c !== '\n'
}


function is_tab(c) {
	return c === '\t'
}


function is_not_reserved(c) {
	return !" \t\r\n()='`~#".includes(c)
}


function is_whitespace(c) {
	return (c === ' ') || (c === '\t')
}


function parse_backquoted_string(input) {
	let string = ''
	const start_pos = input.current_pos
	while (input.has_current) {
		input.pass_while((c) => c !== '`')
		string += input.passed
		if (!input.pass_char('`')) {
			throw `Unterminated backquoted string at ${input.context_at(start_pos)}`
		}
		if (!input.pass_char('`')) {
			break
		}
		string += '`'
	}
	return name_or_value_token.with(string)
}

const escaped_by_char = { '0': '\0', '\'': '\'', '\\': '\\', 'n': '\n', 'r': '\r', 'v': '\v', 't': '\t', 'b': '\b', 'f': '\f' }
const hex_chars = '0123456789ABCDEFabcdef'

function expect_char_from_code_point(input, digits) {
	let hex = ''
	while (digits > 0) {
		if (!input.has_current || !hex_chars.includes(input.current)) {
			throw `Invalid hex code string at ${input.context_at(input.current_pos)}`
		}
		hex += input.current
		input.next()
		digits -= 1
	}
	return String.fromCodePoint(Number.parseInt(hex, 16))
}

function parse_singlequoted_string(input) {
	let string = ''
	const start_pos = input.current_pos;
	while (input.has_current) {
		if (input.pass_char('\'')) {
			return name_or_value_token.with(string)
		}
		if (input.pass_char('\\')) {
			if(!input.has_current) {
				break
			}
			if (input.current in escaped_by_char) {
				string += escaped_by_char[input.current]
				input.next()
			} else if (input.pass_char('x')) {
				string += expect_char_from_code_point(input, 2)
			} else if (input.pass_char('u')) {
				string += expect_char_from_code_point(input, 4)
			} else {
				throw `Invalid escape sequence at ${input.current_context}`
			}
		} else {
			string += input.current
			input.next()
		}
	}
	throw `Unterminated backquoted string at ${input.context_at(start_pos)}`
}


function pass_line_end(input) {
	if (input.current === '#') {
		input.pass_while(is_not_cr_or_lf)
	}
	if (input.pass_char('\r')) {
		input.pass_char('\n')
		return true
	}
	if (input.pass_char('\n')) {
		return true
	}
	return false
}


function parse_indentation(input) {
	while (!input.tag || pass_line_end(input)) {
		input.tag = true
		const start = input.next_pos
		input.pass_while(is_tab)
		if (!input.has_current) {
			return null
		}
		input.pass_while(is_whitespace)
		if (input.current !== '\r' && input.current !== '\n' && input.current !== '#') {
			return indentation_token.with(input.next_pos - start)
		}
	}
	return null
}


function tokenize(input) {
	let token = parse_indentation(input)
	if (token != null) {
		return token
	}
	if (!input.has_current) {
		return null
	}
	if (input.pass_char('=')) {
		token = name_value_separator_token
	} else if (input.pass_char('~')) {
		token = attribute_continuation_token
	} else if (input.pass_char('(')) {
		token = open_list_token
	} else if (input.pass_char(')')) {
		token = close_list_token
	} else if (input.pass_char("'")) {
		token = parse_singlequoted_string(input)
	} else if (input.pass_char("`")) {
		token = parse_backquoted_string(input)
	} else if (input.pass_while(is_not_reserved)) {
		token = name_or_value_token.with(input.passed)
	} else {
		return null
	}
	input.pass_while(is_whitespace)
	return token
}


function parse_attributes(reader, attributes) {
	while (reader.pass(name_or_value_token)) {
		const name = reader.passed.value
		let value = null
		if (reader.pass(name_value_separator_token)) {
			if (reader.pass(open_list_token)) {
				value = []
				while (reader.pass(name_or_value_token)) {
					value.push(reader.passed.value)
				}
				reader.pass_required(close_list_token)
			} else {
				reader.pass_required(name_or_value_token)
				value = reader.passed.value
			}
		}
		attributes.push([name, value])
	}
}


function pass_indentation(reader, expected_indentation) {
	if (reader.has_current && reader.current.type === indentation_token.type &&
		reader.current.value === expected_indentation) {
		reader.next()
		return true
	}
	return false
}


function parse_element(reader, elements, indentation) {
	const attributes = []
	const children = []
	parse_attributes(reader, attributes)
	while (pass_indentation(reader, indentation + 1)) {
		if (reader.pass(attribute_continuation_token)) {
			parse_attributes(reader, attributes)
		} else {
			parse_elements(reader, children, indentation + 1)
			break
		}
	}
	elements.push([attributes, children])
}


function parse_elements(reader, elements, indentation) {
	do {
		parse_element(reader, elements, indentation)
	} while (pass_indentation(reader, indentation))
}


function parse(source) {
	const reader = new parser.Token_reader(source, tokenize)
	const elements = []
	if (reader.pass(indentation_token) && reader.passed.value === 0) {
		parse_elements(reader, elements, 0)
	}
	if (reader.has_current) {
		throw `Unexpected "${reader.current.toString()}" at ${reader.current_context}`
	}
	return elements
}


module.exports.parse = parse
