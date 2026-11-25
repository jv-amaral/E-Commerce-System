import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;

public class MarketPlaceSwing {
    private final JFrame janela = new JFrame("Marketplace");
    private final JLabel estado = new JLabel();
    private final JLabel ocupacao = new JLabel();
    private final DefaultListModel<String> itens = new DefaultListModel<>();
    private final JList<String> carrinho = new JList<>(itens);
    private final TabelaProdutos catalogo = new TabelaProdutos();
    private final TabelaProdutos vitrine = new TabelaProdutos();
    private final TabelaProdutos meusProdutos = new TabelaProdutos();
    private final JComboBox<MarketPlace.Loja> lojas = new JComboBox<>();
    private User usuario;
    private AutoSaveCarrinho autoSave;
    private boolean saindo;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MarketPlaceSwing().abrir());
    }

    private void abrir() {
        janela.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        janela.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { sair(true); }
        });
        lojas.addActionListener(e -> executar(this::atualizarVitrine));
        carrinho.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        mostrarLogin();
        janela.setSize(800, 520);
        janela.setLocationRelativeTo(null);
        janela.setVisible(true);
    }

    private interface Acao { void executar() throws Exception; }

    private void executar(Acao acao) {
        try {
            acao.executar();
        } catch (NumberFormatException e) {
            erro("Confira os campos numéricos.");
        } catch (Exception e) {
            erro(e.getMessage());
        }
    }

    private void erro(String mensagem) {
        JOptionPane.showMessageDialog(janela, mensagem, "Marketplace", JOptionPane.ERROR_MESSAGE);
    }

    private JButton botao(String texto, Acao acao) {
        JButton botao = new JButton(texto);
        botao.addActionListener(e -> executar(acao));
        return botao;
    }

    private static JPanel linha(JComponent... campos) {
        JPanel painel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        for (JComponent campo : campos) painel.add(campo);
        return painel;
    }

    private static JPanel formulario(String[] nomes, JComponent... campos) {
        JPanel painel = new JPanel(new GridLayout(0, 2, 8, 8));
        for (int i = 0; i < nomes.length; i++) {
            painel.add(new JLabel(nomes[i]));
            painel.add(campos[i]);
        }
        return painel;
    }

    private void mostrar(JPanel painel) {
        painel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        janela.setContentPane(painel);
        janela.revalidate();
        janela.repaint();
    }

    private void mostrarLogin() {
        JTextField chave = new JTextField(16);
        JPasswordField senha = new JPasswordField(16);
        JPanel campos = formulario(new String[]{"NROUSP", "Senha"}, chave, senha);
        JButton entrar = botao("Entrar", () -> iniciarSessao(MarketPlace.readUsersData(
                Integer.parseInt(chave.getText().trim()), new String(senha.getPassword()))));
        JPanel painel = new JPanel(new BorderLayout(8, 8));
        JPanel acesso = new JPanel(new BorderLayout(8, 8));
        acesso.add(campos, BorderLayout.CENTER);
        acesso.add(linha(entrar, botao("Cadastrar cliente", () -> cadastrar(false)),
                botao("Cadastrar lojista", () -> cadastrar(true))), BorderLayout.SOUTH);
        painel.add(acesso, BorderLayout.NORTH);
        mostrar(painel);
        janela.getRootPane().setDefaultButton(entrar);
    }

    private void cadastrar(boolean lojista) throws IOException {
        JTextField chave = new JTextField(16);
        JPasswordField senha = new JPasswordField(16);
        JTextField nome = new JTextField();
        JTextField categoria = new JTextField();
        JPanel campos = lojista
                ? formulario(new String[]{"NROUSP", "Senha", "Nome da loja", "Categoria"}, chave, senha, nome, categoria)
                : formulario(new String[]{"NROUSP", "Senha"}, chave, senha);
        if (!confirmar(campos, lojista ? "Cadastrar lojista" : "Cadastrar cliente")) return;
        User novo = MarketPlace.criarConta(lojista ? 1 : 0, Integer.parseInt(chave.getText().trim()),
                new String(senha.getPassword()), nome.getText().trim(), categoria.getText().trim());
        if (novo == null) throw new IllegalArgumentException("Já existe uma conta com esse NROUSP.");
        iniciarSessao(novo);
    }

    private boolean confirmar(Object conteudo, String titulo) {
        return JOptionPane.showConfirmDialog(janela, conteudo, titulo,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION;
    }

    private void iniciarSessao(User conta) {
        usuario = conta;
        autoSave = new AutoSaveCarrinho(conta, estado::setText);
        estado.setText("Sem alterações pendentes");
        JTabbedPane abas = new JTabbedPane();
        JButton adicionar = botao("Adicionar ao carrinho", () -> {
            autoSave.adicionar(catalogo.selecionado().getIdProduto());
            carrinhoAlterado();
        });
        abas.addTab("Catálogo", pagina(catalogo.tabela, linha(adicionar)));
        abas.addTab("Lojas", pagina(vitrine.tabela, linha(new JLabel("Loja"), lojas)));
        abas.addTab("Carrinho", pagina(carrinho, linha(ocupacao, botao("Remover item", () -> {
            autoSave.remover(carrinho.getSelectedIndex());
            carrinhoAlterado();
        }))));
        if (usuario instanceof UserExtended) {
            UserExtended vendedor = (UserExtended) usuario;
            abas.addTab("Minha loja", pagina(meusProdutos.tabela, linha(
                    botao("Cadastrar produto", () -> editarProduto(vendedor, null)),
                    botao("Editar produto", () -> editarProduto(vendedor,
                            vendedor.produtoDaLoja(meusProdutos.selecionado().getIdProduto()))),
                    botao("Remover produto", () -> {
                        Produto produto = meusProdutos.selecionado();
                        if (confirmar("Remover " + produto.getNome() + "?", "Minha loja")) {
                            vendedor.removerProduto(produto.getIdProduto());
                            atualizarProdutos();
                        }
                    }))));
        }
        JPanel painel = new JPanel(new BorderLayout(8, 8));
        painel.add(linha(new JLabel("Conta " + usuario.getKey()), botao("Sair da conta", () -> sair(false))),
                BorderLayout.NORTH);
        painel.add(abas, BorderLayout.CENTER);
        painel.add(estado, BorderLayout.SOUTH);
        janela.getRootPane().setDefaultButton(null);
        mostrar(painel);
        catalogo.preencher(List.of());
        meusProdutos.preencher(List.of());
        itens.clear();
        ocupacao.setText("");
        lojas.removeAllItems();
        executar(() -> {
            for (MarketPlace.Loja loja : MarketPlace.listarLojas()) lojas.addItem(loja);
        });
        executar(this::atualizarProdutos);
    }

    private static JPanel pagina(JComponent lista, JPanel acoes) {
        JPanel painel = new JPanel(new BorderLayout());
        painel.add(acoes, BorderLayout.NORTH);
        painel.add(new JScrollPane(lista), BorderLayout.CENTER);
        return painel;
    }

    private void editarProduto(UserExtended vendedor, Produto produto) throws IOException {
        JTextField nome = new JTextField(produto == null ? "" : produto.getNome(), 24);
        JTextField descricao = new JTextField(produto == null ? "" : produto.getDescricao());
        JTextField preco = new JTextField(produto == null ? "" : String.valueOf(produto.getPreco()));
        JTextField estoque = new JTextField(produto == null ? "" : String.valueOf(produto.getQuantidade()));
        JPanel campos = formulario(new String[]{"Nome", "Descrição", "Preço (R$)", "Estoque"},
                nome, descricao, preco, estoque);
        if (!confirmar(campos, produto == null ? "Cadastrar produto" : "Editar produto")) return;
        double valor = Double.parseDouble(preco.getText().trim().replace(',', '.'));
        int quantidade = Integer.parseInt(estoque.getText().trim());
        if (produto == null) {
            ArquivoProduto.cadastrar(nome.getText(), descricao.getText(), valor, quantidade, vendedor.getKey());
        } else {
            produto.setNome(nome.getText());
            produto.setDescricao(descricao.getText());
            produto.setPreco(valor);
            produto.setQuantidadeEstoque(quantidade);
            ArquivoProduto.atualizar(produto);
        }
        atualizarProdutos();
    }

    private void atualizarProdutos() throws IOException {
        List<Produto> disponiveis = ArquivoProduto.listarTodos();
        disponiveis.removeIf(produto -> produto.getQuantidade() == 0);
        catalogo.preencher(disponiveis);
        atualizarVitrine();
        if (usuario instanceof UserExtended) meusProdutos.preencher(ArquivoProduto.listarPorUsuario(usuario.getKey()));
        atualizarCarrinho();
    }

    private void atualizarVitrine() throws IOException {
        MarketPlace.Loja loja = (MarketPlace.Loja) lojas.getSelectedItem();
        vitrine.preencher(loja == null ? List.of() : ArquivoProduto.listarPorUsuario(loja.vendedor));
    }

    private void carrinhoAlterado() throws IOException {
        estado.setText("Alterações pendentes");
        atualizarCarrinho();
    }

    private void atualizarCarrinho() throws IOException {
        itens.clear();
        int[] rrns = autoSave.itens();
        int usados = 0;
        for (int rrn : rrns) {
            if (rrn == -1) {
                itens.addElement("Posição livre");
            } else {
                usados++;
                Produto produto = ArquivoProduto.buscarPorRRN(rrn);
                itens.addElement(produto == null ? "Produto indisponível" : produto.getNome()
                        + " — " + TabelaProdutos.MOEDA.format(produto.getPreco()));
            }
        }
        ocupacao.setText(usados + "/" + rrns.length);
    }

    private void sair(boolean fecharJanela) {
        if (saindo) return;
        if (autoSave == null) {
            janela.dispose();
            return;
        }
        saindo = true;
        janela.setEnabled(false);
        autoSave.encerrar(() -> {
            autoSave = null;
            usuario = null;
            saindo = false;
            if (fecharJanela) {
                janela.dispose();
            } else {
                janela.setEnabled(true);
                mostrarLogin();
            }
        }, mensagem -> {
            saindo = false;
            janela.setEnabled(true);
            estado.setText(mensagem);
            erro(mensagem + "\nCorrija o problema e tente sair novamente.");
        });
    }

    private static class TabelaProdutos extends AbstractTableModel {
        static final NumberFormat MOEDA = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"));
        private final String[] colunas = {"ID", "Produto", "Descrição", "Preço", "Estoque"};
        private List<Produto> produtos = List.of();
        final JTable tabela = new JTable(this);

        TabelaProdutos() {
            tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        }

        void preencher(List<Produto> produtos) {
            this.produtos = produtos;
            fireTableDataChanged();
        }

        Produto selecionado() {
            int linha = tabela.getSelectedRow();
            if (linha < 0) throw new IllegalArgumentException("Selecione um produto.");
            return produtos.get(linha);
        }

        @Override public int getRowCount() { return produtos.size(); }
        @Override public int getColumnCount() { return colunas.length; }
        @Override public String getColumnName(int coluna) { return colunas[coluna]; }
        @Override public Object getValueAt(int linha, int coluna) {
            Produto produto = produtos.get(linha);
            switch (coluna) {
                case 0: return produto.getIdProduto();
                case 1: return produto.getNome();
                case 2: return produto.getDescricao();
                case 3: return MOEDA.format(produto.getPreco());
                default: return produto.getQuantidade();
            }
        }
    }
}
