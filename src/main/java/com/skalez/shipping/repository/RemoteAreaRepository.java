package com.skalez.shipping.repository;

import com.skalez.shipping.domain.RemoteArea;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RemoteAreaRepository extends JpaRepository<RemoteArea, Long> {
    List<RemoteArea> findByZipFromLessThanEqualAndZipToGreaterThanEqual(int zip, int sameZip);
    List<RemoteArea> findAllByOrderByZipFromAsc();
}
