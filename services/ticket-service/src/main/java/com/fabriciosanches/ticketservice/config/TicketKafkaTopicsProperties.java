package com.fabriciosanches.ticketservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.kafka.topic")
public class TicketKafkaTopicsProperties {

    private String eventoCriado = "EVENTO_CRIADO";
    private String pedidoRealizado = "PEDIDO_REALIZADO";

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
}

