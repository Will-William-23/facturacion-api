package com.tarea.facturacion_api.repository;

import com.tarea.facturacion_api.model.Factura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FacturaRepository extends JpaRepository<Factura, Long> {
    
    // Método para filtrar por rango de fechas
    List<Factura> findByFechaBetween(LocalDateTime inicio, LocalDateTime fin);
}