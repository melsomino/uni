package org.unified.uni.lexer;

/**
 * Uni Char Reader
 * <p>
 * Created by Michael Vlasov on 19.04.2018.
 * <p>
 * Copyright (c) 2018 Michael Vlasov
 */
public class CharReader {
	public interface Test {
		boolean apply(char c);
	}


	public CharReader(String source) {
		this.source = source;
		nextPos = 0;
		currentPos = 0;
		length = source.length();
		hasCurrent = true;
		tag = null;
		current = '\0';
		passedStart = 0;
		passedEnd = 0;
		next();
	}


	public Object getTag() {
		return tag;
	}


	public void setTag(Object tag) {
		this.tag = tag;
	}


	public boolean isHasCurrent() {
		return hasCurrent;
	}


	public int getCurrentPos() {
		return currentPos;
	}


	public int getNextPos() {
		return nextPos;
	}


	public char getCurrent() {
		return current;
	}


	public void next() {
		if (!hasCurrent) {
			return;
		}
		if (nextPos < length) {
			currentPos = nextPos;
			current = source.charAt(nextPos);
			nextPos += 1;
		} else {
			hasCurrent = false;
			current = '\0';
			currentPos = length;
		}
	}


	public String contextAt(int pos) {
		Context context = new Context(pos);
		return "\r\n" +
			"line: " + (context.line + 1) + ", col: " + (context.index - context.lineStart + 1) + "\r\n" +
			"here: " +
			source.substring(context.lineStart, context.index) +
			"👉" + source.substring(context.index, context.lineEnd);
	}


	public String currentContext() {
		return contextAt(currentPos);
	}


	public boolean passChar(char test) {
		if (hasCurrent && current == test) {
			next();
			return true;
		}
		return false;
	}


	public boolean passWhile(Test test) {
		int startPos = currentPos;
		while (hasCurrent && test.apply(current)) {
			next();
		}
		int endPos = currentPos;
		if (endPos == startPos) {
			return false;
		}
		passedStart = startPos;
		passedEnd = endPos;
		return true;
	}


	public String passed() {
		return source.substring(passedStart, passedEnd);
	}

	// Internals

	private String source;
	private int nextPos;
	private int currentPos;
	private int length;
	private boolean hasCurrent;
	private char current;
	private int passedStart;
	private int passedEnd;
	private Object tag;

	private class Context {
		int line;
		int lineStart;
		int lineEnd;
		int index;


		Context(int index) {
			line = 0;
			lineStart = 0;
			int i = 0;
			while (i < length) {
				char c = source.charAt(i);
				if (c == '\r' || c == '\n') {
					lineEnd = i;
					i += 1;
					if (i < length && c == '\r' || source.charAt(i) == '\n') {
						i += 1;
					}
					if (i >= index) {
						this.index = Math.min(index, lineEnd);
						return;
					}
					lineStart = i;
					line += 1;
				} else {
					i += 1;
				}
			}
			lineEnd = length;
			this.index = Math.min(index, length);
		}

	}


}

