package view;

import controller.UserController;
import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import model.DadosNaoEncontradosException;
import model.ItemCarrinho;
import model.Loja;
import model.Produto;
import model.SenhaIncorretaException;

/**
 * View em modo texto (terminal) do sistema de marketplace.
 * Reúne todos os menus e interações com o usuário realocados de User, UserExtended e MarketPlace,
 * comunicando-se exclusivamente por meio de UserController.
 */
public class MarketPlaceView {
    private final UserController controller;
    private final Scanner input;

    public MarketPlaceView(UserController controller) {
        this(controller, new Scanner(System.in));
    }

    public MarketPlaceView(UserController controller, Scanner input) {
        this.controller = controller;
        this.input = input;
    }

    public static void main(String[] args) {
        new MarketPlaceView(new UserController()).painelLogIn();
    }

    public void painelLogIn() {
        while (true) {
            System.out.println("MARKETPLACE SYSTEM\n\n[1] Log-in\n[2] Criar conta");
            if (!input.hasNextLine()) {
                return;
            }
            try {
                int aux = Integer.parseInt(input.nextLine().trim());
                if (aux != 1 && aux != 2) {
                    System.out.println("Funcionalidade inválida!");
                    continue;
                }

                if (aux == 1) {
                    System.out.print("Digite seu NROUSP: ");
                    int key = Integer.parseInt(input.nextLine().trim());
                    System.out.println("Digite a senha:");
                    String senha = input.nextLine();
                    controller.login(key, senha);
                } else {
                    System.out.println("Selecione o tipo de conta:\n[0] Cliente\n[1] Lojista");
                    int tipoConta = Integer.parseInt(input.nextLine().trim());
                    if (tipoConta != 0 && tipoConta != 1) {
                        throw new IllegalArgumentException("Tipo de conta inválido.");
                    }
                    System.out.print("Digite seu NROUSP: ");
                    int key = Integer.parseInt(input.nextLine().trim());
                    System.out.println("Digite a senha:");
                    String senha = input.nextLine();
                    String storeName = "NILL";
                    String storeCategory = "NILL";

                    if (tipoConta == 1) {
                        System.out.println("Digite o nome da loja:");
                        storeName = input.nextLine().trim();
                        System.out.println("Digite a categoria da loja:");
                        storeCategory = input.nextLine().trim();
                    }
                    controller.criarConta(tipoConta, key, senha, storeName, storeCategory);
                    System.out.println("Conta criada com sucesso!");
                }
                painelInterativo();
                return;
            } catch (NumberFormatException e) {
                System.out.println("Digite um número inteiro válido para a opção ou o NROUSP.");
            } catch (SenhaIncorretaException | DadosNaoEncontradosException
                    | IllegalArgumentException e) {
                System.out.println(e.getMessage());
            } catch (IOException e) {
                System.out.println("Não foi possível acessar os dados: " + e.getMessage());
            } catch (NoSuchElementException e) {
                return;
            }
        }
    }

    public void painelInterativo() {
        if (controller.isUsuarioLojista()) {
            painelLojista();
        } else {
            painelCliente();
        }
    }

    private void painelCliente() {
        while (true) {
            System.out.print("MARKETPLACE SYSTEM\n\n[1] Catalogo de Produtos\n[2] Lojas\n"
                    + "[3] Meu Carrinho\n[4] Sair\n");
            if (!input.hasNextLine()) {
                return;
            }
            try {
                int func = Integer.parseInt(input.nextLine().trim());
                switch (func) {
                    case 1: exibirCatalogo(); break;
                    case 2: exibirLojas(); break;
                    case 3: exibirCarrinho(); break;
                    case 4: controller.encerrarSessao(); return;
                    default: System.out.println("Funcionalidade inválida!");
                }
            } catch (NumberFormatException e) {
                System.out.println("Digite uma opção numérica válida.");
            } catch (IOException e) {
                System.out.println("Não foi possível salvar seus dados: " + e.getMessage());
            }
        }
    }

    private void painelLojista() {
        while (true) {
            System.out.print("MARKETPLACE SYSTEM\n\n[1] Catalogo de Produtos\n[2] Lojas\n"
                    + "[3] Meu Carrinho\n[4] Minha Loja\n[5] Sair\n");
            if (!input.hasNextLine()) {
                return;
            }
            try {
                int func = Integer.parseInt(input.nextLine().trim());
                switch (func) {
                    case 1: exibirCatalogo(); break;
                    case 2: exibirLojas(); break;
                    case 3: exibirCarrinho(); break;
                    case 4: exibirMinhaLoja(); break;
                    case 5: controller.encerrarSessao(); return;
                    default: System.out.println("Funcionalidade inválida!");
                }
            } catch (NumberFormatException e) {
                System.out.println("Digite uma opção numérica válida.");
            } catch (IOException e) {
                System.out.println("Não foi possível salvar seus dados: " + e.getMessage());
            }
        }
    }

