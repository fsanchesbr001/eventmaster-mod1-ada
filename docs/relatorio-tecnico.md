# Relatório Técnico de Análise Arquitetural e Implementação — Ecossistema EventMaster

Esta revisão técnica contempla a significativa evolução estrutural realizada no repositório **EventMaster**. O ecossistema transitou de uma fundação baseada em CRUDs isolados para uma arquitetura distribuída complexa, totalmente orientada a eventos (*Event-Driven Architecture*) com suporte a transações distribuídas resilientes, o que demonstra uma maturidade técnica de nível corporativo.

---

## 1. Mapeamento do Fluxo Transacional (Padrão SAGA por Coreografia)

Com a introdução do `order-service` e do `payment-service`, o sistema passou a orquestrar o processo de compra de ingressos utilizando o **Padrão SAGA baseado em Coreografia**, com comunicação totalmente assíncrona operada via **Apache Kafka**.

O ciclo de vida de uma compra segue o fluxo abaixo:

1. **Intenção de Compra e Início da SAGA:** O cliente submete um pedido ao `order-service` mapeando o evento desejado, quantidade e dados dos portadores. O serviço persiste o pedido com o estado incipiente e publica o evento `PedidoRealizadoEvent` no barramento Kafka.
2. **Reserva de Inventário e Processamento de Pagamento:**
* O **`ticket-service`** escuta o evento através do `PedidoRealizadoListener` e bloqueia provisoriamente os assentos vinculando-os ao ID do pedido (conforme estruturado na migração `V003` do banco de dados).
* Em paralelo, o **`payment-service`** intercepta o mesmo evento via `PedidoRealizadoListener` e delega a validação ao `PaymentProcessorService`, emitindo um veredicto sob a forma de `PagamentoConfirmadoEvent` ou `PagamentoNegadoEvent`.


3. **Consolidação ou Ações de Compensação (Resiliência):**
* **Cenário de Sucesso:** O `order-service` consome o evento de sucesso mudando o status do pedido para confirmado, disparando o evento `PedidoConfirmadoEvent`. O `ticket-service` reage consolidando a emissão definitiva dos ingressos.
* **Cenário de Falha (Compensação):** Caso o pagamento seja rejeitado, o `order-service` aciona o `PagamentoNegadoListener`, cancela o pedido localmente e publica o evento `PedidoCanceladoEvent`. O `ticket-service` consome este sinal por meio do `PedidoCanceladoListener` e libera imediatamente os ingressos reservados de volta para o estoque, mantendo a consistência eventual do sistema.



---

## 2. Análise da Evolução dos Componentes Core

### 2.1. `event-service`

* **Implementação Concluída:** Adição da migração `V002__Adicionar_Preco_Ao_Evento.sql`, acoplando com sucesso a coluna `preco_base` de forma tipada (`DECIMAL(10,2)`).
* **Tratamento Monetário:** Uso correto do tipo `BigDecimal` nas camadas de mapeamento de dados do Java, blindando o microsserviço contra imprecisões de arredondamento de ponto flutuante.
* **Mensageria Ativa:** Suporte ao disparo automático do record `EventCreatedEvent` empacotado sob uma arquitetura de pacotes compartilhados (`com.fabriciosanches.shared.events`).

### 2.2. `order-service`

* **Domínio Transacional:** Implementação completa da entidade `Order` agregando coleções granulares de `OrderItem`.
* **Cálculo de Preços:** Implementação precisa das regras de precificação por item de pedido, distinguindo de forma limpa ingressos do tipo `INTEIRA` de ingressos do tipo `MEIA` (computando o desconto normativo de 50%) com agregação cumulativa para o valor total consolidado do pedido.
* **Integração Síncrona Inicial:** Uso adequado de chamadas via `RestTemplate` para verificação de barreiras de pré-requisitos antes do início da transação assíncrona.

### 2.3. `ticket-service`

* **Injeção de Inventário:** Implementação do `EventCreatedListener`. Ao escutar a criação de um show, o serviço executa o cálculo de contingência reduzindo em **5%** a capacidade total informada, isolando a margem de segurança no Redis e inicializando as posições de estoque.

---

## 3. Qualidade de Software e Cobertura de Testes Automatizados

Um dos pontos mais altos desta atualização é a entrega sistemática de coberturas de teste para toda a malha de mensageria assíncrona. O repositório não possui apenas testes de sanidade básica, mas testes complexos que validam o comportamento de ouvintes de eventos:

* **Testes de Listeners e Serviços:** Inclusão de testes robustos como `PagamentoConfirmadoListenerTests`, `PagamentoNegadoListenerTests`, `PedidoRealizadoListenerTests` e `EventCreatedListenerTests`. Esses testes simulam a chegada dos payloads JSON do Kafka para certificar a resiliência e as transições de estado corretas.
* **Validação de Fluxos E2E:** A inclusão de múltiplos cenários de testes ponta a ponta documentados via Postman (como a coleção `eventmaster-platform-e2e-payment-denied-via-gateway.postman_collection.json`) garante que a integridade da plataforma possa ser verificada externamente sob a perspectiva de um teste do tipo *Black Box Pentest*.

---

## 4. Recomendações Técnicas e Próximas Otimizações

Com o ecossistema distribuído agora funcional e robustamente testado, existem pequenas otimizações arquiteturais que podem elevar o sistema a um patamar ainda mais alto de resiliência em produção:

1. **Idempotência nos Ouvintes (Listeners):** Em sistemas baseados em eventos, falhas de rede podem causar o reenvio de mensagens (*At-least-once delivery*). É altamente recomendável garantir que listeners como o `PedidoRealizadoListener` no `ticket-service` validem se o ID do pedido recebido já não foi processado anteriormente, evitando dupla reserva de assentos.
2. **Estratégias de Dead Letter Queues (DLQ):** Configurar tópicos de erro dedicados (ex: `PEDIDO_REALIZADO_DLQ`) para os cenários onde o JSON recebido esteja corrompido ou ocorra um erro sistêmico persistente inviabilizando o processamento automático, impedindo o travamento da fila principal do Kafka.
3. **Externalização de Propriedades de Tópicos:** Mapeamentos explícitos de nomes de tópicos em classes como `TicketKafkaTopicsProperties.java` mostram uma excelente organização de código. Esta abordagem deve ser replicada para os demais serviços core para assegurar que alterações em nomes de filas não exijam modificações em anotações no código-fonte Java.

### Conclusão

A arquitetura atual do **EventMaster** apresenta uma engenharia extremamente limpa, aderência rigorosa ao design pattern SAGA, desacoplamento nativo entre domínios e excelente qualidade de automação de testes. O ecossistema está completamente consolidado para o encerramento com sucesso do desafio prático.
