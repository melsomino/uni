import re
from typing import Callable, List, Optional

TestChar = Callable[[str], bool]


class SourceContext:
	__slots__ = [
		'line',
		'line_start',
		'line_end',
		'index'
	]
	line: int
	line_start: int
	line_end: int
	index: int

	def __init__(self, string: str, index: int):
		line: int = 0
		line_start: int = 0
		i: int = 0
		while i < len(string):
			c: int = string[i]
			if c == '\r' or c == '\n':
				line_end: int = i
				i += 1
				if i < len(string) and c == '\r' or string[i] == '\n':
					i += 1
				if i >= index:
					self.line = line
					self.line_start = line_start
					self.line_end = line_end
					self.index = min(index, line_end)
					return
				line_start = i
				line += 1
			else:
				i += 1
		self.line = line
		self.line_start = line_start
		self.line_end = len(string)
		self.index = min(index, len(string))


reserved_chars_pattern = re.compile('[ \\t\\r\\n()=\'`~#]')


def value_string(value: Optional[any]) -> str:
	if value is None:
		return ''
	if type(value) is list:
		return '(' + ' '.join(map(value_string, value)) + ')'
	string: str = str(value)
	return string if reserved_chars_pattern.search(string) is None else ('`' + value.replace('`', '``') + '`')


class Attribute:
	__slots__ = [
		'name',
		'value'
	]
	name: str
	value: Optional[any]

	def __init__(self, name: str, value: Optional[any]):
		self.name = name
		self.value = value

	def __str__(self):
		name: str = value_string(self.name)
		return name if self.value is None else name + '=' + value_string(self.value)

	def __repr__(self):
		return self.__str__()


class Element:
	__slots__ = [
		'attributes',
		'children'
	]
	attributes: List[Attribute]
	elements: List['Element']

	def __init__(self, attributes: List[Attribute], elements: List['Element']):
		self.attributes = attributes
		self.children = elements

	def __str__(self):
		lines: List[str] = []
		self.append_lines(lines)
		return '\r\n'.join(lines)

	def __repr__(self):
		return self.__str__()

	@classmethod
	def dumps(cls, elements: List['Element']) -> str:
		lines: List[str] = []
		Element.append_lines_for_elements(elements, lines)
		return '\r\n'.join(lines)

	def append_lines(self, lines: List[str], indentation: int = 0):
		lines.append('\t' * indentation + ' '.join(map(str, self.attributes)))
		Element.append_lines_for_elements(self.children, lines, indentation + 1)

	@classmethod
	def append_lines_for_elements(cls, elements: List['Element'], lines: List[str], indentation: int = 0):
		for element in elements:
			element.append_lines(lines, indentation)


class CharReader:
	__slots__ = [
		'source',
		'next_pos',
		'current_pos',
		'length',
		'has_current',
		'tag',
		'current',
		'passed_start',
		'passed_end']
	source: str
	next_pos: int
	current_pos: int
	length: int
	has_current: bool
	tag: any
	current: str
	passed_start: int
	passed_end: int

	def __init__(self, source: str):
		self.source = source
		self.next_pos = 0
		self.current_pos = 0
		self.length = len(source)
		self.has_current = True
		self.tag = False
		self.current = ''
		self.passed_start = 0
		self.passed_end = 0
		self.next()

	def context_at(self, pos: int):
		context: SourceContext = SourceContext(self.source, pos)
		line: str = ''.join([
			self.source[context.line_start:context.index],
			'👉', self.source[context.index:context.line_end]
		])
		return ''.join([
			'\r\n',
			'line: ', str(context.line + 1), ', col:  ', str(context.index - context.line_start + 1),
			'\r\n',
			'here: "', line, '"'
		])

	def current_context(self):
		return self.context_at(self.current_pos)

	def next(self):
		if not self.has_current:
			return
		if self.next_pos < self.length:
			self.current_pos = self.next_pos
			self.current = self.source[self.next_pos]
			self.next_pos += 1
		else:
			self.has_current = False
			self.current = ''
			self.current_pos = self.length

	def pass_char(self, test: str) -> bool:
		if self.has_current and self.current == test:
			self.next()
			return True
		return False

	def pass_while(self, test: TestChar) -> bool:
		start_pos: int = self.current_pos
		while self.has_current and test(self.current):
			self.next()
		end_pos: int = self.current_pos
		if end_pos == start_pos:
			return False
		self.passed_start = start_pos
		self.passed_end = end_pos
		return True

	def passed(self) -> str:
		return self.source[self.passed_start:self.passed_end]


