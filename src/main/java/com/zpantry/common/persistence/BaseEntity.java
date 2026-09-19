package com.zpantry.common.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import java.time.Instant;
import java.util.UUID;

@MappedSuperclass
public abstract class BaseEntity {
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    protected UUID id;

    @Column(name = "created_at", nullable = false, updatable = false)
    protected Instant createdAt;

    @Column(name = "created_by")
    protected UUID createdBy;

    @Column(name = "updated_at")
    protected Instant updatedAt;

    @Column(name = "updated_by")
    protected UUID updatedBy;

    @Column(name = "deleted_at")
    protected Instant deletedAt;

    @Column(name = "deleted_by")
    protected UUID deletedBy;

    @Column(name = "is_deleted", nullable = false)
    protected boolean deleted;

    @PrePersist
    protected void initializeIdentityAndCreationTime() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public Instant getCreatedAt() { return createdAt; }
    public UUID getCreatedBy() { return createdBy; }
    public Instant getUpdatedAt() { return updatedAt; }
    public UUID getUpdatedBy() { return updatedBy; }
    public Instant getDeletedAt() { return deletedAt; }
    public UUID getDeletedBy() { return deletedBy; }
    public boolean isDeleted() { return deleted; }
    public void touch() { updatedAt = Instant.now(); }
    public void softDelete() { deleted = true; deletedAt = Instant.now(); }
}
