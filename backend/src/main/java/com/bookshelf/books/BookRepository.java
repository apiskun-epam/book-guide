package com.bookshelf.books;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Integer> {

    List<Book> findByUserId(String userId);

    Optional<Book> findByIdAndUserId(Integer id, String userId);

    boolean existsByUserIdAndIsbn(String userId, String isbn);
}
