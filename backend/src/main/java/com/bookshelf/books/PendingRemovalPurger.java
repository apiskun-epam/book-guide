package com.bookshelf.books;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Finalizes removals whose undo window has passed. Runs server-side so a removal completes
 * even if the user refreshes or closes the page, and catches up after a restart.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PendingRemovalPurger {

    private final CollectionService collectionService;

    @Scheduled(fixedDelay = 1000)
    public void purge() {
        int purged = collectionService.purgeExpiredRemovals();
        if (purged > 0) {
            log.info("Permanently removed {} book(s) after undo window", purged);
        }
    }
}
