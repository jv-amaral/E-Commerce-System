import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.Scanner;

class SenhaIncorretaException extends Exception {
    public SenhaIncorretaException(String mensagem) {
        super(mensagem);
    }
}

class DadosNaoEncontradosException extends Exception {
    public DadosNaoEncontradosException(String mensagem) {
        super(mensagem);
    }
}

public class MarketPlace {
    static final int TAMANHO_STRING = 60;
    // private static int TAM_REG_USUARIO =
    static final int TAM_REG_USUARIO = Integer.BYTES + TAMANHO_STRING
            + Character.BYTES + 5 * Integer.BYTES;
    static final int TAM_REG_LOJA = Integer.BYTES + 2 * TAMANHO_STRING;
    static final int TAM_CAB_PRODUTO = 8;
    static final int TAM_REG_PRODUTO = 181;
    private static final Scanner INPUT = new Scanner(System.in);

    // Métodos estáticos principais:
    public static User readUserData(User user, int key, String password)
            throws IOException, SenhaIncorretaException, DadosNaoEncontradosException {
        validarChave(key);
        validarTexto(password, TAMANHO_STRING, "Senha");
        File arq = new File("Users.bin");
        if (!arq.exists()) {
            throw new DadosNaoEncontradosException("Usuário não cadastrado!");
        }

        int keyRead, RRNread;
        String passwordRead; // senha a ser lida no arquivo
        char userType;
        try (RandomAccessFile raf = new RandomAccessFile(arq, "r")) {
            conferirRegistros(raf, TAM_REG_USUARIO, "Users.bin");
            while (raf.getFilePointer() < raf.length()) {
                keyRead = raf.readInt();
                if (keyRead == key) {
                    passwordRead = lerString(raf);
                    if (!passwordRead.equals(password)) {
                        throw new SenhaIncorretaException("Senha incorreta!");
                    }

                    userType = raf.readChar();
                    try {
                        if (userType == '0') {
                            user = new User(key, password);
                        } else if (userType == '1') {
                            user = new UserExtended(key, password);
                            try {
                                String[] loja = readStoreNameAndCategory(key);
                                ((UserExtended) user).setStoreName(loja[0]);
                                ((UserExtended) user).setStoreCategory(loja[1]);
                            } catch (DadosNaoEncontradosException e) {
                                // Loja não cadastrada ainda
                            }
                        } else {
                            throw new IOException("Tipo de usuário inválido em Users.bin.");
                        }

                        for (int i = 0; i < 5; i++) {
                            RRNread = raf.readInt();
                            user.setCartProductsRRN(RRNread, i);
                        }
                    } catch (IllegalArgumentException e) {
                        throw new IOException("O cadastro contém dados inválidos.", e);
                    }
                    return user;
                }
                raf.skipBytes(TAM_REG_USUARIO - Integer.BYTES); // pula para o próximo registro
            }
        }
        throw new DadosNaoEncontradosException("Usuário não cadastrado!");
    }

    public static User readUserData(User user)
            throws IOException, SenhaIncorretaException, DadosNaoEncontradosException {
        if (user == null) {
            throw new IllegalArgumentException("O usuário não pode ser nulo.");
        }
        return readUserData(user, user.getKey(), user.getPassword());
    }

    public static User readUsersData(User user, int key, String password)
            throws IOException, SenhaIncorretaException, DadosNaoEncontradosException {
        return readUserData(user, key, password);
    }

    public static User readUsersData(int key, String password)
            throws IOException, SenhaIncorretaException, DadosNaoEncontradosException {
        return readUserData(null, key, password);
    }

    public static User readUsersData(User user)
            throws IOException, SenhaIncorretaException, DadosNaoEncontradosException {
        return readUserData(user);
    }

