package com.beust.kash.parser;


import java.util.List;

public class SimpleList extends AbstractCommand<List<PipeLineCommand>> {
//    public final String precededBy;
    public final boolean ampersand;
    public SimpleList(List<PipeLineCommand> pipelines, boolean ampersand /*, String precededBy */) {
        super(pipelines);
        this.ampersand = ampersand;
//        this.precededBy = precededBy;
    }
}
