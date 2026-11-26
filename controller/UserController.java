package controller;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import model.ArquivoBinario;
import model.ArquivoProduto;
import model.ArquivoUsuario;
import model.DadosNaoEncontradosException;
import model.ItemCarrinho;
import model.Loja;
import model.Produto;
import model.SenhaIncorretaException;
import model.User;
import model.UserExtended;

/**
 * Controlador principal do padrão MVC: intermedia todas as ações entre as Views
 * (terminal e Swing) e as classes de Model (User, UserExtended, Produto, Loja e persistência).
 */
public class UserController {
    private User usuarioAtual;
    private AutoSaveCarrinho autoSave;

    // --- Autenticação, Cadastro e Sessão ---

    public User login(int nroUsp, String senha)
            throws IOException, SenhaIncorretaException, DadosNaoEncontradosException {
        User user = ArquivoUsuario.lerUsuario(nroUsp, senha);
        this.usuarioAtual = user;
        return user;
    }

    public User criarConta(int tipoConta, int nroUsp, String senha,
            String storeName, String storeCategory) throws IOException {
        if (tipoConta != 0 && tipoConta != 1) {
            throw new IllegalArgumentException("Selecione 0 para cliente ou 1 para lojista.");
        }
        ArquivoBinario.validarChave(nroUsp);
        ArquivoBinario.validarTexto(senha, ArquivoBinario.TAMANHO_STRING, "Senha");
        if (tipoConta == 1) {
            ArquivoBinario.validarTexto(storeName, ArquivoBinario.TAMANHO_STRING, "Nome da loja");
            ArquivoBinario.validarTexto(storeCategory, ArquivoBinario.TAMANHO_STRING, "Categoria da loja");
        }
        if (ArquivoUsuario.existeUsuario(nroUsp)) {
            throw new IllegalArgumentException("Já existe um usuário cadastrado com este NROUSP!");
        }

        User novoUsuario = tipoConta == 0
                ? new User(nroUsp, senha)
                : new UserExtended(nroUsp, senha, storeName, storeCategory);
        ArquivoUsuario.salvarNovoUsuario(novoUsuario);
        this.usuarioAtual = novoUsuario;
        return novoUsuario;
    }

    public User criarConta(int tipoConta, int nroUsp, String senha) throws IOException {
        return criarConta(tipoConta, nroUsp, senha, "NILL", "NILL");
    }

    public User getUsuarioAtual() {
        return usuarioAtual;
    }

    public boolean temUsuarioLogado() {
        return usuarioAtual != null;
    }

    public boolean temAutoSaveAtivo() {
        return autoSave != null;
    }

    public boolean isUsuarioLojista() {
        return usuarioAtual != null && usuarioAtual.isLojista();
    }

    public int getChaveUsuarioAtual() {
        return exigirUsuarioLogado().getKey();
    }

    public int getCapacidadeCarrinho() {
        return usuarioAtual != null
                ? usuarioAtual.getCapacidadeCarrinho()
                : ArquivoBinario.TAMANHO_CARRINHO;
    }

    public void salvarDadosUsuario() throws IOException {
        User usuario = exigirUsuarioLogado();
        usuario.validarDados();
        ArquivoUsuario.salvarCarrinho(usuario.getKey(), usuario.getCartProductsRRN());
    }

    public void encerrarSessao() throws IOException {
        if (usuarioAtual != null) {
            salvarDadosUsuario();
            usuarioAtual = null;
        }
    }

    // --- Consultas de Catálogo e Lojas ---

    public List<Produto> listarCatalogoDisponivel() throws IOException {
        List<Produto> disponiveis = ArquivoProduto.listarTodos();
        disponiveis.removeIf(produto -> produto.getQuantidade() <= 0);
        return disponiveis;
    }

    public List<Loja> listarLojas() throws IOException {
        return ArquivoUsuario.listarLojas();
    }

    public Loja buscarLoja(int idVendedor) throws IOException, DadosNaoEncontradosException {
        return ArquivoUsuario.buscarLoja(idVendedor);
    }

    public List<Produto> listarProdutosPorLoja(int idVendedor) throws IOException {
        ArquivoBinario.validarChave(idVendedor);
        return ArquivoProduto.listarPorUsuario(idVendedor);
    }

    // --- Controle do Carrinho (fluxo síncrono e em memória) ---

    public int adicionarEmMemoria(int idProduto) throws IOException {
        User usuario = exigirUsuarioLogado();
        int rrn = ArquivoProduto.rrnDisponivel(idProduto);
        return usuario.adicionarRrnAoCarrinho(rrn);
    }

    public void removerEmMemoria(int posicao) {
        exigirUsuarioLogado().removerPosicaoDoCarrinho(posicao);
    }

    public void adicionarAoCarrinho(int idProduto) throws IOException {
        int posicao = adicionarEmMemoria(idProduto);
        try {
            salvarDadosUsuario();
        } catch (IOException e) {
            removerEmMemoria(posicao);
            throw e;
        }
    }

