package com.camilolopez.polizas.repository;

import com.camilolopez.polizas.entity.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface PolizaRepository extends JpaRepository<Poliza, Long> {
    @Query("select p from Poliza p where p.id > :afterId and (:tipo is null or p.tipo = :tipo) and (:estado is null or p.estado = :estado) order by p.id")
    List<Poliza> buscar(@Param("tipo") TipoPoliza tipo, @Param("estado") EstadoPoliza estado,
                        @Param("afterId") Long afterId, Pageable pageable);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Poliza p where p.id = :id")
    Optional<Poliza> findByIdForUpdate(@Param("id") Long id);
}
