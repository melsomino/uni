package org.unified.uni.declaration;

/**
 * Uni Declaration Element
 * <p>
 * Created by Michael Vlasov on 19.04.2018.
 * <p>
 * Copyright (c) 2018 Michael Vlasov
 */
public final class DeclarationElement {
	public final DeclarationAttribute[] attributes;
	public final DeclarationElement[] children;


	public DeclarationElement(DeclarationAttribute[] attributes, DeclarationElement[] children) {
		this.attributes = attributes;
		this.children = children;
	}


	public static final DeclarationElement[] emptyArray = new DeclarationElement[0];
}
