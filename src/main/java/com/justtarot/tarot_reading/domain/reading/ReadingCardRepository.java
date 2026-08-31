package com.justtarot.tarot_reading.domain.reading;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReadingCardRepository extends JpaRepository<ReadingCard, Long> {
}
