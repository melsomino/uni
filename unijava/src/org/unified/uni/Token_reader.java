package org.unified.uni;

/**
 * Uni parser
 * <p>
 * Created by Michael Vlasov on 19.04.2018.
 * <p>
 * Copyright (c) 2017 Michael Vlasov
 */
public class Token_reader {
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
		Token apply(Char_reader input) throws Error;
	}


	public Token_reader(String source, Tokenizer tokenizer) throws Error {
		this.tokenizer = tokenizer;
		input = new Char_reader(source);
		has_current = true;
		passed = null;
		current_pos = input.current_pos;
		current = null;
		next();
	}


	public String current_context() {
		return input.context_at(current_pos);
	}


	public boolean pass(Token token) throws Error {
		if (!has_current || current.type != token.type) {
			return false;
		}
		passed = current;
		next();
		return true;
	}


	public void pass_required(Token token) throws Error {
		if (!pass(token)) {
			throw new Error("Expected " + Token.names.get(token.type) + " at: " + current_context());
		}
	}


	public void next() throws Error {
		if (!has_current) {
			return;
		}
		if (!input.has_current) {
			current = null;
			has_current = false;
			return;
		}
		current_pos = input.current_pos;
		current = tokenizer.apply(this.input);
		if (current == null) {
			has_current = false;
			if (input.has_current) {
				throw new Error("Error at " + current_context());
			}
		}
	}


	private final Char_reader input;
	boolean has_current;
	Token passed;
	private int current_pos;
	Token current;
	private final Tokenizer tokenizer;
}

