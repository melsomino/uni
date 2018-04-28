package org.unified.uni;

/**
 * Text reader
 * <p>
 * Created by Michael Vlasov on 19.04.2018.
 * <p>
 * Copyright (c) 2017 Michael Vlasov
 */
public class CharReader {
	public interface Test {
		boolean apply(char c);
	}


	public CharReader(String source) {
		this.source = source;
		next_pos = 0;
		current_pos = 0;
		length = source.length();
		has_current = true;
		tag = null;
		current = '\0';
		passed_start = 0;
		passed_end = 0;
		next();
	}


	public void next() {
		if (!has_current) {
			return;
		}
		if (next_pos < length) {
			current_pos = next_pos;
			current = source.charAt(next_pos);
			next_pos += 1;
		} else {
			has_current = false;
			current = '\0';
			current_pos = length;
		}
	}


	public String context_at(int pos) {
		Context context = new Context(pos);
		return "\r\n" +
			"line: " + (context.line + 1) + ", col: " + (context.index - context.line_start + 1) + "\r\n" +
			"here: " +
			source.substring(context.line_start, context.index) +
			"👉" + source.substring(context.index, context.line_end);
	}


	public String current_context() {
		return context_at(current_pos);
	}


	public boolean pass_char(char test) {
		if (has_current && current == test) {
			next();
			return true;
		}
		return false;
	}


	public boolean pass_while(Test test) {
		int start_pos = current_pos;
		while (has_current && test.apply(current)) {
			next();
		}
		int end_pos = current_pos;
		if (end_pos == start_pos) {
			return false;
		}
		passed_start = start_pos;
		passed_end = end_pos;
		return true;
	}


	public String passed() {
		return source.substring(passed_start, passed_end);
	}

	// Internals

	private String source;
	int next_pos;
	protected int current_pos;
	private int length;
	boolean has_current;
	Object tag;
	char current;
	private int passed_start;
	private int passed_end;

	private class Context {
		int line;
		int line_start;
		int line_end;
		int index;


		Context(int index) {
			line = 0;
			line_start = 0;
			int i = 0;
			while (i < length) {
				char c = source.charAt(i);
				if (c == '\r' || c == '\n') {
					line_end = i;
					i += 1;
					if (i < length && c == '\r' || source.charAt(i) == '\n') {
						i += 1;
					}
					if (i >= index) {
						this.index = Math.min(index, line_end);
						return;
					}
					line_start = i;
					line += 1;
				} else {
					i += 1;
				}
			}
			line_end = length;
			this.index = Math.min(index, length);
		}

	}
}

