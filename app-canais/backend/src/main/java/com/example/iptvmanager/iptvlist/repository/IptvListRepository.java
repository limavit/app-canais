package com.example.iptvmanager.iptvlist.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.iptvmanager.iptvlist.entity.IptvList;
import com.example.iptvmanager.user.entity.User;

public interface IptvListRepository extends JpaRepository<IptvList, Long> {

    List<IptvList> findByOwnerOrderByCreatedAtDesc(User owner);

    List<IptvList> findAllByOrderByCreatedAtDesc();

    Optional<IptvList> findByIdAndOwner(Long id, User owner);

    long countByOwner(User owner);

    List<IptvList> findTop5ByOwnerOrderByCreatedAtDesc(User owner);

    List<IptvList> findTop5ByOrderByCreatedAtDesc();
}
