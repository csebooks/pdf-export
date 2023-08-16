package com.techatpark;

import org.junit.jupiter.api.Test;

import java.io.IOException;

class BookReaderTest {

    @Test
    void testExtractBook() throws IOException, InterruptedException {

        new BookReader("Database System Concepts",
                "/home/haripriya/Downloads/Database System Concepts 6th edition.pdf"
                )
                .transformFn(this::transformUsingRegEx)
                .extract("/home/haripriya/Official/relational-database");

    }

    private String transformUsingRegEx(String input) {
        String replace = new String(input);
        return replace;
    }





























}