package com.camilolopez.polizas.repository;

import com.camilolopez.polizas.entity.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface PolizaRepository extends JpaRepository<Poliza, Long> {
    List<Poliza> findByIdGreaterThanOrderByIdAsc(Long afterId, Pageable pageable);
    List<Poliza> findByTipoAndIdGreaterThanOrderByIdAsc(TipoPoliza tipo, Long afterId, Pageable pageable);
    List<Poliza> findByEstadoAndIdGreaterThanOrderByIdAsc(EstadoPoliza estado, Long afterId, Pageable pageable);
    List<Poliza> findByTipoAndEstadoAndIdGreaterThanOrderByIdAsc(
            TipoPoliza tipo, EstadoPoliza estado, Long afterId, Pageable pageable);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Poliza p where p.id = :id")
    Optional<Poliza> findByIdForUpdate(@Param("id") Long id);
}
