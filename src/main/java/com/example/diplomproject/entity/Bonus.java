//package com.example.diplomproject.entity;
//
//import jakarta.persistence.*;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//
//@Entity
//@Table(name = "bonuses")
//@Data
//@NoArgsConstructor
//public class Bonus {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @Column(nullable = false)
//    private String title;
//
//    @Column(columnDefinition = "TEXT")
//    private String description;
//
//    @Column(nullable = false)
//    private String url;
//
//    @ManyToOne(fetch = FetchType.LAZY, optional = false)
//    @JoinColumn(name = "course_id", nullable = false)
//    private Course course;
//
//    @ManyToOne(fetch = FetchType.LAZY, optional=false)
//    @JoinColumn (name="user_id",nullable = false)
//    private User user;
//
//}
