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
    // implementados com o tratamento de erro
    public Produto(String nome, String categoria_produto, String descricao,
            double preco, int quantidade_estoque, int id) {

        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException(
                    "O nome não pode ser vazio.");
        } else {
            this.nome = nome;
        }

        if (categoria_produto == null || categoria_produto.isBlank()) {
            throw new IllegalArgumentException(
                    "A categoria não pode ser vazia.");
        } else {
            this.categoria_produto = categoria_produto;
        }

        if (descricao == null || descricao.isBlank()) {
            throw new IllegalArgumentException(
                    "A descrição não pode ser vazia.");
        } else {
            this.descricao = descricao;
        }

        if (!Double.isFinite(preco) || preco <= 0) {
            throw new IllegalArgumentException(
                    "O preço deve ser finito e maior que zero.");
        } else {
            this.preco = preco;
        }

        if (quantidade_estoque < 0) {
            throw new IllegalArgumentException(
                    "O estoque inicial não pode ser negativo.");
        } else {
            this.quantidade_estoque = quantidade_estoque;
        }

        if (id <= 0) {
            throw new IllegalArgumentException(
                    "O identificador deve ser maior que zero.");
        }

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

    // metodos utilziados na troca de paramentros, como nome, categoria etc
    // eles foram feitos com o tratamento de erro, para evitar falhas no programa
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
        if (!Double.isFinite(novoPreco) || novoPreco <= 0) {
            throw new IllegalArgumentException(
                    "O preço deve ser finito e maior que zero.");
        }
        preco = novoPreco;
    }

    public void adicionarEstoque(int unidadesAacrescentar) {
        if (unidadesAacrescentar <= 0) {
            throw new IllegalArgumentException(
                    "A quantidade a ser acrescentada deve ser maior que zero.");
        }

        if (unidadesAacrescentar > Integer.MAX_VALUE - quantidade_estoque) {
            throw new IllegalArgumentException(
                    "A quantidade ultrapassa o limite do estoque.");
        }

        quantidade_estoque = quantidade_estoque + unidadesAacrescentar;
    }

    public void retirarEstoque(int unidadesAretirar) {
        if (unidadesAretirar <= 0) {
            throw new IllegalArgumentException(
                    "A quantidade a ser retirada deve ser maior que zero.");
        }

        if (unidadesAretirar > quantidade_estoque) {
            throw new IllegalArgumentException(
                    "Estoque insuficiente.");
        }

        quantidade_estoque = quantidade_estoque - unidadesAretirar;
    }
}