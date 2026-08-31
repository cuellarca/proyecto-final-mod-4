package com.nurtricenter.logisticdelivery.infrastructure.acl;

/** El proveedor externo de mapas no respondio (timeout, fallo o circuito abierto). */
public class ProveedorDeMapasNoDisponibleException extends RuntimeException {

    public ProveedorDeMapasNoDisponibleException(String message) {
        super(message);
    }
}
