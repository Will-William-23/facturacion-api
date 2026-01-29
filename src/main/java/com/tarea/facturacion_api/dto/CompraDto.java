package com.tarea.facturacion_api.dto;

import java.util.List;
import lombok.Data;

@Data
public class CompraDto {
    private Long proveedorId;
    private Long metodoPagoId;
    private List<ItemCompraDto> items;

    @Data
    public static class ItemCompraDto {
        private Long productoId;
        private Integer cantidad;
    }
}
