package ru.haritonenko.telegrambotminicrm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.haritonenko.telegrambotminicrm.model.Lead;

public interface LeadRepository extends JpaRepository<Lead, Long> {

    @Query(value = """
        SELECT EXISTS(
          SELECT 1 FROM minicrm.leads
          WHERE regexp_replace(phone, '\\D', '', 'g') = regexp_replace(:phone, '\\D', '', 'g')
        )
        """, nativeQuery = true)
    boolean existsByPhoneEquivalent(@Param("phone") String phone);
}
