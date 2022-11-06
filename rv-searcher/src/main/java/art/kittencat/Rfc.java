package art.kittencat;


public class Rfc {
    private final String id;
    private final String title;
    private final String abstractText;
    private final byte[] htmlCompressed;
    private final byte[] textCompressed;
    private final String words;
    private final byte[] wordsCompressed;

    public Rfc(String id, String title, String abstractText, byte[] htmlCompressed, byte[] textCompressed, String words, byte[] wordsCompressed) {
        this.id = id;
        this.title = title;
        this.abstractText = abstractText;
        this.htmlCompressed = htmlCompressed;
        this.textCompressed = textCompressed;
        this.words = words;
        this.wordsCompressed = wordsCompressed;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getAbstractText() {
        return abstractText;
    }

    public byte[] getHtmlCompressed() {
        return htmlCompressed;
    }

    public byte[] getTextCompressed() {
        return textCompressed;
    }

    public String getWords() {
        return words;
    }

    public byte[] getWordsCompressed() {
        return wordsCompressed;
    }
}
