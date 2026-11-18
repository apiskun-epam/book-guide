package com.bookshelf.books;

public class DuplicateBookException extends IllegalArgumentException {
    public DuplicateBookException() {
        super("Book already exists in your collection or reading list");
    }
}
