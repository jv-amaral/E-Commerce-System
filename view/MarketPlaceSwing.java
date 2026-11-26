package view;

import controller.UserController;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicGraphicsUtils;
import javax.swing.plaf.basic.BasicPasswordFieldUI;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import javax.swing.plaf.basic.BasicTextFieldUI;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import model.ItemCarrinho;
import model.Loja;
import model.Produto;

/** View gráfica (Swing) do marketplace. Toda comunicação com os dados ocorre via UserController. */
public class MarketPlaceSwing {
    // Paleta comum ao logotipo, à navegação e às tabelas.
    private static final Color PETROLEO = new Color(0x153C39);
    private static final Color LIMA = new Color(0xE1F297);
    private static final Color DESTAQUE = new Color(0x086951);
    private static final Color MENTA = new Color(0xD7EFE2);
    private static final Color FUNDO = new Color(0xF4F6F2);
    private static final Color TEXTO = new Color(0x18312E);
    private static final Color SECUNDARIO = new Color(0x50655F);
    private static final Color BORDA = new Color(0xCAD6CE);
    private static final Color PERIGO = new Color(0x98432E);
    private static final Font FONTE = new Font("Inter", Font.PLAIN, 14);
    private static final Border BORDA_CAMPO = new AbstractBorder() {
        @Override public Insets getBorderInsets(Component c, Insets recuo) {
            recuo.set(8, 12, 8, 12);
            return recuo;
        }
        @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D d = pincel(g);
            d.setColor(c.hasFocus() ? DESTAQUE : BORDA);
            d.setStroke(new BasicStroke(c.hasFocus() ? 2f : 1f));
            d.drawRoundRect(x + 1, y + 1, w - 3, h - 3, 8, 8);
            d.dispose();
        }
    };

    private final UserController controller;
    private final JFrame janela = new JFrame("Marketplace");
    private final JLabel estado = new JLabel();
    private final JLabel ocupacao = new JLabel();
    private final DefaultListModel<LinhaItemCarrinho> itens = new DefaultListModel<>();
    private final JList<LinhaItemCarrinho> carrinho = new JList<>(itens);
    private final ConteudoLista listaCarrinho = new ConteudoLista(carrinho, "Seu carrinho está vazio");
    private final TabelaProdutos catalogo = new TabelaProdutos("Nenhum produto disponível no catálogo");
    private final TabelaProdutos vitrine = new TabelaProdutos("Esta loja ainda não tem produtos");
    private final TabelaProdutos meusProdutos = new TabelaProdutos("Você ainda não cadastrou produtos");
    private final JComboBox<Loja> lojas = new JComboBox<>();
    private boolean saindo;

    private final JButton adicionar;
    private final JButton removerItem;
    private final JButton editar;
    private final JButton removerProduto;

    public MarketPlaceSwing() {
        this(new UserController());
    }

    public MarketPlaceSwing(UserController controller) {
        this.controller = controller;
        this.adicionar = principal("Adicionar ao carrinho", () -> {
            this.controller.adicionarAoCarrinhoAutoSave(catalogo.selecionado().getIdProduto());
            carrinhoAlterado();
        });
        this.removerItem = remocao("Remover item", () -> {
            this.controller.removerDoCarrinhoAutoSave(carrinho.getSelectedValue().posicao);
            carrinhoAlterado();
        });
        this.editar = botao("Editar produto", () -> editarProduto(
                this.controller.buscarProdutoDaLoja(meusProdutos.selecionado().getIdProduto())));
        this.removerProduto = remocao("Remover produto", () -> {
            Produto produto = meusProdutos.selecionado();
            if (confirmar("Remover " + produto.getNome() + "?", "Minha loja")) {
                this.controller.removerProdutoDaLoja(produto.getIdProduto());
                atualizarProdutos();
            }
        });
    }

    public static void main(String[] args) {
        iniciarInterface(new UserController());
    }

    public static void iniciarInterface(UserController controller) {
        // A criação dos componentes e os eventos da janela acontecem na EDT do Swing.
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(new NimbusLookAndFeel());
            } catch (UnsupportedLookAndFeelException e) {
                // O Swing pode usar o tema da plataforma se Nimbus estiver indisponível.
            }
            UIManager.put("defaultFont", FONTE);
            UIManager.put("control", FUNDO);
            UIManager.put("text", TEXTO);
            UIManager.put("nimbusBase", new Color(0x4C7769));
            UIManager.put("nimbusBlueGrey", new Color(0xC6D5C9));
            UIManager.put("nimbusFocus", DESTAQUE);
            UIManager.put("nimbusSelectionBackground", DESTAQUE);
            UIManager.put("Panel.background", FUNDO);
            UIManager.put("Label.foreground", TEXTO);
            UIManager.put("OptionPane.cancelButtonText", "Cancelar");
            new MarketPlaceSwing(controller).abrir();
        });
    }

    public void abrir() {
        janela.setIconImages(List.of(imagemDaMarca(16), imagemDaMarca(32), imagemDaMarca(64)));
        janela.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        janela.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { sair(true); }
        });
        lojas.addActionListener(e -> executar(this::atualizarVitrine));
        carrinho.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        carrinho.setFixedCellHeight(50);
        carrinho.setCellRenderer(new LinhaCarrinho());
        ocupacao.setFont(FONTE.deriveFont(Font.BOLD, 17f));
        estado.setFont(FONTE.deriveFont(12f));
        estado.setForeground(SECUNDARIO);
        estado.setIcon(new Icone(Simbolo.SALVAR, 18, DESTAQUE));
        estado.setIconTextGap(8);
        estado.setOpaque(true);
        estado.setBackground(Color.WHITE);
        estado.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, BORDA), margem(8, 24)));
        adicionar.setIcon(new Icone(Simbolo.CARRINHO, 17, Color.WHITE));
        lojas.setBackground(Color.WHITE);
        exigirSelecao(catalogo.tabela.getSelectionModel(), adicionar);
        exigirSelecao(meusProdutos.tabela.getSelectionModel(), editar, removerProduto);
        exigirSelecao(carrinho.getSelectionModel(), removerItem);
        mostrarLogin();
        janela.setMinimumSize(new Dimension(640, 420));
        janela.setSize(960, 620);
        janela.setLocationRelativeTo(null);
        janela.setVisible(true);
    }

    // Os listeners encaminham os erros para a própria janela.
    private interface Acao { void executar() throws Exception; }

    private void executar(Acao acao) {
        try {
            acao.executar();
        } catch (Exception e) {
            erro(e.getMessage());
        }
    }

    private void erro(String mensagem) {
        JOptionPane.showMessageDialog(janela, mensagem, "Marketplace", JOptionPane.ERROR_MESSAGE);
    }

    private static Border margem(int vertical, int horizontal) {
        return BorderFactory.createEmptyBorder(vertical, horizontal, vertical, horizontal);
    }

    private static JLabel marca(int tamanho) {
        JLabel titulo = new JLabel("Marketplace", new Icone(Simbolo.MARCA, tamanho, PETROLEO), JLabel.LEADING);
        titulo.setIconTextGap(10);
        titulo.setForeground(Color.WHITE);
        titulo.setFont(FONTE.deriveFont(Font.BOLD, 23f));
        return titulo;
    }

    private JButton botao(String texto, Acao acao) {
        return botao(texto, acao, Color.WHITE, TEXTO, BORDA);
    }

    private JButton botao(String texto, Acao acao, Color fundo, Color letra, Color contorno) {
        JButton botao = new Botao(texto, fundo, letra, contorno);
        botao.addActionListener(e -> executar(acao));
        return botao;
    }

    private JButton principal(String texto, Acao acao) {
        return botao(texto, acao, DESTAQUE, Color.WHITE, DESTAQUE);
    }

    private JButton remocao(String texto, Acao acao) {
        return botao(texto, acao, Color.WHITE, PERIGO, new Color(0xDAB9AD));
    }

    private static JPanel linha(JComponent... campos) {
        JPanel painel = new JPanel(new GridLayout(1, 0, 8, 0));
        painel.setOpaque(false);
        for (JComponent campo : campos) painel.add(campo);
        return painel;
    }

    // Os mesmos rótulos e campos empilhados são usados no login e nos diálogos.
    private static JPanel formulario(String[] nomes, JTextField... campos) {
        JPanel painel = new JPanel(new GridBagLayout());
        painel.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        for (int i = 0; i < nomes.length; i++) {
            JTextField campo = campos[i];
            campo.setUI(campo instanceof JPasswordField ? new BasicPasswordFieldUI() : new BasicTextFieldUI());
            campo.setFont(FONTE);
            campo.setForeground(TEXTO);
            campo.setBackground(Color.WHITE);
            campo.setCaretColor(DESTAQUE);
            campo.setBorder(BORDA_CAMPO);
            c.gridy = i * 2;
            c.weightx = 1;
            c.insets = new Insets(i == 0 ? 0 : 10, 0, 3, 0);
            JLabel rotulo = new JLabel(nomes[i]);
            rotulo.setFont(FONTE.deriveFont(Font.BOLD, 13f));
            rotulo.setLabelFor(campo);
            painel.add(rotulo, c);
            c.gridy++;
            c.insets = new Insets(0, 0, 0, 0);
            painel.add(campo, c);
        }
        return painel;
    }

    private void mostrar(JPanel painel) {
        janela.setContentPane(painel);
        janela.revalidate();
        janela.repaint();
    }

    private void mostrarLogin() {
        JTextField chave = new JTextField(16);
        JPasswordField senha = new JPasswordField(16);
        JButton entrar = principal("Entrar", () -> {
            controller.login(inteiro(chave, "NROUSP"), new String(senha.getPassword()));
            iniciarSessao();
        });
        entrar.setIcon(new Icone(Simbolo.SETA, 17, Color.WHITE));
        entrar.setHorizontalTextPosition(SwingConstants.LEADING);
        JPanel acoes = new JPanel(new BorderLayout(0, 16));
        acoes.setOpaque(false);
        acoes.add(entrar, BorderLayout.NORTH);
        acoes.add(linha(botao("Cadastrar cliente", () -> cadastrar(false)),
                botao("Cadastrar lojista", () -> cadastrar(true))), BorderLayout.SOUTH);

        JLabel titulo = new JLabel("Acessar conta");
        titulo.setFont(FONTE.deriveFont(Font.BOLD, 30f));
        JPanel acesso = new JPanel(new BorderLayout(0, 18));
        acesso.setBackground(Color.WHITE);
        acesso.add(titulo, BorderLayout.NORTH);
        acesso.add(formulario(new String[]{"NROUSP", "Senha"}, chave, senha), BorderLayout.CENTER);
        acesso.add(acoes, BorderLayout.SOUTH);
        JPanel direita = new JPanel(new GridBagLayout());
        direita.setBackground(Color.WHITE);
        direita.setBorder(margem(20, 18));
        direita.add(acesso);

        JPanel capa = new JPanel(new BorderLayout());
        capa.setBackground(PETROLEO);
        capa.setBorder(margem(26, 22));
        capa.add(marca(36), BorderLayout.NORTH);
        capa.add(new Fachada(), BorderLayout.CENTER);
        JPanel painel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.weightx = 0.48;
        painel.add(capa, c);
        c.weightx = 0.52;
        painel.add(direita, c);
        mostrar(painel);
        janela.getRootPane().setDefaultButton(entrar);
        SwingUtilities.invokeLater(chave::requestFocusInWindow);
    }

    private static int inteiro(JTextField campo, String nome) {
        try {
            return Integer.parseInt(campo.getText().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(nome + " deve ser um número inteiro.");
        }
    }

    private static double lerPreco(JTextField campo) {
        try {
            return Double.parseDouble(campo.getText().trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Informe um preço numérico, por exemplo 12,50.");
        }
    }

    private boolean confirmar(Object conteudo, String titulo) {
        return JOptionPane.showConfirmDialog(janela, conteudo, titulo,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION;
    }

    // Em caso de validação inválida, reapresenta os mesmos campos com os valores digitados.
    private boolean enviarFormulario(JPanel campos, String titulo, Acao gravar) throws Exception {
        while (confirmar(campos, titulo)) {
            try {
                gravar.executar();
                return true;
            } catch (IllegalArgumentException e) {
                erro(e.getMessage());
            }
        }
        return false;
    }

    private void cadastrar(boolean lojista) throws Exception {
        JTextField chave = new JTextField(18);
        JPasswordField senha = new JPasswordField(18);
        JTextField nome = new JTextField();
        JTextField categoria = new JTextField();
        JPanel campos = lojista
                ? formulario(new String[]{"NROUSP", "Senha", "Nome da loja", "Categoria"}, chave, senha, nome, categoria)
                : formulario(new String[]{"NROUSP", "Senha"}, chave, senha);
        enviarFormulario(campos, lojista ? "Cadastrar lojista" : "Cadastrar cliente", () -> {
            controller.criarConta(lojista ? 1 : 0, inteiro(chave, "NROUSP"),
                    new String(senha.getPassword()), nome.getText().trim(), categoria.getText().trim());
            iniciarSessao();
        });
    }

    // Cada lista controla os botões que dependem de sua seleção.
    private static void exigirSelecao(ListSelectionModel selecao, JButton... botoes) {
        for (JButton botao : botoes) botao.setEnabled(false);
        selecao.addListSelectionListener(e -> {
            for (JButton botao : botoes) botao.setEnabled(!selecao.isSelectionEmpty());
        });
    }

    private void iniciarSessao() {
        controller.iniciarAutoSave(estado::setText, SwingUtilities::invokeLater);
        estado.setText("Sem alterações pendentes");
        JTabbedPane abas = new JTabbedPane();
        abas.setUI(new Abas());
        abas.setOpaque(true);
        abas.setBackground(Color.WHITE);
        abas.setForeground(TEXTO);
        abas.setFont(FONTE.deriveFont(Font.BOLD, 13f));
        abas.addTab("Catálogo", iconeAba(Simbolo.CATALOGO),
                pagina(catalogo.conteudo, barra(catalogo.contagem, adicionar)));
        JPanel seletor = new JPanel(new BorderLayout(12, 0));
        JLabel rotuloLoja = new JLabel("Loja selecionada");
        rotuloLoja.setFont(FONTE.deriveFont(Font.BOLD, 14f));
        rotuloLoja.setLabelFor(lojas);
        seletor.add(rotuloLoja, BorderLayout.WEST);
        seletor.add(lojas, BorderLayout.CENTER);
        abas.addTab("Lojas", iconeAba(Simbolo.LOJA), pagina(vitrine.conteudo, seletor));

        abas.addTab("Carrinho", iconeAba(Simbolo.CARRINHO), pagina(listaCarrinho, barra(ocupacao, removerItem)));
        boolean lojista = controller.isUsuarioLojista();
        if (lojista) abas.addTab("Minha loja", iconeAba(Simbolo.CAIXA), minhaLoja());

        JPanel cabecalho = new JPanel(new BorderLayout(12, 0));
        cabecalho.setBackground(PETROLEO);
        cabecalho.setBorder(margem(14, 24));
        cabecalho.add(marca(36), BorderLayout.WEST);
        JLabel perfil = new JLabel(lojista ? "Lojista" : "Cliente");
        perfil.setForeground(Color.WHITE);
        perfil.setFont(FONTE.deriveFont(Font.BOLD, 13f));
        JLabel numero = new JLabel("Conta " + controller.getChaveUsuarioAtual());
        numero.setForeground(new Color(0xC4DCD1));
        numero.setFont(FONTE.deriveFont(12f));
        JPanel identidade = new JPanel(new GridLayout(2, 1));
        identidade.setOpaque(false);
        identidade.add(perfil);
        identidade.add(numero);
        JButton sair = botao("Sair da conta", () -> sair(false),
                new Color(0x254E48), Color.WHITE, new Color(0x5B7B72));
        JPanel contaAtual = new JPanel(new BorderLayout(18, 0));
        contaAtual.setOpaque(false);
        contaAtual.add(identidade, BorderLayout.CENTER);
        contaAtual.add(sair, BorderLayout.EAST);
        cabecalho.add(contaAtual, BorderLayout.EAST);
        JPanel painel = new JPanel(new BorderLayout());
        painel.add(cabecalho, BorderLayout.NORTH);
        painel.add(abas, BorderLayout.CENTER);
        painel.add(estado, BorderLayout.SOUTH);
        janela.getRootPane().setDefaultButton(null);
        mostrar(painel);
        catalogo.preencher(List.of());
        meusProdutos.preencher(List.of());
        itens.clear();
        ocupacao.setText("");
        listaCarrinho.mostrarVazio(true);
        lojas.removeAllItems();
        executar(() -> {
            for (Loja loja : controller.listarLojas()) lojas.addItem(loja);
        });
        executar(this::atualizarProdutos);
    }

    private JPanel minhaLoja() {
        JButton cadastrar = principal("Cadastrar produto", () -> editarProduto(null));
        return pagina(meusProdutos.conteudo, barra(meusProdutos.contagem, linha(cadastrar, editar, removerProduto)));
    }

    // O resumo ocupa o espaço disponível, e as ações se alinham à borda direita.
    private static JPanel barra(JComponent resumo, JComponent acoes) {
        JPanel painel = new JPanel(new BorderLayout(10, 0));
        painel.add(resumo, BorderLayout.CENTER);
        painel.add(acoes, BorderLayout.EAST);
        return painel;
    }

    private static JPanel pagina(JComponent lista, JPanel acoes) {
        JPanel painel = new JPanel(new BorderLayout(0, 14));
        painel.setBackground(FUNDO);
        painel.setBorder(margem(18, 24));
        painel.add(acoes, BorderLayout.NORTH);
        painel.add(lista, BorderLayout.CENTER);
        return painel;
    }

    private void editarProduto(Produto produto) throws Exception {
        JTextField nome = new JTextField(produto == null ? "" : produto.getNome(), 24);
        JTextField descricao = new JTextField(produto == null ? "" : produto.getDescricao());
        JTextField preco = new JTextField(produto == null ? "" : String.valueOf(produto.getPreco()));
        JTextField estoque = new JTextField(produto == null ? "" : String.valueOf(produto.getQuantidade()));
        JPanel campos = formulario(new String[]{"Nome", "Descrição", "Preço (R$)", "Estoque"},
                nome, descricao, preco, estoque);
        boolean gravado = enviarFormulario(campos, produto == null ? "Cadastrar produto" : "Editar produto", () -> {
            double valor = lerPreco(preco);
            int quantidade = inteiro(estoque, "Estoque");
            if (produto == null) {
                controller.cadastrarProduto(nome.getText(), descricao.getText(), valor, quantidade);
            } else {
                controller.atualizarProduto(produto.getIdProduto(),
                        nome.getText(), descricao.getText(), valor, quantidade);
            }
        });
        if (gravado) atualizarProdutos();
    }

    // Uma alteração no produto pode afetar o catálogo, as vitrines e os preços do carrinho.
    private void atualizarProdutos() throws IOException {
        catalogo.preencher(controller.listarCatalogoDisponivel());
        atualizarVitrine();
        if (controller.isUsuarioLojista()) {
            meusProdutos.preencher(controller.listarMeusProdutos());
        }
        atualizarCarrinho();
    }

    private void atualizarVitrine() throws IOException {
        Loja loja = (Loja) lojas.getSelectedItem();
        vitrine.conteudo.aviso.setText(loja == null ? "Nenhuma loja cadastrada" : "Esta loja ainda não tem produtos");
        vitrine.preencher(loja == null ? List.of() : controller.listarProdutosPorLoja(loja.getVendedor()));
    }

    // A edição já ocorreu em memória. Apenas o auto-save confirma a escrita no rodapé.
    private void carrinhoAlterado() throws IOException {
        estado.setText("Alterações pendentes");
        atualizarCarrinho();
    }

    private void atualizarCarrinho() throws IOException {
        itens.clear();
        List<ItemCarrinho> itensConsultados = controller.consultarCarrinhoAutoSave();
        int capacidade = controller.getCapacidadeCarrinho();
        for (ItemCarrinho item : itensConsultados) {
            Produto produto = item.getProduto();
            String nome = produto == null ? "Produto indisponível" : produto.getNome();
            String preco = produto == null ? "" : TabelaProdutos.MOEDA.format(produto.getPreco());
            itens.addElement(new LinhaItemCarrinho(item.getPosicao(), nome, preco));
        }
        ocupacao.setText(itens.size() + " de " + capacidade + " posições ocupadas");
        ocupacao.setIcon(new Medidor(itens.size(), capacidade));
        ocupacao.setIconTextGap(10);
        listaCarrinho.mostrarVazio(itens.isEmpty());
    }

    // O X e o logout aguardam a mesma gravação final. Uma falha permite tentar novamente.
    private void sair(boolean fecharJanela) {
        if (saindo) return;
        if (!controller.temAutoSaveAtivo()) {
            janela.dispose();
            return;
        }
        saindo = true;
        janela.setEnabled(false);
        controller.encerrarSessaoAutoSave(() -> {
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

    // Duas linhas podem apontar para o mesmo produto, mas cada uma ocupa sua própria posição.
    private static class LinhaItemCarrinho {
        final int posicao;
        final String nome;
        final String preco;

        LinhaItemCarrinho(int posicao, String nome, String preco) {
            this.posicao = posicao;
            this.nome = nome;
            this.preco = preco;
        }

        @Override public String toString() { return preco.isEmpty() ? nome : nome + " — " + preco; }
    }

    // O texto e o preço ganham colunas próprias na lista, mantendo a posição real no item.
    private static class LinhaCarrinho extends JPanel implements ListCellRenderer<LinhaItemCarrinho> {
        private final JLabel nome = new JLabel();
        private final JLabel preco = new JLabel();

        LinhaCarrinho() {
            super(new BorderLayout(12, 0));
            setBorder(margem(0, 14));
            nome.setIcon(iconeAba(Simbolo.CAIXA));
            nome.setIconTextGap(12);
            nome.setFont(FONTE);
            nome.putClientProperty("html.disable", true);
            preco.setForeground(DESTAQUE);
            preco.setFont(FONTE.deriveFont(Font.BOLD));
            add(nome, BorderLayout.CENTER);
            add(preco, BorderLayout.EAST);
        }

        @Override public Component getListCellRendererComponent(JList<? extends LinhaItemCarrinho> lista,
                LinhaItemCarrinho item, int indice, boolean selecionado, boolean foco) {
            nome.setText(item.nome);
            preco.setText(item.preco);
            setBackground(fundoLinha(indice, selecionado));
            getAccessibleContext().setAccessibleName(item.toString());
            return this;
        }
    }

    // Alterna entre a lista e seu aviso vazio sem criar uma linha selecionável fictícia.
    private static class ConteudoLista extends JPanel {
        final CardLayout layout = new CardLayout();
        final JLabel aviso;

        ConteudoLista(JComponent lista, String mensagem) {
            setLayout(layout);
            setBorder(BorderFactory.createLineBorder(BORDA));
            aviso = new JLabel(mensagem, SwingConstants.CENTER);
            aviso.setOpaque(true);
            aviso.setBackground(Color.WHITE);
            aviso.setForeground(SECUNDARIO);
            aviso.setIcon(new Icone(lista instanceof JList ? Simbolo.CARRINHO : Simbolo.LOJA, 38, DESTAQUE));
            aviso.setHorizontalTextPosition(SwingConstants.CENTER);
            aviso.setVerticalTextPosition(SwingConstants.BOTTOM);
            aviso.setIconTextGap(12);
            JScrollPane rolagem = new JScrollPane(lista);
            rolagem.setBorder(null);
            rolagem.getViewport().setBackground(Color.WHITE);
            add(rolagem, "lista");
            add(aviso, "vazio");
            mostrarVazio(true);
        }

        void mostrarVazio(boolean vazio) {
            layout.show(this, vazio ? "vazio" : "lista");
        }
    }

    private static Color fundoLinha(int linha, boolean selecionada) {
        if (selecionada) return MENTA;
        return linha % 2 == 0 ? Color.WHITE : FUNDO;
    }

    // Uma marca por posição do carrinho. O rótulo ao lado também informa a ocupação em texto.
    private static class Medidor implements Icon {
        private final int usadas;
        private final int total;

        Medidor(int usadas, int total) {
            this.usadas = usadas;
            this.total = total;
        }

        @Override public int getIconWidth() { return total * 8; }
        @Override public int getIconHeight() { return 14; }
        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D d = pincel(g);
            for (int i = 0; i < total; i++) {
                d.setColor(i < usadas ? DESTAQUE : BORDA);
                d.fillRoundRect(x + 8 * i, y, 6, 14, 4, 4);
            }
            d.dispose();
        }
    }

    // O cabeçalho e as células usam o mesmo alinhamento para cada coluna.
    private static class CelulaProduto extends DefaultTableCellRenderer {
        private final Border recuo = margem(0, 10);

        CelulaProduto() {
            putClientProperty("html.disable", true);
            setOpaque(true);
        }

        @Override public Component getTableCellRendererComponent(JTable tabela, Object valor, boolean selecionada,
                boolean foco, int linha, int coluna) {
            super.getTableCellRendererComponent(tabela, valor, selecionada, foco, linha, coluna);
            boolean cabecalho = linha < 0;
            setBorder(recuo);
            setHorizontalAlignment(coluna == 0 || coluna >= 3 ? RIGHT : LEFT);
            if (cabecalho) {
                setText(valor.toString().toUpperCase(Locale.ROOT));
                setFont(FONTE.deriveFont(Font.BOLD, 11f));
                setForeground(SECUNDARIO);
                setBackground(new Color(0xE7EEE7));
            } else {
                setFont(FONTE.deriveFont(coluna == 1 || coluna == 3 ? Font.BOLD : Font.PLAIN));
                setForeground(coluna == 3 ? DESTAQUE : TEXTO);
                setBackground(fundoLinha(linha, selecionada));
            }
            setToolTipText(!cabecalho && (coluna == 1 || coluna == 2) ? getText() : null);
            return this;
        }
    }

    // A pintura personaliza as abas; a seleção e a navegação por teclado continuam a cargo do Swing.
    private static class Abas extends BasicTabbedPaneUI {
        @Override protected void installDefaults() {
            super.installDefaults();
            tabInsets = new Insets(10, 14, 10, 14);
            tabAreaInsets = new Insets(3, 16, 0, 0);
            selectedTabPadInsets = new Insets(0, 0, 0, 0);
            contentBorderInsets = new Insets(1, 0, 0, 0);
            textIconGap = 7;
            focus = DESTAQUE;
        }

        @Override protected void paintTabBackground(Graphics g, int local, int indice, int x, int y,
                int largura, int altura, boolean ativa) {
            g.setColor(ativa ? FUNDO : Color.WHITE);
            g.fillRect(x, y, largura, altura);
        }

        @Override protected void paintTabBorder(Graphics g, int local, int indice, int x, int y,
                int largura, int altura, boolean ativa) {
            if (ativa) {
                g.setColor(DESTAQUE);
                g.fillRoundRect(x + 6, y + altura - 3, largura - 12, 3, 3, 3);
            }
        }

        @Override protected void paintContentBorder(Graphics g, int local, int indice) {
            if (indice < 0) return;
            Rectangle aba = getTabBounds(tabPane, indice);
            g.setColor(BORDA);
            g.drawLine(0, aba.y + aba.height, tabPane.getWidth(), aba.y + aba.height);
        }
    }

    // O Swing continua tratando o teclado e o mouse. A pintura distingue foco e ações desabilitadas.
    private static class Botao extends JButton {
        private final Color contorno;
        private static final BasicButtonUI UI_PLANA = new BasicButtonUI() {
            @Override protected void paintText(Graphics g, JComponent c, Rectangle area, String texto) {
                if (c.isEnabled()) {
                    super.paintText(g, c, area, texto);
                } else {
                    g.setColor(SECUNDARIO);
                    BasicGraphicsUtils.drawString(c, (Graphics2D) g, texto,
                            area.x, area.y + g.getFontMetrics().getAscent());
                }
            }
        };

        Botao(String texto, Color fundo, Color letra, Color contorno) {
            super(texto);
            this.contorno = contorno;
            setUI(UI_PLANA);
            setBackground(fundo);
            setForeground(letra);
            setFont(FONTE.deriveFont(Font.BOLD, 13f));
            setBorder(margem(9, 14));
            setOpaque(false);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setIconTextGap(7);
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D d = pincel(g);
            int w = getWidth() - 1;
            int h = getHeight() - 1;
            d.setColor(isEnabled() ? getBackground() : new Color(0xE0E7E1));
            d.fillRoundRect(0, 0, w, h, 10, 10);
            if (isEnabled() && getModel().isRollover()) {
                d.setColor(new Color(0, 35, 28, getModel().isPressed() ? 38 : 18));
                d.fillRoundRect(0, 0, w, h, 10, 10);
            }
            d.setColor(isEnabled() ? contorno : BORDA);
            d.drawRoundRect(0, 0, w, h, 10, 10);
            if (hasFocus()) {
                d.setColor(getForeground());
                d.drawRoundRect(3, 3, w - 6, h - 6, 6, 6);
            }
            d.dispose();
            super.paintComponent(g);
        }
    }

    // O símbolo da sacola com a letra M e os ícones são vetoriais, inclusive em telas de alta resolução.
    private enum Simbolo { MARCA, CATALOGO, LOJA, CARRINHO, CAIXA, SALVAR, SETA }

    private static Icon iconeAba(Simbolo forma) {
        return new Icone(forma, 19, DESTAQUE);
    }

    private static Graphics2D pincel(Graphics g) {
        Graphics2D d = (Graphics2D) g.create();
        d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        return d;
    }

    private static Path2D caminho(double... pontos) {
        Path2D linha = new Path2D.Double();
        linha.moveTo(pontos[0], pontos[1]);
        for (int i = 2; i < pontos.length; i += 2) linha.lineTo(pontos[i], pontos[i + 1]);
        return linha;
    }

    private static BufferedImage imagemDaMarca(int tamanho) {
        BufferedImage imagem = new BufferedImage(tamanho, tamanho, BufferedImage.TYPE_INT_ARGB);
        Graphics2D d = imagem.createGraphics();
        new Icone(Simbolo.MARCA, tamanho, PETROLEO).paintIcon(null, d, 0, 0);
        d.dispose();
        return imagem;
    }

    private static class Icone implements Icon {
        private final Simbolo forma;
        private final int tamanho;
        private final Color cor;

        Icone(Simbolo forma, int tamanho, Color cor) {
            this.forma = forma;
            this.tamanho = tamanho;
            this.cor = cor;
        }

        @Override public int getIconWidth() { return tamanho; }
        @Override public int getIconHeight() { return tamanho; }
        @Override public void paintIcon(Component componente, Graphics g, int x, int y) {
            Graphics2D d = pincel(g);
            d.translate(x, y);
            d.scale(tamanho / 24.0, tamanho / 24.0);
            d.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            d.setColor(componente != null && !componente.isEnabled() ? SECUNDARIO : cor);
            switch (forma) {
                case MARCA:
                    d.setColor(LIMA);
                    d.fillRoundRect(0, 0, 24, 24, 7, 7);
                    d.setColor(PETROLEO);
                    d.drawArc(8, 4, 8, 10, 0, 180);
                    d.fill(caminho(5.5, 9, 18.5, 9, 20, 19.5, 4, 19.5));
                    d.setColor(LIMA);
                    d.draw(caminho(8, 16.5, 8.7, 12.5, 12, 15.5, 15.3, 12.5, 16, 16.5));
                    break;
                case CATALOGO:
                    for (int i = 0; i < 4; i++) d.drawRoundRect(3 + i % 2 * 10, 3 + i / 2 * 10, 7, 7, 2, 2);
                    break;
                case LOJA:
                    d.draw(caminho(3, 10, 5, 4, 19, 4, 21, 10, 3, 10));
                    d.draw(caminho(5, 10, 5, 20, 19, 20, 19, 10));
                    d.drawRect(12, 13, 4, 7);
                    break;
                case CARRINHO:
                    d.draw(caminho(2, 4, 5, 4, 8, 15, 18, 15, 21, 7, 6, 7));
                    d.fillOval(7, 18, 3, 3);
                    d.fillOval(16, 18, 3, 3);
                    break;
                case CAIXA:
                    d.draw(caminho(12, 2, 21, 7, 21, 17, 12, 22, 3, 17, 3, 7, 12, 2));
                    d.draw(caminho(3, 7, 12, 12, 21, 7));
                    d.drawLine(12, 12, 12, 21);
                    break;
                case SALVAR:
                    d.draw(caminho(4, 3, 16, 3, 20, 7, 20, 21, 4, 21, 4, 3));
                    d.draw(caminho(8, 3, 8, 9, 15, 9, 15, 3));
                    d.drawRect(8, 14, 8, 7);
                    break;
                case SETA:
                    d.drawLine(3, 12, 20, 12);
                    d.draw(caminho(14, 6, 20, 12, 14, 18));
                    break;
            }
            d.dispose();
        }
    }

    /** Fachada ilustrada do login. O desenho mantém as proporções quando a janela muda de tamanho. */
    private static class Fachada extends JPanel {
        Fachada() {
            setOpaque(false);
            setPreferredSize(new Dimension(310, 260));
            setMinimumSize(new Dimension(0, 0));
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D d = pincel(g);
            double escala = Math.min(getWidth() / 320.0, getHeight() / 260.0);
            d.translate((getWidth() - 320 * escala) / 2, (getHeight() - 260 * escala) / 2);
            d.scale(escala, escala);
            Color creme = new Color(0xF8EBD0);
            Color terracota = new Color(0xDA9268);
            Color vidro = new Color(0xA4D3C0);

            // O arco e a sombra situam a loja dentro do painel da marca.
            d.setPaint(new GradientPaint(25, 20, new Color(0x2F6455), 260, 226, new Color(0x204B44)));
            d.fillRoundRect(19, 12, 275, 217, 190, 190);
            d.setColor(new Color(0xFFDA92));
            d.fillOval(227, 22, 30, 30);
            d.setColor(new Color(0x102E2D));
            d.fillOval(16, 207, 286, 23);

            // A face lateral e a moldura do telhado dão profundidade à fachada.
            d.setColor(new Color(0x87B6A1));
            d.fill(caminho(228, 68, 265, 48, 265, 193, 228, 213));
            d.setColor(creme);
            d.fillRect(58, 68, 172, 145);
            d.setColor(new Color(0xC3DABD));
            d.fill(caminho(54, 66, 89, 47, 269, 47, 233, 66));
            d.setColor(new Color(0xE2D9AE));
            d.fillRect(54, 66, 179, 9);
            d.setColor(new Color(0x609480));
            d.fill(caminho(233, 66, 269, 47, 269, 56, 233, 76));
            d.fill(caminho(239, 117, 256, 108, 256, 166, 239, 175));

            // Vidros, caixilhos e mercadorias da vitrine.
            d.setColor(PETROLEO);
            d.fillRect(69, 113, 99, 77);
            d.fillRect(179, 113, 39, 100);
            d.setColor(vidro);
            d.fillRect(73, 117, 91, 69);
            d.fillRect(183, 117, 31, 67);
            d.setColor(new Color(0xD4EBDA));
            d.fill(caminho(76, 166, 117, 117, 129, 117, 76, 180));
            d.fill(caminho(103, 172, 147, 117, 152, 117, 108, 172));
            d.setColor(new Color(0x579680));
            d.fillRect(73, 176, 91, 10);
            d.setColor(terracota);
            d.fillRect(81, 158, 23, 18);
            d.setColor(creme);
            d.fillRect(89, 158, 5, 9);
            d.fillRect(132, 163, 22, 13);
            d.setColor(PETROLEO);
            d.fillRect(117, 117, 3, 69);
            d.setColor(LIMA);
            d.fillRoundRect(207, 167, 3, 11, 3, 3);
            d.setPaint(new GradientPaint(0, 114, new Color(18, 51, 44, 52), 0, 134, new Color(18, 51, 44, 0)));
            d.fillRect(58, 114, 172, 20);

            // Cada faixa do toldo tem uma parte inclinada e uma aba na frente.
            d.setColor(new Color(0x426E5F));
            d.fill(caminho(232, 82, 260, 66, 272, 92, 242, 110));
            for (int i = 0; i < 8; i++) {
                int topo = 56 + i * 22;
                int base = 46 + i * 25;
                d.setColor(i % 2 == 0 ? terracota : creme);
                d.fill(caminho(topo, 82, topo + 22, 82, base + 25, 108, base, 108));
                d.setColor(i % 2 == 0 ? new Color(0xC67C53) : new Color(0xE5D5AF));
                d.fillRoundRect(base, 104, 25, 14, 9, 9);
            }
            d.setColor(new Color(0xD9C69B));
            d.fillRect(54, 210, 178, 5);

            // Folhagem em primeiro plano e um vaso junto à vitrine.
            Path2D folhas = new Path2D.Double();
            folhas.moveTo(34, 198);
            folhas.quadTo(16, 191, 16, 174);
            folhas.quadTo(33, 178, 34, 198);
            folhas.quadTo(30, 175, 47, 165);
            folhas.quadTo(51, 185, 34, 198);
            d.setColor(new Color(0x8DBB95));
            d.fill(folhas);
            d.setColor(terracota);
            d.fill(caminho(23, 195, 47, 195, 43, 213, 27, 213));
            d.setColor(creme);
            d.fillRoundRect(21, 193, 28, 5, 3, 3);

            // Uma encomenda junto à porta retoma o símbolo de caixa usado no carrinho.
            d.setColor(new Color(0xD49B64));
            d.fillRect(238, 185, 34, 29);
            d.setColor(new Color(0xB0794F));
            d.fill(caminho(272, 185, 284, 178, 284, 207, 272, 214));
            d.setColor(new Color(0xE9BE88));
            d.fill(caminho(238, 185, 250, 178, 284, 178, 272, 185));
            d.setColor(creme);
            d.fill(caminho(252, 185, 264, 178, 270, 178, 258, 185, 258, 196, 252, 196));
            d.dispose();
        }
    }

    /** Apresenta a lista de produtos da entrega 1, com formatação somente na tela. */
    private static class TabelaProdutos extends AbstractTableModel {
        static final NumberFormat MOEDA = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"));
        private final String[] colunas = {"ID", "Produto", "Descrição", "Preço", "Estoque"};
        private List<Produto> produtos = List.of();
        final JTable tabela = new JTable(this);
        final ConteudoLista conteudo;
        final JLabel contagem = new JLabel();

        TabelaProdutos(String mensagemVazia) {
            contagem.setFont(FONTE.deriveFont(Font.BOLD, 21f));
            tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            tabela.setRowHeight(40);
            tabela.setIntercellSpacing(new Dimension(0, 1));
            tabela.setShowVerticalLines(false);
            tabela.setShowHorizontalLines(true);
            tabela.setGridColor(new Color(0xE5EBE6));
            tabela.setFillsViewportHeight(true);
            tabela.setDefaultRenderer(Object.class, new CelulaProduto());
            tabela.getTableHeader().setReorderingAllowed(false);
            tabela.getTableHeader().setDefaultRenderer(new CelulaProduto());
            tabela.getTableHeader().setPreferredSize(new Dimension(0, 34));
            int[] larguras = {62, 220, 308, 115, 80};
            for (int i = 0; i < colunas.length; i++) {
                tabela.getColumnModel().getColumn(i).setPreferredWidth(larguras[i]);
            }
            tabela.getColumnModel().getColumn(4).setMinWidth(78);
            conteudo = new ConteudoLista(tabela, mensagemVazia);
        }

        void preencher(List<Produto> produtos) {
            tabela.clearSelection();
            this.produtos = produtos;
            fireTableDataChanged();
            contagem.setText(produtos.size() + (produtos.size() == 1 ? " produto" : " produtos"));
            conteudo.mostrarVazio(produtos.isEmpty());
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
