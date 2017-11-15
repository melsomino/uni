# Unified Declaration Language (uni)

General purpose language for textual declarative description of hierarchical structures.

Well suited for large range of declarative languages, like config files, schema definitions etc.

# Terms

*attribute* – name with optional value.

*name* – string, containing any number of any characters.

*value* – string, like name, or ordered list or string. Value is optional and can be missed.

*element* – list of attributes and child elements.

*element attributes* – ordered list of attributes. Order is important and always preserved. Element must contain at least one attribute. Attribute name is not unique, e.g. element can contains any number of attributes with same name.

*element children* – ordered list of nested elements. Order is important and always preserved.

*module* – ordered list of elements. Usually module is a content of source file with uni declarations.

# Base Syntax

Every element starts on new line and contains list of attributes, separated by spaces. Each attribute has name and value, separated by "=":

	firstName=Monica lastName=Thompson
	firstName=John lastName=Smith

## Attributes

Attribute value can be omitted:	

	image src=/image.png hidden
	button title=OK checked default

In case on large number of attributes, theirs declarations can be continued on next line, indented with one tab and started with "~":

	label text=Press onClick=onClickPressButton color=red
		~ enable={isImportant} visible={count>0}

If attribute name or value contains reserved characters, it must be enclosed in quotation marks:

	person name="John Smith" job="CEO of \"Laundry Systems\""

Reserved characters are:

	" = , ( ) ~ ` #

Quoted string can contain escape sequences:

	description="\t\tText after two tabs:\nAnd new Line"

String can be enclosed in special quotation mark "\`". In this case string can contain any character. You can twice symbol "\`" to include in string:

	script=`
	function f() {
		return "a\tb and special quote \"``\""
	}
	`

Value can be a list of string, enclosed in "(" and ")" and separated with spaces:

	button padding=(20 1 20 6)
	text font=("Times New Roman" 12)

## Children

Children must be indented with one tab relative to its parent:

	Person struct
		firstName string default=John
		lasName string default=Smith
		address struct
			city string
			street string

## Comments

You can comment rest of line with "#":

	listen port=80 # HTTP
		rewrite /payments "/payment_service?query_all=true" # Payments Services

To comment whole line just start with "#":

	listen port=80

		# Services
		rewrite /payments/* /payments_services/*

		# Static
		rewrite /* /content/*

# Postprocessing

In addition to base syntax you can use optional powerful postprocessing features: templates and mixins. This features can be applied to module declarations after loading.

## Templates

## Mixins

# Libraries

There is implementations for parsing and formatting uni declarations. All implementations contains features:
* Object wrappers for elements and attributes – element tree.
* Building element tree from text representation.
* Generating text representation from element tree.
* Postprocessing element tree.

Library implementations:
* Swift – in development 90%.
* Java – in development 60%.
* JavaScript – none.
* Python – none.

# Editor supports

* Sublime Text – in development – 90%.
* IntelliJ – none.
* Visual Studio Code – none.
* Atom – none.

