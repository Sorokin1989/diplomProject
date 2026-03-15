package com.example.diplomproject.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Entity
@Table(name = "course_access")
@Data
@NoArgsConstructor
public class CourseAccess {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY,optional = false)
    @JoinColumn(name = "user_id", nullable=false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional=false)
    @JoinColumn(name = "course_id", nullable=false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY, optional=false)
    @JoinColumn(name = "order_id", nullable=false)
    private Order order;

    @Column(name = "granted_at", nullable = false)
    private LocalDateTime grantedAt;

    @Column(name = "expires_at",nullable=false)
    private LocalDateTime expiresAt;

    @Column(nullable=false)
    private boolean active=true;

    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        grantedAt = now;
        if (expiresAt == null) {
            expiresAt = now.plusYears(1);
        }
    }


    @PreUpdate
    protected void onUpdate(){
        if (grantedAt==null){
            grantedAt=LocalDateTime.now();
        }
    }

}
