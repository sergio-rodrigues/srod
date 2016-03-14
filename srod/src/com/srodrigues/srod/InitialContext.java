package com.srodrigues.srod;

import java.io.Serializable;

public interface InitialContext {

	public abstract <T extends Serializable> T lookup(final String name, final Class<T> clazz) ;

}