class Token:
	__slots__ = [
		'code',
		'value'
	]
	code: int
	value: any

	names: List[str] = ['']

	def __init__(self, code: int, value: any):
		self.code = code
		self.value = value

	def same_type(self, other):
		return self.code == other.code

	@staticmethod
	def register(name):
		code: int = len(Token.names)
		Token.names.append(name)
		return Token(code, None)

	def with_value(self, value):
		return Token(self.code, value)

	def __str__(self):
		name: str = Token.names[self.code]
		return name + ': [' + str(self.value) + ']' if self.value is not None else name


Tokenizer = Callable[[CharReader], Token]


class TokenReader:
	__slots__ = [
		'chars',
		'has_current',
		'passed',
		'current_pos',
		'current',
		'tokenizer'
	]
	chars: CharReader
	has_current: bool
	passed: Optional[Token]
	current_pos: int
	current: Token
	tokenizer: Tokenizer

	def __init__(self, source: str, tokenizer: Tokenizer):
		self.chars = CharReader(source)
		self.has_current = True
		self.passed = None
		self.current_pos = self.chars.current_pos
		self.current = None
		self.tokenizer = tokenizer
		self.next()

	def current_context(self):
		return self.chars.context_at(self.current_pos)

	def pass_token(self, token: Token) -> bool:
		if not self.has_current or self.current.code != token.code:
			return False
		self.passed = self.current
		self.next()
		return True

	def pass_required(self, token: Token):
		if not self.pass_token(token):
			raise 'Expected ' + Token.names[token.code] + ' at: ' + self.current_context()

	def next(self):
		if not self.has_current:
			return

		if not self.chars.has_current:
			self.current = None
			self.has_current = False
			return

		self.current_pos = self.chars.current_pos
		self.current = self.tokenizer(self.chars)
		if self.current is None:
			self.has_current = False
			if self.chars.has_current:
				raise 'Error at ' + self.current_context()


name_or_value_token = Token.register('name or value')
name_value_separator_token = Token.register('=')
attribute_continuation_token = Token.register('~')
indentation_token = Token.register('⇥')
open_list_token = Token.register('(')
close_list_token = Token.register(')')


def is_not_cr_or_lf(c: str) -> bool:
	return c != '\r' and c != '\n'


def is_tab(c: str) -> bool:
	return c == '\t'


def is_not_reserved(c: str) -> bool:
	return c not in ' \t\r\n()=\'`~#'


def is_whitespace(c: str) -> bool:
	return c == ' ' or c == '\t'


def parse_back_quoted_string(chars: CharReader) -> Token:
	string: str = ''
	start_pos: int = chars.current_pos
	while chars.has_current:
		chars.pass_while(lambda x: x != '`')
		string += chars.passed()
		if not chars.pass_char('`'):
			raise 'Unterminated back quoted string at ' + chars.context_at(start_pos)
		if not chars.pass_char('`'):
			break
		string += '`'

	return name_or_value_token.with_value(string)


escaped_by_char = {'0': '\0', '\'': '\'', '\\': '\\', 'n': '\n', 'r': '\r', 'v': '\v', 't': '\t', 'b': '\b', 'f': '\f'}
# noinspection SpellCheckingInspection
hex_chars = '0123456789ABCDEFabcdef'


def expect_char_from_code_point(chars: CharReader, digits: int) -> str:
	hex_string: str = ''
	while digits > 0:
		if not chars.has_current or chars.current not in hex_chars:
			raise 'Invalid hex code string at ' + chars.context_at(chars.current_pos)
		hex_string += chars.current
		chars.next()
		digits -= 1
	return chr(int(hex_string, 16))


