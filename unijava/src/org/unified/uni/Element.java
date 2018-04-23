package org.unified.uni;

/**
 * Social Server
 * <p>
 * Created by Michael Vlasov on 19.04.2018.
 * <p>
 * Copyright (c) 2017 Social Games
 */
public final class Element {
	public final Attribute[] attributes;
	public final Element[] children;


	public Element(Attribute[] attributes, Element[] children) {
		this.attributes = attributes;
		this.children = children;
	}


	public final void append(StringBuilder lines, int indentation) {
		for (int i = 0; i < indentation; ++i) {
			lines.append('\t');
		}
		Attribute.append_attributes(attributes, lines);
		lines.append("\r\n");
		append(children, lines, indentation + 1);
	}


	public static void append(Element[] elements, StringBuilder lines, int indentation) {
		for (Element element : elements) {
			element.append(lines, indentation);
		}
	}


	public static String to_string(Element[] elements) {
		StringBuilder lines = new StringBuilder();
		append(elements, lines, 0);
		return lines.toString();
	}


	public static final Element[] emptyArray = new Element[0];


}
