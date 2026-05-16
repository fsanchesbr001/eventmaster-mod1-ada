package com.fabriciosanches.ticketservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.kafka.topic")
public class TicketKafkaTopicsProperties {

    private String eventoCriado = "EVENTO_CRIADO";
    private String pedidoRealizado = "PEDIDO_REALIZADO";
    private String pedidoConfirmado = "PEDIDO_CONFIRMADO";
    private String pedidoCancelado = "PEDIDO_CANCELADO";

    public String getEventoCriado() {
        return eventoCriado;
    }

    public void setEventoCriado(String eventoCriado) {
        this.eventoCriado = eventoCriado;
    }

    public String getPedidoRealizado() {
        return pedidoRealizado;
    }

    public void setPedidoRealizado(String pedidoRealizado) {
        this.pedidoRealizado = pedidoRealizado;
    }

    public String getPedidoConfirmado() {
        return pedidoConfirmado;
    }

    public void setPedidoConfirmado(String pedidoConfirmado) {
        this.pedidoConfirmado = pedidoConfirmado;
    }

    public String getPedidoCancelado() {
        return pedidoCancelado;
    }

    public void setPedidoCancelado(String pedidoCancelado) {
        this.pedidoCancelado = pedidoCancelado;
    }
}

