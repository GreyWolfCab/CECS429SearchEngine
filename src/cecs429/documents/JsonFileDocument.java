package cecs429.documents;

import cecs429.json.Article;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;

public class JsonFileDocument implements FileDocument {

    private int mDocumentId;
    private Path mFilePath;
    private String mJsonTitle;
    private String mJsonUrl;

    public JsonFileDocument(int id, Path absoluteFilePath) {
        mDocumentId = id;
        mFilePath = absoluteFilePath;
        Gson gson = new Gson();
        try {
            Article article = gson.fromJson(Files.newBufferedReader(absoluteFilePath), Article.class);//read json file
            mJsonTitle = article.getTitle();//store json article title
            mJsonUrl = article.getUrl();//store json article url
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

    }

    @Override
    public Path getFilePath() {
        return mFilePath;
    }

    @Override
    public int getId() {
        return mDocumentId;
    }

    @Override
    public Reader getContent() {

        Gson gson = new Gson();

        try {
            Article article = gson.fromJson(Files.newBufferedReader(mFilePath), Article.class);//read json file
            return new StringReader(article.getBody());//return content from json file as Reader
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        return null;//file was not found
    }

    @Override
    public String getTitle() {
        return mJsonTitle;
    }

    public String getUrl() {
        return mJsonUrl;
    }

    public static FileDocument loadJsonFileDocument(Path absolutePath, int documentId) {
        return new JsonFileDocument(documentId, absolutePath);
    }
}
