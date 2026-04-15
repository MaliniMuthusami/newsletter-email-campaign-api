package com.newsletter.repository;

import com.newsletter.model.Campaign;
import com.newsletter.model.CampaignStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CampaignRepository extends MongoRepository<Campaign, String> {

    Page<Campaign> findByOwnerId(String ownerId, Pageable pageable);

    Page<Campaign> findByOwnerIdAndStatus(String ownerId, CampaignStatus status, Pageable pageable);

    Optional<Campaign> findByIdAndOwnerId(String id, String ownerId);

    @Query("{ 'status': ?0, 'scheduledAt': { $lte: ?1 } }")
    List<Campaign> findDueCampaigns(CampaignStatus status, LocalDateTime now);
}
