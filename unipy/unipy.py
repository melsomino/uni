from typing import Optional, Callable

TestChar = Callable[[str], bool]

class DeclarationAttribute:
	__slots__ = ["name", "value"]

	def __init__(self, name: str, value: object = None):
		self.name = name
		self.value = value

	def __str__(self):
		if self.value is not None:
			return f"{self.name}={self.value}"
		else:
			return f"{self.name}"

	def __repr__(self):
		return self.__str__()


class DeclarationElement:
	__slots__ = ["attributes", "children"]

	def __init__(self):
		self.attributes = list()
		self.children = list()

	@staticmethod
	def dumps_elements(elements, indent):
		s = ""
		for element in elements:
			s += element.dumps(indent + "\t") + "\n"
		return s

	def dumps(self, indent):
		return indent + " ".join(map(str, self.attributes)) + \
		       DeclarationElement.dumps_elements(self.children, indent + "\t")

	def __str__(self):
		return self.dumps("")

	def __repr__(self):
		return self.__str__()


class DeclarationUnit:
	__slots__ = ["mixins", "elements"]

	def __init__(self):
		self.elements = list()

	@staticmethod
	def parse(source) -> ():
		print(f"parse: {source}")

	def dumps(self):
		return DeclarationElement.dumps_elements(self.elements, "")

	def __str__(self):
		return self.dumps()


class DeclarationDependency:
	def resolve(self, unit: str) -> Optional[DeclarationUnit]:
		return None


class TextReader:
	__slots__ = ["has_current", "current", "value", "_next_index", "_length", "_source"]

	def __init__(self, source: str):
		self._source = source
		self._length = len(source)
		self._next_index = 0
		self.has_current = True
		self.value = ""
		self.current = ""
		self.next()

	def pass_char(self, test: str) -> bool:
		if self.has_current and self.current == test:
			self.next()
			return True
		return False

	def pass_if(self, test: TestChar) -> bool:
		if self.has_current and test(self.current):
			self.next()
			return True
		return False

	def pass_current_to_value(self):
		self.value += self.current
		self.next()

	def pass_value_while(self, test: TestChar) -> Optional[str]:
		self.value = ""
		return self.value if self.pass_to_value_while(test) else None

	def pass_to_value_if(self, test: TestChar) -> bool:
		if self.has_current and test(self.current):
			self.value += self.current
			self.next()
			return True
		return False

	def pass_to_value_char(self, test: str) -> bool:
		if self.has_current and test == self.current:
			self.value += self.current
			self.next()
			return True
		return False

	def pass_to_value_while(self, test: TestChar) -> bool:
		passed = self.pass_to_value_if(test)
		if passed:
			while self.pass_to_value_if(test):
				pass
		return passed

	def pass_while(self, test: TestChar):
		if self.has_current and test(self.current):
			self.next()

	def pass_until(self, test: TestChar):
		if self.has_current and not test(self.current):
			self.next()

	def next(self):
		if not self.has_current:
			return
		if self._next_index < self._length:
			self.current = self._source[self._next_index]
			self._next_index += 1
		else:
			self.has_current = False
			self.current = ""
