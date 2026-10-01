package com.camilolopez.polizas.repository;

import com.camilolopez.polizas.entity.Riesgo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface RiesgoRepository extends JpaRepository<Riesgo, Long> {
    List<Riesgo> findByPolizaIdOrderByIdAsc(Long polizaId);
    @Modifying(flushAutomatically = true)
    @Query("update Riesgo r set r.estado = com.camilolopez.polizas.entity.EstadoRiesgo.CANCELADO "
            + "where r.poliza.id = :polizaId and r.estado <> com.camilolopez.polizas.entity.EstadoRiesgo.CANCELADO")
    int cancelarPorPoliza(@Param("polizaId") Long polizaId);
    @Query("select r.poliza.id from Riesgo r where r.id = :id")
    Optional<Long> findPolizaId(@Param("id") Long id);
}
