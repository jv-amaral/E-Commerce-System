import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
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
    
    private static int TAMANHO_STRING = 60; 
    //private static int TAM_REG_USUARIO = 
    
    // Métodos estáticos principais: 
    public static User readUserData(User user, int key, String password) throws IOException, SenhaIncorretaException, DadosNaoEncontradosException {

        File arq = new File("Users.bin");
        int keyRead, RRNread; 
        String passwordRead; //senha a ser lida no arquivo 
        char userType; 

        try (RandomAccessFile raf = new RandomAccessFile(arq, "r")) {

            while (true) {
                try {
                    keyRead = raf.readInt(); 

                    if (keyRead == key) { 

                        passwordRead = lerString(raf); 

                        if (!passwordRead.equals(password)) {
                            throw new SenhaIncorretaException("Senha incorreta!");
                        }

                        userType = raf.readChar(); 

                        if (userType == '0') {
                            user = new User(key, password);
                        } else {
                            user = new UserExtended(key, password);
                            try {
                                String[] loja = readStoreNameAndCategory(key);
                                ((UserExtended) user).setStoreName(loja[0]);
                                ((UserExtended) user).setStoreCategory(loja[1]);
                            } catch (DadosNaoEncontradosException e) {
                                // Loja não cadastrada ainda
                            }
                        }

                        for (int i = 0; i < 5; i++) {
                            RRNread = raf.readInt(); 
                            user.setCartProductsRRN(RRNread, i);
                        }

                        return user; 
                    }
                    raf.skipBytes(82); // pula para o próximo registro

                } catch (EOFException e) {
                    break;
                }
            }
            throw new DadosNaoEncontradosException("Usuario nao cadastrado!");
        }
    }

    public static void readUserData(User user) throws IOException, SenhaIncorretaException, DadosNaoEncontradosException {
        readUserData(user, user.getKey(), user.getPassword());
    }

    public static User readUsersData(User user, int key, String password) throws IOException, SenhaIncorretaException, DadosNaoEncontradosException {
        return readUserData(user, key, password);
    }

    public static User readUsersData(int key, String password) throws IOException, SenhaIncorretaException, DadosNaoEncontradosException {
        return readUserData(null, key, password);
    }

    public static void readUsersData(User user) throws IOException, SenhaIncorretaException, DadosNaoEncontradosException {
        readUserData(user);
    }

    // Método para criar uma conta e salvar como registro em Users.bin (e Stores.bin para lojista):
    public static User criarConta(int tipoConta, int key, String password, String storeName, String storeCategory) throws IOException {
        File arq = new File("Users.bin");

        if (arq.exists()) {
            try (RandomAccessFile raf = new RandomAccessFile(arq, "r")) {
                while (true) {
                    try {
                        int keyRead = raf.readInt();
                        if (keyRead == key) {
                            System.out.println("Erro: Já existe um usuário cadastrado com este NROUSP!");
                            return null;
                        }
                        raf.skipBytes(82);
                    } catch (EOFException e) {
                        break;
                    }
                }
            }
        }

        User user;
        if (tipoConta == 0) {
            user = new User(key, password);
        } else {
            user = new UserExtended(key, password, storeName, storeCategory);
        }
        user.newUserDataSaving();

        System.out.println("Conta criada com sucesso!");
        return user;
    }

    public static User criarConta(int tipoConta, int key, String password) throws IOException {
        return criarConta(tipoConta, key, password, "NILL", "NILL");
    }

    public static String[] readStoreNameAndCategory(int key) throws IOException, DadosNaoEncontradosException {

        int keyRead; 
        String[] result = new String[2]; 

        try (RandomAccessFile raf = new RandomAccessFile("Stores.bin", "rw")) {

            while(true){
                try {
                    keyRead = raf.readInt();

                    if (keyRead == key){

                        result[0] = lerString(raf);
                        result[1] = lerString(raf); 
                        return result;

                    }   
                    raf.skipBytes(120);


                } catch (EOFException e){
                    break;
                }
            }
            throw new DadosNaoEncontradosException("Loja não cadastrada!");
        }

    }

    public static void exibirLoja(int key){

        int keyRead;
        byte removido;
        String nome = new String();
        String descricao = new String();
        double preco;
        int estoque, id;

        try (RandomAccessFile raf = new RandomAccessFile("Products.bin", "rw")) {

            raf.seek(8);
            while (true) {
                try {
                    removido = raf.readByte();
                    keyRead = raf.readInt();
                    id = raf.readInt();
                    preco = raf.readDouble();
                    estoque = raf.readInt();
                    nome = MarketPlace.lerString(raf);
                    descricao = MarketPlace.lerString(raf, 100);

                    if (removido == 1 || key != keyRead) {
                        continue;
                    }

                    System.out.printf("%nProduto: %s%nPreço: %.2f%nEstoque: %d%nID: %d%nDescricao: %s%n", nome, preco,
                            estoque, id, descricao);

                } catch (EOFException e) {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void painelLogIn(){

        Scanner input = new Scanner(System.in);
        System.out.println("MARKETPLACE SYSTEM\n\n[1] Log-in\n[2] Sing-in");
        User user = null; 

        int aux = input.nextInt(); 
        if (aux < 1 || aux > 2){
            painelLogIn();
            return; 
        }

        if (aux == 1){
            String senha = new String(); 
            int key; 
            System.out.print("Digite seu NROUSP");
            key = input.nextInt(); 
            System.out.println("Digite a senha");
            senha = input.next(); 

            try {
                user = readUserData(user, key, senha); 
            } catch (IOException e){
                e.printStackTrace(); 
            } catch (DadosNaoEncontradosException e){
                System.out.println("Dados não encontrados!"); 
                painelLogIn();
                return;
            } catch (SenhaIncorretaException e){
                System.out.println("Senha incorreta!");
                painelLogIn();
                return; 
            }
        } else {
            System.out.println("Selecione o tipo de conta:\n[0] Cliente\n[1] Lojista");
            int tipoConta = input.nextInt();
            System.out.print("Digite seu NROUSP: ");
            int key = input.nextInt();
            System.out.println("Digite a senha: ");
            String senha = input.next();

            String storeName = "NILL";
            String storeCategory = "NILL";

            if (tipoConta == 1) {
                input.nextLine(); // Limpa o buffer residual do teclado após input.next()

                System.out.println("Digite o nome da loja: ");
                storeName = input.nextLine().trim();

                System.out.println("Digite a categoria da loja: ");
                storeCategory = input.nextLine().trim();

                byte[] bytesNome = storeName.getBytes(StandardCharsets.UTF_8);
                byte[] bytesCat = storeCategory.getBytes(StandardCharsets.UTF_8);

                if (storeName.isBlank() || storeCategory.isBlank() || bytesNome.length > TAMANHO_STRING || bytesCat.length > TAMANHO_STRING) {
                    System.err.println("Nome ou categoria inválidos!");
                    painelLogIn();
                    return;
                }
            }

            try {
                user = criarConta(tipoConta, key, senha, storeName, storeCategory);
                if (user == null) {
                    painelLogIn();
                    return;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (user != null) user.painelInterativo();
    }

    // Métos estáticos auxiliares:
    public static void escreverString(RandomAccessFile raf, String texto) throws IOException {
        if (texto == null) {
            texto = "";
        }

        byte[] bytesOriginal = texto.getBytes(StandardCharsets.UTF_8);
        if (bytesOriginal.length > TAMANHO_STRING) {
            throw new IllegalArgumentException("O texto excede o limite máximo de " + TAMANHO_STRING + " bytes!");
        }

        byte[] bufferFixo = new byte[TAMANHO_STRING];
        System.arraycopy(bytesOriginal, 0, bufferFixo, 0, bytesOriginal.length);

        raf.write(bufferFixo);
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

    public static void escreverString(RandomAccessFile raf, String texto, int tam_bytes) throws IOException {
        if (texto == null) {
            texto = "";
        }

        byte[] bytesOriginal = texto.getBytes(StandardCharsets.UTF_8);
        if (bytesOriginal.length > tam_bytes) {
            throw new IllegalArgumentException("O texto excede o limite máximo de " + tam_bytes + " bytes!");
        }

        byte[] bufferFixo = new byte[tam_bytes];
        System.arraycopy(bytesOriginal, 0, bufferFixo, 0, bytesOriginal.length);

        raf.write(bufferFixo);
    }

    public static String lerString(RandomAccessFile raf, int tam_bytes) throws IOException {
        byte[] buffer = new byte[tam_bytes];
        raf.readFully(buffer); // Garante a leitura de exatamente TAMANHO_STRING bytes
        
        // Determina o tamanho real da string até o primeiro byte nulo de preenchimento
        int tamanhoReal = 0;
        while (tamanhoReal < buffer.length && buffer[tamanhoReal] != 0) {
            tamanhoReal++;
        }

        // Decodifica preservando eventuais espaços válidos e sem truncamento indevido
        return new String(buffer, 0, tamanhoReal, StandardCharsets.UTF_8);
    }

    // main:
    public static void main(String[] args)
    {
        painelLogIn();
    }

}
