package DAO;

import model.Book;

/**
 * DAO interface for Book entities.
 */
public interface BookDAO extends BaseDAO<Book> {
    // Add Book-specific query methods here if needed, e.g.:
    // List<Book> findByTitle(String title);
}