    // Método para criar uma conta e salvar como registro em Users.bin (e Stores.bin
    // para lojista):
    public static User criarConta(int tipoConta, int key, String password,
            String storeName, String storeCategory) throws IOException {
        if (tipoConta != 0 && tipoConta != 1) {
            throw new IllegalArgumentException("Selecione 0 para cliente ou 1 para lojista.");
        }
        validarChave(key);
        validarTexto(password, TAMANHO_STRING, "Senha");
        if (tipoConta == 1) {
            validarTexto(storeName, TAMANHO_STRING, "Nome da loja");
            validarTexto(storeCategory, TAMANHO_STRING, "Categoria da loja");
        }

        File arq = new File("Users.bin");
        if (arq.exists()) {
            try (RandomAccessFile raf = new RandomAccessFile(arq, "r")) {
                conferirRegistros(raf, TAM_REG_USUARIO, "Users.bin");
                while (raf.getFilePointer() < raf.length()) {
                    if (raf.readInt() == key) {
                        System.out.println("Erro: Já existe um usuário cadastrado com este NROUSP!");
                        return null;
                    }
                    raf.skipBytes(TAM_REG_USUARIO - Integer.BYTES);
                }
            }
        }

        User user = tipoConta == 0
                ? new User(key, password)
                : new UserExtended(key, password, storeName, storeCategory);
        user.newUserDataSaving();
        System.out.println("Conta criada com sucesso!");
        return user;
    }

    public static User criarConta(int tipoConta, int key, String password) throws IOException {
        return criarConta(tipoConta, key, password, "NILL", "NILL");
    }

    public static String[] readStoreNameAndCategory(int key)
            throws IOException, DadosNaoEncontradosException {
        validarChave(key);
        if (!new File("Stores.bin").exists()) {
            throw new DadosNaoEncontradosException("Loja não cadastrada!");
        }
        try (RandomAccessFile raf = new RandomAccessFile("Stores.bin", "r")) {
            conferirRegistros(raf, TAM_REG_LOJA, "Stores.bin");
            while (raf.getFilePointer() < raf.length()) {
                int keyRead = raf.readInt();
                if (keyRead == key) {
                    return new String[] { lerString(raf), lerString(raf) };
                }
                raf.skipBytes(2 * TAMANHO_STRING);
            }
        }
        throw new DadosNaoEncontradosException("Loja não cadastrada!");
    }

    public static void exibirLoja(int key) {
        try {
            validarChave(key);
            boolean encontrou = false;
            for (Produto produto : ArquivoProduto.listarPorUsuario(key)) {
                exibirProduto(produto.getNome(), produto.getPreco(), produto.getQuantidade(),
                        produto.getIdProduto(), produto.getDescricao());
                encontrou = true;
            }
            if (!encontrou) {
                System.out.println("Esta loja ainda não possui produtos ativos.");
            }
        } catch (IOException | IllegalArgumentException e) {
            System.out.println("Não foi possível exibir a loja: " + e.getMessage());
        }
    }

    public static void painelLogIn() {
        Scanner input = getInput();
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

                User user;
                if (aux == 1) {
                    System.out.print("Digite seu NROUSP: ");
                    int key = Integer.parseInt(input.nextLine().trim());
                    System.out.println("Digite a senha:");
                    String senha = input.nextLine();
                    user = readUserData(null, key, senha);
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
                        // A leitura por linha evita o buffer residual do teclado.
                        System.out.println("Digite o nome da loja:");
                        storeName = input.nextLine().trim();
                        System.out.println("Digite a categoria da loja:");
                        storeCategory = input.nextLine().trim();
                    }
                    user = criarConta(tipoConta, key, senha, storeName, storeCategory);
                }
                if (user != null) {
                    user.painelInterativo();
                    return;
                }
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

    // Métos estáticos auxiliares:
    public static void escreverString(RandomAccessFile raf, String texto) throws IOException {
        escreverString(raf, texto, TAMANHO_STRING);
    }

