package com.resilientbanking.domain.model;

/** Conteudo personalizado e opcional do extrato (equivale a "sessao para voce" do cardapio). */
public record Insight(String type, String message) {
}
