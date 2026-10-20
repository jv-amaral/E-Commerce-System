import java.util.Scanner;
import java.util.NoSuchElementException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

class FuncionalidadeInvalidaException extends Exception {
    public FuncionalidadeInvalidaException(String mensagem) {
        super(mensagem);
    }
}

public class User {
    // Strings com no máximo 60 bytes
    protected int key;
    protected String password;
    protected int[] cartProductsRRN;

    // Construtores:
    public User(int key, String password) {
        // utilizado para criar um usuário novo no sistema
        setKey(key);
        setPassword(password);
        cartProductsRRN = new int[] { -1, -1, -1, -1, -1 };
    }

    // Setters e Getters:
    public void setKey(int key) {
        MarketPlace.validarChave(key);
        this.key = key;
    }

    public void setPassword(String password) {
        MarketPlace.validarTexto(password, MarketPlace.TAMANHO_STRING, "Senha");
        this.password = password;
    }

    public void setCartProductsRRN(int RRN, int pos) {
        if (pos < 0 || pos >= cartProductsRRN.length || RRN < -1) {
            throw new IllegalArgumentException("Posição ou RRN do carrinho inválido.");
        }
        this.cartProductsRRN[pos] = RRN;
    }

    public int getKey() {
        return key;
    }

    public String getPassword() {
        return password;
    }

