package com.bookshelf.books;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Books scheduled for deletion are invisible to the active-collection queries below;
 * only {@link #findPendingDeletion} and {@link #deleteExpired} see them.
 */
@Repository
public interface BookRepository extends JpaRepository<Book, Integer> {

    @Query("select b from Book b where b.userId = :userId and b.deletionScheduledAt is null")
    List<Book> findByUserId(@Param("userId") String userId);

    @Query("select b from Book b where b.id = :id and b.userId = :userId and b.deletionScheduledAt is null")
    Optional<Book> findByIdAndUserId(@Param("id") Integer id, @Param("userId") String userId);

    @Query("select count(b) > 0 from Book b where b.userId = :userId and b.isbn = :isbn and b.deletionScheduledAt is null")
    boolean existsByUserIdAndIsbn(@Param("userId") String userId, @Param("isbn") String isbn);

    @Query("select b from Book b where b.id = :id and b.userId = :userId and b.deletionScheduledAt is not null")
    Optional<Book> findPendingDeletion(@Param("id") Integer id, @Param("userId") String userId);

    @Modifying
    @Query("delete from Book b where b.deletionScheduledAt is not null and b.deletionScheduledAt <= :now")
    int deleteExpired(@Param("now") LocalDateTime now);
}
