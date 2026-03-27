package org.example.repository;

import org.example.model.CheckInRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CheckInRecordRepository extends JpaRepository<CheckInRecord, Long> {
    Optional<CheckInRecord> findByUserIdAndCheckInDate(Long userId, LocalDate checkInDate);
    long countByUserId(Long userId);
    List<CheckInRecord> findTop7ByUserIdOrderByCheckInDateDesc(Long userId);
}
