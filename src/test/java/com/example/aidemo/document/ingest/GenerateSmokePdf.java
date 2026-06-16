package com.example.aidemo.document.ingest;

import java.nio.file.Path;

public final class GenerateSmokePdf {

    public static void main(String[] args) throws Exception {
        Path target = Path.of(args.length > 0 ? args[0] : "target/smoke-text-layer.pdf");
        Path created = SmokePdfFactory.createSample(target);
        System.out.println(created.toAbsolutePath());
    }

    private GenerateSmokePdf() {}
}
