package com.sky.ordermanagement.inventory.repository;

import com.sky.ordermanagement.inventory.domain.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {
}
