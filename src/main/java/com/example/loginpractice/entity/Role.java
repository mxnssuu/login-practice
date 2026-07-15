package com.example.loginpractice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "roles")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Short roleId;

    @Column(nullable = false, unique = true, length = 12)
    private String code;

    @Column(nullable = false, length = 30)
    private String name;
}