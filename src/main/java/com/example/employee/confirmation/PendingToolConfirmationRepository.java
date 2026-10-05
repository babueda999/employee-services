package com.example.employee.confirmation;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PendingToolConfirmationRepository extends JpaRepository<PendingToolConfirmation, String> {
}
