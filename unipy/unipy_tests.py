import unittest
import uni


class TestUniMethods(unittest.TestCase):

	def test_a(self):
		elements = uni.parse('element a=b\r\n\tc a=`b\r``d`')
		print(uni.Element.dumps(elements))


if __name__ == '__main__':
	unittest.main()
