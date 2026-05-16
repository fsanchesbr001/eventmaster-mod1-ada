package com.fabriciosanches.orderservice.domain;

import com.fabriciosanches.orderservice.enums.FormaPagamento;
import com.fabriciosanches.orderservice.enums.StatusPedido;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pedidos")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "usuario_id", nullable = false, length = 255)
    private String usuarioId;

    @Column(name = "evento_id", nullable = false)
    private Long eventoId;

    @Column(name = "valor_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorTotal;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "forma_pagamento", nullable = false, length = 50)
    private FormaPagamento formaPagamento;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 50)
    private StatusPedido status;

    @Column(name = "data_criacao", nullable = false)
    private LocalDateTime dataCriacao;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> itens = new ArrayList<>();

    public void adicionarItem(OrderItem item) {
        item.setOrder(this);
        this.itens.add(item);
    }

    public void marcarComoConfirmado() {
        validarTransicao(StatusPedido.CONFIRMADO);
        this.status = StatusPedido.CONFIRMADO;
    }

    public void marcarComoCancelado() {
        validarTransicao(StatusPedido.CANCELADO);
        this.status = StatusPedido.CANCELADO;
    }

    private void validarTransicao(StatusPedido novoStatus) {
        if (status == null) {
            throw new IllegalStateException("Pedido precisa ter um status atual antes de transicionar");
        }
        if (status == novoStatus) {
            return;
        }
        if (status != StatusPedido.REALIZADO) {
            throw new IllegalStateException("Pedido em estado final não pode transicionar novamente");
        }
    }
}

