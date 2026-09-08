package com.nivya.family.entity;

import com.nivya.role.RoleType;
import com.nivya.user.entity.User;
import jakarta.persistence.*;

import java.time.Instant;

/**
 * Membership association linking Users to Families.
 */
@Entity
@Table(name = "family_members", uniqueConstraints = {
        @UniqueConstraint(name = "uq_family_user", columnNames = {"family_id", "user_id"})
})
public class FamilyMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_id", nullable = false)
    private Family family;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "member_role", nullable = false, length = 20)
    private RoleType memberRole;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    public FamilyMember() {
    }

    public FamilyMember(Family family, User user, RoleType memberRole) {
        this.family = family;
        this.user = user;
        this.memberRole = memberRole;
    }

    @PrePersist
    protected void onCreate() {
        if (this.joinedAt == null) {
            this.joinedAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Family getFamily() {
        return family;
    }

    public void setFamily(Family family) {
        this.family = family;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public RoleType getMemberRole() {
        return memberRole;
    }

    public void setMemberRole(RoleType memberRole) {
        this.memberRole = memberRole;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(Instant joinedAt) {
        this.joinedAt = joinedAt;
    }
}
