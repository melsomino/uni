# Unified Declaration Language (uni)

General purpose language for textual declarative description of hierarchical structures.

Well suited for large range of declarative languages, like config files, schema definitions etc.

# Examples

Declare something like types:

	Person struct
		firstName string default=John
		lasName string default=Smith
		address struct
			city string
			street string

Layouts:

	button title=Press onClick=onClickPressButton color=red margins=(8 4 8 4)
		~ enabled={isImportant} visible={count>0}

Configs:

	listen port=80

		# Services
		rewrite /payments/* /payments_services/*

		# Static
		rewrite /* /content/*

And much more...

#Install

## JavaScript

	npm install unijs

Usage: 

	const uni = require('uni')
	const elements = uni.parse('element attr=value', { postprocess: true })
	
## Java/Android

gradle:

	compile { ... }
	
Usage:

	import org.unified.uni.*;
	
	Element[] elements = Uni.parse("element attr=value", true);

## Swift/iOS

	pod '...'

Usage:

	import Uni
	
	let elements = Uni.parse("element attr=value", postprocess: true)

## c#/.Net

	nuget ...
	
Usage:

	import Uni;
	
	var elements = Uni.parse("element attr=value", true);

## Python
	
	pip3 uni
	
Usage:

	import uni;
	
	elements: List[uni.Element] = uni.parse('element attr=value', True)

# Terms

*name* – string, containing any number of any characters. Empty string is also valid name.

*value* – string, like name, or ordered list or string.

*attribute* – name with optional value.

*element* – list of attributes and child elements.

*element attributes* – ordered list of attributes. Order is important and always preserved. Element must contain at least one attribute. Attribute name is not unique, e.g. element can contains any number of attributes with same name.

*element children* – ordered list of nested elements. Order is important and always preserved.

*module* – ordered list of elements. Usually module is a content of source file with uni declarations.

# Base Syntax

The element starts on new line and it includes list of attributes, separated by spaces. Each attribute has name and value, separated by "=":

	firstName=Monica lastName=Thompson
	firstName=John lastName=Smith

## Attributes

Attribute value can be omitted:	

	image src=/image.png hidden
	button title=OK checked default

In case of large number of attributes, theirs declarations can be continued on next line, indented with one tab and started with "~":

	label text=Press onClick=onClickPressButton color=red
		~ enable={isImportant} visible={count>0}

If attribute name or value contains reserved characters, it must be enclosed in single quotation marks:

	person name='John Smith' job='CEO of "Laundry Systems"'

Reserved characters which are can't be used in names and values without quoting:

	' = , ( ) ~ ` #

Single quoted string can contain escape sequences:

	description='\t\tText after two tabs:\nAnd new Line'

Alternatively string can be enclosed in back quotation mark "\`". In this case string can contain any character. You can twice symbol "\`" to include it in string:

	script=`
	function f() {
		return "a\tb and special quote \"``\""
	}
	`

Value can be a list of strings, enclosed in "(" and ")" and separated by spaces:

	button padding=(20 1 20 6)
	text font=('Times New Roman' 12)

## Children

Children must be indented with one tab relative to its parent:

	Person struct
		firstName string default=John
		lasName string default=Smith
		address struct
			city string
			street string

## Comments

You can comment a rest of line with "#":

	listen port=80 # HTTP
		rewrite /payments '/payment_service?query_all=true' # Payments Services

To comment whole line just start it with "#":

	listen port=80

		# Services
		rewrite /payments/* /payments_services/*

		# Static
		rewrite /* /content/*

# Postprocessing

In addition to base syntax you can use optional powerful postprocessing features:
* Dictionaries and Substitutions
* Mixins
* Localization
* Imports

This features can be applied to module declarations after loading.


## Dictionaries and Substitutions

The postprocessor interprets all root elements as dictionary entries in three ways.
First declares single entry in single root element:

	color accent orange
	color success green
	color error red
	
Where *string* and *color* are *types*, *app-name* and *accent* are keys.
Second form declares multiple same-typed entries in single root element:  

	color success=green warning=orange error=red
	
Third form declares multiple same-typed entries in root element with children:

	color
		accent orange
		success green
		error red
	
You can mix forms of declaration:

	color
		accent=orange success=green error=red
		table-background gray
	

Each dictionary entry has three parts:
* type
* key
* value

You can use dictionary entries for substitution in attribute names and values.
The fully qualified substitution must be specified as "^[module:type:key]", where module and type is optional if there is no conflicts with keys:

	string app-name 'Awesome App'
	color accent orange
	
	label title='Welcome to ^[app-name]' color=^accent
	
As you see, in *color* attribute used shorthand form of substitution without square brackets. Such form suitable when you substitute whole name or value.
Note that we omit module and type parts of entry reference because of no key conflicts.
But if we have entries with same key but different types, we must specify type explicitly:   

	string alert ALERT!
	color alert red

	label title='We have ^[string:alert]' color=^color:alert

Application can define default entry types for substitution in values of specific attributes.
For example, layout engine can tell to parser, that substitution entries in *background-color* attribute has default entry type *color*.
And define default entry type *string* for *title* attributes.
With this context suggestions you can omit types even in case on duplicated keys:  

	string alert ALERT!
	color alert red

	label title='We have ^[alert]' color=^alert

## Mixins

	mixin
		header color=red font=(Arial 20) margin=(20 0 20 0)

	fragment PersonCell
		label text={title} +header

Will produce:

	fragment PersonCell
		label text={title} color=red font=(Arial 20) margin=(20 0 20 0)

## Localization

## Imports

You can import dictionaries and mixins from other modules.

Module *loc.uni*:

	string localization=en
		app-name 'App Name'

	string localization=ru
		app-name 'Приложение'

Module *design.uni*:

	color
		accent orange


Module *main-screen.uni*:

	import loc design
	
	fragment main
	
		# Fully Qualified Form
		label title='App: ^[loc:string:app-name]' color=^design:color:accent
		
		# Short Form
		label title='App: ^[app-name]' color=^accent

# Libraries

There is implementations for parsing and formatting uni declarations. All implementations contains features:
* Object wrappers for elements and attributes – element tree.
* Building element tree from text representation.
* Generating text representation from element tree.
* Postprocessing element tree.

Library implementations:
* Swift – in development 90%.
* Java – in development 60%.
* C# – none.
* JavaScript – none.
* Python – none.
* C++ – none.

# Editor supports

* Sublime Text – in development – 90%.
* IntelliJ – none.
* Visual Studio Code – none.
* Atom – none.