    public void exibirCatalogo() {
        try {
            List<Produto> disponiveis = controller.listarCatalogoDisponivel();
            if (disponiveis.isEmpty()) {
                System.out.println("Nenhum produto disponível no catálogo.");
            } else {
                for (Produto produto : disponiveis) {
                    exibirProduto(produto);
                }
            }
        } catch (IOException e) {
            System.out.println("Não foi possível consultar o catálogo: " + e.getMessage());
        }

        System.out.print("\nUse addcart <id_Produto> para adicionar um produto ao carrinho\n[1] Voltar\n");
        while (input.hasNextLine()) {
            String comando = input.nextLine().trim();
            if (comando.equals("1")) {
                return;
            }
            int idProduto;
            try {
                idProduto = lerIdComando(comando, "addcart");
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());
                continue;
            }
            try {
                controller.adicionarAoCarrinho(idProduto);
                System.out.printf("Produto ID %d adicionado ao carrinho!%n", idProduto);
            } catch (IOException | IllegalArgumentException e) {
                System.out.println("Não foi possível adicionar ao carrinho: " + e.getMessage());
            }
        }
    }

    public void exibirLojas() {
        boolean encontrou = false;
        try {
            for (Loja loja : controller.listarLojas()) {
                System.out.printf("%nLoja: %s%nCategoria: %s%nID Vendedor: %d%n",
                        loja.getNome(), loja.getCategoria(), loja.getVendedor());
                encontrou = true;
            }
        } catch (IOException e) {
            System.out.println("Não foi possível consultar as lojas: " + e.getMessage());
        }
        if (!encontrou) {
            System.out.println("Nenhuma loja cadastrada.");
        }

        System.out.print("\nUse openstore <id_Vendedor> para abrir uma loja\n[1] Voltar\n");
        while (input.hasNextLine()) {
            String comando = input.nextLine().trim();
            if (comando.equals("1")) {
                return;
            }
            try {
                int vendedor = lerIdComando(comando, "openstore");
                Loja loja = controller.buscarLoja(vendedor);
                System.out.printf("%nLoja: %s%nCategoria: %s%n", loja.getNome(), loja.getCategoria());
                exibirLoja(vendedor);
            } catch (IllegalArgumentException | DadosNaoEncontradosException e) {
                System.out.println(e.getMessage());
            } catch (IOException e) {
                System.out.println("Não foi possível abrir a loja: " + e.getMessage());
            }
        }
    }

    public void exibirLoja(int idVendedor) {
        try {
            List<Produto> produtos = controller.listarProdutosPorLoja(idVendedor);
            if (produtos.isEmpty()) {
                System.out.println("Esta loja ainda não possui produtos ativos.");
            } else {
                for (Produto produto : produtos) {
                    exibirProduto(produto);
                }
            }
        } catch (IOException | IllegalArgumentException e) {
            System.out.println("Não foi possível exibir a loja: " + e.getMessage());
        }
    }

    public void exibirCarrinho() {
        boolean encontrou = false;
        try {
            for (ItemCarrinho item : controller.consultarCarrinho()) {
                switch (item.getEstado()) {
                    case ATIVO:
                        exibirProduto(item.getProduto());
                        encontrou = true;
                        break;
                    case REMOVIDO_PELO_VENDEDOR:
                        System.out.println("Produto ID " + item.getIdProduto() + " foi removido pelo vendedor.");
                        break;
                    case INDISPONIVEL:
                        System.out.println("Um item do carrinho não está mais disponível.");
                        break;
                }
            }
        } catch (IOException e) {
            System.out.println("Não foi possível consultar o carrinho: " + e.getMessage());
        }
        if (!encontrou) {
            System.out.println("Nenhum produto ativo para exibir no carrinho.");
        }

        System.out.print("\nUse cartrem <id_Produto> para remover um produto do carrinho\n[1] Voltar\n");
        while (input.hasNextLine()) {
            String comando = input.nextLine().trim();
            if (comando.equals("1")) {
                return;
            }
            int idProduto;
            try {
                idProduto = lerIdComando(comando, "cartrem");
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());
                continue;
            }
            try {
                if (controller.removerDoCarrinho(idProduto)) {
                    System.out.printf("Produto ID %d removido do carrinho com sucesso!%n", idProduto);
                } else {
                    System.out.printf("Produto com ID %d não foi encontrado no carrinho.%n", idProduto);
                }
            } catch (IllegalStateException e) {
                System.out.println(e.getMessage());
            } catch (IOException | IllegalArgumentException e) {
                System.out.println("Não foi possível remover do carrinho: " + e.getMessage());
            }
        }
    }

    public void exibirMinhaLoja() {
        Loja minhaLoja;
        try {
            minhaLoja = controller.atualizarDadosMinhaLoja();
        } catch (IOException | DadosNaoEncontradosException | IllegalArgumentException e) {
            System.out.println("Não foi possível consultar sua loja: " + e.getMessage());
            return;
        }
        System.out.printf("Nome: %s%nCategoria: %s%n", minhaLoja.getNome(), minhaLoja.getCategoria());
        exibirLoja(minhaLoja.getVendedor());
        System.out.print("\nUse addproduct para adicionar um produto\n"
                + "Use remproduct para remover um produto\n"
                + "Use editproduct para editar um produto\n[1] Voltar\n");

        while (true) {
            System.out.print("\nComando: ");
            if (!input.hasNextLine()) {
                return;
            }
            String comando = input.nextLine().trim();
            if (comando.equals("1")) {
                return;
            }
            try {
                if (comando.equals("addproduct")) {
                    System.out.print("Nome do produto: ");
                    String nome = input.nextLine();
                    System.out.print("Descrição: ");
                    String descricao = input.nextLine();
                    System.out.print("Preço: ");
                    double preco = Double.parseDouble(input.nextLine().trim().replace(',', '.'));
                    System.out.print("Quantidade em estoque: ");
                    int estoque = Integer.parseInt(input.nextLine().trim());
                    Produto cadastrado = controller.cadastrarProduto(nome, descricao, preco, estoque);
                    System.out.println("Produto cadastrado! ID: " + cadastrado.getIdProduto());
                } else if (comando.equals("editproduct")) {
                    System.out.print("ID do produto: ");
                    int idProduto = Integer.parseInt(input.nextLine().trim());
                    controller.buscarProdutoDaLoja(idProduto);
                    System.out.print("\nO que deseja alterar?\n[1] Nome\n[2] Descrição\n"
                            + "[3] Preço\n[4] Definir estoque total\nOpção: ");
                    int opcao = Integer.parseInt(input.nextLine().trim());
                    switch (opcao) {
                        case 1:
                            System.out.print("Novo nome: ");
                            controller.editarNomeProduto(idProduto, input.nextLine());
                            break;
                        case 2:
                            System.out.print("Nova descrição: ");
                            controller.editarDescricaoProduto(idProduto, input.nextLine());
                            break;
                        case 3:
                            System.out.print("Novo preço: ");
                            controller.editarPrecoProduto(
                                    idProduto,
                                    Double.parseDouble(input.nextLine().trim().replace(',', '.')));
                            break;
                        case 4:
                            System.out.print("Novo total em estoque: ");
                            controller.editarEstoqueProduto(
                                    idProduto,
                                    Integer.parseInt(input.nextLine().trim()));
                            break;
                        default:
                            throw new IllegalArgumentException("Opção de edição inválida.");
                    }
                    System.out.println("Produto atualizado!");
                } else if (comando.equals("remproduct")) {
                    System.out.print("ID do produto: ");
                    int idProduto = Integer.parseInt(input.nextLine().trim());
                    controller.removerProdutoDaLoja(idProduto);
                    System.out.println("Produto removido!");
                } else {
                    System.out.println("Comando inválido.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Número inválido. Digite um valor numérico válido.");
            } catch (IllegalArgumentException | IllegalStateException e) {
                System.out.println(e.getMessage());
            } catch (IOException e) {
                System.out.println("Não foi possível acessar o arquivo: " + e.getMessage());
            } catch (NoSuchElementException e) {
                return;
            }
        }
    }

    private static void exibirProduto(Produto produto) {
        System.out.printf("%nProduto: %s%nPreço: %.2f%nEstoque: %d%nID: %d%nDescricao: %s%n",
                produto.getNome(), produto.getPreco(), produto.getQuantidade(),
                produto.getIdProduto(), produto.getDescricao());
    }

    private static int lerIdComando(String entrada, String comando) {
        String[] partes = entrada.split("\\s+");
        if (partes.length != 2 || !partes[0].equals(comando)) {
            throw new IllegalArgumentException("Use " + comando + " <ID> ou 1 para voltar.");
        }
        try {
            int id = Integer.parseInt(partes[1]);
            if (id <= 0) {
                throw new NumberFormatException();
            }
            return id;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("O ID deve ser um inteiro maior que zero.");
        }
    }
}
