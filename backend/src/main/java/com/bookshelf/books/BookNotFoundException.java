package com.bookshelf.books;

public class BookNotFoundException extends IllegalArgumentException {
    public BookNotFoundException() {
        super("Book not found");
    }
}
