package com.beust.kash.parser;

public class Command {
    public final SimpleCommand simpleCommand;
    public final SubShell subShell;
    public Command(SimpleCommand sc, SubShell ss) {
        this.simpleCommand = sc; this.subShell = ss;
    }
    @Override public boolean equals(Object other) {
        Command c = (Command) other;
        return simpleCommand.equals(c.simpleCommand) && subShell == c.subShell;
    }
    @Override public String toString() {
        if (simpleCommand != null) return simpleCommand.toString();
        else return subShell.toString();
    }
}

