import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

// Programa separado para preencher os arquivos com dados fictícios de teste.
// Usa as classes do projeto; não substitui a main do MarketPlace.
public class PopularDados {
    private static final String[] ARQUIVOS = {
            "Users.bin", "Stores.bin", "Products.bin"
    };

    private static final int PRIMEIRO_VENDEDOR = 1000001;
    private static final int PRIMEIRO_COMPRADOR = 2000001;

    public static void main(String[] args) {
        // Os arquivos são criados na pasta em que o comando java é executado.
        System.out.println(
                "Pasta dos dados: "
                        + Paths.get("").toAbsolutePath().normalize());

        // Recusa a execução se qualquer .bin já existir, mesmo que esteja vazio.
        // Assim, executar este programa novamente não duplica nem apaga cadastros.
        for (String nome : ARQUIVOS) {
            Path caminho = Paths.get(nome);

            if (Files.exists(caminho, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
                System.err.println(
                        "Preenchimento cancelado: já existe " + nome + ".");

                System.err.println(
                        "Execute em uma pasta separada, sem os três arquivos .bin.");

                System.exit(1);
            }
        }

        try {
            LojaExemplo[] lojas = montarLojas();
            validarExemplos(lojas);

            // Cada vendedor também terá sua loja cadastrada em Stores.bin.
            for (int i = 0; i < lojas.length; i++) {
                int idUsuario = PRIMEIRO_VENDEDOR + i;

                String senha = String.format(
                        java.util.Locale.ROOT, "vendedor%02d", i + 1);

                User vendedor = MarketPlace.criarConta(
                        1, idUsuario, senha, lojas[i].nome, lojas[i].categoria);

                if (vendedor == null) {
                    throw new IllegalStateException(
                            "Não foi possível cadastrar o vendedor " + idUsuario);
                }
            }

            // Compradores são usuários do tipo 0 e não recebem loja.
            for (int i = 0; i < 10; i++) {
                int idUsuario = PRIMEIRO_COMPRADOR + i;

                String senha = String.format(
                        java.util.Locale.ROOT, "comprador%02d", i + 1);

                User comprador = MarketPlace.criarConta(0, idUsuario, senha);

                if (comprador == null) {
                    throw new IllegalStateException(
                            "Não foi possível cadastrar o comprador " + idUsuario);
                }
            }

            // ArquivoProduto gera os IDs 1 a 100 e grava a flag ativa automaticamente.
            // O último argumento é o ID do vendedor, conforme o formato atual do projeto.
            int totalProdutos = 0;

            for (int i = 0; i < lojas.length; i++) {
                int idUsuario = PRIMEIRO_VENDEDOR + i;

                for (ProdutoExemplo exemplo : lojas[i].produtos) {
                    Produto produto = ArquivoProduto.cadastrar(
                            exemplo.nome,
                            exemplo.descricao,
                            exemplo.preco,
                            exemplo.estoque,
                            idUsuario);

                    totalProdutos++;

                    if (produto.getIdProduto() != totalProdutos) {
                        throw new IllegalStateException(
                                "A sequência de IDs divergiu do PDF de consulta.");
                    }
                }
            }

            System.out.println("\nPreenchimento concluído!");

            System.out.println(
                    "10 vendedores, 10 compradores, 10 lojas e "
                            + totalProdutos + " produtos.");

            System.out.println(
                    "Vendedores: NROUSP 1000001 a 1000010; "
                            + "senhas vendedor01 a vendedor10.");

            System.out.println(
                    "Compradores: NROUSP 2000001 a 2000010; "
                            + "senhas comprador01 a comprador10.");

            System.out.println(
                    "Todos os carrinhos começam vazios; "
                            + "todos os produtos começam ativos.");

            System.out.println(
                    "Abra o MarketPlace usando esta mesma pasta para os dados.");

        } catch (IOException | IllegalArgumentException | IllegalStateException erro) {
            System.err.println(
                    "Falha no preenchimento: " + erro.getMessage());

            System.err.println(
                    "A execução pode ter deixado dados parciais "
                            + "na pasta informada acima.");

            System.err.println(
                    "Para tentar novamente, use outra pasta vazia.");

            System.exit(1);
        }
    }

    // Confere todos os exemplos antes de iniciar qualquer gravação.
    private static void validarExemplos(LojaExemplo[] lojas) {
        if (lojas.length != 10) {
            throw new IllegalArgumentException(
                    "Devem existir dez lojas de exemplo.");
        }

        for (int i = 0; i < lojas.length; i++) {
            LojaExemplo loja = lojas[i];

            MarketPlace.validarTexto(loja.nome, 60, "Nome da loja");
            MarketPlace.validarTexto(loja.categoria, 60, "Categoria da loja");

            if (loja.produtos.length != 10) {
                throw new IllegalArgumentException(
                        "Cada loja deve possuir dez produtos.");
            }

            for (int j = 0; j < loja.produtos.length; j++) {
                ProdutoExemplo p = loja.produtos[j];

                MarketPlace.validarTexto(p.nome, 60, "Nome do produto");
                MarketPlace.validarTexto(p.descricao, 100, "Descrição");

                // O construtor de Produto verifica preço, estoque, flag e IDs.
                new Produto(
                        (byte) 0,
                        p.nome,
                        p.descricao,
                        p.preco,
                        p.estoque,
                        i * 10 + j + 1,
                        PRIMEIRO_VENDEDOR + i);
            }
        }
    }

    // Estas duas classes auxiliares apenas agrupam os exemplos abaixo.
    private static class ProdutoExemplo {
        final String nome;
        final String descricao;
        final double preco;
        final int estoque;

        ProdutoExemplo(String nome, String descricao, double preco, int estoque) {
            this.nome = nome;
            this.descricao = descricao;
            this.preco = preco;
            this.estoque = estoque;
        }
    }

    private static class LojaExemplo {
        final String nome;
        final String categoria;
        final ProdutoExemplo[] produtos;

        LojaExemplo(String nome, String categoria, ProdutoExemplo... produtos) {
            this.nome = nome;
            this.categoria = categoria;
            this.produtos = produtos;
        }
    }

    private static LojaExemplo[] montarLojas() {
        return new LojaExemplo[] {
                // Vendedor 1000001; produtos 1 a 10.
                new LojaExemplo("Conecta Tech", "Eletrônicos",
                        new ProdutoExemplo("Mouse sem fio",
                                "Mouse óptico com conexão USB e três botões.", 49.90, 12),
                        new ProdutoExemplo("Teclado compacto",
                                "Teclado USB com layout ABNT2 e teclas silenciosas.", 89.90, 20),
                        new ProdutoExemplo("Fone com microfone",
                                "Fone estéreo com microfone para estudos e chamadas.", 79.90, 8),
                        new ProdutoExemplo("Cabo USB-C",
                                "Cabo de um metro para carga e transferência de dados.", 24.90, 30),
                        new ProdutoExemplo("Suporte para notebook",
                                "Suporte ajustável para elevar a tela do notebook.", 69.90, 15),
                        new ProdutoExemplo("Hub USB quatro portas",
                                "Hub USB compacto para conectar quatro acessórios.", 59.90, 10),
                        new ProdutoExemplo("Webcam HD",
                                "Câmera HD com microfone integrado e encaixe para monitor.", 129.90, 25),
                        new ProdutoExemplo("Caixa de som USB",
                                "Par de caixas de som compactas com alimentação USB.", 64.90, 18),
                        new ProdutoExemplo("Mousepad grande",
                                "Mousepad de tecido com base de borracha antiderrapante.", 29.90, 40),
                        new ProdutoExemplo("Luminária USB",
                                "Luminária de mesa com haste flexível e três intensidades.", 44.90, 6)),

                // Vendedor 1000002; produtos 11 a 20.
                new LojaExemplo("Casa Aconchego", "Casa e cozinha",
                        new ProdutoExemplo("Caneca de cerâmica",
                                "Caneca de 300 ml com acabamento liso para bebidas quentes.", 24.90, 14),
                        new ProdutoExemplo("Garrafa térmica",
                                "Garrafa de 500 ml com tampa de rosca e parede dupla.", 59.90, 22),
                        new ProdutoExemplo("Jogo de potes",
                                "Conjunto de três potes com tampas para organizar alimentos.", 39.90, 10),
                        new ProdutoExemplo("Tábua de bambu",
                                "Tábua retangular de bambu para preparo de alimentos.", 34.90, 32),
                        new ProdutoExemplo("Pano de prato",
                                "Conjunto de três panos de algodão para uso na cozinha.", 19.90, 17),
                        new ProdutoExemplo("Organizador de gaveta",
                                "Organizador com divisórias para talheres e utensílios.", 27.90, 12),
                        new ProdutoExemplo("Almofada decorativa",
                                "Almofada de 40 cm com capa removível e enchimento.", 44.90, 27),
                        new ProdutoExemplo("Tapete de entrada",
                                "Tapete de entrada com base antiderrapante.", 32.90, 20),
                        new ProdutoExemplo("Escorredor de louça",
                                "Escorredor compacto com bandeja coletora removível.", 74.90, 42),
                        new ProdutoExemplo("Porta-temperos",
                                "Conjunto de seis frascos com suporte para temperos.", 49.90, 8)),

                // Vendedor 1000003; produtos 21 a 30.
                new LojaExemplo("Página Aberta", "Livros",
                        new ProdutoExemplo("Introdução à programação",
                                "Livro fictício com exemplos de algoritmos para iniciantes.", 69.90, 16),
                        new ProdutoExemplo("Java para começar",
                                "Livro fictício sobre classes, objetos e métodos em Java.", 79.90, 24),
                        new ProdutoExemplo("Estruturas de dados",
                                "Livro fictício sobre listas, pilhas, filas e árvores.", 89.90, 12),
                        new ProdutoExemplo("Matemática em exemplos",
                                "Livro fictício com exercícios de matemática comentados.", 54.90, 34),
                        new ProdutoExemplo("Histórias de ciência",
                                "Livro fictício de divulgação científica para jovens.", 39.90, 19),
                        new ProdutoExemplo("Caderno de viagens",
                                "Livro fictício de crônicas sobre cidades e culturas.", 34.90, 14),
                        new ProdutoExemplo("Receitas do dia a dia",
                                "Livro fictício com receitas simples para a semana.", 44.90, 29),
                        new ProdutoExemplo("Pequenos contos",
                                "Coletânea fictícia de contos curtos para leitura diária.", 29.90, 22),
                        new ProdutoExemplo("Guia de astronomia",
                                "Livro fictício sobre estrelas, planetas e observação do céu.", 59.90, 44),
                        new ProdutoExemplo("Organização dos estudos",
                                "Livro fictício com sugestões de planejamento de estudos.", 32.90, 10)),

                // Vendedor 1000004; produtos 31 a 40.
                new LojaExemplo("Movimento Livre", "Esportes",
                        new ProdutoExemplo("Corda de pular",
                                "Corda ajustável com pegadores leves para exercícios.", 24.90, 18),
                        new ProdutoExemplo("Tapete de exercícios",
                                "Tapete de espuma para alongamentos e exercícios no solo.", 69.90, 26),
                        new ProdutoExemplo("Faixa elástica leve",
                                "Faixa elástica de resistência leve para exercícios.", 19.90, 14),
                        new ProdutoExemplo("Garrafa esportiva",
                                "Garrafa de 700 ml com bico e alça para transporte.", 29.90, 36),
                        new ProdutoExemplo("Bola de futebol",
                                "Bola de futebol tamanho cinco para uso recreativo.", 79.90, 21),
                        new ProdutoExemplo("Bola de vôlei",
                                "Bola de vôlei para treinos recreativos em quadra.", 74.90, 16),
                        new ProdutoExemplo("Par de halteres",
                                "Par de halteres revestidos de dois quilos cada.", 89.90, 31),
                        new ProdutoExemplo("Bolsa esportiva",
                                "Bolsa com compartimento principal e bolso lateral.", 99.90, 24),
                        new ProdutoExemplo("Toalha de treino",
                                "Toalha compacta de microfibra para atividades físicas.", 22.90, 46),
                        new ProdutoExemplo("Kit de cones",
                                "Conjunto de dez cones baixos para organizar treinos.", 34.90, 12)),

                // Vendedor 1000005; produtos 41 a 50.
                new LojaExemplo("Estilo Cotidiano", "Roupas e acessórios",
                        new ProdutoExemplo("Camiseta básica",
                                "Camiseta unissex de algodão, tamanho M, na cor azul.", 39.90, 20),
                        new ProdutoExemplo("Moletom com capuz",
                                "Moletom unissex com capuz, tamanho G, na cor cinza.", 119.90, 28),
                        new ProdutoExemplo("Boné ajustável",
                                "Boné de tecido com fecho ajustável e aba curva.", 34.90, 16),
                        new ProdutoExemplo("Par de meias",
                                "Par de meias de algodão, tamanho adulto, na cor branca.", 12.90, 38),
                        new ProdutoExemplo("Cinto de tecido",
                                "Cinto de tecido com fivela metálica ajustável.", 29.90, 23),
                        new ProdutoExemplo("Ecobag",
                                "Bolsa reutilizável de algodão com alças reforçadas.", 24.90, 18),
                        new ProdutoExemplo("Camiseta estampada",
                                "Camiseta de algodão com estampa geométrica, tamanho G.", 49.90, 33),
                        new ProdutoExemplo("Bermuda casual",
                                "Bermuda de sarja com bolsos, tamanho 42, na cor bege.", 79.90, 26),
                        new ProdutoExemplo("Carteira compacta",
                                "Carteira com espaços para cartões e documentos.", 44.90, 48),
                        new ProdutoExemplo("Cachecol de tricô",
                                "Cachecol de tricô macio para uso em dias frios.", 54.90, 14)),

                // Vendedor 1000006; produtos 51 a 60.
                new LojaExemplo("Ponto do Estudante", "Papelaria",
                        new ProdutoExemplo("Caderno universitário",
                                "Caderno espiral com 200 folhas pautadas e capa dura.", 29.90, 22),
                        new ProdutoExemplo("Kit de canetas",
                                "Conjunto de quatro canetas esferográficas azuis.", 9.90, 30),
                        new ProdutoExemplo("Lápis grafite",
                                "Caixa com doze lápis grafite HB para escrita e desenho.", 14.90, 18),
                        new ProdutoExemplo("Borracha branca",
                                "Conjunto de duas borrachas brancas macias.", 4.90, 40),
                        new ProdutoExemplo("Marca-texto",
                                "Conjunto de quatro marca-textos em cores variadas.", 12.90, 25),
                        new ProdutoExemplo("Bloco adesivo",
                                "Bloco com cem notas adesivas amarelas.", 7.90, 20),
                        new ProdutoExemplo("Estojo com zíper",
                                "Estojo de tecido com zíper e um compartimento.", 19.90, 35),
                        new ProdutoExemplo("Régua de trinta cm",
                                "Régua transparente de trinta centímetros.", 5.90, 28),
                        new ProdutoExemplo("Pasta sanfonada",
                                "Pasta tamanho A4 com doze divisórias e elástico.", 24.90, 50),
                        new ProdutoExemplo("Agenda de estudos",
                                "Agenda não datada com planejamento semanal.", 34.90, 16)),

                // Vendedor 1000007; produtos 61 a 70.
                new LojaExemplo("Amigo Pet", "Acessórios para animais",
                        new ProdutoExemplo("Comedouro pequeno",
                                "Comedouro de aço com base de borracha para animais.", 24.90, 24),
                        new ProdutoExemplo("Bebedouro portátil",
                                "Garrafa com recipiente acoplado para passeios com cães.", 34.90, 32),
                        new ProdutoExemplo("Coleira ajustável",
                                "Coleira de nylon com regulagem para cães de porte médio.", 19.90, 20),
                        new ProdutoExemplo("Guia de passeio",
                                "Guia de nylon de um metro com mosquetão metálico.", 29.90, 42),
                        new ProdutoExemplo("Brinquedo de corda",
                                "Brinquedo de corda trançada com dois nós para cães.", 16.90, 27),
                        new ProdutoExemplo("Bolinha para pet",
                                "Bolinha de borracha para brincadeiras supervisionadas.", 12.90, 22),
                        new ProdutoExemplo("Escova para pelos",
                                "Escova manual com cabo para escovação de cães e gatos.", 22.90, 37),
                        new ProdutoExemplo("Cama pequena",
                                "Cama acolchoada com capa removível para animais pequenos.", 89.90, 30),
                        new ProdutoExemplo("Arranhador compacto",
                                "Arranhador de papelão com base para gatos.", 39.90, 52),
                        new ProdutoExemplo("Porta-sacos de passeio",
                                "Suporte com um rolo de sacos para passeios.", 14.90, 18)),

                // Vendedor 1000008; produtos 71 a 80.
                new LojaExemplo("Verde em Casa", "Jardinagem",
                        new ProdutoExemplo("Vaso de cerâmica",
                                "Vaso de cerâmica de quinze centímetros com furo de drenagem.", 29.90, 26),
                        new ProdutoExemplo("Regador pequeno",
                                "Regador plástico de um litro com alça lateral.", 19.90, 34),
                        new ProdutoExemplo("Pá de jardinagem",
                                "Pá manual pequena para vasos e canteiros.", 14.90, 22),
                        new ProdutoExemplo("Luvas de jardinagem",
                                "Par de luvas de tecido com palma revestida.", 17.90, 44),
                        new ProdutoExemplo("Tesoura de poda",
                                "Tesoura manual para pequenos galhos e manutenção de plantas.", 39.90, 29),
                        new ProdutoExemplo("Borrifador",
                                "Borrifador plástico de 500 ml com gatilho ajustável.", 12.90, 24),
                        new ProdutoExemplo("Suporte para vasos",
                                "Suporte metálico para um vaso pequeno.", 34.90, 39),
                        new ProdutoExemplo("Kit de etiquetas",
                                "Conjunto de vinte etiquetas para identificação de plantas.", 9.90, 32),
                        new ProdutoExemplo("Prato para vaso",
                                "Prato plástico de vinte centímetros para apoio de vasos.", 7.90, 54),
                        new ProdutoExemplo("Kit de ferramentas",
                                "Conjunto com pá, rastelo e transplantador para jardinagem.", 49.90, 20)),

                // Vendedor 1000009; produtos 81 a 90.
                new LojaExemplo("Oficina Prática", "Ferramentas",
                        new ProdutoExemplo("Martelo de unha",
                                "Martelo com cabeça de aço e cabo emborrachado.", 34.90, 28),
                        new ProdutoExemplo("Chave de fenda",
                                "Chave de fenda de ponta reta com cabo plástico.", 12.90, 36),
                        new ProdutoExemplo("Chave Phillips",
                                "Chave Phillips média com cabo anatômico.", 12.90, 24),
                        new ProdutoExemplo("Trena de cinco metros",
                                "Trena retrátil com trava e presilha para cinto.", 24.90, 46),
                        new ProdutoExemplo("Alicate universal",
                                "Alicate universal de oito polegadas para tarefas gerais.", 39.90, 31),
                        new ProdutoExemplo("Jogo de chaves Allen",
                                "Conjunto de nove chaves Allen em suporte plástico.", 29.90, 26),
                        new ProdutoExemplo("Estilete retrátil",
                                "Estilete com trava e lâmina retrátil para cortes leves.", 9.90, 41),
                        new ProdutoExemplo("Nível de bolha",
                                "Nível de bolha de trinta centímetros para alinhamento.", 22.90, 34),
                        new ProdutoExemplo("Caixa de ferramentas",
                                "Caixa plástica com bandeja interna e fecho frontal.", 59.90, 56),
                        new ProdutoExemplo("Kit de parafusos",
                                "Estojo com parafusos e buchas em tamanhos variados.", 19.90, 22)),

                // Vendedor 1000010; produtos 91 a 100.
                new LojaExemplo("Cuidado Diário", "Beleza e cuidados",
                        new ProdutoExemplo("Necessaire",
                                "Bolsa organizadora com zíper para itens de cuidado pessoal.", 29.90, 30),
                        new ProdutoExemplo("Espelho de mesa",
                                "Espelho de mesa com suporte dobrável.", 24.90, 38),
                        new ProdutoExemplo("Escova de cabelo",
                                "Escova de cabelo com cabo e cerdas flexíveis.", 19.90, 26),
                        new ProdutoExemplo("Pente largo",
                                "Pente de dentes largos com pontas arredondadas.", 8.90, 48),
                        new ProdutoExemplo("Toalha de rosto",
                                "Toalha de rosto de algodão na cor branca.", 17.90, 33),
                        new ProdutoExemplo("Kit de pincéis",
                                "Conjunto de cinco pincéis para maquiagem com estojo.", 39.90, 28),
                        new ProdutoExemplo("Faixa para cabelo",
                                "Faixa elástica de tecido para prender o cabelo.", 12.90, 43),
                        new ProdutoExemplo("Organizador de bancada",
                                "Organizador de acrílico com três compartimentos.", 34.90, 36),
                        new ProdutoExemplo("Saboneteira",
                                "Saboneteira com tampa para transporte em viagens.", 9.90, 58),
                        new ProdutoExemplo("Kit de frascos de viagem",
                                "Conjunto de quatro frascos pequenos com tampas.", 22.90, 24))
        };
    }
}