package cecs429.json;

/**
 * stored in "document" array value, an article has 3 name/value pairs to be broken up into json
 */
public class Article {

    private String title;
    private String body;
    private String url;

    public Article(String title, String body, String url) {
        this.title = title;
        this.body = body;
        this.url = url;
    }

    public String getTitle() {
        return this.title;
    }

    public String getBody() {
        return this.body;
    }

    public String getUrl() {
        return this.url;
    }

    public String toString() {
        return "\nTitle: " + getTitle() + "\nBody: " + getBody() + "\nURL: " + getUrl();
    }

}
