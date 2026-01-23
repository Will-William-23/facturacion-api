package com.tarea.facturacion_api.dto;

import java.util.List;

public class FacturaRequest {
    
    public static class ClienteRef {
        public Long id;
    }
    
    public static class ProductoRef {
        public Long id;
    }
    
    public static class DetalleVenta {
        public ProductoRef producto;
        public Integer cantidad;
    }
    
    public ClienteRef cliente;
    public List<DetalleVenta> detalles;
    public String metodoPago;
    public Object detallesPago;
    
    public ClienteRef getCliente() {
        return cliente;
    }
    
    public void setCliente(ClienteRef cliente) {
        this.cliente = cliente;
    }
    
    public List<DetalleVenta> getDetalles() {
        return detalles;
    }
    
    public void setDetalles(List<DetalleVenta> detalles) {
        this.detalles = detalles;
    }
    
    public String getMetodoPago() {
        return metodoPago;
    }
    
    public void setMetodoPago(String metodoPago) {
        this.metodoPago = metodoPago;
    }
    
    public Object getDetallesPago() {
        return detallesPago;
    }
    
    public void setDetallesPago(Object detallesPago) {
        this.detallesPago = detallesPago;
    }
}
