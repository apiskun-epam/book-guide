package com.bookshelf.books;

/** Which of the user's two lists a book belongs to. */
public enum ListType {
    /** Books the user has read; these can be rated. */
    COLLECTION,
    /** Books the user plans to read; never rated. */
    READING_LIST
}
