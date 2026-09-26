package com.firstClubAssignment.membershipProgram.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String phoneNo;
    private String email;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "membership_plan_id")
    private MembershipPlan membershipPlan;

    private String tier; // e.g., SILVER, GOLD, PLATINUM

    private Instant membershipStartDate;
    private Instant membershipExpiryDate;

    @Version
    private Long version; // Optimistic locking for concurrency safety
}
