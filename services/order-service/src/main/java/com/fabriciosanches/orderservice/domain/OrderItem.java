package com.fabriciosanches.orderservice.domain;

import com.fabriciosanches.orderservice.enums.TipoIngresso;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "pedido_itens")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Order order;

    @Column(name = "nome_portador", nullable = false, length = 255)
    private String nomePortador;

    @Column(name = "cpf_portador", nullable = false, length = 14)
    private String cpfPortador;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_ingresso", nullable = false, length = 20)
    private TipoIngresso tipoIngresso;

    @Column(name = "preco_pago", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoPago;
}

