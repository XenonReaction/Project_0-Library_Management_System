package model;

/**
 * Represents a single physical copy of a book.
 */
public class Book {

    private int id;
    private String title;
    private String author;
    private String isbn;             // optional
    private Integer publicationYear; // optional

    // No-arg constructor (required by JDBC / frameworks)
    public Book() {}

    // Full constructor
    public Book(int id, String title, String author, String isbn, Integer publicationYear) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.publicationYear = publicationYear;
    }

    // Constructor without ID (used before saving to DB)
    public Book(String title, String author, String isbn, Integer publicationYear) {
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.publicationYear = publicationYear;
    }

    // Getters & setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }

    public Integer getPublicationYear() { return publicationYear; }
    public void setPublicationYear(Integer publicationYear) { this.publicationYear = publicationYear; }

    @Override
    public String toString() {
        return "Book { " +
                "id=" + id +
                ", title='" + title + '\'' +
                ", author='" + author + '\'' +
                (isbn != null ? ", isbn='" + isbn + '\'' : "") +
                (publicationYear != null ? ", year=" + publicationYear : "") +
                " }";
    }
}
