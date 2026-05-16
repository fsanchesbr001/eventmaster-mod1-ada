package com.fabriciosanches.ticketservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity(name = "Ticket")
@Table(name = "ingresso")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "id_ingresso", nullable = false, unique = true)
    private Long idIngresso;

    @Column(name = "pedido_id")
    private Long pedidoId;

    @Column(nullable = false, length = 255)
    private String evento;

    @Column(nullable = false)
    private LocalDate data;

    @Column(nullable = false)
    private LocalTime hora;

    @Column(name = "tipo_ingresso", nullable = false, length = 25)
    private String tipoIngresso;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(name = "nome_participante", nullable = false, length = 255)
    private String nomeParticipante;

    @Column(name = "cpf_participante", nullable = false, length = 20)
    private String cpfParticipante;

    @Column(nullable = false, length = 20)
    private String situacao;

    public Ticket(Long idIngresso,
                  String evento,
                  LocalDate data,
                  LocalTime hora,
                  String tipoIngresso,
                  BigDecimal valor,
                  String nomeParticipante,
                  String cpfParticipante,
                  String situacao) {
        this.idIngresso = idIngresso;
        this.evento = evento;
        this.data = data;
        this.hora = hora;
        this.tipoIngresso = tipoIngresso;
        this.valor = valor;
        this.nomeParticipante = nomeParticipante;
        this.cpfParticipante = cpfParticipante;
        this.situacao = situacao;
    }
}

