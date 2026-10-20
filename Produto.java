//definicao da classe produto

public class Produto {
    private byte removido; // sera usada futuramente
    private String nome;
    private String descricao;
    private double preco;
    private int quantidade_estoque;
    private int idProduto;
    private int idUsuario;

    // construtor da classe
    // implementados com o tratamento de erro
    public Produto(byte removido, String nome, String descricao, double preco, int quantidade_estoque, int idProduto,
            int idUsuario) {
        if (removido != 0 && removido != 1) {
            throw new IllegalArgumentException("A flag deve ser 0 (ativo) ou 1 (removido).");
        }
        this.removido = removido;

        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O nome não pode ser vazio.");
        } else {
            this.nome = nome;
        }

        if (descricao == null || descricao.isBlank()) {
            throw new IllegalArgumentException("A descrição não pode ser vazia.");
        } else {
            this.descricao = descricao;
        }

        if (!Double.isFinite(preco) || preco <= 0) {
            throw new IllegalArgumentException("O preço deve ser finito e maior que zero.");
        } else {
            this.preco = preco;
        }

        if (quantidade_estoque < 0) {
            throw new IllegalArgumentException("O estoque inicial não pode ser negativo.");
        } else {
            this.quantidade_estoque = quantidade_estoque;
        }

        if (idProduto <= 0) {
            throw new IllegalArgumentException("O identificador deve ser maior que zero.");
        }

        if (idUsuario <= 0) {
            throw new IllegalArgumentException("O identificador do usuário deve ser maior que zero.");
        }

        this.idProduto = idProduto;
        this.idUsuario = idUsuario;
    }

    // implementacao dos metodos

    public byte getRemovido() {
        return removido;
    }

    public String getNome() {
        return nome;
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

    public int getIdProduto() {
        return idProduto;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public int getIdLoja() {
        return idUsuario;
    }

    public void setQuantidadeEstoque(int novaQuantidade) {
        if (novaQuantidade < 0) {
            throw new IllegalArgumentException("A quantidade em estoque não pode ser negativa.");
        }
        quantidade_estoque = novaQuantidade;
    }

    public void setIdProduto(int novoIdProduto) {
        if (novoIdProduto <= 0) {
            throw new IllegalArgumentException("O identificador deve ser maior que zero.");
        }
        idProduto = novoIdProduto;
    }

    // metodos utilziados na troca de paramentros, como nome, etc
    // eles foram feitos com o tratamento de erro, para evitar falhas no programa
    public void setNome(String novoNome) {
        if (novoNome == null || novoNome.isBlank()) {
            throw new IllegalArgumentException("O nome não pode ser vazio.");
        }
        nome = novoNome;
    }

    public void setDescricao(String novaDescricao) {
        if (novaDescricao == null || novaDescricao.isBlank()) {
            throw new IllegalArgumentException("A descrição não pode estar vazia.");
        }
        descricao = novaDescricao;
    }

    public void setPreco(double novoPreco) {
        if (!Double.isFinite(novoPreco) || novoPreco <= 0) {
            throw new IllegalArgumentException("O preço deve ser finito e maior que zero.");
        }
        preco = novoPreco;
    }
}
