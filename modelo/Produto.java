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

    //metodos utilziados na troca de paramentros, como nome, categoria etc
    //eles foram feitos com o tratamento de erro, para evitar falhas no programa
    public void setNome(String novoNome) {
        if (novoNome == null || novoNome.isBlank()) {
            throw new IllegalArgumentException(
                    "O nome não pode ser vazio.");
        }
        nome = novoNome;

    }

    public void setCategoria(String novaCategoria) {
        if (novaCategoria == null || novaCategoria.isBlank()) {
            throw new IllegalArgumentException(
                    "A Categoria não pode ser vazia.");
        }
        categoria_produto = novaCategoria;
    }

    public void setDescricao(String novaDescricao) {
        if (novaDescricao == null || novaDescricao.isBlank()) {
            throw new IllegalArgumentException(
                    "A descrição não pode estar vazia.");
        }
        descricao = novaDescricao;
    }

    public void setPreco(double novoPreco) {
        if (novoPreco < 0) {
            throw new IllegalArgumentException(
                    "O preço não pode ser negativo.");
        }
        preco = novoPreco;
    }

    public void adicionarEstoque(int unidadesAacrescentar) {
        if (unidadesAacrescentar <= 0) {
            throw new IllegalArgumentException(
                    "A quantidade a ser acrescentada deve ser diferente de 0.");
        }
        quantidade_estoque = quantidade_estoque + unidadesAacrescentar;
    }

    public void retirarEstoque(int unidadesAretirar) {
        if (unidadesAretirar >= 0) {
            throw new IllegalArgumentException(
                    "A quantidade a ser retirada deve ser diferente de 0.");
        }
        quantidade_estoque = quantidade_estoque - unidadesAretirar;
    }

}