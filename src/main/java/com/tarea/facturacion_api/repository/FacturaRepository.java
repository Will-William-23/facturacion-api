package com.tarea.facturacion_api.repository;

import com.tarea.facturacion_api.model.Factura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FacturaRepository extends JpaRepository<Factura, Long> {

    @Query("SELECT DISTINCT f FROM Factura f LEFT JOIN FETCH f.detalles d LEFT JOIN FETCH d.producto WHERE f.id = :id")
    Optional<Factura> findByIdWithDetails(@Param("id") Long id);
}