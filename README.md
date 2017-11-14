# Unified Declaration Language (uni)

General purpose language for textual declarative description of hierarchical structures.

# Terms

*attribute* – name with optional value.

*name* – string, containing any number of any characters.

*value* – string like name or ordered list or string. Value is optional and can be missed.

*element* – list of attributes and child elements.

*element attributes* – ordered list of attributes. Order is important and always preserved. Element must contain at least one attribute. Attribute name is not unique, e.g. element can contains any number of attributes with same name.

*element children* – ordered list of nested elements. Order is important and always preserved.

# Syntax

Every element starts on new line and contains list of attrbutes, separated by spaces. Each attribute has name and value, separated by "=":

	first_name=Monica last_name=Tompson
	first_name=John last_name=Smith

Attribute value can be omitted:	


