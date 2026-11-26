package model;

/** Representa uma posição ocupada do carrinho após consulta ao arquivo de produtos. */
public class ItemCarrinho {
    public enum Estado {
        ATIVO,
        REMOVIDO_PELO_VENDEDOR,
        INDISPONIVEL
    }

    private final int posicao;
    private final Estado estado;
    private final int idProduto;
    private final Produto produto;

    private ItemCarrinho(int posicao, Estado estado, int idProduto, Produto produto) {
        this.posicao = posicao;
        this.estado = estado;
        this.idProduto = idProduto;
        this.produto = produto;
    }

    public static ItemCarrinho ativo(int posicao, Produto produto) {
        return new ItemCarrinho(posicao, Estado.ATIVO, produto.getIdProduto(), produto);
    }

    public static ItemCarrinho removidoPeloVendedor(int posicao, int idProduto) {
        return new ItemCarrinho(posicao, Estado.REMOVIDO_PELO_VENDEDOR, idProduto, null);
    }

    public static ItemCarrinho indisponivel(int posicao) {
        return new ItemCarrinho(posicao, Estado.INDISPONIVEL, -1, null);
    }

    public int getPosicao() {
        return posicao;
    }

    public Estado getEstado() {
        return estado;
    }

    public boolean estaAtivo() {
        return estado == Estado.ATIVO && produto != null;
    }

    public int getIdProduto() {
        return idProduto;
    }

    public Produto getProduto() {
        return produto;
    }
}
