package com.example.aidemo.document.support;

import io.agentscope.core.rag.reader.ReaderInput;

public final class ReaderInputs {

    private ReaderInputs() {}

    public static ReaderInput fromText(String text) {
        return ReaderInput.fromString(text);
    }
}
