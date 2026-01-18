package com.tarea.facturacion_api.config;

import com.tarea.facturacion_api.model.Usuario;
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
    private PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initData() {
        return args -> {
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
}
