package org.unified.uni.declaration;

import java.util.regex.Pattern;

/**
 * Uni Declaration String
 * <p>
 * Created by Michael Vlasov on 06.05.2018.
 * <p>
 * Copyright (c) 2018 Michael Vlasov
 */
public final class DeclarationString {
	public static String toString(DeclarationElement[] elements) {
		StringBuilder lines = new StringBuilder();
		appendElements(elements, lines, 0);
		return lines.toString();
	}


	public static void appendElements(DeclarationElement[] elements, StringBuilder lines, int indentation) {
		for (DeclarationElement element : elements) {
			appendElement(element, lines, indentation);
		}
	}


	public static void appendElement(DeclarationElement element, StringBuilder lines, int indentation) {
		for (int i = 0; i < indentation; ++i) {
			lines.append('\t');
		}
		appendAttributes(element.attributes, lines);
		lines.append("\r\n");
		appendElements(element.children, lines, indentation + 1);
	}


	public static void appendAttributes(DeclarationAttribute[] attributes, StringBuilder builder) {
		boolean isFirst = true;
		for (DeclarationAttribute attribute : attributes) {
			if (isFirst) {
				isFirst = false;
			} else {
				builder.append(' ');
			}
			appendAttribute(attribute, builder);
		}
	}


	public static void appendAttribute(DeclarationAttribute attribute, StringBuilder builder) {
		appendValue(attribute.name, builder);
		if (attribute.value != null) {
			builder.append('=');
			appendValue(attribute.value, builder);
		}
	}


	public static void appendValue(Object value, StringBuilder builder) {
		if (value.getClass().isArray()) {
			boolean isFirst = true;
			for (Object item : (Object[]) value) {
				builder.append(isFirst ? '(' : ' ');
				appendValue(item, builder);
				isFirst = false;
			}
			builder.append(isFirst ? "()" : ')');
		} else {
			String string = value.toString();
			if (containsReservedChar(string)) {
				builder.append('`').append(string.replaceAll("`", "``")).append('`');
			} else {
				builder.append(string);
			}
		}
	}


	// Internals

	private static boolean containsReservedChar(String string) {
		for (int i = 0; i < string.length(); ++i) {
			if (reservedChars.indexOf(string.charAt(i)) >= 0) {
				return true;
			}
		}
		return false;
	}

	private static String reservedChars = " \t\r\n()='`~#";

}
