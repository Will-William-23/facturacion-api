package com.tarea.facturacion_api.repository;

import com.tarea.facturacion_api.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    // Métodos para validar duplicados
    boolean existsByCedula(String cedula);
    boolean existsByEmail(String email);
    
    // Método para buscar (por si queremos recuperar datos)
    Optional<Cliente> findByCedula(String cedula);
}