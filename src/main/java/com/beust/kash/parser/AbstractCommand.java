package com.beust.kash.parser;

public class AbstractCommand<T> {
    public final T content;
    public AbstractCommand(T content) { this.content = content; }
}


