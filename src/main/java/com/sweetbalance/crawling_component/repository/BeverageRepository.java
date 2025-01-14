package com.sweetbalance.crawling_component.repository;

import com.sweetbalance.crawling_component.entity.Beverage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BeverageRepository extends JpaRepository<Beverage, Long> {

    Optional<Beverage> findByName(String username);
}
