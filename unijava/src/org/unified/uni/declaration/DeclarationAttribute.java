package org.unified.uni.declaration;

/**
 * Uni Declaration Attribute
 * <p>
 * Created by Michael Vlasov on 19.04.2018.
 * <p>
 * Copyright (c) 2018 Michael Vlasov
 */
public final class DeclarationAttribute {
	public final String name;
	public final Object value;


	public DeclarationAttribute(String name, Object value) {
		this.name = name;
		this.value = value;
	}


	public static final DeclarationAttribute[] emptyArray = new DeclarationAttribute[0];
}
