package com.tarea.facturacion_api.service;

import com.tarea.facturacion_api.model.*;
import com.tarea.facturacion_api.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class CompraService {

    @Autowired private CompraRepository compraRepository;
    @Autowired private ProveedorRepository proveedorRepository;
    @Autowired private ProductoRepository productoRepository;

    @Transactional
    public Compra registrarCompra(Compra compra) {
        // 1. Validar Proveedor
        Proveedor prov = proveedorRepository.findById(compra.getProveedor().getId())
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado"));
        compra.setProveedor(prov);
        compra.setFecha(LocalDateTime.now());

        double totalCalculado = 0.0;

        // 2. Procesar Detalles y AUMENTAR STOCK
        for (DetalleCompra det : compra.getDetalles()) {
            Producto prod = productoRepository.findById(det.getProducto().getId())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

            // AUMENTAR STOCK (Lógica Inversa a Venta)
            prod.setStock(prod.getStock() + det.getCantidad());
            // Opcional: Actualizar precio de compra si varía mucho (No lo haremos por ahora para no complicar)
            productoRepository.save(prod);

            det.setProducto(prod);
            det.setCompra(compra);
            
            totalCalculado += det.getCantidad() * det.getCostoUnitario();
        }

        compra.setTotal(totalCalculado);
        return compraRepository.save(compra);
    }
}