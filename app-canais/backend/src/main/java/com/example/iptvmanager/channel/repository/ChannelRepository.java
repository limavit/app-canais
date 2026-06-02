package com.example.iptvmanager.channel.repository;

import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.example.iptvmanager.channel.entity.Channel;
import com.example.iptvmanager.iptvlist.entity.IptvList;
import com.example.iptvmanager.user.entity.User;

public interface ChannelRepository extends JpaRepository<Channel, Long>, JpaSpecificationExecutor<Channel> {

    @Override
    @EntityGraph(attributePaths = {"iptvList", "owner"})
    Page<Channel> findAll(Specification<Channel> specification, Pageable pageable);

    List<Channel> findByIptvList(IptvList iptvList);

    void deleteByIptvList(IptvList iptvList);

    long countByOwner(User owner);

    long countByFavoriteTrue();

    long countByOwnerAndFavoriteTrue(User owner);

    @Query("select count(distinct c.groupTitle) from Channel c")
    long countDistinctGroupTitle();

    @Query("select count(distinct c.groupTitle) from Channel c where c.owner = :owner")
    long countDistinctGroupTitleByOwner(User owner);

    @Query("select distinct c.groupTitle from Channel c where c.owner = :owner order by c.groupTitle")
    List<String> findGroupsByOwner(User owner);

    @Query("select distinct c.groupTitle from Channel c order by c.groupTitle")
    List<String> findAllGroups();

    @Query("select distinct c.groupTitle from Channel c where c.iptvList.id = :listId order by c.groupTitle")
    List<String> findGroupsByListId(Long listId);

    @Query("select distinct c.groupTitle from Channel c where c.owner = :owner and c.iptvList.id = :listId order by c.groupTitle")
    List<String> findGroupsByOwnerAndListId(User owner, Long listId);

    @Query("select c.streamUrl from Channel c where c.iptvList = :iptvList and c.favorite = true")
    Set<String> findFavoriteStreamUrlsByIptvList(IptvList iptvList);

    @Query("""
            select c.id from Channel c
            where (:owner is null or c.owner = :owner)
              and (:listId is null or c.iptvList.id = :listId)
              and (:group is null or c.groupTitle = :group)
              and (:term is null or lower(c.name) like lower(concat('%', :term, '%')))
              and (
                    :testStatus is null
                    or (:testStatus = 'UNTESTED' and c.testStatus is null)
                    or c.testStatus = :testStatus
              )
            order by c.id
            """)
    List<Long> findIdsForBatch(User owner, Long listId, String group, String term, String testStatus);

    List<Channel> findTop5ByOwnerOrderByCreatedAtDesc(User owner);

    List<Channel> findTop5ByOrderByCreatedAtDesc();
}
