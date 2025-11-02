package ru.haritonenko.telegrambotminicrm.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.haritonenko.telegrambotminicrm.model.User;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Query(value = """
            SELECT * FROM minicrm.crm_user 
            WHERE regexp_replace(phone, '\\D', '', 'g') = regexp_replace(:phone, '\\D', '', 'g')
            """, nativeQuery = true)
    List<User> findByPhoneEquivalent(@Param("phone") String phone);

    Page<User> findByDistrictIgnoreCase(String district, Pageable pageable);
    Page<User> findBySourceIgnoreCase(String source, Pageable pageable);
}
