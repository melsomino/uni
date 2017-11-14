# Unified Declaration Language (uni)

General purpose language for textual declarative description of hierarchical structures.

Well suited for large range of declarative languages, like config files, schema definitions etc.

# Terms

*attribute* – name with optional value.

*name* – string, containing any number of any characters.

*value* – string like name or ordered list or string. Value is optional and can be missed.

*element* – list of attributes and child elements.

*element attributes* – ordered list of attributes. Order is important and always preserved. Element must contain at least one attribute. Attribute name is not unique, e.g. element can contains any number of attributes with same name.

*element children* – ordered list of nested elements. Order is important and always preserved.

*module* – ordered list of elements. Usally module is a content of source file with uni declarations.

# Base Syntax

Every element starts on new line and contains list of attrbutes, separated by spaces. Each attribute has name and value, separated by "=":

	firstName=Monica lastName=Tompson
	firstName=John lastName=Smith

Attribute value can be omitted:	

	image src=/image.png hidden
	button title=OK checked default

Children must be indented with one tab relative to its parent:

	Person struct
		firstName string default=John
		lasName string default=Smith
		address struct
			city string
			street string

In case on large number of attributes, theirs declarations can be continued on next line, indented with one tab and started with "~":

	label text=Press onClick=onClickPressButton color=red
		~ enable={isImportant} visible={count>0}

If attribute name or value contains reserved characters, it must be enclosed in quotation marks:

	person name="John Smith" job="CEO of \"Laundry Systems\""

Quoted attribute values can contain escape sequences:

	description="\t\tText after two tabs:\nAnd new Line"