    public boolean removerDoCarrinho(int idProduto) throws IOException {
        User usuario = exigirUsuarioLogado();
        if (usuario.isCarrinhoVazio()) {
            throw new IllegalStateException("Seu carrinho está vazio!");
        }
        int[] rrns = usuario.getCartProductsRRN();
        int posicao = ArquivoProduto.localizarPosicaoNoCarrinhoPorId(rrns, idProduto);
        if (posicao < 0) {
            return false;
        }
        int rrnAnterior = rrns[posicao];
        removerEmMemoria(posicao);
        try {
            salvarDadosUsuario();
        } catch (IOException e) {
            usuario.setCartProductsRRN(rrnAnterior, posicao);
            throw e;
        }
        return true;
    }

    public List<ItemCarrinho> consultarCarrinho() throws IOException {
        User usuario = exigirUsuarioLogado();
        return ArquivoProduto.consultarItensPorRRNs(usuario.getCartProductsRRN());
    }

    // --- Controle do Carrinho com Auto-Save (interface gráfica) ---

    public void iniciarAutoSave(Consumer<String> mostrarEstado, Executor despachanteInterface) {
        User usuario = exigirUsuarioLogado();
        this.autoSave = new AutoSaveCarrinho(usuario, mostrarEstado, despachanteInterface);
    }

    public void adicionarAoCarrinhoAutoSave(int idProduto) throws IOException {
        exigirAutoSave().adicionar(idProduto);
    }

    public void removerDoCarrinhoAutoSave(int posicao) {
        exigirAutoSave().remover(posicao);
    }

    public List<ItemCarrinho> consultarCarrinhoAutoSave() throws IOException {
        return ArquivoProduto.consultarItensPorRRNs(exigirAutoSave().itens());
    }

    public void encerrarSessaoAutoSave(Runnable aoSair, Consumer<String> aoFalhar) {
        exigirAutoSave().encerrar(() -> {
            autoSave = null;
            usuarioAtual = null;
            aoSair.run();
        }, aoFalhar);
    }

    // --- Controle da Loja e Produtos do Lojista (UserExtended) ---

    public Loja atualizarDadosMinhaLoja() throws IOException, DadosNaoEncontradosException {
        UserExtended lojista = exigirLojista();
        Loja loja = ArquivoUsuario.buscarLoja(lojista.getKey());
        lojista.setStoreName(loja.getNome());
        lojista.setStoreCategory(loja.getCategoria());
        return loja;
    }

    public List<Produto> listarMeusProdutos() throws IOException {
        UserExtended lojista = exigirLojista();
        return ArquivoProduto.listarPorUsuario(lojista.getKey());
    }

    public Produto buscarProdutoDaLoja(int idProduto) throws IOException {
        UserExtended lojista = exigirLojista();
        Produto produto = ArquivoProduto.buscarPorId(idProduto);
        if (produto.getIdUsuario() != lojista.getKey()) {
            throw new IllegalArgumentException("Você só pode alterar seus próprios produtos.");
        }
        return produto;
    }

    public Produto cadastrarProduto(String nome, String descricao, double preco, int estoque)
            throws IOException {
        UserExtended lojista = exigirLojista();
        return ArquivoProduto.cadastrar(nome, descricao, preco, estoque, lojista.getKey());
    }

    public void atualizarProduto(int idProduto, String nome, String descricao,
            double preco, int estoque) throws IOException {
        Produto produto = buscarProdutoDaLoja(idProduto);
        produto.setNome(nome);
        produto.setDescricao(descricao);
        produto.setPreco(preco);
        produto.setQuantidadeEstoque(estoque);
        ArquivoProduto.atualizar(produto);
    }

    public void editarNomeProduto(int idProduto, String novoNome) throws IOException {
        Produto produto = buscarProdutoDaLoja(idProduto);
        produto.setNome(novoNome);
        ArquivoProduto.atualizar(produto);
    }

    public void editarDescricaoProduto(int idProduto, String novaDescricao) throws IOException {
        Produto produto = buscarProdutoDaLoja(idProduto);
        produto.setDescricao(novaDescricao);
        ArquivoProduto.atualizar(produto);
    }

    public void editarPrecoProduto(int idProduto, double novoPreco) throws IOException {
        Produto produto = buscarProdutoDaLoja(idProduto);
        produto.setPreco(novoPreco);
        ArquivoProduto.atualizar(produto);
    }

    public void editarEstoqueProduto(int idProduto, int novoEstoque) throws IOException {
        Produto produto = buscarProdutoDaLoja(idProduto);
        produto.setQuantidadeEstoque(novoEstoque);
        ArquivoProduto.atualizar(produto);
    }

    public void removerProdutoDaLoja(int idProduto) throws IOException {
        buscarProdutoDaLoja(idProduto);
        ArquivoProduto.removerRegistro(idProduto);
    }

    // --- Validações internas de estado da sessão ---

    private User exigirUsuarioLogado() {
        if (usuarioAtual == null) {
            throw new IllegalStateException("Nenhum usuário autenticado na sessão.");
        }
        return usuarioAtual;
    }

    private UserExtended exigirLojista() {
        User usuario = exigirUsuarioLogado();
        if (!(usuario instanceof UserExtended)) {
            throw new IllegalStateException("Operação exclusiva para contas de lojista.");
        }
        return (UserExtended) usuario;
    }

    private AutoSaveCarrinho exigirAutoSave() {
        if (autoSave == null) {
            throw new IllegalStateException("O serviço de auto-save do carrinho não foi iniciado.");
        }
        return autoSave;
    }
}
