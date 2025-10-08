package com.gomdol.concert.user.infra.query.persistnece.entity;

import com.gomdol.concert.common.domain.SoftDeleteEntity;
import com.gomdol.concert.user.domain.Role;
import jakarta.persistence.*;

@Entity
@Table
public class UserEntity extends SoftDeleteEntity {

    @Id
    @Column(length = 36)
    private String id; // UUID 문자열 (char(36))

    @Column(name = "login_id", nullable = false, length = 20, unique = true)
    private String loginId;

    @Column(name = "username", nullable = false, length = 20)
    private String username;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "email", nullable = false, length = 50, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;
}
