package com.techatpark;

import org.junit.jupiter.api.Test;

import java.io.IOException;

class BookReaderTest {

    @Test
    void testExtractBook() throws IOException, InterruptedException {

        new BookReader("Web Essentials",
                "/Users/sathishkumarthiyagarajan/Downloads/Internet-and-world-wide-web-how-to-program-5th-rev-ed-9780132151009-0273764020-9780273764021-0132151006.pdf"
                )
                .transformFn(this::transformUsingRegEx)
                .extract("/Users/sathishkumarthiyagarajan/IdeaProjects/web-essentials");

    }

    private String transformUsingRegEx(String input) {
        String replace = new String(input);
        return replace;
    }





























}