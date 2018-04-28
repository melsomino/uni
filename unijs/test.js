const fs = require('fs')
const  uni = require('./uni')

const reservedChars = /[ \t\r\n()='`~#]/g

function valueString(value) {
	if (Array.isArray(value)) {
		return '(' + value.map(valueString).join(' ') + ')'
	}
	return reservedChars.test(value) ? ('`' + value.split('`').join('``') + '`') : value
}


function attributeString(attribute) {
	const name = valueString(attribute[0])
	const value = attribute.length > 1 ? attribute[1] : null
	return value === null ? name : (name + '=' + valueString(value))
}


function attributesString(element) {
	return element[0].map(attributeString).join(' ')
}


function elementsToLines(elements, indentation, lines) {
	elements.forEach((e) => elementToLines(e, indentation, lines))
}


function elementToLines(element, indentation, lines) {
	lines.push('\t'.repeat(indentation) + attributesString(element))
	elementsToLines(element[1], indentation + 1, lines)
}



// const source = fs.readFileSync('/Users/vlasov/projects/sbis/ios/rc/ios-news/SbisNews/SbisNews/Resources/MySbisRepository.uni', 'utf8')
const source = "element a=(d r f))"
const elements = uni.parse(source)

const lines = []
elementsToLines(elements, 0, lines)

console.log(lines.join('\r\n'))