    // Demais métodos:
    protected void exibirCatalogo() {
        try {
            boolean encontrou = false;
            for (Produto produto : ArquivoProduto.listarTodos()) {
                if (produto.getQuantidade() > 0) {
                    MarketPlace.exibirProduto(produto.getNome(), produto.getPreco(),
                            produto.getQuantidade(), produto.getIdProduto(), produto.getDescricao());
                    encontrou = true;
                }
            }
            if (!encontrou) {
                System.out.println("Nenhum produto disponível no catálogo.");
            }
        } catch (IOException e) {
            System.out.println("Não foi possível consultar o catálogo: " + e.getMessage());
        }

        System.out.print("\nUse addcart <id_Produto> para adicionar um produto ao carrinho\n[1] Voltar\n");
        Scanner input = MarketPlace.getInput();
        while (input.hasNextLine()) {
            String comando = input.nextLine().trim();
            if (comando.equals("1")) {
                return;
            }
            try {
                adicionarAoCarrinho(lerIdComando(comando, "addcart"));
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    protected void exibirLojas() {
        boolean encontrou = false;
        if (new File("Stores.bin").exists()) {
            try (RandomAccessFile raf = new RandomAccessFile("Stores.bin", "r")) {
                MarketPlace.conferirRegistros(raf, MarketPlace.TAM_REG_LOJA, "Stores.bin");
                while (raf.getFilePointer() < raf.length()) {
                    int sellerid = raf.readInt();
                    String name = MarketPlace.lerString(raf);
                    String category = MarketPlace.lerString(raf);
                    System.out.printf("%nLoja: %s%nCategoria: %s%nID Vendedor: %d%n",
                            name, category, sellerid);
                    encontrou = true;
                }
            } catch (IOException e) {
                System.out.println("Não foi possível consultar as lojas: " + e.getMessage());
            }
        }
        if (!encontrou) {
            System.out.println("Nenhuma loja cadastrada.");
        }

        System.out.print("\nUse openstore <id_Vendedor> para abrir uma loja\n[1] Voltar\n");
        Scanner input = MarketPlace.getInput();
        while (input.hasNextLine()) {
            String comando = input.nextLine().trim();
            if (comando.equals("1")) {
                return;
            }
            try {
                int vendedor = lerIdComando(comando, "openstore");
                String[] loja = MarketPlace.readStoreNameAndCategory(vendedor);
                System.out.printf("%nLoja: %s%nCategoria: %s%n", loja[0], loja[1]);
                MarketPlace.exibirLoja(vendedor);
            } catch (IllegalArgumentException | DadosNaoEncontradosException e) {
                System.out.println(e.getMessage());
            } catch (IOException e) {
                System.out.println("Não foi possível abrir a loja: " + e.getMessage());
            }
        }
    }

    protected void exibirCarrinho() {
        boolean encontrou = false;
        if (new File("Products.bin").exists()) {
            try (RandomAccessFile raf = new RandomAccessFile("Products.bin", "r")) {
                MarketPlace.conferirProdutos(raf);
                for (int rrn : cartProductsRRN) {
                    if (rrn == -1) {
                        continue;
                    }
                    long pos = MarketPlace.TAM_CAB_PRODUTO + (long) rrn * MarketPlace.TAM_REG_PRODUTO;
                    if (pos + MarketPlace.TAM_REG_PRODUTO > raf.length()) {
                        System.out.println("Um item do carrinho não está mais disponível.");
                        continue;
                    }
                    raf.seek(pos);
                    byte removido = raf.readByte();
                    raf.skipBytes(Integer.BYTES);
                    int id = raf.readInt();
                    if (removido == 1) {
                        System.out.println("Produto ID " + id + " foi removido pelo vendedor.");
                        continue;
                    }
                    if (removido != 0) {
                        throw new IOException("Flag de produto inválida.");
                    }
                    double preco = raf.readDouble();
                    int estoque = raf.readInt();
                    String nome = MarketPlace.lerString(raf);
                    String descricao = MarketPlace.lerString(raf, 100);
                    MarketPlace.exibirProduto(nome, preco, estoque, id, descricao);
                    encontrou = true;
                }
            } catch (IOException e) {
                System.out.println("Não foi possível consultar o carrinho: " + e.getMessage());
            }
        }
        if (!encontrou) {
            System.out.println("Nenhum produto ativo para exibir no carrinho.");
        }

        System.out.print("\nUse cartrem <id_Produto> para remover um produto do carrinho\n[1] Voltar\n");
        Scanner input = MarketPlace.getInput();
        while (input.hasNextLine()) {
            String comando = input.nextLine().trim();
            if (comando.equals("1")) {
                return;
            }
            try {
                removerDoCarrinho(lerIdComando(comando, "cartrem"));
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    public boolean adicionarAoCarrinho(int idProduto) {
        int posLivre = -1;
        for (int i = 0; i < cartProductsRRN.length; i++) {
            if (cartProductsRRN[i] == -1) {
                posLivre = i;
                break;
            }
        }
        if (posLivre == -1) {
            System.out.println("Seu carrinho está cheio!");
            return false;
        }
        try (RandomAccessFile raf = new RandomAccessFile("Products.bin", "r")) {
            if (idProduto <= 0) {
                throw new IllegalArgumentException("O ID do produto deve ser maior que zero.");
            }
            MarketPlace.conferirProdutos(raf);
            int rrn = 0;
            for (long pos = MarketPlace.TAM_CAB_PRODUTO; pos < raf.length();
                    pos += MarketPlace.TAM_REG_PRODUTO, rrn++) {
                raf.seek(pos);
                byte removido = raf.readByte();
                raf.skipBytes(Integer.BYTES);
                int idLido = raf.readInt();
                if (removido != 0 && removido != 1) {
                    throw new IOException("Flag de produto inválida.");
                }
                if (removido == 0 && idLido == idProduto) {
                    raf.skipBytes(Double.BYTES);
                    if (raf.readInt() <= 0) {
                        System.out.println("Produto sem estoque.");
                        return false;
                    }
                    cartProductsRRN[posLivre] = rrn;
                    try {
                        userDataSaving();
                    } catch (IOException e) {
                        cartProductsRRN[posLivre] = -1;
                        throw e;
                    }
                    System.out.printf("Produto ID %d adicionado ao carrinho!%n", idProduto);
                    return true;
                }
            }
            System.out.printf("Produto com ID %d não foi encontrado.%n", idProduto);
        } catch (IOException | IllegalArgumentException e) {
            System.out.println("Não foi possível adicionar ao carrinho: " + e.getMessage());
        }
        return false;
    }

    public boolean removerDoCarrinho(int idProduto) {
        boolean vazio = true;
        for (int rrn : cartProductsRRN) {
            if (rrn != -1) {
                vazio = false;
                break;
            }
        }
        if (vazio) {
            System.out.println("Seu carrinho está vazio!");
            return false;
        }
        try (RandomAccessFile raf = new RandomAccessFile("Products.bin", "r")) {
            if (idProduto <= 0) {
                throw new IllegalArgumentException("O ID do produto deve ser maior que zero.");
            }
            MarketPlace.conferirProdutos(raf);
            for (int i = 0; i < cartProductsRRN.length; i++) {
                int rrn = cartProductsRRN[i];
                if (rrn == -1) {
                    continue;
                }
                long posRegistro = MarketPlace.TAM_CAB_PRODUTO + (long) rrn * MarketPlace.TAM_REG_PRODUTO;
                if (posRegistro + MarketPlace.TAM_REG_PRODUTO > raf.length()) {
                    continue;
                }
                raf.seek(posRegistro + 1 + Integer.BYTES);
                int idLido = raf.readInt();
                if (idLido == idProduto) {
                    // A flag de remoção pertence ao catálogo e não deve mudar ao retirar do carrinho.
                    // 2. Coloca o RRN referente ao removido na lista cartProductsRRN como -1
                    cartProductsRRN[i] = -1;
                    try {
                        userDataSaving();
                    } catch (IOException e) {
                        cartProductsRRN[i] = rrn;
                        throw e;
                    }
                    System.out.printf("Produto ID %d removido do carrinho com sucesso!%n", idProduto);
                    return true;
                }
            }
            System.out.printf("Produto com ID %d não foi encontrado no carrinho.%n", idProduto);
        } catch (IOException | IllegalArgumentException e) {
            System.out.println("Não foi possível remover do carrinho: " + e.getMessage());
        }
        return false;
    }

    public void painelInterativo() {
        Scanner input = MarketPlace.getInput();
        while (true) {
            System.out.print("MARKETPLACE SYSTEM\n\n[1] Catalogo de Produtos\n[2] Lojas\n[3] Meu Carrinho\n[4] Sair\n");
            if (!input.hasNextLine()) {
                return;
            }
            try {
                int func = Integer.parseInt(input.nextLine().trim());
                switch (func) {
                    case 1: exibirCatalogo(); break;
                    case 2: exibirLojas(); break;
                    case 3: exibirCarrinho(); break;
                    case 4: userDataSaving(); return;
                    default: System.out.println("Funcionalidade inválida!");
                }
            } catch (NumberFormatException e) {
                System.out.println("Digite uma opção numérica válida.");
            } catch (IOException e) {
                System.out.println("Não foi possível salvar seus dados: " + e.getMessage());
            }
        }
    }

    public void newUserDataSaving() throws IOException {
        validarDadosUsuario();
        try (RandomAccessFile raf = new RandomAccessFile("Users.bin", "rw")) {
            conferirNovoUsuario(raf);
            long tamanhoAnterior = raf.length();
            try {
                raf.seek(tamanhoAnterior);
                escreverUsuario(raf, '0');
            } catch (IOException | RuntimeException e) {
                MarketPlace.restaurarTamanho(raf, tamanhoAnterior, e);
                throw e;
            }
        }
    }

    public void userDataSaving() throws IOException {
        validarDadosUsuario();
        if (!new File("Users.bin").exists()) {
            throw new IOException("Usuário não cadastrado em Users.bin.");
        }
        try (RandomAccessFile raf = new RandomAccessFile("Users.bin", "rw")) {
            MarketPlace.conferirRegistros(raf, MarketPlace.TAM_REG_USUARIO, "Users.bin");
            while (raf.getFilePointer() < raf.length()) {
                int keyRead = raf.readInt();
                if (keyRead == key) {
                    raf.skipBytes(MarketPlace.TAMANHO_STRING + Character.BYTES);
                    for (int rrn : cartProductsRRN) {
                        raf.writeInt(rrn);
                    }
                    return;
                }
                raf.skipBytes(MarketPlace.TAM_REG_USUARIO - Integer.BYTES);
            }
        }
        throw new IOException("Usuário não cadastrado em Users.bin.");
    }

    protected void validarDadosUsuario() {
        MarketPlace.validarChave(key);
        MarketPlace.validarTexto(password, MarketPlace.TAMANHO_STRING, "Senha");
        if (cartProductsRRN == null || cartProductsRRN.length != 5) {
            throw new IllegalArgumentException("O carrinho deve possuir cinco posições.");
        }
        for (int rrn : cartProductsRRN) {
            if (rrn < -1) {
                throw new IllegalArgumentException("RRN do carrinho inválido.");
            }
        }
    }

    protected void conferirNovoUsuario(RandomAccessFile raf) throws IOException {
        MarketPlace.conferirRegistros(raf, MarketPlace.TAM_REG_USUARIO, "Users.bin");
        while (raf.getFilePointer() < raf.length()) {
            if (raf.readInt() == key) {
                throw new IllegalArgumentException("Já existe um usuário com este NROUSP.");
            }
            raf.skipBytes(MarketPlace.TAM_REG_USUARIO - Integer.BYTES);
        }
    }

    protected void escreverUsuario(RandomAccessFile raf, char tipo) throws IOException {
        raf.writeInt(key);
        MarketPlace.escreverString(raf, password);
        raf.writeChar(tipo);
        for (int rrn : cartProductsRRN) {
            raf.writeInt(rrn);
        }
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

class UserExtended extends User {
    // Strings com no máximo 60 bytes
    private String storeName;
    private String storeCategory;

    public UserExtended(int key, String password, String storeName, String storeCategory) {
        super(key, password);
        setStoreName(storeName);
        setStoreCategory(storeCategory);
    }

    public UserExtended(int key, String password) {
        this(key, password, "NILL", "NILL");
    }

    // Getters e Setters:
    public String getStoreName() {
        return storeName;
    }

    public String getStoreCategory() {
        return storeCategory;
    }

    public void setStoreName(String storeName) {
        MarketPlace.validarTexto(storeName, MarketPlace.TAMANHO_STRING, "Nome da loja");
        this.storeName = storeName;
    }

    public void setStoreCategory(String storeCategory) {
        MarketPlace.validarTexto(storeCategory, MarketPlace.TAMANHO_STRING, "Categoria da loja");
        this.storeCategory = storeCategory;
    }

    // Demais Métodos:
    private void exibirMinhaLoja() {
        try {
            String[] loja = MarketPlace.readStoreNameAndCategory(this.key);
            setStoreName(loja[0]);
            setStoreCategory(loja[1]);
        } catch (IOException | DadosNaoEncontradosException | IllegalArgumentException e) {
            System.out.println("Não foi possível consultar sua loja: " + e.getMessage());
            return;
        }
        System.out.printf("Nome: %s%nCategoria: %s%n", storeName, storeCategory);
        MarketPlace.exibirLoja(this.key);
        System.out.print("\nUse addproduct para adicionar um produto\n"
                + "Use remproduct para remover um produto\n"
                + "Use editproduct para editar um produto\n[1] Voltar\n");
        Scanner input = MarketPlace.getInput();

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
                    Produto cadastrado = ArquivoProduto.cadastrar(nome, descricao, preco, estoque, this.key);
                    System.out.println("Produto cadastrado! ID: " + cadastrado.getIdProduto());
                } else if (comando.equals("editproduct")) {
                    System.out.print("ID do produto: ");
                    int idProduto = Integer.parseInt(input.nextLine().trim());
                    Produto produto = ArquivoProduto.buscarPorId(idProduto);
                    if (produto.getIdUsuario() != this.key) {
                        throw new IllegalArgumentException("Você só pode editar seus próprios produtos.");
                    }
                    System.out.print("\nO que deseja alterar?\n[1] Nome\n[2] Descrição\n"
                            + "[3] Preço\n[4] Definir estoque total\nOpção: ");
                    int opcao = Integer.parseInt(input.nextLine().trim());
                    switch (opcao) {
                        case 1:
                            System.out.print("Novo nome: ");
                            produto.setNome(input.nextLine());
                            break;
                        case 2:
                            System.out.print("Nova descrição: ");
                            produto.setDescricao(input.nextLine());
                            break;
                        case 3:
                            System.out.print("Novo preço: ");
                            produto.setPreco(Double.parseDouble(input.nextLine().trim().replace(',', '.')));
                            break;
                        case 4:
                            System.out.print("Novo total em estoque: ");
                            produto.setQuantidadeEstoque(Integer.parseInt(input.nextLine().trim()));
                            break;
                        default:
                            throw new IllegalArgumentException("Opção de edição inválida.");
                    }
                    ArquivoProduto.atualizar(produto);
                    System.out.println("Produto atualizado!");
                } else if (comando.equals("remproduct")) {
                    System.out.print("ID do produto: ");
                    int idProduto = Integer.parseInt(input.nextLine().trim());
                    Produto produto = ArquivoProduto.buscarPorId(idProduto);
                    if (produto.getIdUsuario() != this.key) {
                        throw new IllegalArgumentException("Você só pode remover seus próprios produtos.");
                    }
                    try (RandomAccessFile arquivo = new RandomAccessFile("Products.bin", "rw")) {
                        ArquivoProduto.remover_registro(arquivo, idProduto);
                    }
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

    @Override
    public void painelInterativo() {
        Scanner input = MarketPlace.getInput();
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
                    case 5: userDataSaving(); return;
                    default: System.out.println("Funcionalidade inválida!");
                }
            } catch (NumberFormatException e) {
                System.out.println("Digite uma opção numérica válida.");
            } catch (IOException e) {
                System.out.println("Não foi possível salvar seus dados: " + e.getMessage());
            }
        }
    }

    @Override
    public void newUserDataSaving() throws IOException {
        validarDadosUsuario();
        MarketPlace.validarTexto(storeName, MarketPlace.TAMANHO_STRING, "Nome da loja");
        MarketPlace.validarTexto(storeCategory, MarketPlace.TAMANHO_STRING, "Categoria da loja");
        try (RandomAccessFile users = new RandomAccessFile("Users.bin", "rw");
                RandomAccessFile stores = new RandomAccessFile("Stores.bin", "rw")) {
            conferirNovoUsuario(users);
            MarketPlace.conferirRegistros(stores, MarketPlace.TAM_REG_LOJA, "Stores.bin");
            while (stores.getFilePointer() < stores.length()) {
                if (stores.readInt() == key) {
                    throw new IllegalArgumentException("Já existe uma loja com este NROUSP.");
                }
                stores.skipBytes(2 * MarketPlace.TAMANHO_STRING);
            }
            long tamanhoUsers = users.length();
            long tamanhoStores = stores.length();
            try {
                users.seek(tamanhoUsers);
                escreverUsuario(users, '1');
                stores.seek(tamanhoStores);
                stores.writeInt(key);
                MarketPlace.escreverString(stores, storeName);
                MarketPlace.escreverString(stores, storeCategory);
            } catch (IOException | RuntimeException e) {
                MarketPlace.restaurarTamanho(users, tamanhoUsers, e);
                MarketPlace.restaurarTamanho(stores, tamanhoStores, e);
                throw e;
            }
        }
    }
}
