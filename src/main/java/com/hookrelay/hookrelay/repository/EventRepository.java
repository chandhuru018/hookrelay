package com.hookrelay.hookrelay.repository;

import com.hookrelay.hookrelay.domain.Event;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {
}