package model;

/** Modelo que agrupa os dados de uma loja cadastrada em Stores.bin. */
public class Loja {
    private final int vendedor;
    private final String nome;
    private final String categoria;

    public Loja(int vendedor, String nome, String categoria) {
        this.vendedor = vendedor;
        this.nome = nome;
        this.categoria = categoria;
    }

    public int getVendedor() {
        return vendedor;
    }

    public String getNome() {
        return nome;
    }

    public String getCategoria() {
        return categoria;
    }

    @Override
    public String toString() {
        return nome + " — " + categoria + " (vendedor " + vendedor + ")";
    }
}
