import unittest
import unipy


class TestUniMethods(unittest.TestCase):
	def test_upper(self):
		self.assertEqual('foo'.upper(), 'FOO')

	def test_isupper(self):
		self.assertTrue('FOO'.isupper())
		self.assertFalse('Foo'.isupper())

	def test_split(self):
		s = 'hello world'
		self.assertEqual(s.split(), ['hello', 'world'])
		# check that s.split fails when the separator is not a string
		with self.assertRaises(TypeError):
			s.split(2)

	def test_a(self):
		with open('test_a.uni') as f:
			read_data = f.read()
			print(read_data)


if __name__ == '__main__':
	unittest.main()
