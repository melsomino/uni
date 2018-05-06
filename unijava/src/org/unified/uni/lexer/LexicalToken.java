package org.unified.uni.lexer;

import com.sun.istack.internal.NotNull;

import java.util.ArrayList;

/**
 * Uni Lexical Token
 * <p>
 * Created by Michael Vlasov on 19.04.2018.
 * <p>
 * Copyright (c) 2018 MichaelVlasov
 */
public class LexicalToken {
	public final int name;


	public String nameString() {
		return names.get(name);
	}


	@NotNull
	public String valueString() {
		return value != null ? value.toString() : "";
	}


	public int valueInt() {
		Integer intValue = (Integer) value;
		return intValue != null ? intValue : 0;
	}


	public LexicalToken with(Object value) {
		return new LexicalToken(name, value);
	}


	public static LexicalToken register(String nameString) {
		int name = LexicalToken.names.size();
		LexicalToken.names.add(nameString);
		return new LexicalToken(name, null);
	}


	@Override
	public String toString() {
		String name = LexicalToken.names.get(this.name);
		return value != null ? "(" + nameString() + ", " + value.toString() + ")" : name;
	}


	// Internals

	private Object value;


	private LexicalToken(int name, Object value) {
		this.name = name;
		this.value = value;
	}


	private static ArrayList<String> names = new ArrayList<>();
}
