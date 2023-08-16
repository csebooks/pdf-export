package com.techatpark;

import io.github.furstenheim.CopyDown;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.tools.PDFText2HTML;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class BookReader {

    private final String bookName;

    private final String bookPdf;

    private final List<Language> languages;


    private Function<String, String> transformFn;

    public BookReader(final String bookName, final String bookPdf) {
        this.bookName = bookName;
        this.bookPdf = bookPdf;
        this.languages = new ArrayList<>();


    }

    public BookReader transformFn(Function<String, String> transformFn) {
        this.transformFn = transformFn;
        return this;
    }



    public BookReader addLanguage(String code, final String bookName, final String bookPdf) {
        this.languages.add(new Language(code, bookName, bookPdf));
        return this;
    }


    public void extract(String bookPath) throws IOException, InterruptedException {

        File bookRoot = new File(bookPath);

        bookRoot.mkdirs();

        File tempFolder = new File("temp" + File.separator + bookName.toLowerCase());
// pdf split to chapter wise seperated files

        java.util.List<File> chapterFiles = getChapters(new File(tempFolder, "content.en"), new File(bookPdf));

        this.languages.forEach(language -> {
            try {
                getChapters(new File(tempFolder, "content." + language.code), new File(language.bookPdf));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        // creating folders language specific
        File englishContentFolder = new File(bookRoot, "content.en" + File.separator + bookName.toLowerCase());

        englishContentFolder.mkdirs();

        for (int i = 0; i < chapterFiles.size(); i++) {
            int number = i;
            extractChapter(bookRoot, tempFolder, chapterFiles, englishContentFolder, number);
        }

    }

    private Void extractChapter(final File bookRoot, final File tempFolder, final List<File> chapterFiles, final File englishContentFolder, final int i) throws IOException {
        try (PDDocument pdDocument = Loader.loadPDF(chapterFiles.get(i))) {
            PDDocumentInformation info = pdDocument.getDocumentInformation();
            String folderName = info.getAuthor();

            //creating a sub folder for each chapter
            File mdFile = new File(englishContentFolder, folderName + File.separator + "_index.md");
            mdFile.getParentFile().mkdirs();

            String frontMatter = "---\n" +
                    "title: '"+ info.getTitle() +"'\n" +
                    "weight: "+ (i +1) +"\n" +
                    "---\n\n" + getMarkdown(chapterFiles.get(i));

            Files.writeString(mdFile.toPath(), frontMatter);
//            language wise parsing pdf
            for (Language language:
            this.languages) {

                File languageContentFolder = new File(bookRoot, "content." + language.code + File.separator + bookName.toLowerCase());
                languageContentFolder.mkdirs();
                File languageMdFile = new File(languageContentFolder, folderName + File.separator + "_index.md");
                languageMdFile.getParentFile().mkdirs();

                File languagePdf = new File(tempFolder, "content." + language.code + File.separator + chapterFiles.get(i).getName());

                if(languagePdf.exists()) {
                    PDDocument pdDocumentForLanguage = Loader.loadPDF(languagePdf);

                    String markdown = getMarkdown(languagePdf);


                    String frontMatterForLanguage = "---\n" +
                            "title: '"+ pdDocumentForLanguage.getDocumentInformation().getTitle() +"'\n" +
                            "weight: "+ (i +1) +"\n" +
                            "---\n\n" +
                            markdown;
                    Files.writeString(languageMdFile.toPath(), frontMatterForLanguage);
                }



            }

        }
        return null;
    }


    private java.util.List<File> getChapters(File folder, File pdfFile) throws IOException {
        folder.mkdirs();

        List<File> children;

        try (PDDocument pdDocument = Loader.loadPDF(pdfFile)) {

            PDDocumentOutline outline = pdDocument.getDocumentCatalog().getDocumentOutline();

            if (outline != null) {
                PDPageTree pageTree = pdDocument.getPages();

                children = new ArrayList<>();

                int chapterNumber = 0;

                int startPg = 0;

                for (PDOutlineItem item : outline.children()) {
                    chapterNumber++;

                    String chapterPdfName = "chapter-" + chapterNumber;

                    File child = new File(folder, chapterPdfName + ".pdf");

                    PDPage currentPage = item.findDestinationPage(pdDocument);
                    startPg = pageTree.indexOf(currentPage);

                    if (item.getNextSibling() != null) {
                        PDPage nextIndexPage = item.getNextSibling().findDestinationPage(pdDocument);
                        int endPg = pageTree.indexOf(nextIndexPage);
                        PDDocument childDocument = new PDDocument();
                        for (int i = startPg; i < endPg; i++) {
                            childDocument.addPage(pageTree.get(i));
                        }




                        PDDocumentInformation info = childDocument.getDocumentInformation();

                        String title = item.getTitle();
                        info.setTitle(title);
                        info.setAuthor(getSeoFolderName(title));

                        childDocument.save(child);
                        childDocument.close();

                    }

                    if(child.exists()) {
                        children.add(child);
                    }


                }

                if(startPg < pageTree.getCount()) {
                    PDDocument pdDocument1 = new PDDocument();

                    for (int i = startPg; i < pageTree.getCount(); i++) {
                        pdDocument1.addPage(pdDocument.getPage(i));
                    }

                    String chapterPdfName = "chapter-" + chapterNumber;

                    File child = new File(folder, chapterPdfName + ".pdf");

                    PDDocumentInformation info = pdDocument1.getDocumentInformation();

                    String title = "No Title";
                    info.setTitle(title);
                    info.setAuthor(getSeoFolderName(title));

                    pdDocument1.save(child);
                    pdDocument1.close();
                    children.add(child);
                }


            } else {
                children = new ArrayList<>();
            }

        }

        return children;
    }

    private String getSeoFolderName(final String title) {
        String fName = title.toLowerCase()
                .replaceAll("\n", "-")
                .replaceAll(" ", "-")
                .replaceAll("---", "-")
                .replaceAll("--", "-")
                .replaceAll("[^a-z0-9-]", "");

        if(fName.length() > 75) {
            fName = fName.substring(0,75);
        }

        if(fName.startsWith("-")) {
            fName = fName.substring(1);
        }

        return fName;
    }


    public String getMarkdown(File chapterPdfFile) throws IOException {
        StringBuilder pdfText = new StringBuilder();


            try (PDDocument chapterPdfDocument = Loader.loadPDF(chapterPdfFile)) {
                int pageNumber = chapterPdfDocument.getNumberOfPages();
                for (int i = 0; i < pageNumber; i++) {
                    pdfText.append(getMarkdown(chapterPdfDocument.getPage(i)));
                }
            }



        return transformFn == null ? pdfText.toString() : transformFn.apply(pdfText.toString());
    }

    private String getMarkdown(final PDPage pdPage) throws IOException {
        StringBuilder pageMarkdown = new StringBuilder();
        CopyDown copyDown = new CopyDown();

        PDFText2HTML leftContentExtractor = new PDFText2HTML();
        PDDocument document = new PDDocument();
        document.addPage(pdPage);

        pageMarkdown
                .append(copyDown.convert(leftContentExtractor.getText(document)));

        return pageMarkdown.toString();
    }

    private class Language {

        private final String code;
        private final String bookName;
        private final String bookPdf;

        Language(String code, final String bookName, final String bookPdf) {
            this.code = code;
            this.bookName = bookName;
            this.bookPdf = bookPdf;
        }
    }

}
