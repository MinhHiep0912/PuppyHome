package com.puppyshome.backend.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "customers")
public class CustomerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    protected CustomerEntity() {
    }

    public CustomerEntity(String email, String passwordHash) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = email;
        this.status = "ACTIVE";
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public boolean isActive() {
        return "ACTIVE".equals(status);
    }
}