    public static String lerString(RandomAccessFile raf) throws IOException {
        byte[] buffer = new byte[TAMANHO_STRING];
        raf.readFully(buffer); // Garante a leitura de exatamente TAMANHO_STRING bytes
        // Determina o tamanho real da string até o primeiro byte nulo de preenchimento
        int tamanhoReal = 0;
        while (tamanhoReal < buffer.length && buffer[tamanhoReal] != 0) {
            tamanhoReal++;
        }
        // Decodifica preservando eventuais espaços válidos e sem truncamento indevido
        return new String(buffer, 0, tamanhoReal, StandardCharsets.UTF_8);
    }

    public static void escreverString(RandomAccessFile raf, String texto, int tam_bytes)
            throws IOException {
        if (tam_bytes <= 0) {
            throw new IllegalArgumentException("O tamanho do campo deve ser positivo.");
        }
        if (texto == null) {
            texto = "";
        }
        byte[] bytesOriginal = texto.getBytes(StandardCharsets.UTF_8);
        if (bytesOriginal.length > tam_bytes || texto.indexOf('\0') >= 0) {
            throw new IllegalArgumentException(
                    "O texto deve caber em " + tam_bytes + " bytes e não conter caractere nulo.");
        }
        byte[] bufferFixo = new byte[tam_bytes];
        System.arraycopy(bytesOriginal, 0, bufferFixo, 0, bytesOriginal.length);
        raf.write(bufferFixo);
    }

    public static String lerString(RandomAccessFile raf, int tam_bytes) throws IOException {
        if (tam_bytes <= 0) {
            throw new IllegalArgumentException("O tamanho do campo deve ser positivo.");
        }
        byte[] buffer = new byte[tam_bytes];
        raf.readFully(buffer); // Garante a leitura de exatamente tam_bytes bytes
        // Determina o tamanho real da string até o primeiro byte nulo de preenchimento
        int tamanhoReal = 0;
        while (tamanhoReal < buffer.length && buffer[tamanhoReal] != 0) {
            tamanhoReal++;
        }
        // Decodifica preservando eventuais espaços válidos e sem truncamento indevido
        return new String(buffer, 0, tamanhoReal, StandardCharsets.UTF_8);
    }

    public static Scanner getInput() {
        return INPUT;
    }

    static void validarChave(int key) {
        if (key <= 0) {
            throw new IllegalArgumentException("O NROUSP deve ser maior que zero.");
        }
    }

    static void validarTexto(String texto, int tamanho, String campo) {
        if (texto == null || texto.isBlank() || texto.indexOf('\0') >= 0
                || texto.getBytes(StandardCharsets.UTF_8).length > tamanho) {
            throw new IllegalArgumentException(
                    campo + " deve conter texto válido de até " + tamanho + " bytes.");
        }
    }

    static void conferirRegistros(RandomAccessFile raf, int tamanho, String nome)
            throws IOException {
        if (raf.length() % tamanho != 0) {
            throw new IOException(nome + " contém um registro incompleto.");
        }
        raf.seek(0);
    }

    static void conferirProdutos(RandomAccessFile raf) throws IOException {
        if (raf.length() < TAM_CAB_PRODUTO
                || (raf.length() - TAM_CAB_PRODUTO) % TAM_REG_PRODUTO != 0) {
            throw new IOException("Products.bin contém um cabeçalho ou registro incompleto.");
        }
    }

    static void restaurarTamanho(RandomAccessFile raf, long tamanho, Throwable erro) {
        try {
            raf.setLength(tamanho);
        } catch (IOException falha) {
            erro.addSuppressed(falha);
        }
    }

    static void exibirProduto(String nome, double preco, int estoque, int id, String descricao) {
        System.out.printf("%nProduto: %s%nPreço: %.2f%nEstoque: %d%nID: %d%nDescricao: %s%n",
                nome, preco, estoque, id, descricao);
    }

    // main:
    public static void main(String[] args) {
        painelLogIn();
    }
}
