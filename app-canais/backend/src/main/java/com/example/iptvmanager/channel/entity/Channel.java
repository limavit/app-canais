package com.example.iptvmanager.channel.entity;

import java.time.LocalDateTime;

import com.example.iptvmanager.iptvlist.entity.IptvList;
import com.example.iptvmanager.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "channels")
public class Channel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String streamUrl;

    private String groupTitle;
    private String logoUrl;
    private String tvgId;
    private String tvgName;
    private String duration;

    @Column(nullable = false)
    private Boolean favorite = false;

    @Column(nullable = false)
    private Boolean active = true;

    private String testStatus;
    private Integer testHttpStatus;
    private String testMessage;
    private LocalDateTime lastTestAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "iptv_list_id", nullable = false)
    private IptvList iptvList;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        if (favorite == null) {
            favorite = false;
        }
        if (active == null) {
            active = true;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getStreamUrl() { return streamUrl; }
    public void setStreamUrl(String streamUrl) { this.streamUrl = streamUrl; }
    public String getGroupTitle() { return groupTitle; }
    public void setGroupTitle(String groupTitle) { this.groupTitle = groupTitle; }
    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
    public String getTvgId() { return tvgId; }
    public void setTvgId(String tvgId) { this.tvgId = tvgId; }
    public String getTvgName() { return tvgName; }
    public void setTvgName(String tvgName) { this.tvgName = tvgName; }
    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }
    public Boolean getFavorite() { return favorite; }
    public void setFavorite(Boolean favorite) { this.favorite = favorite; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public String getTestStatus() { return testStatus; }
    public void setTestStatus(String testStatus) { this.testStatus = testStatus; }
    public Integer getTestHttpStatus() { return testHttpStatus; }
    public void setTestHttpStatus(Integer testHttpStatus) { this.testHttpStatus = testHttpStatus; }
    public String getTestMessage() { return testMessage; }
    public void setTestMessage(String testMessage) { this.testMessage = testMessage; }
    public LocalDateTime getLastTestAt() { return lastTestAt; }
    public void setLastTestAt(LocalDateTime lastTestAt) { this.lastTestAt = lastTestAt; }
    public IptvList getIptvList() { return iptvList; }
    public void setIptvList(IptvList iptvList) { this.iptvList = iptvList; }
    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
