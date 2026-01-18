package com.tarea.facturacion_api.dto;

import lombok.Data;
import java.util.List;

@Data
public class SolicitudCompraDto {
    private Long clienteId;
    private List<ItemCompraDto> items;

    @Data
    public static class ItemCompraDto {
        private Long productoId;
        private Integer cantidad;
    }
}
