package com.tarea.facturacion_api.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data // Lombok: Crea getters, setters, toString, etc.
@NoArgsConstructor // Lombok: Constructor vacío
@AllArgsConstructor // Lombok: Constructor con todos los campos
@Entity // JPA: Esto es una entidad (tabla)
@Table(name = "clientes") // JPA: Nombre de la tabla en MySQL
public class Cliente {

    @Id // JPA: Clave Primaria
    @GeneratedValue(strategy = GenerationType.IDENTITY) // JPA: Autoincremental
    private Long id;

    private String nombre;
    private String apellido;
    private String direccion;
    private String email;
}