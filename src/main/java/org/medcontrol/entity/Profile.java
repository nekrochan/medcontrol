package org.medcontrol.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "profiles")
public class Profile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, updatable = false, name = "userId")
    private User user;

    @Column
    private String name;

    @Column(nullable = false)
    private boolean isDefault;

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Scheme> schemes = new ArrayList<>();

    public Profile() {}

    public Profile(User user, String profileName, boolean isDefault) {
        this.id = UUID.randomUUID();
        this.user = user;
        this.name = profileName;
        this.isDefault = isDefault;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
