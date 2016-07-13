package com.labs.okey.oneride.model;

import java.util.concurrent.Callable;

/**
 * @author Oleg Kleiman
 * created 11-Jul-16.
 */
public abstract class PropertyHolder<T> implements Callable<Void> {

    public T property;

    @Override
    public abstract Void call() throws Exception;
}
