package com.newsletter.repository;

import com.newsletter.model.MailingList;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MailingListRepository extends MongoRepository<MailingList, String> {
    List<MailingList> findByOwnerId(String ownerId);
    Optional<MailingList> findByIdAndOwnerId(String id, String ownerId);
    boolean existsByNameAndOwnerId(String name, String ownerId);
}
