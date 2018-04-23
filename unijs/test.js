const fs = require('fs')
const  uni = require('./uni')

const reserved_chars = /[ \t\r\n()='`~#]/g

function value_string(value) {
	if (Array.isArray(value)) {
		return '(' + value.map(value_string).join(' ') + ')'
	}
	return reserved_chars.test(value) ? ('`' + value.split('`').join('``') + '`') : value
}


function attribute_string(attribute) {
	const name = value_string(attribute[0])
	const value = attribute.length > 1 ? attribute[1] : null
	return value === null ? name : (name + '=' + value_string(value))
}


function attributes_string(element) {
	return element[0].map(attribute_string).join(' ')
}


function elements_to_lines(elements, indentation, lines) {
	elements.forEach((e) => element_to_lines(e, indentation, lines))
}


function element_to_lines(element, indentation, lines) {
	lines.push('\t'.repeat(indentation) + attributes_string(element))
	elements_to_lines(element[1], indentation + 1, lines)
}



// const source = fs.readFileSync('/Users/vlasov/projects/sbis/ios/rc/ios-news/SbisNews/SbisNews/Resources/MySbisRepository.uni', 'utf8')
const source = "element a=(d r f))"
const elements = uni.parse(source)

const lines = []
elements_to_lines(elements, 0, lines)

console.log(lines.join('\r\n'))
