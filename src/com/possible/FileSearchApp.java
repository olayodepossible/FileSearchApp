package com.possible;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FileSearchApp {
    String filePath;
    String regex;
    String zipFileName;
    Pattern pattern;
    List<File> zipFiles = new ArrayList<>();

    public static void main(String[] args) {
        FileSearchApp app = new FileSearchApp();

        switch (Math.min(args.length, 3)) {
            case 0:
                System.out.println("USAGE: FileSearchApp file-path [regex] [zipfile]");
                return;
            case 3:
                app.setZipFileName(args[2]);
            case 2:
                app.setRegex(args[1]);
            case 1:
                app.setFilePath(args[0]);
        }
        ;

        try {
            app.searchDirectory(app.getFilePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void searchDirectory(String filePath) throws IOException {
        searchDirectory8(filePath);
        zipFilesJava7();
    }

    public void processFile(File file) {
        try {
            if(searchFile(file)){
                addFileToZip(file);
            }
        }
        catch (IOException | UncheckedIOException e){
            System.out.println("Error processing file: "+ file+ ": "+ e);
        }
    }

    public void addFileToZip(File file) {
        if(getZipFileName() != null){
            zipFiles.add(file);
        }
    }
    public boolean searchFile(File file) throws IOException {
        return searchFile8(file);
    }

    public boolean searchText(String text) {
        return (this.getRegex() == null) || this.pattern.matcher(text).matches();
//        return (this.getRegex() == null) || text.toLowerCase().contains(this.getRegex().toLowerCase());
    }

    public void searchDirectory6 (String path) {
        //Java 6 implementation
        File dir = new File(path);
        File[] files = dir.listFiles();

        // In case it encounter a sub-directory --> Recursive call
        assert files != null;
        for (File file : files) {
            if (file.isDirectory()) {
                searchDirectory6(file.getAbsolutePath());
            } else {
                processFile(file);
            }
        }
    }

    public void searchDirectory7 (String path) throws IOException{
        //Java 7 implementation

        Files.walkFileTree(Paths.get(path), new SimpleFileVisitor<Path>(){
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)throws IOException{
                processFile(file.toFile());
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public void searchDirectory8(String path) throws IOException{
        // Java 8 Implementation
        Files.walk(Paths.get(path)).forEach(file -> processFile(file.toFile()));
    }

    public boolean searchFile6(File file) throws FileNotFoundException {
        boolean found = false;
        Scanner scanner = new Scanner(file, "UTF-8");
        while (scanner.hasNextLine()){
            found =searchText(scanner.nextLine());
            if(found) break;
        }
        scanner.close();
        return found;
    }

    public boolean searchFile7(File file) throws IOException {
        List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        for (String line: lines) {
            if (searchText(line)) return true;
        }
        return false;
    }

    public boolean searchFile8(File file) throws IOException {
        return Files.lines(file.toPath(), StandardCharsets.UTF_8).anyMatch(text -> searchText(text));
    }

    public void zipFilesJava6()throws IOException{
        ZipOutputStream out = null;
        try{
            out = new ZipOutputStream(new FileOutputStream(getZipFileName()));
            File baseDir = new File(getFilePath());

            for (File file: zipFiles) {
                // fileName must be a relative path, not an absolute one.
                String fileName = getRelativeFilename(file, baseDir);

                ZipEntry zipEntry = new ZipEntry(fileName);
                zipEntry.setTime(file.lastModified());
                out.putNextEntry(zipEntry);

                int bufferSize = 2048;
                byte[] buffer = new byte[bufferSize];
                int len = 0;
                BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(file), bufferSize);

                while ((len = inputStream.read(buffer, 0, bufferSize)) != -1){
                    out.write(buffer, 0, len);
                }
                inputStream.close();

                out.closeEntry();
            }
        }finally {
            assert out != null;
            out.closeEntry();
        }
    }

    public void zipFilesJava7()throws IOException{
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(getZipFileName()))){
            File baseDir = new File(getFilePath());

            for (File file: zipFiles) {
                // fileName must be a relative path, not an absolute one
                String fileName = getRelativeFilename(file, baseDir);

                ZipEntry zipEntry = new ZipEntry(fileName);
                zipEntry.setTime(file.lastModified());
                out.putNextEntry(zipEntry);

                Files.copy(file.toPath(), out);

                out.closeEntry();
            }

        }
    }

    private String getRelativeFilename(File file, File baseDir) {
        String fileName = file.getAbsolutePath().substring(baseDir.getAbsolutePath().length());

        // IMPORTANT: the ZipEntry file name must use "/" not "\"
        fileName = fileName.replace('\\', '/');

        while(fileName.startsWith("/")){
            fileName = fileName.substring(1);
        }

        return fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getRegex() {
        return regex;
    }

    public void setRegex(String regex) {
        this.regex = regex;
        this.pattern = Pattern.compile(regex);
    }

    public String getZipFileName() {
        return zipFileName;
    }

    public void setZipFileName(String zipFileName) {
        this.zipFileName = zipFileName;
    }
}