def parse_single_quoted_string(chars: CharReader) -> Token:
	string: str = ''
	start_pos: int = chars.current_pos
	while chars.has_current:
		if chars.pass_char('\''):
			return name_or_value_token.with_value(string)
		if chars.pass_char('\\'):
			if not chars.has_current:
				break
			if chars.current in escaped_by_char:
				string += escaped_by_char[chars.current]
				chars.next()
			elif chars.pass_char('x'):
				string += expect_char_from_code_point(chars, 2)
			elif chars.pass_char('u'):
				string += expect_char_from_code_point(chars, 4)
			else:
				raise 'Invalid escape sequence at ' + chars.current_context()
		else:
			string += chars.current
			chars.next()

	raise 'Unterminated string at ' + chars.context_at(start_pos)


def pass_line_end(chars: CharReader) -> bool:
	if chars.current == '#':
		chars.pass_while(is_not_cr_or_lf)
	if chars.pass_char('\r'):
		chars.pass_char('\n')
		return True
	if chars.pass_char('\n'):
		return True
	return False


def parse_indentation(chars: CharReader) -> Optional[Token]:
	while not chars.tag or pass_line_end(chars):
		chars.tag = True
		start: int = chars.next_pos
		chars.pass_while(is_tab)
		if not chars.has_current:
			return None
		chars.pass_while(is_whitespace)
		if chars.current != '\r' and chars.current != '\n' and chars.current != '#':
			return indentation_token.with_value(chars.next_pos - start)
	return None


def tokenize(chars: CharReader) -> Optional[Token]:
	token: Optional[Token] = parse_indentation(chars)
	if token is not None:
		return token
	if not chars.has_current:
		return None
	if chars.pass_char('='):
		token = name_value_separator_token
	elif chars.pass_char('~'):
		token = attribute_continuation_token
	elif chars.pass_char('('):
		token = open_list_token
	elif chars.pass_char(')'):
		token = close_list_token
	elif chars.pass_char('\''):
		token = parse_single_quoted_string(chars)
	elif chars.pass_char('`'):
		token = parse_back_quoted_string(chars)
	elif chars.pass_while(is_not_reserved):
		token = name_or_value_token.with_value(chars.passed())
	else:
		return None
	chars.pass_while(is_whitespace)
	return token


def parse_attributes(reader: TokenReader, attributes: List[Attribute]):
	while reader.pass_token(name_or_value_token):
		name: str = reader.passed.value
		value: any = None
		if reader.pass_token(name_value_separator_token):
			if reader.pass_token(open_list_token):
				list_value: list = []
				while reader.pass_token(name_or_value_token):
					list_value.append(reader.passed.value)
				reader.pass_required(close_list_token)
				value = list_value
			else:
				reader.pass_required(name_or_value_token)
				value = reader.passed.value
		attributes.append(Attribute(name, value))
	return


def pass_indentation(reader: TokenReader, expected_indentation: int) -> bool:
	if reader.has_current and reader.current.same_type(indentation_token) and \
		int(reader.current.value) == expected_indentation:
		reader.next()
		return True
	return False


def parse_element(reader: TokenReader, elements: List[Element], indentation: int):
	attributes: List[Attribute] = []
	children: List[Element] = []
	parse_attributes(reader, attributes)
	while pass_indentation(reader, indentation + 1):
		if reader.pass_token(attribute_continuation_token):
			parse_attributes(reader, attributes)
		else:
			parse_elements(reader, children, indentation + 1)
			break
	elements.append(Element(attributes, children))
	return


def parse_elements(reader: TokenReader, elements: List[Element], indentation: int):
	while True:
		parse_element(reader, elements, indentation)
		if not pass_indentation(reader, indentation):
			break


def parse(source: str) -> List[Element]:
	reader: TokenReader = TokenReader(source, tokenize)
	elements: List[Element] = []
	if reader.pass_token(indentation_token) and int(reader.passed.value) == 0:
		parse_elements(reader, elements, 0)
	if reader.has_current:
		raise 'Unexpected "' + str(reader.current) + '" at ' + reader.current_context()
	return elements
