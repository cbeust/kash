package com.beust.kash.parser;

import java.util.List;

public class CompoundList extends AbstractCommand<List<SimpleCommand>> {
    public CompoundList(List<SimpleCommand> content) { super(content); }
    @Override public boolean equals(Object other) {
        CompoundList cl = (CompoundList) other;
        return this.content.equals(cl.content);
    }
}

