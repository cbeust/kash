package com.beust.kash.parser;

public class SubShell {
    public final CompoundList command;
    public SubShell(CompoundList command) {
        this.command = command;
    }
    @Override public boolean equals(Object other) {
        SubShell ss = (SubShell) other;
        return command.equals(ss.command);
    }
}
