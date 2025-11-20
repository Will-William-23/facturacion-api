package com.tarea.facturacion_api.controller;

import com.tarea.facturacion_api.dto.AuthRequest;
import com.tarea.facturacion_api.dto.AuthResponse;
import com.tarea.facturacion_api.model.Usuario;
import com.tarea.facturacion_api.repository.UsuarioRepository;
import com.tarea.facturacion_api.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    // Endpoint para registrar un usuario nuevo (necesario para crear el primer usuario)
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Usuario usuario) {
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword())); // Encriptar password
        usuarioRepository.save(usuario);
        return ResponseEntity.ok("Usuario registrado exitosamente");
    }

    // Endpoint para hacer Login y obtener el Token
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        try {
            // Validar usuario y contraseña con Spring Security
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
            
            // Si es correcto, generar Token
            final String jwt = jwtUtil.generateToken(request.getUsername());
            return ResponseEntity.ok(new AuthResponse(jwt));
            
        } catch (Exception e) {
            return ResponseEntity.status(403).body("Credenciales inválidas");
        }
    }
}