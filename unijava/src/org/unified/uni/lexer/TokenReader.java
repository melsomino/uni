package org.unified.uni.lexer;

/**
 * Uni Token Reader
 * <p>
 * Created by Michael Vlasov on 19.04.2018.
 * <p>
 * Copyright (c) 2018 Michael Vlasov
 */
public class TokenReader {
	public static class Error extends Exception {
		public String message;


		public Error(String message) {
			super();
			this.message = message;
		}


		@Override
		public String toString() {
			return message;
		}
	}


	public interface Tokenizer {
		LexicalToken apply(CharReader input) throws Error;
	}


	public TokenReader(CharReader chars, Tokenizer tokenizer) throws Error {
		this.tokenizer = tokenizer;
		this.chars = chars;
		hasCurrent = true;
		passed = null;
		currentPos = chars.getCurrentPos();
		current = null;
		next();
	}


	public String currentContext() {
		return chars.contextAt(currentPos);
	}


	public boolean isHasCurrent() {
		return hasCurrent;
	}


	public LexicalToken getCurrent() {
		return current;
	}


	public LexicalToken getPassed() {
		return passed;
	}


	public boolean pass(LexicalToken token) throws Error {
		if (!hasCurrent || current.name != token.name) {
			return false;
		}
		passed = current;
		next();
		return true;
	}


	public void passRequired(LexicalToken token) throws Error {
		if (!pass(token)) {
			throw new Error("Expected " + token.nameString() + " at: " + currentContext());
		}
	}


	public void next() throws Error {
		if (!hasCurrent) {
			return;
		}
		if (!chars.isHasCurrent()) {
			current = null;
			hasCurrent = false;
			return;
		}
		currentPos = chars.getCurrentPos();
		current = tokenizer.apply(this.chars);
		if (current == null) {
			hasCurrent = false;
			if (chars.isHasCurrent()) {
				throw new Error("Error at " + currentContext());
			}
		}
	}

	// Internals

	private final CharReader chars;
	private boolean hasCurrent;
	private LexicalToken passed;
	private int currentPos;
	private LexicalToken current;
	private final Tokenizer tokenizer;
}
