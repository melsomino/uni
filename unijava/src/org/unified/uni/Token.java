package org.unified.uni;

import java.util.ArrayList;

/**
 * Uni Parser
 * <p>
 * Created by Michael Vlasov on 19.04.2018.
 * <p>
 * Copyright (c) 2017 MichaelVlasov
 */
public class Token {
	private Token(int type, Object value) {
		this.type = type;
		this.value = value;
	}


	public static Token register(String name) {
		int type = Token.names.size();
		Token.names.add(name);
		return new Token(type, null);
	}


	public Token with(Object value) {
		return new Token(type, value);
	}


	@Override
	public String toString() {
		String name = Token.names.get(type);
		return value != null ? name + ": [" + value.toString() + "]" : name;
	}


	// Internals

	int type;
	Object value;

	static ArrayList<String> names = new ArrayList<String>();
}
