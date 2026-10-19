package modelo;

//definicao da classe produto

public class Produto {
    private String nome;
    private String categoria_produto;
    private String descricao;
    private double preco;
    private int quantidade_estoque;
    private int id;

    // construtor da classe
    public Produto(String nome, String categoria_produto, String descricao, double preco, int quantidade_estoque,
            int id) {
        this.nome = nome;
        this.categoria_produto = categoria_produto;
        this.descricao = descricao;
        this.preco = preco;
        this.quantidade_estoque = quantidade_estoque;
        this.id = id;
    }

    // implementacao dos metodos

    public String getNome() {
        return nome;
    }

    public String getCategoria() {
        return categoria_produto;
    }

    public String getDescricao() {
        return descricao;
    }

    public double getPreco() {
        return preco;
    }

    public int getQuantidade() {
        return quantidade_estoque;
    }

    public int getId() {
        return id;
    }

    public void setNome(String novoNome) {
        nome = novoNome;

    }

    public void setCategoria(String novaCategoria) {
        categoria_produto = novaCategoria;
    }

    public void setDescricao(String novaDescricao) {
        descricao = novaDescricao;
    }

    public void setPreco(double novoPreco) {
        preco = novoPreco;
    }

    public void adicionarEstoque(int unidadesAacrescentar) {
        quantidade_estoque = quantidade_estoque + unidadesAacrescentar;
    }

    public void retirarEstoque(int unidadesAretirar) {
        quantidade_estoque = quantidade_estoque - unidadesAretirar;
    }

}