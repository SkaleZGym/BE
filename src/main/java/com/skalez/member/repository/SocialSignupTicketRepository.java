package com.skalez.member.repository;

import com.skalez.member.domain.SocialSignupTicket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SocialSignupTicketRepository extends JpaRepository<SocialSignupTicket, Long> {
    Optional<SocialSignupTicket> findByTicketHash(String ticketHash);
}
