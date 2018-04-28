package org.unified.uni;

import java.util.regex.Pattern;

/**
 * Social Server
 * <p>
 * Created by Michael Vlasov on 19.04.2018.
 * <p>
 * Copyright (c) 2017 Social Games
 */
public final class Attribute {
	public final String name;
	public final Object value;


	public Attribute(String name, Object value) {
		this.name = name;
		this.value = value;
	}


	public static final Attribute[] emptyArray = new Attribute[0];


	private static Pattern reserved_chars = Pattern.compile(".*[ \\t\\r\\n()='`~#].*");


	public static void append_value(Object value, StringBuilder builder) {
		if (value.getClass().isArray()) {
			boolean is_first = true;
			for (Object item : (Object[]) value) {
				builder.append(is_first ? '(' : ' ');
				append_value(item, builder);
				is_first = false;
			}
			builder.append(is_first ? "()" : ')');
		} else {
			String string = value.toString();
			if (reserved_chars.matcher(string).matches()) {
				builder.append('`').append(string.replaceAll("`", "``")).append('`');
			} else {
				builder.append(string);
			}
		}
	}


	public final void append(StringBuilder builder) {
		append_value(name, builder);
		if (value != null) {
			builder.append('=');
			append_value(value, builder);
		}
	}


	public static void append_attributes(Attribute[] attributes, StringBuilder builder) {
		boolean is_first = true;
		for (Attribute attribute : attributes) {
			if (is_first) {
				is_first = false;
			} else {
				builder.append(' ');
			}
			attribute.append(builder);
		}
	}
}
