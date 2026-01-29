package com.tarea.facturacion_api.config;

import com.tarea.facturacion_api.model.MetodoPago;
import com.tarea.facturacion_api.model.Usuario;
import com.tarea.facturacion_api.repository.MetodoPagoRepository;
import com.tarea.facturacion_api.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private MetodoPagoRepository metodoPagoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initData() {
        return args -> {
            // Inicializar Métodos de Pago
            if (metodoPagoRepository.count() == 0) {
                crearMetodoPago("01", "SIN UTILIZACION DEL SISTEMA FINANCIERO");
                crearMetodoPago("15", "COMPENSACIÓN DE DEUDAS");
                crearMetodoPago("16", "TARJETA DE DÉBITO");
                crearMetodoPago("19", "TARJETA DE CREDITO");
                crearMetodoPago("20", "OTROS CON UTILIZACION DEL SISTEMA FINANCIERO");
                crearMetodoPago("21", "ENDOSO DE TÍTULOS");
                System.out.println(">>> Métodos de Pago inicializados.");
            }

            // Verificar si existe el usuario admin
            if (usuarioRepository.findByUsername("admin").isEmpty()) {
                Usuario admin = new Usuario();
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode("admin")); // Encriptada
                admin.setRole("ADMIN");

                usuarioRepository.save(admin);
                System.out.println(">>> Usuario ADMIN creado exitosamente (user: admin, pass: admin)");
            }
        };
    }

    private void crearMetodoPago(String codigo, String descripcion) {
        MetodoPago mp = new MetodoPago();
        mp.setCodigo(codigo);
        mp.setDescripcion(descripcion);
        metodoPagoRepository.save(mp);
    }

}
