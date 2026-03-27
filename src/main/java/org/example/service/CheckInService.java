package org.example.service;

import org.example.model.CheckInRecord;
import org.example.model.UserAccount;
import org.example.repository.CheckInRecordRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class CheckInService {
    private final CheckInRecordRepository checkInRecordRepository;

    public CheckInService(CheckInRecordRepository checkInRecordRepository) {
        this.checkInRecordRepository = checkInRecordRepository;
    }

    public boolean hasCheckedInToday(UserAccount user) {
        return checkInRecordRepository.findByUserIdAndCheckInDate(user.getId(), LocalDate.now()).isPresent();
    }

    public void checkIn(UserAccount user) {
        if (hasCheckedInToday(user)) {
            return;
        }
        CheckInRecord record = new CheckInRecord();
        record.setUser(user);
        record.setCheckInDate(LocalDate.now());
        checkInRecordRepository.save(record);
    }

    public long totalCount(UserAccount user) {
        return checkInRecordRepository.countByUserId(user.getId());
    }

    public List<CheckInRecord> recent(UserAccount user) {
        return checkInRecordRepository.findTop7ByUserIdOrderByCheckInDateDesc(user.getId());
    }
}
